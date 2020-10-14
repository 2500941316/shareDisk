package com.shu.hbase.test.fastdfs;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FastDFSFileMetaBuilder {

    /**
     * 根据上传附件构造FastDFSFileMeta
     * @param file
     * @return
     * @throws IOException
     */
    public static FastDFSFileMeta build(MultipartFile file) throws IOException {
        if(file == null) {
            return null;
        }

        FastDFSFileMeta meta = new FastDFSFileMeta();
        meta.setContent(file.getBytes());
        meta.setName(file.getOriginalFilename());
        meta.setExt(file.getOriginalFilename().substring(file.getOriginalFilename().indexOf(".")+1));
        meta.setFileSize(file.getSize());

        return meta;
    }

    public static FastDFSFileMeta buildBigFile(MultipartFile file) {
        if(file == null) {
            return null;
        }

        FastDFSFileMeta meta = new FastDFSFileMeta();
        meta.setName(file.getOriginalFilename());
        meta.setExt(file.getOriginalFilename().substring(file.getOriginalFilename().indexOf(".")+1));
        meta.setFileSize(file.getSize());

        return meta;
    }

    public static FastDFSFileMeta build(File file) throws IOException {
        if(file == null) {
            return null;
        }

        FastDFSFileMeta meta = new FastDFSFileMeta();
        meta.setName(file.getName());
        meta.setExt(file.getName().substring(file.getName().indexOf(".")+1));
        meta.setFileSize(file.length());

        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);

            meta.setContent(bytes);
        } finally {
            if(in!=null) {
                in.close();
            }
        }

        return meta;
    }

    public static FastDFSFileMeta buildBigFile(File file) throws IOException {
        if(file == null) {
            return null;
        }

        FastDFSFileMeta meta = new FastDFSFileMeta();
        meta.setName(file.getName());
        meta.setExt(file.getName().substring(file.getName().indexOf(".")+1));
        meta.setFileSize(file.length());

        return meta;
    }
}