package com.yupi.springbootinit.mapper;


import com.yupi.springbootinit.entity.SjyfwYhxx;
import com.yupi.springbootinit.entity.ZdbBm;
import org.springframework.stereotype.Component;

@Component
public interface LoginDao {



    SjyfwYhxx initUserInfo(Integer yhbz);

    ZdbBm getTopBm(String bmnm);


}
