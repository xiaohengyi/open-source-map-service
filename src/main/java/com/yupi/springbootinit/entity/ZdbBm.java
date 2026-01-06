package com.yupi.springbootinit.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class ZdbBm {
    private String bmnm;

    private String xh;

    private String mc;

    private String bmcj;
    /**
     * 创建人用户标志
     */
    //@JSONField(name = "cjryhbz")
    private Long bmcjr;
    /**
     * 数据时间
     */
    //用了这个JSONfied后入库没有新增时间了！！！！！谁再改这里联系下刘柳
    //@JSONField(name = "sjsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date bmcjsj;

    private boolean isLeaf;

    private int a;

    String fjxh;

    String bjxh;


}
