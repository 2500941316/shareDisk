package com.shu.hbase.controller;

import com.shu.hbase.pojo.NewGroupInfoVO;
import com.shu.hbase.pojo.ShareToFileVO;
import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
import io.swagger.annotations.ApiOperation;
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
    @ApiOperation(value="查询当前用户的共享分组")
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
    @ApiOperation(value="创建一个分组")
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
    @ApiOperation(value="获得某个分组的文件")
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
    @ApiOperation(value="删除一个分组")
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
    @ApiOperation(value="当前用户向一个分组中共享文件")
    public TableModel shareTo(@RequestBody ShareToFileVO shareToFileVO, Authentication authentication) {
        shareToFileVO.setUId(username);
        return userService.shareTo(shareToFileVO);
    }


    /**
     * 获得分组中我的共享文件
     *
     * @param
     * @throws IOException
     */
    @GetMapping("getMyShare")
    @ApiOperation(value="查询某个分组中只属于当前用户的文件")
    public TableModel getMyShare(@RequestParam("gId") String gId, Authentication authentication) {

        if (gId.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.getMyShare(gId, username);
    }


    /**
     * 删除共享组中的文件
     *
     * @param
     * @throws IOException
     */
    @ApiOperation(value="在分组中删除我的共享文件")
    @GetMapping("deleteShare")
    public TableModel deleteShare(@RequestParam("fileId") String fileId, @RequestParam("gId") String gId, Authentication authentication) {

        if (fileId.isEmpty() || gId.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.deleteShare(fileId, gId, username);

    }

}
