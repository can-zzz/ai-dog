package com.can.happydog.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * AI配置类
 */
@Configuration
public class AiConfig {

    /**
     * 创建RestTemplate实例用于API调用
     */
    /**
     * 创建HTTP客户端
     */
    @Bean
    public CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }

    /**
     * 创建RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(30000);
        factory.setConnectionRequestTimeout(30000);
        return new RestTemplate(factory);
    }
}
