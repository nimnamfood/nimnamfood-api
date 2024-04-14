alter table recipes
add column creation_date_time timestamp not null default (timezone('utc', now()));