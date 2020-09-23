package com.shu.hbase.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@CrossOrigin
@Controller
public class CommonController {


    @RequestMapping("Login")
    public String login()
    {
        return "/html/login";
    }

}
