package com.helmx.tutorial.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 * @param <T>
 */
@Data
@ApiModel
public class PageResult<T> implements Serializable {

    @ApiModelProperty("当前页码")
    private long current;

    @ApiModelProperty("每页大小")
    private long size;

    @ApiModelProperty("数据列表")
    private List<T> items;

    @ApiModelProperty("总记录数")
    private long total;

    @ApiModelProperty("总页数")
    private long pages;

    public PageResult(Page<T> page) {
        this.current = page.getCurrent();
        this.size = page.getSize();
        this.items = page.getRecords();
        this.total = page.getTotal();
        this.pages = page.getPages();
    }

//    public PageResult() {}
}
