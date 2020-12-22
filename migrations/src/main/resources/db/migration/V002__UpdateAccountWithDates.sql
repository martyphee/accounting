alter table account
add column created_at timestamp without time zone;

alter table account
    add column updated_at timestamp without time zone;
