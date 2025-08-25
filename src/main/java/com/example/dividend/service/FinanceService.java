package com.example.dividend.service;

import com.example.dividend.exception.impl.NoCompanyException;
import com.example.dividend.model.Company;
import com.example.dividend.model.Dividend;
import com.example.dividend.model.ScrapedResult;
import com.example.dividend.model.constants.CacheKey;
import com.example.dividend.persist.CompanyRepository;
import com.example.dividend.persist.DividendRepository;
import com.example.dividend.persist.entity.CompanyEntity;
import com.example.dividend.persist.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;



    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getdividendByCompanyName(String companyName) {
        log.info("search Company -> {}", companyName);
        // 1. 회사명 기준 회사 정보 조회
        var optionalCompany = this.companyRepository.findByName(companyName);
        if(optionalCompany.isEmpty()) {
            log.warn("회사 조회 실패 : {}", companyName);
            throw new NoCompanyException();
        }

        CompanyEntity company = optionalCompany.get();
        // 2. 조회된 회사 ID로 배당금 정보 조회
        List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(company.getId());


        List<Dividend> dividends = dividendEntities.stream()
                        .map(e -> new  Dividend(e.getDate(), e.getDividend()))
                        .collect(Collectors.toList());


        log.info("회사 조회 성공 : {}", companyName);
        return new ScrapedResult(new Company(company.getTicker(), company.getName()), dividends);
    }

}
