package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageHistoryItem {

    @ApiModelProperty(value = "层ID")
    private String id;

    @ApiModelProperty(value = "创建时间（ISO 8601 格式）")
    private String created;

    @ApiModelProperty(value = "创建该层的命令")
    private String layer;

    @ApiModelProperty(value = "层大小")
    private String size;

    @ApiModelProperty(value = "标签")
    private List<String> tags;

    @ApiModelProperty(value = "注释")
    private String comment;
}
