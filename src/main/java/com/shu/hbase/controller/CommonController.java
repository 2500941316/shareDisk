package com.shu.hbase.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@CrossOrigin
@Controller
public class CommonController {

    @ApiOperation(value="跳转到登录页面", notes="用户未登录时会自动跳转到该api，转入登录页面")
    @GetMapping("Login")
    public String login()
    {
        return "/html/login";
    }

}
