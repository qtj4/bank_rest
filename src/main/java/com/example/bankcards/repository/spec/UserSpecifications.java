package com.example.bankcards.repository.spec;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> search(String search) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(search)) {
                return builder.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("username")), pattern),
                    builder.like(builder.lower(root.get("fullName")), pattern)
            );
        };
    }

    public static Specification<User> role(Role role) {
        return (root, query, builder) -> role == null
                ? builder.conjunction()
                : builder.equal(root.get("role"), role);
    }

    public static Specification<User> enabled(Boolean enabled) {
        return (root, query, builder) -> enabled == null
                ? builder.conjunction()
                : builder.equal(root.get("enabled"), enabled);
    }
}
