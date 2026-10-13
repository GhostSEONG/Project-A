
package com.example.sb.controller;

import com.example.sb.domain.domain;
import com.example.sb.service.service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class controller {

    private final service jobService;

    /**
     * 1. 다나와 스타일 조건 검색 API
     * 예: /api/jobs/search?stacks=Java,Spring&career=신입&platform=SARAMIN
     */
    @GetMapping("/search")
    public ResponseEntity<List<domain>> searchJobs(
            @RequestParam(required = false) List<String> stacks,
            @RequestParam(required = false) String career,
            @RequestParam(required = false) String platform
    ) {
        List<domain> result = jobService.searchJobs(stacks, career, platform);
        return ResponseEntity.ok(result);
    }

    /**
     * 2. 수동 크롤링 실행 API (테스트 및 수집 버튼용)
     * 예: /api/jobs/crawl?keyword=백엔드
     */
    @PostMapping("/crawl")
    public ResponseEntity<String> triggerCrawl(@RequestParam(defaultValue = "백엔드") String keyword) {
        int count = jobService.crawlAndSave(keyword);
        return ResponseEntity.ok("크롤링 완료: " + count + "건의 신규 공고가 수집되었습니다.");
    }

    /**
     * 3. 전체 공고 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<List<domain>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }
}