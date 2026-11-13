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
public class GameJobCrawler {

    private final parser parser;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern GI_NO_PATTERN = Pattern.compile("GI_No=(\\d+)", Pattern.CASE_INSENSITIVE);

    public List<domain> crawl(String keyword) {
        List<domain> result = new ArrayList<>();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://www.gamejob.co.kr/Recruit/joblist?menucode=search&kw=" + encodedKeyword;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.gamejob.co.kr/")
                    .timeout(10000)
                    .maxBodySize(0)
                    .get();

            // 게임잡 채용공고 목록 테이블 행 선택
            Elements items = doc.select("table.tbl_list tbody tr, div.tbl_list tbody tr, tr:has(a[href*=GIB_Read.asp])");

            for (Element item : items) {
                try {
                    // 1. 공고 제목 및 링크 추출 (.tit a 또는 GIB_Read.asp 링크)
                    Element titleLinkElem = item.selectFirst("div.tit a, td.tit a, a[href*=GIB_Read.asp]");
                    if (titleLinkElem == null) continue;

                    Element titleStrong = titleLinkElem.selectFirst("strong");
                    String title = (titleStrong != null ? titleStrong.text() : titleLinkElem.text()).trim();
                    if (title.isEmpty()) continue;

                    // 2. 회사명 추출 (첫 번째 컬럼의 strong/a 또는 기업명 클래스)
                    Element corpElem = item.selectFirst("td:first-child strong, td:first-child a, .corp_name a, .name");
                    String company = corpElem != null ? corpElem.text().trim() : "";
                    if (company.isEmpty()) continue;

                    // 3. 상세 페이지 링크 및 고유 공고 번호(GI_No) 추출
                    String rawHref = titleLinkElem.attr("href").trim();
                    String platformJobId = extractGiNo(rawHref);
                    if (platformJobId == null) continue;

                    String link = rawHref.startsWith("http")
                            ? rawHref
                            : "https://www.gamejob.co.kr" + (rawHref.startsWith("/") ? "" : "/") + rawHref;

                    // 4. 조건(경력/지역) 및 기술스택 파싱
                    Element infoElem = item.selectFirst("p.info, .info, td.tit");
                    String conditionText = infoElem != null ? infoElem.text() : item.text();
                    String rowText = item.text();

                    String career = parser.extractCareer(conditionText);
                    String region = parser.extractRegion(conditionText);
                    String techStacks = parser.extractTechStacks(title + " " + rowText);

                    result.add(domain.builder()
                            .platform("GAMEJOB")
                            .platformJobId(platformJobId)
                            .title(title)
                            .companyName(company)
                            .career(career)
                            .region(region)
                            .techStacks(techStacks)
                            .detailUrl(link)
                            .build());

                } catch (Exception e) {
                    log.warn("[GAMEJOB] 파싱 에러: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[GAMEJOB] 크롤링 실패: {}", e.getMessage());
        }

        return result;
    }

    private String extractGiNo(String url) {
        if (url == null) return null;
        Matcher matcher = GI_NO_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

