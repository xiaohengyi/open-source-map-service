package com.yupi.springbootinit.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeVo implements Serializable {

    private String id;

    private String parentId;

    private String name;

    private List<TreeVo> children;

    private String label;

    public TreeVo(String id, String parentId, String name, String label) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.label = label;
    }
}
