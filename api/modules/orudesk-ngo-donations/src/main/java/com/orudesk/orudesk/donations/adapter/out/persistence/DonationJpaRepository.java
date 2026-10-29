package com.orudesk.orudesk.donations.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

interface DonationJpaRepository extends JpaRepository<DonationJpaEntity, UUID> {

    /** Same day: the one recorded later first, so the list doesn't jump around between two loads. */
    List<DonationJpaEntity> findByDonorIdOrderByReceivedOnDescIdDesc(UUID donorId);

    /** SUM in the database, in NUMERIC: exact, and one number over the wire instead of every row. NULL: none. */
    @Query("SELECT SUM(d.amount) FROM DonationJpaEntity d WHERE d.donorId = :donorId")
    BigDecimal sumByDonor(UUID donorId);
}
