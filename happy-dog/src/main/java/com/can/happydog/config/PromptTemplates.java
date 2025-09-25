package com.can.happydog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

/**
 * 提示词模板配置管理
 * 提示词体系
 */
@Component
@ConfigurationProperties(prefix = "ai.prompt-templates")
public class PromptTemplates {
    
    // 基础Chat系统提示词
    private String basicChatSystemPrompt;
    private String deepThinkingSystemPrompt;
    
    // 专业领域提示词
    private String technicalAnalysisPrompt;
    private String creativeWritingPrompt;
    private String problemSolvingPrompt;
    private String factualQueryPrompt;
    private String conversationalPrompt;
    
    // 思考策略提示词
    private String strategicThinkingPrompt;
    private String stepByStepAnalysisPrompt;
    private String criticalThinkingPrompt;
    
    // 内容生成提示词
    private String articleWritingPrompt;
    private String summaryGenerationPrompt;
    private String explanationPrompt;
    
    // 交互增强提示词
    private String followUpQuestionPrompt;
    private String clarificationPrompt;
    private String feedbackPrompt;
    
    /**
     * 获取基础Chat系统提示词
     */
    public String getBasicChatSystemPrompt() {
        if (basicChatSystemPrompt == null || basicChatSystemPrompt.isEmpty()) {
            return getDefaultBasicChatPrompt();
        }
        return renderTemplate(basicChatSystemPrompt);
    }
    
    /**
     * 获取深度思考系统提示词
     */
    public String getDeepThinkingSystemPrompt() {
        if (deepThinkingSystemPrompt == null || deepThinkingSystemPrompt.isEmpty()) {
            return getDefaultDeepThinkingPrompt();
        }
        return renderTemplate(deepThinkingSystemPrompt);
    }
    
    /**
     * 根据思考策略获取提示词
     */
    public String getThinkingStrategyPrompt(String strategy) {
        switch (strategy.toLowerCase()) {
            case "factual_analysis":
                return renderTemplate(getFactualQueryPrompt());
            case "creative_thinking":
                return renderTemplate(getCreativeWritingPrompt());
            case "problem_solving":
                return renderTemplate(getProblemSolvingPrompt());
            case "comparative_analysis":
                return renderTemplate(getTechnicalAnalysisPrompt());
            default:
                return renderTemplate(getStrategicThinkingPrompt());
        }
    }
    
    /**
     * 根据生成策略获取提示词
     */
    public String getGenerationStrategyPrompt(String strategy) {
        switch (strategy.toLowerCase()) {
            case "rag_generation":
                return renderTemplate(getArticleWritingPrompt());
            case "thinking_based_generation":
                return renderTemplate(getStepByStepAnalysisPrompt());
            case "context_aware_generation":
                return renderTemplate(getConversationalPrompt());
            case "enhanced_generation":
                return renderTemplate(getCriticalThinkingPrompt());
            case "simple_generation":
                return renderTemplate(getConversationalPrompt());
            default:
                return renderTemplate(getBasicChatSystemPrompt());
        }
    }
    
    /**
     * 获取追问建议提示词
     */
    public String getFollowUpPrompt(String questionType, String context) {
        String template = getFollowUpQuestionPrompt();
        return template.replace("{{question_type}}", questionType)
                      .replace("{{context}}", context);
    }
    
