package com.example.bankcards;

import java.time.LocalDate;

public final class TestDates {

    public static final LocalDate PAST_EXPIRY = LocalDate.of(2020, 1, 1);
    public static final LocalDate FUTURE_EXPIRY = LocalDate.of(2030, 12, 31);
    public static final LocalDate FAR_FUTURE_EXPIRY = LocalDate.of(2032, 6, 15);

    private TestDates() {
    }
}
