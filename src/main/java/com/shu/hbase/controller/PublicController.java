package com.shu.hbase.controller;

import com.shu.hbase.service.interfaces.PublicService;
import com.shu.hbase.tools.TableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@CrossOrigin
public class PublicController {

    @Autowired
    PublicService publicService;

    @GetMapping("getPublicFiles")
    public TableModel getPublicFiles(Principal principal) {
        String uid = "19721631";
        if (principal != null) {
            uid = principal.getName();
        }
        return publicService.getPublicFiles(uid);
    }
}
