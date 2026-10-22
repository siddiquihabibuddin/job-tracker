package com.jobtracker.stats.service;

import com.jobtracker.stats.api.dto.AveragePeriodDto;

import java.time.LocalDate;
import java.time.YearMonth;

/** Pure date math for calendar-based averages; kept free of Spring/JDBC so it is unit-testable. */
final class AverageCalculator {

    record Result(double daily, double weekly, double monthly) {
        static final Result ZERO = new Result(0, 0, 0);
    }

    private AverageCalculator() {}

    /**
     * Averages of {@code total} over the inclusive range [start, end].
     * A partial month counts as elapsedDays / daysInMonth, so an in-progress
     * period is pro-rated rather than treated as complete.
     */
    static Result compute(long total, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return Result.ZERO;
        long days = end.toEpochDay() - start.toEpochDay() + 1;

        double months = 0;
        for (YearMonth ym = YearMonth.from(start); !ym.isAfter(YearMonth.from(end)); ym = ym.plusMonths(1)) {
            LocalDate from = ym.atDay(1).isBefore(start) ? start : ym.atDay(1);
            LocalDate to = ym.atEndOfMonth().isAfter(end) ? end : ym.atEndOfMonth();
            long elapsed = to.toEpochDay() - from.toEpochDay() + 1;
            months += (double) elapsed / ym.lengthOfMonth();
        }

        return new Result(
                round1(total / (double) days),
                round1(total / (days / 7.0)),
                round1(total / months));
    }

    /**
     * Row for one whole calendar period [periodStart, periodEnd]. The part of the period
     * that is still in the future is ignored: a period starting after {@code today} is
     * flagged future (zeros); one that ends after {@code today} is flagged partial and
     * averaged over the days elapsed so far.
     */
    static AveragePeriodDto period(String label, int periodNum, long total,
                                   LocalDate periodStart, LocalDate periodEnd, LocalDate today) {
        if (periodStart.isAfter(today)) {
            return new AveragePeriodDto(label, periodNum, total, 0, 0, 0, false, true);
        }
        boolean partial = periodEnd.isAfter(today);
        Result r = compute(total, periodStart, partial ? today : periodEnd);
        return new AveragePeriodDto(label, periodNum, total, r.daily(), r.weekly(), r.monthly(), partial, false);
    }

    static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
