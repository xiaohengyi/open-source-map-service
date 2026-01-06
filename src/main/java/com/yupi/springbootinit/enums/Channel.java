package com.yupi.springbootinit.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Channel {
    INTERNET("互联网检测"), // 新增
    SELLING("数据服务售卖"),  // 编辑
    INTERNAL("军内单位引接数据更新");

    final String description;

    Channel(String description) {
        this.description = description;
    }
    public static List<String> getChannels() {
       return Arrays.stream(Channel.values()).map(s -> s.description).collect(Collectors.toList());
    }

}
