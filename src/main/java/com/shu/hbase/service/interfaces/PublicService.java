package com.shu.hbase.service.interfaces;

import com.shu.hbase.tools.TableModel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PublicService {

    TableModel getPublicFiles(String uId);

    TableModel searchFile(String value, String name);

    void downLoad(String fileId, String gId, HttpServletResponse response, HttpServletRequest request, String username);
}
