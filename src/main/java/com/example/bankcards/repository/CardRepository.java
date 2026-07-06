package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    boolean existsByNumberHash(String numberHash);

    boolean existsByOwnerId(UUID ownerId);

    Optional<Card> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Card> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Card> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c join fetch c.owner where c.id in :ids and c.deletedAt is null order by c.id")
    List<Card> findAllByIdWithWriteLock(@Param("ids") Collection<UUID> ids);
}