    /**
     * 渲染模板（替换动态变量）
     */
    private String renderTemplate(String template) {
        if (template == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> variables = new HashMap<>();
        
        // 时间相关变量
        variables.put("{{datetime.year}}", String.valueOf(now.getYear()));
        variables.put("{{datetime.month}}", String.valueOf(now.getMonthValue()));
        variables.put("{{datetime.day}}", String.valueOf(now.getDayOfMonth()));
        variables.put("{{datetime.hour}}", String.valueOf(now.getHour()));
        variables.put("{{datetime.weekday_zh}}", getChineseWeekday(now.getDayOfWeek().getValue()));
        variables.put("{{datetime.dayparts_zh}}", getChineseDayPart(now.getHour()));
        
        // 地理位置变量（可以后续扩展）
        variables.put("{{geo_info_location}}", "未知位置");
        
        // 替换所有变量
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 获取中文星期
     */
    private String getChineseWeekday(int dayOfWeek) {
        String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        return weekdays[dayOfWeek - 1];
    }
    
    /**
     * 获取中文时段
     */
    private String getChineseDayPart(int hour) {
        if (hour >= 5 && hour < 8) return "清晨";
        if (hour >= 8 && hour < 11) return "上午";
        if (hour >= 11 && hour < 13) return "中午";
        if (hour >= 13 && hour < 17) return "下午";
        if (hour >= 17 && hour < 19) return "傍晚";
        if (hour >= 19 && hour < 22) return "晚上";
        return "深夜";
    }
    
    /**
     * 默认基础Chat提示词
     */
    private String getDefaultBasicChatPrompt() {
        return """
            你是一个智能快乐小狗，能够响应用户的各种问询。你具备以下能力：
            
            1. **语言适应**：
               - 默认使用中文进行思考和回答
               - 根据用户使用的语言进行相应的回复
               - 保持回复语言的一致性和准确性
            
            2. **思考流程**：
               - 首先理解用户的意图和需求
               - 分析问题的类型和复杂度
               - 制定合适的回答策略
               - 提供准确、有用的信息
            
            3. **回答原则**：
               - 保持客观、准确、有用
               - 结构清晰，逻辑连贯
               - 适当使用Markdown格式
               - 避免重复和冗余信息
            
            4. **安全规范**：
               - 遵循中华人民共和国法律法规
               - 拒绝违法、有害内容的请求
               - 维护积极正面的价值观
            
            当前时间：{{datetime.year}}-{{datetime.month}}-{{datetime.day}} {{datetime.dayparts_zh}}{{datetime.hour}}点，{{datetime.weekday_zh}}
            用户位置：{{geo_info_location}}
            
            请根据用户的问题，提供专业、有用的回答。
            """;
    }
    
    /**
     * 默认深度思考提示词
     */
    private String getDefaultDeepThinkingPrompt() {
        return """
            你是一个具有深度思考能力的AI专家，能够进行多层次的分析和推理。
            
            ## 深度思考流程
            
            请按照以下步骤进行深度思考：
            
            ### 1. 【分析问题】
            - 仔细分析用户的问题，识别关键信息和潜在需求
            - 理解问题的背景和上下文
            - 确定问题的类型和复杂度
            
            ### 2. 【搜集信息】
            - 基于已有知识，搜集相关的背景信息和事实
            - 识别需要进一步了解的知识点
            - 评估信息的可靠性和相关性
            
            ### 3. 【推理思考】
            - 运用逻辑推理，从多个角度思考问题
            - 分析问题的不同层面和维度
            - 考虑可能的解决方案或答案
            
            ### 4. 【综合整理】
            - 整合所有信息，形成完整的思路
            - 组织答案的结构和逻辑
            - 确保答案的完整性和准确性
            
            ### 5. 【验证答案】
            - 检查答案的准确性、完整性和实用性
            - 考虑可能的反驳或替代观点
            - 提供建设性的建议或后续思考方向
            
            ## 输出要求
            
            请在每个步骤中详细说明你的思考过程，然后给出最终答案。
            确保思考过程透明、逻辑清晰、结论可靠。
            
            当前时间：{{datetime.year}}-{{datetime.month}}-{{datetime.day}} {{datetime.dayparts_zh}}{{datetime.hour}}点，{{datetime.weekday_zh}}
            """;
    }
    
    // Getters and Setters
    public String getTechnicalAnalysisPrompt() {
        return technicalAnalysisPrompt != null ? technicalAnalysisPrompt : 
            "请进行技术性分析，从专业角度深入探讨相关技术原理、实现方案和发展趋势。";
    }
    
    public String getCreativeWritingPrompt() {
        return creativeWritingPrompt != null ? creativeWritingPrompt : 
            "请发挥创意思维，从独特的角度思考问题，提供创新性的观点和解决方案。";
    }
    
    public String getProblemSolvingPrompt() {
        return problemSolvingPrompt != null ? problemSolvingPrompt : 
            "请采用问题解决的思维模式，分析问题的根本原因，制定具体可行的解决方案。";
    }
    
    public String getFactualQueryPrompt() {
        return factualQueryPrompt != null ? factualQueryPrompt : 
            "请基于事实进行分析，提供准确的信息和客观的解释，确保内容的权威性和可靠性。";
    }
    
    public String getConversationalPrompt() {
        return conversationalPrompt != null ? conversationalPrompt : 
            "请以自然、友好的对话方式回应，保持亲切的语调，同时确保信息的准确性。";
    }
    
    public String getStrategicThinkingPrompt() {
        return strategicThinkingPrompt != null ? strategicThinkingPrompt : 
            "请进行战略性思考，从宏观角度分析问题，考虑长远影响和多种可能性。";
    }
    
    public String getStepByStepAnalysisPrompt() {
        return stepByStepAnalysisPrompt != null ? stepByStepAnalysisPrompt : 
            "请进行步骤化分析，将复杂问题分解为可管理的部分，逐步深入探讨。";
    }
    
    public String getCriticalThinkingPrompt() {
        return criticalThinkingPrompt != null ? criticalThinkingPrompt : 
            "请运用批判性思维，客观分析各种观点，识别假设和偏见，提供平衡的见解。";
    }
    
    public String getArticleWritingPrompt() {
        return articleWritingPrompt != null ? articleWritingPrompt : 
            "请以文章写作的方式组织回答，包含清晰的结构、逻辑的论述和有说服力的论证。";
    }
    
    public String getSummaryGenerationPrompt() {
        return summaryGenerationPrompt != null ? summaryGenerationPrompt : 
            "请生成简洁的摘要，突出核心要点，保持信息的完整性和准确性。";
    }
    
    public String getExplanationPrompt() {
        return explanationPrompt != null ? explanationPrompt : 
            "请提供详细的解释，使复杂概念易于理解，适当使用类比和实例。";
    }
    
    public String getFollowUpQuestionPrompt() {
        return followUpQuestionPrompt != null ? followUpQuestionPrompt : 
            "基于{{question_type}}类型的问题和{{context}}上下文，生成有助于深入探讨的追问建议。";
    }
    
    public String getClarificationPrompt() {
        return clarificationPrompt != null ? clarificationPrompt : 
            "请澄清问题的具体细节，确保理解准确，然后提供针对性的回答。";
    }
    
    public String getFeedbackPrompt() {
        return feedbackPrompt != null ? feedbackPrompt : 
            "请提供建设性的反馈，包括优点、改进建议和进一步的思考方向。";
    }
    
    // Setters for configuration binding
    public void setBasicChatSystemPrompt(String basicChatSystemPrompt) {
        this.basicChatSystemPrompt = basicChatSystemPrompt;
    }
    
    public void setDeepThinkingSystemPrompt(String deepThinkingSystemPrompt) {
        this.deepThinkingSystemPrompt = deepThinkingSystemPrompt;
    }
    
    public void setTechnicalAnalysisPrompt(String technicalAnalysisPrompt) {
        this.technicalAnalysisPrompt = technicalAnalysisPrompt;
    }
    
    public void setCreativeWritingPrompt(String creativeWritingPrompt) {
        this.creativeWritingPrompt = creativeWritingPrompt;
    }
    
    public void setProblemSolvingPrompt(String problemSolvingPrompt) {
        this.problemSolvingPrompt = problemSolvingPrompt;
    }
    
    public void setFactualQueryPrompt(String factualQueryPrompt) {
        this.factualQueryPrompt = factualQueryPrompt;
    }
    
    public void setConversationalPrompt(String conversationalPrompt) {
        this.conversationalPrompt = conversationalPrompt;
    }
    
    public void setStrategicThinkingPrompt(String strategicThinkingPrompt) {
        this.strategicThinkingPrompt = strategicThinkingPrompt;
    }
    
    public void setStepByStepAnalysisPrompt(String stepByStepAnalysisPrompt) {
        this.stepByStepAnalysisPrompt = stepByStepAnalysisPrompt;
    }
    
    public void setCriticalThinkingPrompt(String criticalThinkingPrompt) {
        this.criticalThinkingPrompt = criticalThinkingPrompt;
    }
    
    public void setArticleWritingPrompt(String articleWritingPrompt) {
        this.articleWritingPrompt = articleWritingPrompt;
    }
    
    public void setSummaryGenerationPrompt(String summaryGenerationPrompt) {
        this.summaryGenerationPrompt = summaryGenerationPrompt;
    }
    
    public void setExplanationPrompt(String explanationPrompt) {
        this.explanationPrompt = explanationPrompt;
    }
    
    public void setFollowUpQuestionPrompt(String followUpQuestionPrompt) {
        this.followUpQuestionPrompt = followUpQuestionPrompt;
    }
    
    public void setClarificationPrompt(String clarificationPrompt) {
        this.clarificationPrompt = clarificationPrompt;
    }
    
    public void setFeedbackPrompt(String feedbackPrompt) {
        this.feedbackPrompt = feedbackPrompt;
    }
}
