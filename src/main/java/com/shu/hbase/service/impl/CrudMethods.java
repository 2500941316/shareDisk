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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class CrudMethods {
    private static Logger logger = LoggerFactory.getLogger(CrudMethods.class);

    //更新用户存储文件总大小
    public static boolean insertOrUpdateUser(Table userTable, String size, String uid, String type) {
        try {
            logger.info("开始查询用户的存储大小");
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
            logger.info("磁盘校验完毕");
            Put put = new Put(Bytes.toBytes(uid));
            assert newSize != null;
            put.addColumn(Bytes.toBytes(Static.USER_TABLE_CF), Bytes.toBytes(Static.USER_TABLE_SIZE), Bytes.toBytes(newSize));
            userTable.put(put);
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage());
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
                    fileInfoVO.setDir(Bytes.toString(CellUtil.cloneValue(cell)).equals("true"));
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


    //根据fileId和用户来校验权限
    public static boolean verifite(Table fileTable, String authId, String filedId, String gId) throws IOException {
        //通过fileTable查出该文件的权限信息
        //如果fileId为8位，则说明在查首页,只有本人能查到
        if ((filedId.length() == 8) || filedId.substring(0, 8).equals("00000000")) {
            return true;
        } else {

            Get get = new Get(Bytes.toBytes(filedId));
            get.setMaxVersions();
            get.addColumn(Bytes.toBytes(Static.FILE_TABLE_CF), Bytes.toBytes(Static.FILE_TABLE_Auth));
            Result result = fileTable.get(get);
            List<Cell> cells = result.listCells();
            String newAuthId = null;
            if (!gId.isEmpty()) {
                newAuthId = gId + authId;
            }
            for (Cell cell : cells) {
                if (Bytes.toString(CellUtil.cloneValue(cell)).equals(newAuthId) || Bytes.toString(CellUtil.cloneValue(cell)).equals(authId) || Bytes.toString(CellUtil.cloneValue(cell)).equals("公开")) {
                    return true;
                }
            }
        }
        return false;
    }

}
