package com.shu.hbase.service.interfaces;

import com.shu.hbase.tools.TableModel;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface UserService {
    TableModel selectFile(String detSrc, String type, String name, String gId);

    TableModel getFilesByType(String type, String username);

    TableModel uploadTomvc(MultipartFile file, Integer chunk, Integer chunks, String username, HttpServletRequest request, String backId);

    TableModel buildDirect(String backId, String dirName, String username);

    TableModel deleteFile(String fileId, String username);

    TableModel getShares(String username);
}
