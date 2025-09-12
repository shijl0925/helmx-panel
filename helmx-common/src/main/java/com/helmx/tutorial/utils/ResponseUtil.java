package com.helmx.tutorial.utils;

import org.springframework.http.ResponseEntity;
import com.helmx.tutorial.dto.Result;

public class ResponseUtil {

    private static final int SUCCESS_CODE = 0; // only for vben

    public static <T> ResponseEntity<Result> success(T data) {
        Result result = new Result();
        result.setMessage("success");
        result.setData(data);
        result.setCode(SUCCESS_CODE);
        return ResponseEntity.ok(result);
    }

    public static <T> ResponseEntity<Result> success(String message, T data) {
        Result result = new Result();
        result.setMessage(message != null ? message : "success");
        result.setData(data);
        result.setCode(SUCCESS_CODE);
        return ResponseEntity.ok(result);
    }

    public static <T> ResponseEntity<Result> failed(Integer code, T data, String message) {
        Result result = new Result();
        result.setMessage(message != null ? message : "failed");
        result.setData(data);
        result.setCode(code);
        return ResponseEntity.ok(result);
    }
}

