package com.shu.hbase.service.impl.upload;

import com.shu.hbase.pojo.Static;
import com.shu.hbase.service.impl.CrudMethods;
import com.shu.hbase.tools.TableModel;
import com.shu.hbase.tools.filetype.FileType;
import com.shu.hbase.tools.filetype.FileTypeJudge;
import com.shu.hbase.tools.hbasepool.HbaseConnectionPool;
import com.shu.hbase.tools.hdfspool.HdfsConnectionPool;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class MvcToHadoop {
    private static Logger logger = LoggerFactory.getLogger(MvcToHadoop.class);

    public static TableModel createFile(String mvcPath, String backId, String fileId, String uId) throws Exception {
        FileSystem fs = null;
        Connection hBaseConn = null;
        Table userTable = null;
        InputStream typeIn = null;
        InputStream in = null;
        String hdfsPath = null;
        File localPath = null;
        try {
            logger.info("开始将文件写入hdfs");
            fs = HdfsConnectionPool.getHdfsConnection();
            hBaseConn = HbaseConnectionPool.getHbaseConnection();
            userTable = hBaseConn.getTable(TableName.valueOf(Static.USER_TABLE));
            //权限验证：上传只能自己给自己上传，backId的前8位必须等于uId
            if (!backId.substring(0, 8).equals(uId)) {
                logger.error("权限不足，无法写入");
                return TableModel.error("您的权限不足");
            }
            localPath = new File(mvcPath);
            if (!CrudMethods.insertOrUpdateUser(userTable, localPath.length() + "", uId, "upload")) {
                return TableModel.error("文件超出存储容量");
            }

            in = new FileInputStream(localPath);
            typeIn = new FileInputStream(localPath);
            FileType type = FileTypeJudge.getType(typeIn);
            String fileType = FileTypeJudge.isFileType(type);
            //做二次判断，如果还是"其他",则对文件后缀再次判断
            String[] videoTypes = new String[]{"avi", "ram", "rm", "mpg", "mov", "asf", "mp4", "flv", "mid"};
            String[] audioTypes = new String[]{"wav", "mp3"};
            if (fileType.equals("other")) {
                int i = localPath.getName().lastIndexOf(".");

                for (String videoType : videoTypes) {
                    if (videoType.equals(localPath.getName().substring(i + 1))) {
                        fileType = "video";
                    } else if (audioTypes.equals(localPath.getName().substring(i + 1))) {
                        fileType = "audio";
                    }
                }
            }
            //查询hdfs的路径
            hdfsPath = CrudMethods.findUploadPath(backId);
            if (hdfsPath != null) {
                hdfsPath = hdfsPath + "/" + localPath.getName();
            }
            CrudMethods.insertToFiles(localPath, fileType, hdfsPath, backId, uId, fileId);
            FSDataOutputStream out = fs.create(new Path(hdfsPath));
            IOUtils.copyBytes(in, out, 4096, true);
            out.close();
            logger.info("文件上传成功");
            return TableModel.success("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            return TableModel.error("上传失败");
        } finally {
            localPath.delete();
            userTable.close();
            HbaseConnectionPool.releaseConnection(hBaseConn);
            HdfsConnectionPool.releaseConnection(fs);
        }
    }
}
