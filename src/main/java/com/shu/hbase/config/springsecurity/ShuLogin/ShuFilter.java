package com.shu.hbase.config.springsecurity.ShuLogin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shu.hbase.exceptions.BusinessException;
import com.shu.hbase.exceptions.Exceptions;
import com.shu.hbase.pojo.User;
import com.shu.hbase.tools.Shutool;
import com.shu.hbase.tools.TableModel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Data
public class ShuFilter extends OncePerRequestFilter {
    private Logger logger = LoggerFactory.getLogger(ShuFilter.class);
    private  String password = "";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().contains("/login")) {
            String username = request.getParameter("username").trim();
            password = request.getParameter("password").trim();
            if (StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(password)) {
                if (!Shutool.getAuth(username, password)) {
                    logger.error("用户名或密码错误");
                    throw new BusinessException(Exceptions.SERVER_AUTH_ERROR.getEcode());
                }
                logger.info(username+"登录成功");
            } else {
                logger.error("用户名或密码为空");
                throw new BusinessException(Exceptions.SERVER_UNAMEISNULL_ERROR.getEcode());
            }
        }
        filterChain.doFilter(request, response);
    }
}
