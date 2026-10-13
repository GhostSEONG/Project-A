package com.example.sb.repository;

import com.example.sb.domain.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface repository extends JpaRepository<domain, Long> {
    // 중복 체크용 메서드
    boolean existsByPlatformAndPlatformJobId(String platform, String platformJobId);
}