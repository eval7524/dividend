package com.example.dividend.scraper;

import com.example.dividend.exception.impl.FailedScrapingDividendException;
import com.example.dividend.model.Company;
import com.example.dividend.model.Dividend;
import com.example.dividend.model.ScrapedResult;
import com.example.dividend.model.constants.Month;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class YahooFinanceScraper implements Scraper{

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history/?frequency=1mo&period1=%d&period2=%d";

    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";

    private static final long START_TIME = 86400;

    @Override
    public ScrapedResult scrap(Company company) {
        log.info("회사 [{}] 배당금 스크래핑 시작", company.getName());
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);

        try {
            long now = System.currentTimeMillis() / 1000;


            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);

            Connection connection = Jsoup.connect(url);
            Document document = connection.get();

            Elements parsingDivs = document.select("table.yf-1jecxey.noDl.hideOnPrint");
            if (parsingDivs.isEmpty()) {
                log.warn("회사 [{}] 배당금 테이블을 찾을수 없습니다. ", company.getName());
                return scrapResult;
            }
            Element tableElement = parsingDivs.get(0);

            Elements children = tableElement.children();
            //Jsoup Element.children()은 Elements 객체를 반환하며, null을 반환하지 않음 -> 자식이 없으면 그냥 빈 Elements를 줌 -> 널 체크는 불필요 하다고 함
            if(children.size() < 2) {
                //get(1)은 두번째 요소를 가져오기에 children.size() >= 2가 되어야 안전하다 -> children.size() < 2 인 경우 는 경고 로그를 남기고 빈 결과 반환
                log.warn("회사 [{}] 배당금 테이블 구조가 예상과 다릅니다. Children Size = {}", company.getName(), (children == null ? "null" : children.size()));
                return scrapResult;
            }
            Element tbody = children.get(1);

            List<Dividend> dividends = new ArrayList<>();
            for (Element e : tbody.children()) {
                String txt = e.text();
                if (!txt.endsWith("Dividend")) {
                    continue;
                }

                String[] split = txt.split(" ");
                int month = Month.strToNumber(split[0]);
                int day = Integer.valueOf(split[1].replace(",", ""));
                int year = Integer.parseInt(split[2]);
                String dividend = split[3];

                if (month < 0) {
                    log.error("unexpected month value : {}" , split[0]);
                    throw new RuntimeException("Unexpected Month enum value -> " + split[0]);
                }

                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));
//                System.out.println(year + "/" + month + "/" + day + " -> " + dividend);

            }
            scrapResult.setDividendEntities(dividends);
        } catch (IOException e) {
            //TODO
            log.error("회사 [{}] 배당금 정보 스크래핑에 실패했습니다. " , company.getTicker());
            throw new FailedScrapingDividendException(company.getTicker());
        }
        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url).get();
            Element titleEle = document.select("h1.yf-4vbjci").first();
            String title = titleEle.text().trim();
            title = title.replaceAll("\\s+\\([^)]*\\)$", "").trim();
            // abc - def - xyz -> def를 가져옴 이후 앞뒤의 공백을 삭제

            return new Company(ticker, title);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
