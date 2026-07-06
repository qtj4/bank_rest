--liquibase formatted sql

--changeset codex:002-insert-default-admin
insert into app_user (id, username, password, full_name, role, enabled, created_at, updated_at)
values (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '$2b$12$eQX0vNnG/kJuQ5NjAXV1Ve9ejYzGvF8GZDcdo/bdqqLnChhYx6D2G',
    'Local Administrator',
    'ADMIN',
    true,
    now(),
    now()
);
