package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Card> findAll(Specification<Card> specification, Pageable pageable);

    boolean existsByNumberHash(String numberHash);

    @EntityGraph(attributePaths = "owner")
    Optional<Card> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = "owner")
    Optional<Card> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c join fetch c.owner where c.id in :ids and c.deletedAt is null order by c.id")
    List<Card> findAllByIdWithWriteLock(@Param("ids") Collection<UUID> ids);
}
