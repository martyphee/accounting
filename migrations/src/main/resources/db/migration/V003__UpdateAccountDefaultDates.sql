alter table account
alter column created_at set default now();

alter table account
    alter column updated_at set default now()
