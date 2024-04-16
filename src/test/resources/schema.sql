drop table if exists far;
create table far (
    id text primary key,
    name text not null
);

drop table if exists tags;
create table tags (
    id uuid primary key,
    name text not null,
    unique(name)
);

drop table if exists ingredients;
create table ingredients (
    id uuid primary key,
    name text not null,
    unit text not null,
    unique(name)
);

drop table if exists recipe_tags;
create table recipe_tags (
    recipe_id uuid not null,
    tag_id uuid not null
);

drop table if exists recipe_ingredients;
create table recipe_ingredients (
    id uuid primary key,
    recipe_id uuid not null,
    ingredient_id uuid not null,
    quantity numeric not null,
    unit text not null,
    quantity_fixed boolean not null
);

drop table if exists recipes;
create table recipes (
    id uuid primary key,
    name text not null,
    illustration_id uuid null,
    portions_count smallint not null,
    instructions text not null,
    creation_date_time timestamp not null
);

drop table if exists view_tags;
create table view_tags (
    id uuid primary key,
    name text not null
);

drop table if exists view_ingredients;
create table view_ingredients (
    id uuid primary key,
    name text not null,
    unit text not null
);