package com.shu.hbase.controller;

import com.shu.hbase.service.interfaces.PublicService;
import com.shu.hbase.service.interfaces.UserService;
import com.shu.hbase.tools.TableModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.security.Principal;


@RestController
@CrossOrigin
public class PublicController {
    private String username = "19721631";

    @Autowired
    PublicService publicService;

    @Autowired
    UserService userService;

    /**
     * 首页获得公共共享的文件
     *
     * @param principal
     * @return
     */
    @ApiOperation(value="查询公共共享文件")
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
    @ApiOperation(value="web下载接口")
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
    @ApiOperation(value="根据关键字搜索文件")
    @GetMapping("searchFile")
    public TableModel searchFile(@RequestParam String value, @RequestParam String type, Authentication authentication) {

        if (value.isEmpty()) {
            return TableModel.error("参数为空");
        }
        if (type.equals("private"))
            return publicService.searchFile(value, username);
        else
            return publicService.searchFile(value, "00000000");
    }


    /**
     * 查询某个目录下的文件信息
     *
     * @param
     * @throws IOException
     */
    @ApiOperation(value="查询一个文件夹中的文件")
    @GetMapping("selectFile")
    public TableModel selectFile(@Validated @Size(min = 8) @RequestParam("detSrc") String detSrc, @RequestParam("type") String type,
                                 @RequestParam("gId") String gId, Authentication authentication) {

        if (detSrc.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.selectFile(detSrc, type, username, gId);
    }


    /**
     * 查找对应类型的文件
     *
     * @param
     * @throws IOException
     */
    @ApiOperation(value="根据类型查找文件")
    @GetMapping("getFilesByType")
    public TableModel getFilesByType(@RequestParam String type, Authentication authentication) {
        if (type.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.getFilesByType(type, username);
    }


    /**
     * 上传文件到mvc后端
     *
     * @param file
     * @param chunk
     * @param chunks
     * @param request
     */
    @ApiOperation(value="上传文件到后端服务器")
    @PostMapping("uploadToBacken")
    public TableModel uploadTomvc(@RequestParam MultipartFile file, Integer chunk, Integer chunks, String backId, HttpServletRequest request, Authentication authentication) {
        if (file.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.uploadTomvc(file, chunk, chunks, username, request, backId);
    }


    /**
     * 创建文件夹
     *
     * @param
     */
    @ApiOperation(value="新建文件夹")
    @GetMapping("buildDirect")
    public TableModel buildDirect(@RequestParam("backId") String backId, @RequestParam("dirName") String dirName, Authentication authentication) {
        if (backId.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.buildDirect(backId, dirName, username);
    }


    /**
     * 删除文件
     *
     * @param
     * @throws IOException
     */
    @ApiOperation(value="删除一个文件")
    @GetMapping("deleteFile")
    public TableModel deleteFile(@RequestParam("fileId") String fileId, HttpServletRequest request, Authentication authentication) {

        if (fileId.isEmpty()) {
            return TableModel.error("参数为空");
        }
        return userService.deleteFile(fileId, username);
    }



}
