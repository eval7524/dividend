package com.example.dividend.exception.impl;

import com.example.dividend.exception.AbstractException;

public class FailedScrapingDividendException extends AbstractException {

    private final String ticker;

    public FailedScrapingDividendException(String ticker) {
        this.ticker = ticker;
    }

    @Override
    public int getStatusCode() {
        return 500; // Internal Server Error
    }

    @Override
    public String getMessage() {
        return "배당금 스크래핑 실패 - 티커: " + ticker;
    }
}
