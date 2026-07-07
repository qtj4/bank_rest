package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransferRepository extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {

    @Override
    @EntityGraph(attributePaths = {"fromCard", "fromCard.owner", "toCard", "toCard.owner"})
    Page<Transfer> findAll(Specification<Transfer> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"fromCard", "fromCard.owner", "toCard", "toCard.owner"})
    Optional<Transfer> findByIdAndDeletedAtIsNull(UUID id);
}
