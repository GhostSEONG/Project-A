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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SaraminCrawler {

    private final parser parser;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern REC_IDX_PATTERN = Pattern.compile("rec_idx=(\\d+)");

    public List<domain> crawl(String keyword) {
        List<domain> result = new ArrayList<>();
        String url = "https://www.saramin.co.kr/zf_user/search/recruit?searchword=" + keyword;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(5000)
                    .get();

            Elements items = doc.select(".item_recruit");

            for (Element item : items) {
                try {
                    Element titleElem = item.selectFirst(".job_tit a");
                    Element corpElem = item.selectFirst(".corp_name a");
                    Element conditionElem = item.selectFirst(".job_condition");
                    Element sectorElem = item.selectFirst(".job_sector");

                    if (titleElem == null || corpElem == null) continue;

                    String title = titleElem.text().trim();
                    String link = "https://www.saramin.co.kr" + titleElem.attr("href");
                    String company = corpElem.text().trim();
                    String conditionText = conditionElem != null ? conditionElem.text() : "";
                    String sectorText = sectorElem != null ? sectorElem.text() : "";

                    // 고유 번호 추출
                    String platformJobId = extractRecIdx(link);
                    if (platformJobId == null) continue;

                    // 파서를 통한 데이터 구조화
                    String career = parser.extractCareer(conditionText);
                    String region = parser.extractRegion(conditionText);
                    String techStacks = parser.extractTechStacks(title + " " + sectorText);

                    result.add(domain.builder()
                            .platform("SARAMIN")
                            .platformJobId(platformJobId)
                            .title(title)
                            .companyName(company)
                            .career(career)
                            .region(region)
                            .techStacks(techStacks)
                            .detailUrl(link)
                            .build());

                } catch (Exception e) {
                    log.warn("파싱 에러: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("크롤링 실패: {}", e.getMessage());
        }

        return result;
    }

    private String extractRecIdx(String url) {
        Matcher matcher = REC_IDX_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}