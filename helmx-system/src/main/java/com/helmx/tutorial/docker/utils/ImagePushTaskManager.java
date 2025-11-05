package com.helmx.tutorial.docker.utils;

import com.helmx.tutorial.utils.BaseTaskManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImagePushTaskManager extends BaseTaskManager<ImagePushTask> {
}
