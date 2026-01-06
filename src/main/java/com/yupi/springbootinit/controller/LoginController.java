package com.yupi.springbootinit.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.yupi.springbootinit.domain.LoginInfo;
import com.yupi.springbootinit.domain.SysResult;
import com.yupi.springbootinit.service.LoginService;
import com.yupi.springbootinit.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Tag(name = "登录功能")
@RestController
@RequestMapping("/login")
@Slf4j
public class LoginController {
    @Value("${third.url}")
    private String thirdUrl;
    @Value("${third.logout}")
    private String logout;
    @Value("${third.token}")
    private String token;
    @Value("${third.softname}")
    private String softname;
    @Value("${third.rjmc}")
    private String rjmc;
    @Value("${third.login}")
    private String login;
    @Autowired
    private LoginService loginService;
    @Value("${third.checkToken}")
    private String checkToken;


    /**
     * 集成单点登录
     * 前台使用wjn统一第三方登录界面进行登录 需要xtbs
     * 调用后台退出和是否登录接口。
     *
     * @return
     */
    @Operation(summary = "单点登录")
    @PostMapping("/login")
    public SysResult login(@RequestBody @Valid LoginInfo info, HttpServletResponse response, HttpServletRequest request) {
        //走单点登录
        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("yhmc", URLEncoder.encode(info.getUsername(), StandardCharsets.UTF_8.toString()));
            String password = StringUtils.sha256ByHex(info.getPassword());
            paramMap.put("password", password.toString());
            paramMap.put("rjmc", URLEncoder.encode(rjmc, StandardCharsets.UTF_8.toString()));
            paramMap.put("softname", URLEncoder.encode(softname, StandardCharsets.UTF_8.toString()));
            paramMap.put("userIp", getClientIpAddress(request));
            paramMap.put("token", token);
            HttpRequest httpRequest = HttpRequest.get(thirdUrl + login);
            HttpResponse execute = httpRequest.form(paramMap).execute();
            JSONObject responseBody = JSONUtil.parseObj(execute.body());
            if (responseBody.getInt("code") == 0) {
                return SysResult.success(responseBody.getStr("msg"), responseBody.getJSONObject("data"));
            } else {
                return SysResult.error1(responseBody.getStr("msg"));
            }
        } catch (Exception e) {
            return SysResult.error();
        }

    }

    private void returnResult(HttpServletResponse response, Object json) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Operation(summary = "是否登录")
    @PostMapping("/loginUser")
    public SysResult isLogin(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        /**
         * 如需要拦截非法请求，打开注释内容
         */
        if (token == null) {
            token = "";
        }
        HashMap<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("token", token);
        tokenMap.put("rjmc", URLEncoder.encode(rjmc, StandardCharsets.UTF_8.toString()));
        tokenMap.put("softname", URLEncoder.encode(softname, StandardCharsets.UTF_8.toString()));
        tokenMap.put("userIp", getClientIpAddress(request));
        HttpRequest httpRequest = HttpRequest.get(thirdUrl + checkToken);
        HttpResponse execute = httpRequest.form(tokenMap).execute();
        JSONObject responseBody = JSONUtil.parseObj(execute.body());
        if (responseBody.getInt("code") == 0) {
            return SysResult.success(responseBody.getStr("msg"), responseBody.getJSONObject("data"));
        } else {
            return SysResult.error1(responseBody.getStr("msg"));
        }
        //JSONObject data = json.getJSONObject("data");
        //data.put("xtbs", xtbs);
//            if ("200".equals(json.get("status").toString())) {
//                returnResult(response, JSON.toJSONString(Result.failure(ResultCode.SUCCESS.code(), json.get("message").toString(), data)));
//                return true;
//            } else {
//                returnResult(response, JSON.toJSONString(Result.failure(ResultCode.ERROR.code(), json.get("message").toString(), data)));
//                return false;
//            }

//        if (ObjectUtils.isEmpty(map)) {
//            throw new RuntimeException("未登录");
//        }
//        String token = map.get("token");
//        Map<String, Object> userInfo = loginService.loginUser(token);
//        return SysResult.success(userInfo);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public SysResult logout(@RequestBody Map<String, String> map,HttpServletRequest request) {
        if (ObjectUtils.isEmpty(map)) {
            throw new RuntimeException("未登录");
        }

        try {
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("rjmc", URLEncoder.encode(rjmc, StandardCharsets.UTF_8.toString()));
            paramMap.put("softname", URLEncoder.encode(softname, StandardCharsets.UTF_8.toString()));
            paramMap.put("token", token);
            paramMap.put("userIp", getClientIpAddress(request));
            HttpRequest httpRequest = HttpRequest.get(thirdUrl + logout);
            HttpResponse execute = httpRequest.form(paramMap).execute();
            JSONObject responseBody = JSONUtil.parseObj(execute.body());
            if (responseBody.getInt("code") == 0) {
                return SysResult.success(responseBody.getStr("msg"), responseBody.getJSONObject("data"));
            } else {
                return SysResult.error1(responseBody.getStr("msg"));
            }
        } catch (Exception e) {
            return SysResult.error();
        }

    }

    public String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理 X-Forwarded-For 多个 IP 的情况，取第一个有效 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

}
