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
    private final DiscordWebhookService discordWebhookService; // 1. 웹훅 서비스 주입 추가

    /**
     * 크롤링 후 중복 검증하여 DB 저장 + 디스코드 알림
     */
    @Transactional
    public int crawlAndSave(String keyword) {
        List<domain> crawled = saraminCrawler.crawl(keyword);
        int savedCount = 0;

        for (domain post : crawled) {
            if (!jobPostingRepository.existsByPlatformAndPlatformJobId(post.getPlatform(), post.getPlatformJobId())) {
                jobPostingRepository.save(post);
                savedCount++;

                // 2. 신규 공고 발견 시 디스코드 카드로 전송
                discordWebhookService.sendJobNotification(
                        post.getCompanyName(),
                        post.getTitle(),
                        post.getCareer(),
                        post.getRegion(),
                        post.getTechStacks(),
                        post.getDetailUrl()
                );
            }
        }

        // 3. 크롤링 종료 후 전체 결과 요약 메시지 전송
        if (savedCount > 0) {
            discordWebhookService.sendMessage("🎉 [" + keyword + "] 크롤링 완료: 총 " + savedCount + "건의 신규 공고가 등록되었습니다.");
        }

        return savedCount;
    }

    @Transactional(readOnly = true)
    public List<domain> searchJobs(List<String> stacks, String career, String platform) {
        return jobPostingRepository.findAll().stream()
                .filter(post -> platform == null || platform.isBlank() || post.getPlatform().equalsIgnoreCase(platform))
                .filter(post -> career == null || career.isBlank() || post.getCareer().contains(career) || "경력무관".equals(post.getCareer()))
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
