package com.shu.hbase.test.fastdfs;

import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.FileInfo;
import org.csource.fastdfs.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
public class FastDFSClient {
    private static final Logger logger = LoggerFactory.getLogger(FastDFSClient.class);

    @Autowired
    private StorageClient storageClient;

    public String[] upload(File file, FastDFSFileMeta meta) {
        logger.debug("文件上传[fileMeta={}]", meta);

        // 文件属性
        NameValuePair[] metas = new NameValuePair[1];
        metas[0] = new NameValuePair("author", meta.getAuthor());

        String[] results = null;
        try {
            results = storageClient.upload_file(file.getPath(), meta.getExt(), metas);

        } catch (IOException e) {
            logger.error("文件上传错误file={}", meta.getName(), e);
        } catch (Exception e) {
            logger.error("文件上传错误file={}", meta.getName(), e);
        }

        if (results==null || results.length!=2) {
            logger.error("文件上传错误[error code={}]", storageClient.getErrorCode());
            return null;
        }

        logger.debug("文件上传成功[group_name={}, remoteFileName={}]", results[0], results[1]);
        return results;
    }

    /**
     * 文件上传
     * @param meta
     * @return
     */
    public String[] upload(FastDFSFileMeta meta) {
        logger.debug("文件上传[fileMeta={}]", meta);

        // 文件属性
        NameValuePair[] metas = new NameValuePair[1];
        metas[0] = new NameValuePair("author", meta.getAuthor());

        String[] results = null;
        try {
            results = storageClient.upload_file(meta.getContent(), meta.getExt(), metas);

        } catch (IOException e) {
            logger.error("文件上传错误file={}", meta.getName(), e);
        } catch (Exception e) {
            logger.error("文件上传错误file={}", meta.getName(), e);
        }

        if (results==null || results.length!=2) {
            logger.error("文件上传错误[error code={}]", storageClient.getErrorCode());
            return null;
        }

        logger.debug("文件上传成功[group_name={}, remoteFileName={}]", results[0], results[1]);
        return results;
    }

    /**
     * 读取文件元数据
     * @param groupName
     * @param remoteFileName
     * @return
     */
    public FileInfo fileMeta(String groupName, String remoteFileName) {
        try {
            logger.debug("读取文件元数据[groupName={}, remoteFileName={}]", groupName, remoteFileName);

            return storageClient.get_file_info(groupName, remoteFileName);

        } catch (IOException e) {
            logger.error("读取文件元数据错误", e);
        } catch (Exception e) {
            logger.error("读取文件元数据错误", e);
        }
        return null;
    }

    /**
     * 文件下载
     * @param groupName
     * @param remoteFileName
     * @return
     */
    public InputStream download(String groupName, String remoteFileName) {
        try {
            logger.debug("文件下载[groupName={}, remoteFileName={}]", groupName, remoteFileName);

            byte[] fileByte = storageClient.download_file(groupName, remoteFileName);
            InputStream ins = new ByteArrayInputStream(fileByte);
            return ins;

        } catch (IOException e) {
            logger.error("文件下载错误", e);
        } catch (Exception e) {
            logger.error("文件下载错误", e);
        }
        return null;
    }

    /**
     * 文件删除
     * @param groupName
     * @param remoteFileName
     * @return
     * @throws IOException
     * @throws MyException
     */
    public int delete(String groupName, String remoteFileName) throws IOException, MyException {
        logger.debug("文件删除[groupName={}, remoteFileName={}]", groupName, remoteFileName);
        return storageClient.delete_file(groupName, remoteFileName);
    }
}