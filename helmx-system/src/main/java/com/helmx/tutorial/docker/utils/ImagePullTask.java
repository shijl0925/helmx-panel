package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.utils.BaseTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImagePullTask extends BaseTask {
    private String imageName;
}
