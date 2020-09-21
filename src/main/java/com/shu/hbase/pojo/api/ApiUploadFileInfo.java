package com.shu.hbase.pojo.api;

import lombok.Data;

@Data
public class ApiUploadFileInfo {
    private String userId;
    private String fileName;
    private String fileStr;
    private String time;
    private String key;

}
