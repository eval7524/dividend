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

            Element tbody = tableElement.children().get(1);
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
