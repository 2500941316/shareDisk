package com.shu.hbase.controller;

import com.shu.hbase.pojo.NewGroupInfoVO;
import com.shu.hbase.pojo.ShareToFileVO;
import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@CrossOrigin
public class GroupController {

    private String username = "19721631";

    @Autowired
    UserService userService;

    /**
     * 获得分享文件组
     *
     * @param
     * @throws IOException
     */
    @GetMapping("getShares")
    public TableModel getShares(Authentication authentication) {

        return userService.getShares(username);
    }


    /**
     * 新建分组
     *
     * @param
     * @throws IOException
     */
    @PostMapping("buildGroup")
    public TableModel buildGroup(@RequestBody NewGroupInfoVO newGroupInfoVO, Authentication authentication) {

        if (newGroupInfoVO.getUId() == null) {
            newGroupInfoVO.setUId(username);
        }
        return userService.buildGroup(newGroupInfoVO);
    }


    /**
     * 获得某个分组的文件
     *
     * @param
     * @throws IOException
     */
    @GetMapping("getGroupFile")
    public TableModel getGroupFile(@RequestParam("gId") String gId) {

        if (gId.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.getGroupFile(gId);
    }


    /**
     * 删除一个分组
     *
     * @param
     * @throws IOException
     */
    @GetMapping("deleteGroup")
    public TableModel deleteGroup(@RequestParam("gid") String gid, Authentication authentication) {

        if (gid.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.deleteGroup(gid, username);
    }


    /**
     * 共享文件方法
     *
     * @param
     * @throws IOException
     */
    @PostMapping("shareTo")
    public TableModel shareTo(@RequestBody ShareToFileVO shareToFileVO, Authentication authentication) {
        shareToFileVO.setUId(username);
        return userService.shareTo(shareToFileVO);
    }
}
