package com.example.sb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DiscordWebhookService {

    @Value("${discord.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 단순 텍스트 메시지 전송
     */
    public boolean sendMessage(String content) {
        if (!isConfigured()) return false;
        return sendPayload(Map.of("content", content));
    }

    /**
     * 신규 공고 알림용 카드(Embed) 전송
     */
    public boolean sendJobNotification(String companyName, String title, String career, String region, String techStacks, String detailUrl) {
        if (!isConfigured()) return false;

        Map<String, Object> embed = new HashMap<>();
        embed.put("title", "📢 [" + companyName + "] " + title);
        embed.put("url", detailUrl);
        embed.put("color", 0x3498DB); // 파란색

        List<Map<String, Object>> fields = List.of(
                Map.of("name", "경력", "value", career != null && !career.isBlank() ? career : "정보 없음", "inline", true),
                Map.of("name", "지역", "value", region != null && !region.isBlank() ? region : "정보 없음", "inline", true),
                Map.of("name", "기술 스택", "value", techStacks != null && !techStacks.isBlank() ? techStacks : "정보 없음", "inline", false)
        );
        embed.put("fields", fields);

        return sendPayload(Map.of("embeds", List.of(embed)));
    }

    private boolean isConfigured() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Discord Webhook] URL이 설정되지 않았습니다. application.properties를 확인해주세요.");
            return false;
        }
        return true;
    }

    private boolean sendPayload(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);
            return true;
        } catch (Exception e) {
            log.error("[Discord Webhook] 전송 실패: {}", e.getMessage());
            return false;
        }
    }
}
