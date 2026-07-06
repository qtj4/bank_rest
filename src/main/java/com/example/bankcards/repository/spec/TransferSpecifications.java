package com.example.bankcards.repository.spec;

import com.example.bankcards.entity.Transfer;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TransferSpecifications {

    private TransferSpecifications() {
    }

    public static Specification<Transfer> participantUser(UUID userId) {
        return (root, query, builder) -> userId == null
                ? builder.conjunction()
                : builder.or(
                        builder.equal(root.get("fromCard").get("owner").get("id"), userId),
                        builder.equal(root.get("toCard").get("owner").get("id"), userId)
                );
    }

    public static Specification<Transfer> from(LocalDateTime from) {
        return (root, query, builder) -> from == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Transfer> to(LocalDateTime to) {
        return (root, query, builder) -> to == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
