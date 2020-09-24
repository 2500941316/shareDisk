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

    @GetMapping("testJson")
    public String testJson()
    {
        return "{\n" +
                "\t\"code\": 0,\n" +
                "\t\"total\": 5,\n" +
                "\t\"msg\": null,\n" +
                "\t\"count\": 12,\n" +
                "\t\"data\": [{\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请2.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076739607,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请2.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076739607\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请1.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076739393,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请1.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076739393\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请0.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076739203,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请0.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076739203\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请4.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076730452,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请2.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076730452\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请5.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076730270,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请1.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076730270\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请6.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076730091,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请0.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076730091\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请7.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076728697,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请2.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076728696\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请8.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076728523,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请1.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076728522\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请9.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076728332,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请0.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076728332\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请10.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076726158,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请2.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076726158\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请11.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076725968,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请1.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076725968\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}, {\n" +
                "\t\t\"name\": \"上海大学奖学金评选申请12.docx\",\n" +
                "\t\t\"size\": null,\n" +
                "\t\t\"time\": 1598076725421,\n" +
                "\t\t\"type\": \".docx\",\n" +
                "\t\t\"path\": \"/shuwebfs/00000000/上海大学奖学金评选申请0.docx\",\n" +
                "\t\t\"back\": \"00000000\",\n" +
                "\t\t\"precent\": 100.0,\n" +
                "\t\t\"fileId\": \"00000000_1598076725421\",\n" +
                "\t\t\"sharer\": \"00000000\",\n" +
                "\t\t\"dir\": false,\n" +
                "\t\t\"myShare\": false,\n" +
                "\t\t\"new\": false\n" +
                "\t}]\n" +
                "}";
    }

}
