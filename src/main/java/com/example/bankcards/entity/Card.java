package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "card",
        indexes = {
                @Index(name = "card_owner_id_index", columnList = "owner_id"),
                @Index(name = "card_status_index", columnList = "status"),
                @Index(name = "card_last_four_digits_index", columnList = "last_four_digits")
        }
)
public class Card extends BaseEntity {

    @Column(name = "encrypted_number", nullable = false, columnDefinition = "text")
    private String encryptedNumber;

    @Column(name = "number_hash", nullable = false, unique = true)
    private String numberHash;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "block_requested", nullable = false)
    private boolean blockRequested = false;

    @Column(name = "block_requested_at")
    private LocalDateTime blockRequestedAt;

    public String getEncryptedNumber() {
        return encryptedNumber;
    }

    public void setEncryptedNumber(String encryptedNumber) {
        this.encryptedNumber = encryptedNumber;
    }

    public String getNumberHash() {
        return numberHash;
    }

    public void setNumberHash(String numberHash) {
        this.numberHash = numberHash;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isBlockRequested() {
        return blockRequested;
    }

    public void setBlockRequested(boolean blockRequested) {
        this.blockRequested = blockRequested;
    }

    public LocalDateTime getBlockRequestedAt() {
        return blockRequestedAt;
    }

    public void setBlockRequestedAt(LocalDateTime blockRequestedAt) {
        this.blockRequestedAt = blockRequestedAt;
    }
}
