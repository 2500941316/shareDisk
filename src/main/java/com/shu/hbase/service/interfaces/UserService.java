package com.shu.hbase.service.interfaces;

import com.shu.hbase.tools.TableModel;

public interface UserService {
    TableModel selectFile(String detSrc, String type, String name, String gId);

    TableModel getFilesByType(String type, String username);
}
