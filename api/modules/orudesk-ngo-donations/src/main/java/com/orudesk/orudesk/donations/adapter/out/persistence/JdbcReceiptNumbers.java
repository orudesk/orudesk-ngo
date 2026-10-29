package com.orudesk.orudesk.donations.adapter.out.persistence;

import com.orudesk.orudesk.donations.application.port.out.ReceiptNumbers;
import com.orudesk.orudesk.donations.domain.FinancialYear;
import com.orudesk.orudesk.donations.domain.ReceiptNumber;
import com.orudesk.orudesk.shared.CurrentTenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One statement takes the next number: the first receipt of a year inserts the counter at 1, every later one adds
 * 1 to it. The row stays locked until the caller's transaction ends (see V2__receipts.sql for why that's the point).
 *
 * The SQL is written here, so Hibernate's tenant filter doesn't apply: the tenant comes from CurrentTenant.
 */
@Component
class JdbcReceiptNumbers implements ReceiptNumbers {

    private final JdbcTemplate jdbc;
    private final CurrentTenant tenant;

    JdbcReceiptNumbers(JdbcTemplate jdbc, CurrentTenant tenant) {
        this.jdbc = jdbc;
        this.tenant = tenant;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)   // outside a transaction the lock would end at once
    public ReceiptNumber next(FinancialYear year) {
        Integer number = jdbc.queryForObject("""
                INSERT INTO ngo_receipt_counter (tenant_id, financial_year, last_number) VALUES (?, ?, 1)
                ON CONFLICT (tenant_id, financial_year) DO UPDATE SET last_number = ngo_receipt_counter.last_number + 1
                RETURNING last_number
                """, Integer.class, tenant.require(), year.label());
        return new ReceiptNumber(year, number);
    }
}
