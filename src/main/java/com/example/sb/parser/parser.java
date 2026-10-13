package com.example.sb.parser;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class parser {

    private static final List<String> TECH_DICTIONARY = List.of(
            "Java", "Spring Boot", "Spring", "Python", "React", "Vue",
            "Node.js", "MySQL", "JPA", "Docker", "AWS", "Kotlin"
    );

    private static final List<String> REGIONS = List.of(
            "서울", "경기", "인천", "판교", "대전", "대구", "부산", "광주"
    );

    // 1. 기술 스택 추출
    public String extractTechStacks(String text) {
        if (text == null) return "";
        Set<String> matched = new LinkedHashSet<>();
        String upperText = text.toUpperCase();

        for (String tech : TECH_DICTIONARY) {
            if (upperText.contains(tech.toUpperCase())) {
                matched.add(tech);
            }
        }
        return String.join(", ", matched);
    }

    // 2. 경력 조건 판별
    public String extractCareer(String conditionText) {
        if (conditionText == null) return "경력무관";
        if (conditionText.contains("신입") && conditionText.contains("경력")) return "신입/경력";
        if (conditionText.contains("신입")) return "신입";
        if (conditionText.contains("경력")) return "경력";
        return "경력무관";
    }

    // 3. 지역 추출
    public String extractRegion(String conditionText) {
        if (conditionText == null) return "기타";
        for (String region : REGIONS) {
            if (conditionText.contains(region)) return region;
        }
        return "기타";
    }
}