--liquibase formatted sql

--changeset codex:001-create-users-cards-transfers
create table app_user
(
    id uuid not null constraint app_user_pk primary key,
    username varchar(255) not null,
    password varchar(255) not null,
    full_name varchar(255) not null,
    role varchar(32) not null,
    enabled boolean not null default true,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    deleted_at timestamp without time zone,
    deleted_by uuid,
    constraint app_user_role_check check (role in ('ADMIN', 'USER'))
);

create unique index app_user_username_uindex on app_user (lower(username));

create table card
(
    id uuid not null constraint card_pk primary key,
    encrypted_number text not null,
    number_hash varchar(255) not null,
    last_four_digits varchar(4) not null,
    owner_id uuid not null,
    expiration_date date not null,
    status varchar(32) not null,
    balance numeric(19, 2) not null default 0,
    block_requested boolean not null default false,
    block_requested_at timestamp without time zone,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    deleted_at timestamp without time zone,
    deleted_by uuid,
    constraint card_owner_fk foreign key (owner_id) references app_user (id),
    constraint card_status_check check (status in ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    constraint card_balance_non_negative_check check (balance >= 0),
    constraint card_last_four_digits_check check (last_four_digits ~ '^[0-9]{4}$')
);

create unique index card_number_hash_uindex on card (number_hash);
create index card_owner_id_index on card (owner_id);
create index card_status_index on card (status);
create index card_last_four_digits_index on card (last_four_digits);

create table card_transfer
(
    id uuid not null constraint card_transfer_pk primary key,
    from_card_id uuid not null,
    to_card_id uuid not null,
    amount numeric(19, 2) not null,
    description varchar(255),
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    deleted_at timestamp without time zone,
    deleted_by uuid,
    constraint card_transfer_from_card_fk foreign key (from_card_id) references card (id),
    constraint card_transfer_to_card_fk foreign key (to_card_id) references card (id),
    constraint card_transfer_amount_positive_check check (amount > 0),
    constraint card_transfer_different_cards_check check (from_card_id <> to_card_id)
);

create index card_transfer_from_card_id_index on card_transfer (from_card_id);
create index card_transfer_to_card_id_index on card_transfer (to_card_id);
create index card_transfer_created_at_index on card_transfer (created_at);
