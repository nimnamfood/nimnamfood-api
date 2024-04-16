create table view_ingredients (
    id uuid primary key,
    name text not null,
    unit text not null
);

insert into view_ingredients (id, name, unit)
select id, name, unit from ingredients;