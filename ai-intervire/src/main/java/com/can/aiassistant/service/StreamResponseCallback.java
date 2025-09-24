package com.can.aiassistant.service;

import com.can.aiassistant.dto.StreamResponse;

/**
 * 流式响应回调接口
 */
@FunctionalInterface
public interface StreamResponseCallback {
    void onResponse(StreamResponse response);
}


