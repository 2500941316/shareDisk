package com.shu.hbase.service.impl;

import com.shu.hbase.pojo.FileInfoVO;
import com.shu.hbase.pojo.Static;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.text.DecimalFormat;

public class CrudMethods {

    //更新用户存储文件总大小
    public static boolean insertOrUpdateUser(Table userTable, String size, String uid, String type) {
        try {
            //先获取当前用户存储的大小
            Get get = new Get(Bytes.toBytes(uid));
            get.addColumn(Bytes.toBytes(Static.USER_TABLE_CF), Bytes.toBytes(Static.USER_TABLE_SIZE));
            Result userRes = userTable.get(get);
            int KB = 1024;//定义GB的计算常量
            DecimalFormat df = new DecimalFormat("0.00");//格式化小数
            String resultSize = "";
            String newSize = null;
            resultSize = df.format(Integer.parseInt(size) / (float) KB);
            if (userRes.isEmpty()) {
                if (Double.parseDouble(resultSize) > 20 * 1024 * 1024) {
                    return false;
                }
                newSize = size;
            } else {
                String curSize = null;
                //先获得当前的存储大小
                for (Cell cell : userRes.rawCells()) {
                    if (Bytes.toString(CellUtil.cloneQualifier(cell)).equals(Static.USER_TABLE_SIZE)) {
                        curSize = Bytes.toString(CellUtil.cloneValue(cell));
                        String GSize = df.format(Double.parseDouble(curSize) / (float) KB);
                        //如果是up类型则判断是否超限
                        if (type.equals("upload")) {
                            if (Double.parseDouble(GSize) + Double.parseDouble(resultSize) > 20 * 1024 * 1024)
                                return false;
                            newSize = Double.parseDouble(size) + Double.parseDouble(curSize) + "";
                        } else {
                            newSize = Double.parseDouble(curSize) - Double.parseDouble(size) + "";
                        }
                    }
                }
            }
            Put put = new Put(Bytes.toBytes(uid));
            put.addColumn(Bytes.toBytes(Static.USER_TABLE_CF), Bytes.toBytes(Static.USER_TABLE_SIZE), Bytes.toBytes(newSize));
            userTable.put(put);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    //将查询出来的cell封装为返回list的方法
    public static FileInfoVO packageCells(Result result) {
        FileInfoVO fileInfoVO = new FileInfoVO();
        String fileId = null;
        for (Cell cell : result.rawCells()) {
            fileId = Bytes.toString(CellUtil.cloneRow(cell));
            if (fileId.length() == 0) {
                return null;
            }
            fileInfoVO.setFileId(fileId);

            fileInfoVO.setSharer(fileId.substring(0, 8));

            String re = Bytes.toString(CellUtil.cloneQualifier(cell));
            switch (re) {
                case "name":
                    fileInfoVO.setName(Bytes.toString(CellUtil.cloneValue(cell)));
                    break;
                case "path":
                    fileInfoVO.setPath(Bytes.toString(CellUtil.cloneValue(cell)));
                    break;
                case "size":
                    if (!Bytes.toString(CellUtil.cloneValue(cell)).equals("-")) {
                        fileInfoVO.setSize(Long.parseLong(Bytes.toString(CellUtil.cloneValue(cell))));
                    } else {
                        fileInfoVO.setSize(null);
                    }
                    break;
                case "dir":
                    fileInfoVO.setDir(Bytes.toString(CellUtil.cloneValue(cell)).equals("true") ? true : false);
                    break;
                case "time":
                    fileInfoVO.setTime(Long.parseLong(Bytes.toString(CellUtil.cloneValue(cell))));
                    break;
                case "back":
                    fileInfoVO.setBack(Bytes.toString(CellUtil.cloneValue(cell)));
                    break;
                case "auth":
                    break;
                default:
                    break;
            }
        }
        //设置类型
        if (!fileInfoVO.isDir()) {
            //截取最后的后缀名
            String name = fileInfoVO.getName();
            String substring = "";
            if (name.lastIndexOf(".") != -1) {
                substring = name.substring(name.lastIndexOf("."));
            }
            if (!substring.isEmpty()) {
                fileInfoVO.setType(substring);
            } else {
                fileInfoVO.setType("common");
            }
        } else {
            fileInfoVO.setType("dir");
        }
        fileInfoVO.setNew(false);
        return fileInfoVO;
    }

}