-- ShelfTap schema. SPEC.md 6.1 plus the auth tables. IDs are UUIDs; card and tag IDs are 16 uppercase hex.

create table member (
    id                   uuid primary key,
    full_name            varchar(100) not null,
    email                varchar(254) not null,
    date_of_birth        date         not null,
    cccd                 varchar(12)  not null,
    phone                varchar(20)  not null,
    member_type          varchar(10)  not null check (member_type in ('STUDENT', 'STAFF', 'PUBLIC')),
    status               varchar(10)  not null check (status in ('ACTIVE', 'SUSPENDED')),
    password_hash        varchar(255) not null,
    must_change_password boolean      not null,
    history_enabled      boolean      not null,
    created_at           timestamptz  not null
);
create unique index member_email_uq on member (lower(email));
create unique index member_cccd_uq on member (cccd);

create table staff (
    id            uuid primary key,
    username      varchar(50)  not null,
    password_hash varchar(255) not null,
    role          varchar(10)  not null check (role in ('LIBRARIAN', 'ADMIN')),
    active        boolean      not null,
    created_at    timestamptz  not null
);
create unique index staff_username_uq on staff (lower(username));

create table card (
    card_id             varchar(16) primary key check (card_id ~ '^[0-9A-F]{16}$'),
    member_id           uuid        not null references member (id),
    public_key_modulus  bytea,
    public_key_exponent bytea,
    status              varchar(10) not null check (status in ('ACTIVE', 'BLOCKED', 'LOST', 'REVOKED')),
    issued_at           timestamptz not null
);
create index card_member_ix on card (member_id);

create table title (
    id       uuid primary key,
    isbn     varchar(17)  not null unique,
    title    varchar(300) not null,
    authors  text[]       not null,
    subjects text[]       not null,
    cover_url text
);

create table copy (
    id       uuid primary key,
    title_id uuid        not null references title (id),
    barcode  varchar(40) not null unique,
    tag_id   varchar(16) not null unique check (tag_id ~ '^[0-9A-F]{16}$'),
    shelf    varchar(30),
    status   varchar(10) not null check (status in ('LENDABLE', 'WITHDRAWN', 'LOST'))
);
create index copy_title_ix on copy (title_id);

-- member_id becomes null when a returned loan is anonymised (Decree 13/2023).
create table loan (
    id          uuid primary key,
    copy_id     uuid        not null references copy (id),
    member_id   uuid references member (id),
    out_at      timestamptz not null,
    due_at      timestamptz not null,
    returned_at timestamptz,
    renewals    integer     not null default 0
);
-- Database backstop for the row lock: a copy can never have two open loans.
create unique index loan_one_open_per_copy on loan (copy_id) where returned_at is null;
create index loan_member_ix on loan (member_id);

-- seq is the queue order: placed_at has one-second resolution, so two reservations can tie.
create table reservation (
    id           uuid primary key,
    seq          bigint generated always as identity,
    title_id     uuid        not null references title (id),
    member_id    uuid        not null references member (id),
    placed_at    timestamptz not null,
    ready_at     timestamptz,
    held_copy_id uuid references copy (id),
    status       varchar(10) not null check (status in ('WAITING', 'READY', 'FULFILLED', 'CANCELLED', 'EXPIRED'))
);
create unique index reservation_one_open_per_member_title on reservation (title_id, member_id)
    where status in ('WAITING', 'READY');
create index reservation_held_copy_ix on reservation (held_copy_id) where status = 'READY';

create table fine (
    id          uuid primary key,
    loan_id     uuid references loan (id),
    member_id   uuid        not null references member (id),
    amount_vnd  integer     not null check (amount_vnd > 0),
    assessed_at timestamptz not null,
    paid_at     timestamptz
);
create index fine_unpaid_ix on fine (member_id) where paid_at is null;

create table refresh_token (
    id           uuid primary key,
    family_id    uuid         not null,
    subject_type varchar(10)  not null check (subject_type in ('MEMBER', 'STAFF')),
    subject_id   uuid         not null,
    token_hash   varchar(64)  not null unique,
    device_id    uuid         not null,
    method       varchar(10)  not null check (method in ('PWD', 'CARD')),
    card_id      varchar(16),
    issued_at    timestamptz  not null,
    expires_at   timestamptz  not null,
    used_at      timestamptz,
    revoked_at   timestamptz
);
create index refresh_token_family_ix on refresh_token (family_id);
create index refresh_token_subject_ix on refresh_token (subject_id);
create index refresh_token_card_ix on refresh_token (card_id) where card_id is not null;

create table card_challenge (
    id         uuid primary key,
    card_id    varchar(16) not null references card (card_id),
    nonce      varchar(44) not null,
    expires_at timestamptz not null,
    used_at    timestamptz
);
create unique index card_challenge_nonce_uq on card_challenge (card_id, nonce);

-- Keyed by SHA-256 of the login name, so an unknown email is counted exactly like a known one.
create table login_attempt (
    id       uuid primary key,
    key_hash varchar(64)  not null,
    at       timestamptz  not null
);
create index login_attempt_key_ix on login_attempt (key_hash, at);

-- No foreign keys: the log must outlive and never block the rows it describes.
create table audit_log (
    id         bigserial primary key,
    at         timestamptz  not null,
    actor_type varchar(10)  not null,
    actor_id   varchar(64),
    action     varchar(40)  not null,
    target     varchar(64),
    outcome    varchar(60)  not null,
    ip         varchar(45),
    device_id  varchar(36),
    integrity  varchar(60)
);
create index audit_log_at_ix on audit_log (at);

-- Append-only: rows cannot be changed or removed through SQL row operations.
create function audit_log_immutable() returns trigger as $$
begin
    raise exception 'audit_log is append-only';
end;
$$ language plpgsql;

create trigger audit_log_no_change before update or delete on audit_log
    for each row execute function audit_log_immutable();
