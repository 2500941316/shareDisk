package com.shu.hbase.pojo.api;

import lombok.Data;

@Data
public class ApiDownLoadInfo {
    private String userId;
    private String fileId;
    private String time;
    private String key;
    private String desPath;

}
