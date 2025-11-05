package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.utils.BaseTaskManager;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

@Component
public class ImageBuildTaskManager extends BaseTaskManager<ImageBuildTask> {
}
