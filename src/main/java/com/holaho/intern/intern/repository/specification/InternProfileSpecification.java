package com.holaho.intern.intern.repository.specification;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.dto.InternSearchCriteria;
import com.holaho.intern.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InternProfileSpecification {

    public static Specification<InternProfile> buildSpecification(InternSearchCriteria criteria, Long tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Multi-tenant isolation (allow tenantId match or fallback null tenantId)
            if (tenantId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.isNull(root.get("tenantId"))
                ));
            }

            if (criteria != null) {
                // University filter
                if (StringUtils.hasText(criteria.getUniversity())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("university")),
                            "%" + criteria.getUniversity().trim().toLowerCase() + "%"
                    ));
                }

                // Major filter
                if (StringUtils.hasText(criteria.getMajor())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("major")),
                            "%" + criteria.getMajor().trim().toLowerCase() + "%"
                    ));
                }

                // Status filter
                if (StringUtils.hasText(criteria.getStatus())) {
                    predicates.add(cb.equal(root.get("status"), criteria.getStatus().trim()));
                }

                // Keyword search (fullName, email, studentCode)
                if (StringUtils.hasText(criteria.getKeyword())) {
                    String kw = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                    Join<InternProfile, User> userJoin = root.join("user", JoinType.LEFT);

                    Predicate nameMatch = cb.like(cb.lower(userJoin.get("fullName")), kw);
                    Predicate emailMatch = cb.like(cb.lower(userJoin.get("email")), kw);
                    Predicate codeMatch = cb.like(cb.lower(root.get("studentCode")), kw);

                    predicates.add(cb.or(nameMatch, emailMatch, codeMatch));
                }

                // Exclude busy interns
                if (Boolean.TRUE.equals(criteria.getExcludeBusy())) {
                    predicates.add(cb.isNull(root.get("mentor")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
