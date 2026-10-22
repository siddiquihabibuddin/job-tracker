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

    @Test
    void periodFlagsInProgressMonthAndProRatesIt() {
        // October 2026 with today = Oct 22: 22 days elapsed of 31
        var p = AverageCalculator.period("Oct", 10, 22,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 22));
        assertEquals(true, p.partial());
        assertEquals(false, p.future());
        assertEquals(1.0, p.dailyAverage());
        assertEquals(7.0, p.weeklyAverage());
    }

    @Test
    void periodForCompletedMonthUsesFullMonth() {
        // March 2026 (31 days): 62 applications -> 2.0/day
        var p = AverageCalculator.period("Mar", 3, 62,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 10, 22));
        assertEquals(false, p.partial());
        assertEquals(2.0, p.dailyAverage());
    }

    @Test
    void periodAfterTodayIsFutureWithZeros() {
        var p = AverageCalculator.period("Dec", 12, 0,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 10, 22));
        assertEquals(true, p.future());
        assertEquals(0.0, p.dailyAverage());
    }
}
