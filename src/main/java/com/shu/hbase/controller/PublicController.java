package com.shu.hbase.controller;

import com.shu.hbase.service.interfaces.PublicService;
import com.shu.hbase.tools.TableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;


@RestController
@CrossOrigin
public class PublicController {
    private String username = "19721631";

    @Autowired
    PublicService publicService;

    /**
     * 首页获得公共共享的文件
     *
     * @param principal
     * @return
     */
    @GetMapping("getPublicFiles")
    public TableModel getPublicFiles(Principal principal) {
        if (principal != null) {
            username = principal.getName();
        }
        return publicService.getPublicFiles(username);
    }


    /**
     * 下载文件api
     *
     * @param
     * @throws IOException
     */
    @PostMapping("downLoad")
    public void downLoad(@RequestParam String fileId, String gId, HttpServletResponse response, HttpServletRequest request, Principal principal) {
        if (principal != null) {
            username = principal.getName();
        }
        publicService.downLoad(fileId, gId, response, request, username);
    }


    /**
     * 查找文件
     *
     * @param
     * @throws IOException
     */
    @GetMapping("searchFile")
    public TableModel searchFile(@RequestParam String value, @RequestParam String type, Authentication authentication) {

        if (value.isEmpty()) {
            return TableModel.error("参数为空");
        }
        if (type.equals("private"))
            return publicService.searchFile(value, authentication.getName());
        else
            return publicService.searchFile(value, "00000000");
    }
}
