package com.yupi.springbootinit.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Data
public class PageResult<E> {


    private Integer code = 200;


    private String message = "success";


    private Pagination pagination;


    private List<E> data = new ArrayList<E>();


    @NoArgsConstructor
    @Data
    public static class Pagination {


        private int page;

        private int total;

        private int size;
    }

}
