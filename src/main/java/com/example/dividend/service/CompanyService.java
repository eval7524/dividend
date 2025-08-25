package com.example.dividend.service;

import com.example.dividend.exception.impl.NoCompanyException;
import com.example.dividend.model.Company;
import com.example.dividend.model.ScrapedResult;
import com.example.dividend.persist.CompanyRepository;
import com.example.dividend.persist.DividendRepository;
import com.example.dividend.persist.entity.CompanyEntity;
import com.example.dividend.persist.entity.DividendEntity;
import com.example.dividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie;
    private final Scraper yahooFinanceScraper;
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        log.info("Ticker 저장 요청 : {}", ticker);
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if (exists) {
            log.warn("이미 존재하는 Ticker 이므로 저장 불가 : {}", ticker);
            throw new RuntimeException("already exists ticker -> " + ticker);
        }
        log.info("Ticker 저장 완료 : {}", ticker);
        return this.storeCompanyAndDividend(ticker);
    }

    private Company storeCompanyAndDividend(String ticker) {
        log.debug("ticker 기반 회사 스크래핑 시작 : {}", ticker);
        //ticker 기반 회사 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if(ObjectUtils.isEmpty(company)) {
            log.error("회사 스크래핑 실패 : {}", ticker);
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }
        //해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        log.debug("배당금 스크래핑 시작 : {}", company.getName());
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        //스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrapedResult.getDividendEntities().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());
        log.info("DB에 회사 및 배당금 정보 저장 : {}", company.getName());
        this.dividendRepository.saveAll(dividendEntities);
        return company;
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        log.debug("회사 조회 완료");
        return this.companyRepository.findAll(pageable);
    }

    public void addAutoCompleteKeyword(String keyword) {
        log.debug("AutoComplete 키워드 추가 : {}", keyword);
        this.trie.put(keyword, null);
    }

    public List<String> autoComplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream()
                .collect(Collectors.toList());
    }

    void deleteAutoCompleteKeyword(String keyword) {
        log.debug("AutoComplete 키워드 삭제 : {}", keyword);
        this.trie.remove(keyword);
    }


    public List<String> getCompanyNamesByKeyword(String keyword) {
        log.debug("회사 조회 요청, keyword={}", keyword);
        Pageable limit = PageRequest.of(0,10, Sort.by("name").ascending());
        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        log.debug("조회 완료, 반환 회사 수={}", companyEntities.getNumberOfElements());
        return companyEntities.stream()
                .map(CompanyEntity::getName)
                .collect(Collectors.toList());
    }

    @Transactional
    public String deleteCompany(String ticker) {
        log.info("삭제 진행 중 : {}", ticker);
        var optionalCompany = this.companyRepository.findByTicker(ticker);
        if (optionalCompany.isEmpty()) {
            log.warn("삭제 실패, 존재하지 않는 Ticker : {}", ticker);
            throw new NoCompanyException();
        }
        var company = optionalCompany.get();

        this.dividendRepository.deleteAllByCompanyId(company.getId());
        this.companyRepository.delete(company);

        log.info("삭제 완료 : {}", ticker);
        this.deleteAutoCompleteKeyword(company.getName());
        return company.getName();
    }
}
