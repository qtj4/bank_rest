package com.example.bankcards.security;

import org.springframework.stereotype.Component;

@Component("permission")
public class Permission {

    public static final String USER_VIEW = "bank:user:view";
    public static final String USER_CREATE = "bank:user:create";
    public static final String USER_UPDATE = "bank:user:update";
    public static final String USER_DELETE = "bank:user:delete";

    public static final String CARD_VIEW_ALL = "bank:card:view_all";
    public static final String CARD_VIEW_OWN = "bank:card:view_own";
    public static final String CARD_CREATE = "bank:card:create";
    public static final String CARD_UPDATE = "bank:card:update";
    public static final String CARD_DELETE = "bank:card:delete";
    public static final String CARD_BLOCK = "bank:card:block";
    public static final String CARD_ACTIVATE = "bank:card:activate";
    public static final String CARD_BLOCK_REQUEST = "bank:card:block_request";

    public static final String TRANSFER_CREATE = "bank:transfer:create";
    public static final String TRANSFER_VIEW_OWN = "bank:transfer:view_own";
    public static final String TRANSFER_VIEW_ALL = "bank:transfer:view_all";
}
