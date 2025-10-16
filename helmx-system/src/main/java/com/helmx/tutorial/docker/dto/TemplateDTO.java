package com.helmx.tutorial.docker.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.helmx.tutorial.docker.entity.Template;

@Data
public class TemplateDTO {

    @ApiModelProperty(value = "模板ID")
    private Long id;

    @ApiModelProperty(value = "模板名称")
    private String name;

    @ApiModelProperty(value = "环境描述")
    private String remark;

    @ApiModelProperty(value = "模板内容")
    private String content;

    @ApiModelProperty(value = "模板类型")
    private String type;

    public TemplateDTO(Template template) {
        this.id = template.getId();
        this.name = template.getName();
        this.remark = template.getRemark();
        this.content = template.getContent();
        this.type = template.getType();
    }
}