package com.yupi.springbootinit.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.yupi.springbootinit.entity.*;

import com.yupi.springbootinit.mapper.LoginDao;
import com.yupi.springbootinit.service.LoginService;
import com.yupi.springbootinit.utils.MapperUtil;
import com.yupi.springbootinit.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yupi.springbootinit.entity.SecurityConstants.ALIVE_MINUTE;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Autowired
    private LoginDao dao;

//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;

    private final static int EXPIRE_TIME = ALIVE_MINUTE * 60;
    private static final String MSG = "用户账户未启用,请联系中心管理员!";
    private final static int COOKIE_TIME = SsoConstants.ALIVE_DAYS * 24 * 60 * 60;


    private String getTokenKey(String token) {
        return SsoConstants.CACHE_TOKEN_KEY + token;
    }


    @Override
    public Map<String, Object> loginUser(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new RuntimeException("未登录");
        }
//        User user = new User();
//        if (StringUtils.isNotEmpty(token)) {
//            String userKey = getTokenKey(token);
//            String userJson = stringRedisTemplate.opsForValue().get(userKey);
//            //刷新浏览器缓存
//            stringRedisTemplate.expire(userKey, 3 * 60 * 60, TimeUnit.SECONDS);
//            //刷新集成单点的过期时间
//            stringRedisTemplate.expire("auth_center_token:login:token:" + token, 3 * 60 * 60, TimeUnit.SECONDS);
//            user = MapperUtil.toObject(userJson, User.class);
//        }
//        if (ObjectUtils.isEmpty(user)) {
//            throw new RuntimeException("未登录");
//        }
//        SjyfwYhxx yhxx = dao.initUserInfo(Integer.valueOf(String.valueOf(user.getYhbz())));
//        if ("0".equals(yhxx.getDlzt())) {
//            throw new RuntimeException("账号未启用或被禁用");
//        }
//
//        DateTime now = DateTime.now();
//        DateTime exptime = now.offsetNew(DateField.MINUTE, ALIVE_MINUTE);
//        Map<String, Object> payload = new HashMap<String, Object>(4);
//        payload.put(JWTPayload.ISSUED_AT, now);
//        payload.put(JWTPayload.EXPIRES_AT, exptime);
//        payload.put(JWTPayload.NOT_BEFORE, now);
//        payload.put(SecurityConstants.DETAILS_YHBZ, yhxx.getYhbz());
//        payload.put(SecurityConstants.DETAILS_ZH, yhxx.getZh());
//        payload.put(SecurityConstants.DETAILS_XM, yhxx.getXm());
//        payload.put(SecurityConstants.DETAILS_YHJB, yhxx.getYhjb());
//        payload.put(SsoConstants.TOKEN_NAME_XQTB, token);
//        payload.put("topbm", null);
////        payload.put("qx", getQx(yhxx.getYhbz()));
//        if (StringUtils.isNotEmpty(yhxx.getDw())) {
//            ZdbBm topBm = dao.getTopBm(yhxx.getDw());
//            if (StringUtils.isNotNull(topBm)) {
//                payload.put("topbm", topBm.getMc());
//                String per = topBm.getMc() + "," + "军委联参情报分析中心";
//                payload.put("per", per);
//            }
//        }
//
//        String newtoken = JWTUtil.createToken(payload, SecurityConstants.SECRET.getBytes());
//        log.info("jwt生成:" + newtoken);
//        LoginUser loginUser = new LoginUser();
//        BeanUtils.copyProperties(yhxx, loginUser);
//        user.setToken(newtoken);
        return null;
    }


}
