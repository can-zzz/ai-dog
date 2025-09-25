package com.can.happydog.service;

import com.can.happydog.dto.StreamResponse;

/**
 * 流式响应回调接口
 */
@FunctionalInterface
public interface StreamResponseCallback {
    void onResponse(StreamResponse response);
}


