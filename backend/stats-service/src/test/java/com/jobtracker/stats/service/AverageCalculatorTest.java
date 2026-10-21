package com.jobtracker.stats.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AverageCalculatorTest {

    @Test
    void fullPastYearDividesByDaysWeeksAndTwelveMonths() {
        // 2025: 365 days, 12 whole months
        var r = AverageCalculator.compute(365, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertEquals(1.0, r.daily());
        assertEquals(7.0, r.weekly());
        assertEquals(30.4, r.monthly());
    }

    @Test
    void inProgressMonthIsProRated() {
        // Oct 1-21 2026: 21 days = 3 weeks, 21/31 of a month
        var r = AverageCalculator.compute(21, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21));
        assertEquals(1.0, r.daily());
        assertEquals(7.0, r.weekly());
        assertEquals(31.0, r.monthly());
    }

    @Test
    void partialYearCountsWholeMonthsPlusFraction() {
        // Jan 1 - Feb 14 2026: 45 days; Jan whole + 14/28 of Feb = 1.5 months
        var r = AverageCalculator.compute(45, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 14));
        assertEquals(1.0, r.daily());
        assertEquals(30.0, r.monthly());
    }

    @Test
    void emptyOrFutureRangeReturnsZeros() {
        var r = AverageCalculator.compute(0, LocalDate.of(2027, 1, 1), LocalDate.of(2026, 10, 21));
        assertEquals(0.0, r.daily());
        assertEquals(0.0, r.weekly());
        assertEquals(0.0, r.monthly());
    }

    @Test
    void singleDayPeriod() {
        var r = AverageCalculator.compute(2, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21));
        assertEquals(2.0, r.daily());
        assertEquals(14.0, r.weekly());
    }
}
