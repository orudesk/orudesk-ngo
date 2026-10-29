package com.orudesk.orudesk.donations.application.service;

import com.orudesk.orudesk.donations.application.port.in.DonationReportsQuery;
import com.orudesk.orudesk.donations.application.port.in.DonationSummary;
import com.orudesk.orudesk.donations.application.port.in.Form10BDRow;
import com.orudesk.orudesk.donations.application.port.out.DonationTotals;
import com.orudesk.orudesk.donations.application.port.out.OrganisationDetails;
import com.orudesk.orudesk.donations.domain.Amount;
import com.orudesk.orudesk.donations.domain.FinancialYear;
import com.orudesk.orudesk.donors.application.port.in.DonorForReceipt;
import com.orudesk.orudesk.donors.application.port.in.DonorReceiptQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class DonationReportService implements DonationReportsQuery {

    private final DonationTotals totals;
    private final DonorReceiptQuery donors;
    private final OrganisationDetails organisation;

    DonationReportService(DonationTotals totals, DonorReceiptQuery donors, OrganisationDetails organisation) {
        this.totals = totals;
        this.donors = donors;
        this.organisation = organisation;
    }

    @Override
    public DonationSummary summary(String financialYear) {
        FinancialYear year = FinancialYear.parse(financialYear);
        List<DonationTotals.Total> months = totals.byMonth(year);
        List<DonationSummary.Line> byMonth = lines(months);
        List<DonationSummary.Line> byMode = lines(totals.byMode(year));
        BigDecimal total = months.stream().map(DonationTotals.Total::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = byMonth.stream().mapToLong(DonationSummary.Line::count).sum();
        return new DonationSummary(year.label(), total.setScale(2).toPlainString(),
                total.signum() == 0 ? "₹0.00" : new Amount(total).formatted(), count, totals.cancelledCount(year),
                byMonth, byMode);
    }

    /**
     * One line per donor and mode, for donors with a PAN who asked for tax receipts, sorted by name. The donors
     * module is asked once per donor: a few thousand small reads once a year is fine; a batch lookup on the donors
     * API is the change to make when an NGO has tens of thousands.
     */
    @Override
    public List<Form10BDRow> form10BD(String financialYear) {
        if (organisation.registration80G() == null) {
            throw new IllegalArgumentException("Form 10BD is for 80G-registered organisations; no 80G registration is configured");
        }
        FinancialYear year = FinancialYear.parse(financialYear);
        Map<UUID, Optional<DonorForReceipt>> known = new HashMap<>();
        record Line(DonorForReceipt donor, boolean cash, BigDecimal amount) {
        }
        List<Line> lines = new ArrayList<>();
        for (DonationTotals.DonorTotal total : totals.deductibleByDonor(year)) {
            known.computeIfAbsent(total.donorId(), donors::findForReceipt)
                    .filter(donor -> donor.wantsTaxReceipt() && donor.pan() != null)
                    .ifPresent(donor -> lines.add(new Line(donor, total.cash(), total.amount())));
        }
        lines.sort(Comparator.comparing((Line l) -> l.donor().fullName()).thenComparing(Line::cash));
        List<Form10BDRow> rows = new ArrayList<>();
        for (Line line : lines) {
            rows.add(new Form10BDRow(rows.size() + 1, line.donor().pan(), line.donor().fullName(), "Others",
                    line.cash() ? "Cash" : "Electronic modes including account payee cheque/draft",
                    line.amount().setScale(2).toPlainString()));
        }
        return rows;
    }

    private static List<DonationSummary.Line> lines(List<DonationTotals.Total> totals) {
        return totals.stream()
                .map(t -> new DonationSummary.Line(t.key(), t.amount().setScale(2).toPlainString(), t.count()))
                .toList();
    }
}
