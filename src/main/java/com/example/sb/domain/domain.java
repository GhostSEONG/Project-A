package com.example.sb.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_job", columnNames = {"platform", "platformJobId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platform;         // SARAMIN, WORK24

    @Column(nullable = false)
    private String platformJobId;    // 고유 번호 (중복 방지)

    @Column(nullable = false)
    private String title;            // 공고 제목

    @Column(nullable = false)
    private String companyName;      // 회사명

    private String career;           // 신입, 경력, 경력무관

    private String region;           // 서울, 경기 등

    private String techStacks;       // "Java, Spring Boot" (콤마 구분)

    @Column(length = 1000)
    private String detailUrl;        // 공고 링크

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}