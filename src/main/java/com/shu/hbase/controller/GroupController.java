package com.shu.hbase.controller;

import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
