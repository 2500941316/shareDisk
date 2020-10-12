package com.shu.hbase.service.interfaces;

import com.shu.hbase.pojo.NewGroupInfoVO;
import com.shu.hbase.pojo.ShareToFileVO;
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

    TableModel buildGroup(NewGroupInfoVO newGroupInfoVO);

    TableModel getGroupFile(String gId);

    TableModel deleteGroup(String gid, String username);

    TableModel shareTo(ShareToFileVO shareToFileVO);

    TableModel getMyShare(String gId, String username);

    TableModel deleteShare(String fileId, String gId, String username);

    TableModel getMembersBygid(String gid, String username);
}
