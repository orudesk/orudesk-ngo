package com.orudesk.orudesk.donations.adapter.out.persistence;

import com.orudesk.orudesk.donations.application.port.out.DonationTotals;
import com.orudesk.orudesk.donations.domain.FinancialYear;
import com.orudesk.orudesk.donations.domain.TaxDeduction;
import com.orudesk.orudesk.shared.CurrentTenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.List;

/**
 * Reports in SQL (Day 59): GROUP BY in the database returns twelve rows for a year, where loading every donation
 * into Java to add them up would return thousands. Each query names the tenant itself (no Hibernate filter here)
 * and asks for "cancelled_on IS NULL", which lets PostgreSQL use the partial index from V3.
 */
@Component
class JdbcDonationTotals implements DonationTotals {

    private final JdbcTemplate jdbc;
    private final CurrentTenant tenant;

    JdbcDonationTotals(JdbcTemplate jdbc, CurrentTenant tenant) {
        this.jdbc = jdbc;
        this.tenant = tenant;
    }

    /**
     * generate_series makes the twelve months, so a month without donations is a row with zero rather than a
     * missing row the chart has to guess about. LEFT JOIN keeps those empty months.
     */
    @Override
    public List<Total> byMonth(FinancialYear year) {
        return jdbc.query("""
                SELECT to_char(m.month, 'YYYY-MM') AS key, COALESCE(SUM(d.amount), 0) AS amount, COUNT(d.id) AS count
                FROM generate_series(?::date, ?::date, interval '1 month') AS m(month)
                LEFT JOIN ngo_donation d
                       ON d.tenant_id = ? AND d.cancelled_on IS NULL
                      AND d.received_on >= m.month AND d.received_on < m.month + interval '1 month'
                GROUP BY m.month
                ORDER BY m.month
                """, (rs, i) -> new Total(rs.getString("key"), rs.getBigDecimal("amount"), rs.getLong("count")),
                Date.valueOf(year.firstDay()), Date.valueOf(year.lastDay().withDayOfMonth(1)), tenant.require());
    }

    @Override
    public List<Total> byMode(FinancialYear year) {
        return jdbc.query("""
                SELECT mode AS key, SUM(amount) AS amount, COUNT(*) AS count
                FROM ngo_donation
                WHERE tenant_id = ? AND cancelled_on IS NULL AND received_on BETWEEN ? AND ?
                GROUP BY mode
                ORDER BY SUM(amount) DESC
                """, (rs, i) -> new Total(rs.getString("key"), rs.getBigDecimal("amount"), rs.getLong("count")),
                tenant.require(), Date.valueOf(year.firstDay()), Date.valueOf(year.lastDay()));
    }

    @Override
    public long cancelledCount(FinancialYear year) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ngo_donation
                WHERE tenant_id = ? AND cancelled_on IS NOT NULL AND received_on BETWEEN ? AND ?
                """, Long.class, tenant.require(), Date.valueOf(year.firstDay()), Date.valueOf(year.lastDay()));
        return count == null ? 0 : count;
    }

    @Override
    public List<DonorTotal> deductibleByDonor(FinancialYear year) {
        return jdbc.query("""
                SELECT donor_id, mode = 'CASH' AS cash, SUM(amount) AS amount
                FROM ngo_donation
                WHERE tenant_id = ? AND cancelled_on IS NULL AND received_on BETWEEN ? AND ?
                  AND NOT (mode = 'CASH' AND amount > ?)
                GROUP BY donor_id, mode = 'CASH'
                ORDER BY donor_id, cash
                """, (rs, i) -> new DonorTotal(rs.getObject("donor_id", java.util.UUID.class), rs.getBoolean("cash"),
                        rs.getBigDecimal("amount")),
                tenant.require(), Date.valueOf(year.firstDay()), Date.valueOf(year.lastDay()),
                TaxDeduction.CASH_DEDUCTION_LIMIT.value());
    }
}
