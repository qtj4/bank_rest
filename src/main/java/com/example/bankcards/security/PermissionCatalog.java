package com.example.bankcards.security;

import com.example.bankcards.entity.enums.Role;
import java.util.Set;

public final class PermissionCatalog {

    private static final Set<String> USER_PERMISSIONS = Set.of(
            Permission.CARD_VIEW_OWN,
            Permission.CARD_BLOCK_REQUEST,
            Permission.TRANSFER_CREATE,
            Permission.TRANSFER_VIEW_OWN
    );

    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            Permission.USER_VIEW,
            Permission.USER_CREATE,
            Permission.USER_UPDATE,
            Permission.USER_DELETE,
            Permission.CARD_VIEW_ALL,
            Permission.CARD_VIEW_OWN,
            Permission.CARD_CREATE,
            Permission.CARD_UPDATE,
            Permission.CARD_DELETE,
            Permission.CARD_BLOCK,
            Permission.CARD_ACTIVATE,
            Permission.CARD_BLOCK_REQUEST,
            Permission.TRANSFER_CREATE,
            Permission.TRANSFER_VIEW_OWN,
            Permission.TRANSFER_VIEW_ALL
    );

    private PermissionCatalog() {
    }

    public static Set<String> forRole(Role role) {
        return role == Role.ADMIN ? ADMIN_PERMISSIONS : USER_PERMISSIONS;
    }
}
