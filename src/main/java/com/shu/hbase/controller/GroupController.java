package com.shu.hbase.controller;

import com.shu.hbase.pojo.NewGroupInfoVO;
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

}
