package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.utils.BaseTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImagePushTask extends BaseTask {
    private String imageName;
}