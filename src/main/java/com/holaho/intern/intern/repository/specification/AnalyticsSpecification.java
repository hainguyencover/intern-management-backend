package com.holaho.intern.intern.repository.specification;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.dto.request.AnalyticsFilterRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsSpecification {

    public static Specification<InternProfile> buildSpecification(AnalyticsFilterRequest filter, Long tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Multi-tenant filter
            if (tenantId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.isNull(root.get("tenantId"))
                ));
            }

            if (filter != null) {
                // Date range filters (created_at or start_date)
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"),
                            filter.getFromDate().atStartOfDay()
                    ));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"),
                            filter.getToDate().atTime(LocalTime.MAX)
                    ));
                }

                // University / School filter
                if (StringUtils.hasText(filter.getUniversity())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("university")),
                            "%" + filter.getUniversity().trim().toLowerCase() + "%"
                    ));
                }

                // Major filter
                if (StringUtils.hasText(filter.getMajor())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("major")),
                            "%" + filter.getMajor().trim().toLowerCase() + "%"
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
