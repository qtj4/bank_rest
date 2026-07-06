package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransferRepository extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {

    boolean existsByFromCardIdOrToCardId(UUID fromCardId, UUID toCardId);
}
