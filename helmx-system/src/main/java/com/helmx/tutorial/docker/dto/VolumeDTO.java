package com.helmx.tutorial.docker.dto;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Volume;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VolumeDTO {

    @ApiModelProperty(value = "名称")
    private String name;

    @ApiModelProperty(value = "标签")
    private Map<String, String> labels;

    @ApiModelProperty(value = "驱动")
    private String driver;

    @ApiModelProperty(value = "挂载点")
    private String mountPoint;

    @ApiModelProperty(value = "参数")
    private Map<String, String> options;

    @ApiModelProperty(value = "作用域")
    private String scope;

    @ApiModelProperty(value = "是否正在使用")
    private Boolean isUsed;

    @ApiModelProperty(value = "创建时间")
    private String createdAt;

    @ApiModelProperty(value = "容器列表")
    private List<Map<String, String>> containers;

    public VolumeDTO(InspectVolumeResponse volume) {
        this.name = volume.getName();
        this.driver = volume.getDriver();
        this.mountPoint = volume.getMountpoint();
        this.options = volume.getOptions();
        this.labels = volume.getLabels();
        this.scope = volume.getRawValues().get("Scope") == null ? "" : volume.getRawValues().get("Scope").toString();
    }
}
