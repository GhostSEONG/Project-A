package com.example.sb.crawler;

import com.example.sb.domain.domain;
import com.example.sb.parser.parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class work25Crawler {

    private final parser parser;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern WANTED_AUTH_NO_PATTERN = Pattern.compile("wantedAuthNo=([A-Za-z0-9]+)");

    public List<domain> crawl(String keyword) {
        List<domain> result = new ArrayList<>();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://www.work24.go.kr/wk/a/b/1200/retriveDtlEmpSrchList.do?srcKeyword=" + encodedKeyword + "&keyword=" + encodedKeyword;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.work24.go.kr/")
                    .timeout(10000)
                    .maxBodySize(0)
                    .get();

            // 고용24 채용정보 상세검색 목록 행 선택 (tr[id^=list] 또는 테이블 본문 행)
            Elements items = doc.select("tr[id^=list], table.box_table tbody tr");

            for (Element item : items) {
                try {
                    // 1. 공고 제목 및 상세 링크 요소 탐색
                    Element titleElem = item.selectFirst("a.t3_sb, .t3_sb a, a[href*=empDetailAuthView], a[href*=wantedAuthNo]");
                    // 2. 회사명 요소 탐색
                    Element corpElem = item.selectFirst(".cp_name, a.cp_name");

                    if (titleElem == null || corpElem == null) continue;

                    String title = titleElem.text().trim();
                    String company = corpElem.text().trim();
                    if (title.isEmpty() || company.isEmpty()) continue;

                    // 3. 상세 페이지 링크 및 고유 공고 번호(wantedAuthNo) 추출
                    String rawHref = titleElem.attr("href");
                    String onclick = titleElem.attr("onclick");
                    String platformJobId = extractWantedAuthNo(rawHref + " " + onclick);

                    String link;
                    if (rawHref.startsWith("http")) {
                        link = rawHref;
                    } else if (rawHref.startsWith("/")) {
                        link = "https://www.work24.go.kr" + rawHref;
                    } else if (platformJobId != null) {
                        link = "https://www.work24.go.kr/wk/a/b/1500/empDetailAuthView.do?wantedAuthNo=" + platformJobId;
                    } else {
                        continue;
                    }

                    // wantedAuthNo가 URL 파라미터에 없을 경우 제목+회사명 해시값으로 대체
                    if (platformJobId == null) {
                        platformJobId = String.valueOf(Math.abs((company + "_" + title).hashCode()));
                    }

                    // 4. 경력, 지역, 기술스택 텍스트 추출 및 파싱
                    String rowText = item.text();
                    String career = parser.extractCareer(rowText);
                    String region = parser.extractRegion(rowText);
                    String techStacks = parser.extractTechStacks(title + " " + rowText);

                    result.add(domain.builder()
                            .platform("WORK24")
                            .platformJobId(platformJobId)
                            .title(title)
                            .companyName(company)
                            .career(career)
                            .region(region)
                            .techStacks(techStacks)
                            .detailUrl(link)
                            .build());

                } catch (Exception e) {
                    log.warn("[WORK24] 파싱 에러: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[WORK24] 크롤링 실패: {}", e.getMessage());
        }

        return result;
    }

    private String extractWantedAuthNo(String text) {
        if (text == null) return null;
        Matcher matcher = WANTED_AUTH_NO_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
