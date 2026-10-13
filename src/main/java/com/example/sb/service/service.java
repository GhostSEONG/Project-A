package com.example.sb.service;

import com.example.sb.crawler.SaraminCrawler;
import com.example.sb.domain.domain;
import com.example.sb.repository.repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class service {

    private final SaraminCrawler saraminCrawler;
    private final repository jobPostingRepository;

    /**
     * 크롤링 후 중복 검증하여 DB 저장
     */
    @Transactional
    public int crawlAndSave(String keyword) {
        List<domain> crawled = saraminCrawler.crawl(keyword);
        int savedCount = 0;

        for (domain post : crawled) {
            if (!jobPostingRepository.existsByPlatformAndPlatformJobId(post.getPlatform(), post.getPlatformJobId())) {
                jobPostingRepository.save(post);
                savedCount++;
            }
        }
        return savedCount;
    }

    /**
     * 다나와 스타일 조건 필터링 검색
     */
    @Transactional(readOnly = true)
    public List<domain> searchJobs(List<String> stacks, String career, String platform) {
        return jobPostingRepository.findAll().stream()
                // 1. 플랫폼 필터
                .filter(post -> platform == null || platform.isBlank() || post.getPlatform().equalsIgnoreCase(platform))
                // 2. 경력 필터
                .filter(post -> career == null || career.isBlank() || post.getCareer().contains(career) || "경력무관".equals(post.getCareer()))
                // 3. 기술 스택 필터 (체크된 스택 중 하나라도 포함되어 있는지)
                .filter(post -> {
                    if (stacks == null || stacks.isEmpty()) return true;
                    if (post.getTechStacks() == null || post.getTechStacks().isBlank()) return false;
                    return stacks.stream().anyMatch(s -> post.getTechStacks().toLowerCase().contains(s.toLowerCase()));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<domain> getAllJobs() {
        return jobPostingRepository.findAll();
    }
}