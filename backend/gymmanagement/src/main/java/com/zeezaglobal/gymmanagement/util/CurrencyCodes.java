package com.zeezaglobal.gymmanagement.util;

import com.zeezaglobal.gymmanagement.exception.BadRequestException;

import java.util.Currency;

public final class CurrencyCodes {

    private CurrencyCodes() {
    }

    /** Validates an ISO 4217 currency code (e.g. "USD", "EUR", "INR") and returns it upper-cased. */
    public static String validate(String code) {
        try {
            return Currency.getInstance(code.toUpperCase()).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid currency code: " + code);
        }
    }
}
