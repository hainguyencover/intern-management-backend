package com.example.backend.repository.spec;

import com.example.backend.entity.InternProfile;
import com.example.backend.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InternSpecifications {

    public static Specification<InternProfile> filter(String q, String university, String major) {
        return (root, query, cb) -> {
            // join user để search name/email/phone
            Join<InternProfile, User> user = root.join("user");

            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.trim().isEmpty()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("fullName")), like),
                        cb.like(cb.lower(user.get("email")), like),
                        cb.like(cb.lower(user.get("phone")), like)
                ));
            }

            if (university != null && !university.trim().isEmpty()) {
                String like = "%" + university.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("university")), like));
            }

            if (major != null && !major.trim().isEmpty()) {
                String like = "%" + major.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("major")), like));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
