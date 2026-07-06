package com.example.bankcards.repository.spec;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> ownerId(UUID ownerId) {
        return (root, query, builder) -> ownerId == null
                ? builder.conjunction()
                : builder.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Card> status(CardStatus status) {
        return (root, query, builder) -> status == null
                ? builder.conjunction()
                : builder.equal(root.get("status"), status);
    }

    public static Specification<Card> lastFourDigits(String lastFourDigits) {
        return (root, query, builder) -> !StringUtils.hasText(lastFourDigits)
                ? builder.conjunction()
                : builder.equal(root.get("lastFourDigits"), lastFourDigits.trim());
    }
}
