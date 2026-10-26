package com.orudesk.orudesk.donors.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface DonorJpaRepository extends JpaRepository<DonorJpaEntity, UUID> {
}
