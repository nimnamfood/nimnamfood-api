create table view_tags (
    id uuid primary key,
    name text not null
);

insert into view_tags (id, name)
select id, name from tags;