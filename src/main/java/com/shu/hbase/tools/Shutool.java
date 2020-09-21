package com.shu.hbase.tools;

import com.shu.hbase.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Shutool {
    private Shutool() {
        throw new IllegalStateException("Shutool class");
    }

    private static Logger logger = LoggerFactory.getLogger(Shutool.class);
    private static final String str="<string>";
    public static boolean getAuth(String userId, String password) {
        try {

            String xmlFile = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<CheckUserLogin xmlns=\"www.lib.shu.edu.cn\">" +
                    "<UserID>" + userId + "</UserID>" +
                    "<password>" + password + "</password>" +
                    "</CheckUserLogin>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";
            String urlStr = "http://202.120.121.204:8888/WebService/shulibLogin.asmx?WSDL";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setDoOutput(true);    // 可以发送数据
            conn.setDoInput(true);    // 可以接收数据
            conn.setRequestMethod("POST");    // POST方法

            conn.connect();
            //    写入的POST数据
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
            osw.write(xmlFile);
            osw.flush();
            osw.close();
            // 读取响应数据
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseStr = new StringBuilder();
            String tempStr;
            while ((tempStr = in.readLine()) != null) {
                responseStr.append(tempStr);
            }
            int site1 = responseStr.indexOf("<CheckUserLoginResult>");
            char number = responseStr.charAt(site1 + 22);
            return number == '1';
        } catch (Exception e) {
           logger.error("libLogin WebService Exception,无法链接图书馆学号/一卡通服务进行登录。如多次出现，请检查网络是否故障，或者请向信息化办公室求证libLogin WebService服务是否正常");
            return false;
        }
    }

    public static User getInfo(String userId) {
        try {
            String xmlFile = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<GetStudentInfo xmlns=\"www.lib.shu.edu.cn\">" +
                    "<UserID>" + userId + "</UserID>" +
                    "</GetStudentInfo>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";
            String urlStr = "http://202.120.121.204:8888/WebService/shulibLogin.asmx?WSDL";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setDoOutput(true);    // 可以发送数据
            conn.setDoInput(true);    // 可以接收数据
            conn.setRequestMethod("POST");    // POST方法

            conn.connect();
            //    写入的POST数据
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
            osw.write(xmlFile);
            osw.flush();
            osw.close();
            // 读取响应数据
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder responseStr = new StringBuilder();
            String tempStr;
            User user = new User();
            while ((tempStr = in.readLine()) != null) {
                responseStr.append(tempStr);
            }
            int site1 = responseStr.indexOf("<GetStudentInfoResult><string>");
            String sub1 = responseStr.substring(site1 + 30);

            int site11 = sub1.indexOf(str);
            user.setName(sub1.substring(0, site11));

            int site2 = sub1.indexOf(str);
            String sub2 = sub1.substring(site2 + 8);
            int site22 = sub2.indexOf(str);
            user.setDuty(sub2.substring(0, site22));

            int site3 = sub2.indexOf(str);
            String sub3 = sub2.substring(site3 + 8);
            int site33 = sub3.indexOf(str);
            user.setDepartment(sub3.substring(0, site33));

            user.setUserId(userId);
            return user;
        } catch (Exception e) {
            logger.error("验证过程中发生异常,一般是由于工号/学号无效!");
            return null;
        }
    }
}
