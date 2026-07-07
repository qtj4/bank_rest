package com.example.bankcards.exception;

public final class BusinessErrorCode {

    public static final String CARD_ALREADY_BLOCKED = "CARD_ALREADY_BLOCKED";
    public static final String CARD_CREATE_EXPIRED = "CARD_CREATE_EXPIRED";
    public static final String CARD_EXPIRED = "CARD_EXPIRED";
    public static final String CARD_EXPIRATION_IN_PAST = "CARD_EXPIRATION_IN_PAST";
    public static final String CARD_NOT_ACTIVE = "CARD_NOT_ACTIVE";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String INVALID_MONEY_SCALE = "INVALID_MONEY_SCALE";
    public static final String INVALID_TRANSFER_AMOUNT = "INVALID_TRANSFER_AMOUNT";
    public static final String SAME_CARD_TRANSFER = "SAME_CARD_TRANSFER";

    private BusinessErrorCode() {
    }
}
