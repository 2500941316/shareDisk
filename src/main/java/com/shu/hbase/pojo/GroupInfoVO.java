package com.shu.hbase.pojo;

import lombok.Data;

import java.util.Set;

@Data
public class GroupInfoVO {
    private String gId;
    private String name;
    private Set<String> member;
    private Set<ShareFileVO> file;

}
