package com.can.travelplanner.service;

import com.can.travelplanner.dto.TravelPlanRequest;
import com.can.travelplanner.dto.TravelPlanResponse;
import com.can.travelplanner.dto.LocationInfo;
import com.can.travelplanner.exception.TravelPlannerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
public class TravelPlannerService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public TravelPlannerService(
            RestTemplate restTemplate,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.model}") String model,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    public TravelPlanResponse generateTravelPlan(TravelPlanRequest request) {
        try {
            log.info("开始生成旅行计划: {}", request);
            
            // 构建提示词
            String prompt = String.format("""
                请为以下旅行需求制定详细的旅行计划：
                
                目的地：%s
                旅行天数：%d天
                特殊需求：%s
                
                请提供以下信息：
                1. 每日行程安排
                2. 推荐景点（包含开放时间）
                3. 交通建议
                4. 当地美食推荐
                5. 注意事项
                
                请用中文回答，并确保信息准确、实用。
                """, request.getDestination(), request.getDuration(), request.getPreferences());

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-DashScope-SSE", "disable");

            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, entity, Map.class);
            
            // 解析响应
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("choices")) {
                throw new TravelPlannerException("AI服务返回的响应格式不正确");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices.isEmpty()) {
                throw new TravelPlannerException("AI服务未返回任何建议");
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, String> message = (Map<String, String>) choice.get("message");
            String content = message.get("content");

            // 获取位置信息
            LocationInfo locationInfo = getLocationInfo(request.getDestination());

            // 构建响应
            TravelPlanResponse travelPlan = new TravelPlanResponse();
            travelPlan.setDestination(request.getDestination());
            travelPlan.setDuration(request.getDuration());
            travelPlan.setPlan(content);
            travelPlan.setLocationInfo(locationInfo);

            log.info("旅行计划生成成功");
            return travelPlan;

        } catch (Exception e) {
            log.error("生成旅行计划时发生错误", e);
            throw new TravelPlannerException("生成旅行计划失败: " + e.getMessage());
        }
    }

    private LocationInfo getLocationInfo(String destination) {
        try {
            log.info("开始获取位置信息: {}", destination);
            
            String url = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1", 
                destination.replace(" ", "+"));
            
            ResponseEntity<LocationInfo[]> response = restTemplate.getForEntity(url, LocationInfo[].class);
            LocationInfo[] locations = response.getBody();
            
            if (locations != null && locations.length > 0) {
                log.info("成功获取位置信息");
                return locations[0];
            }
            
            log.warn("未找到位置信息");
            return null;
            
        } catch (Exception e) {
            log.error("获取位置信息时发生错误", e);
            return null;
        }
    }
} 