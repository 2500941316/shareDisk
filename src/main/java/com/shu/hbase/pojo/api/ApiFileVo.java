package com.shu.hbase.pojo.api;

import lombok.Data;

@Data
public class ApiFileVo {
    private String fileId;
    private String name;
    private Long size;
    private String type;
    private Long time;
}
