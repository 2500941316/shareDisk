package com.shu.hbase.service.impl.downLoad;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DownLoad {
    public static void downloadFromHDFSinOffset(FileSystem fs, HttpServletResponse response, String encryptfilename,String fileName, HttpServletRequest request) throws IOException {

        if (response == null || encryptfilename == null || encryptfilename.equals(""))
            return;

        response.setContentType("application/x-msdownload");
        response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
        ServletOutputStream sos = response.getOutputStream();

        DownloadInOffset dfb = null;
        try {
            long offSet = 0;
            dfb = new DownloadInOffset(fs, encryptfilename);
            byte[] buffer = new byte[1024];
            long size = dfb.getFileSize(fs, encryptfilename);// 文件总大小
            response.setHeader("Content-Length", size + "");
            int len = 0;// 每次读取字节长度
            long length = 0;// 已读取总长度
            len = dfb.download(buffer);// 将指针指向文件起始处
            do {
                // 开始循环，往buffer中写入输出流
                sos.write(buffer, 0, len);
                length += len;
            } while ((len = dfb.download(buffer)) != -1 && length + offSet <= size);
            sos.flush();
        } catch (Exception ignored) {
        } finally {
            assert dfb != null;
            dfb.close();
        }
    }
}