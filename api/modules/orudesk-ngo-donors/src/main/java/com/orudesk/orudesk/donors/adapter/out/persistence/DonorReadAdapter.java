package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.donors.application.port.in.DonorCount;
import com.orudesk.orudesk.donors.application.port.in.DonorDetails;
import com.orudesk.orudesk.donors.application.port.in.DonorListItem;
import com.orudesk.orudesk.donors.application.port.out.DonorPosition;
import com.orudesk.orudesk.donors.application.port.out.ReadDonorsPort;
import com.orudesk.orudesk.donors.domain.DonorId;
import com.orudesk.orudesk.donors.domain.DonorType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reads donors from the summary view. HQL only, never native SQL: Hibernate adds the tenant filter to HQL.
 * Like party's search, the query is built from fixed pieces; the user's input is always a bound parameter.
 */
@Component
class DonorReadAdapter implements ReadDonorsPort {

    private final EntityManager entityManager;

    DonorReadAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<DonorDetails> findDetails(DonorId id) {
        return Optional.ofNullable(entityManager.find(DonorSummaryJpaEntity.class, id.value()))
                .map(row -> new DonorDetails(row.getId(), row.getPersonId(), row.getFirstName(), row.getLastName(),
                        row.getPhone(), row.getEmail(), row.getDonorType(), row.getPanMasked(),
                        row.isWantsTaxReceipt(), row.isReceivesUpdates(), row.getRegisteredOn()));
    }

    @Override
    public List<DonorListItem> search(String text, DonorType type, DonorPosition after, int limit) {
        StringBuilder hql = new StringBuilder("from DonorSummaryJpaEntity d where 1 = 1");
        if (text != null) {
            hql.append(" and d.searchText like :pattern escape '\\'");
        }
        if (type != null) {
            hql.append(" and d.donorType = :type");
        }
        if (after != null) {
            hql.append(" and (d.lastName, d.firstName, d.id) > (:lastName, :firstName, :id)");
        }
        hql.append(" order by d.lastName, d.firstName, d.id");

        TypedQuery<DonorSummaryJpaEntity> query = entityManager.createQuery(hql.toString(), DonorSummaryJpaEntity.class);
        if (text != null) {
            query.setParameter("pattern", "%" + escapeLike(text) + "%");
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        if (after != null) {
            query.setParameter("lastName", after.lastName());
            query.setParameter("firstName", after.firstName());
            query.setParameter("id", after.id());
        }
        return query.setMaxResults(limit).getResultList().stream()
                .map(row -> new DonorListItem(row.getId(), row.getFirstName(), row.getLastName(), row.getPhone(),
                        row.getDonorType(), row.isWantsTaxReceipt()))
                .toList();
    }

    /** GROUP BY in the database: it counts thousands of rows; Java only gets one line per type. */
    @Override
    public List<DonorCount> countByType() {
        return entityManager.createQuery("""
                        select new com.orudesk.orudesk.donors.application.port.in.DonorCount(
                            d.donorType, count(*), count(*) filter (where d.wantsTaxReceipt = true))
                        from DonorSummaryJpaEntity d
                        group by d.donorType
                        """, DonorCount.class)
                .getResultList().stream()
                .sorted(Comparator.comparing(DonorCount::type))   // enum order, not alphabetical
                .toList();
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
