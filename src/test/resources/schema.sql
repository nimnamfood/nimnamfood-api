create extension if not exists "unaccent";

drop table if exists far;
create table far
(
    id   text primary key,
    name text not null
);

drop table if exists tags;
create table tags
(
    id   uuid primary key,
    name text not null,
    unique (name)
);

drop table if exists ingredients;
create table ingredients
(
    id   uuid primary key,
    name text not null,
    unit text not null,
    unique (name)
);

drop table if exists recipe_tags;
create table recipe_tags
(
    recipe_id uuid not null,
    tag_id    uuid not null
);

drop table if exists recipe_ingredients;
create table recipe_ingredients
(
    id            uuid primary key,
    recipe_id     uuid    not null,
    ingredient_id uuid    not null,
    quantity      numeric not null,
    unit          text    not null
);

drop table if exists recipes cascade;
create table recipes
(
    id                 uuid primary key,
    name               text      not null,
    illustration_id    uuid      null,
    portions_count     smallint  not null,
    instructions       text      not null,
    creation_date_time timestamp not null
);

drop table if exists plans cascade;
create table plans
(
    id         uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null
);

drop table if exists meals;
create table meals
(
    id         uuid primary key,
    plan_id    uuid    not null references plans (id) on delete cascade,
    meal_index integer not null,
    recipe_id  uuid
);


drop table if exists view_tags;
create table view_tags
(
    id   uuid primary key,
    name text not null
);

drop table if exists view_ingredients;
create table view_ingredients
(
    id   uuid primary key,
    name text not null,
    unit text not null
);

drop table if exists view_part_recipe_tags;
create table view_part_recipe_tags
(
    id   uuid primary key,
    name text not null
);

drop table if exists view_recipe_search;
create table view_recipe_search
(
    id                 uuid primary key,
    name               text      not null,
    illustration_url   text      null,
    creation_date_time timestamp not null,
    tags               jsonb     not null
);

drop table if exists view_part_recipe_ingredients;
create table view_part_recipe_ingredients
(
    id   uuid primary key,
    name text not null
);

drop table if exists view_recipes;
create table view_recipes
(
    id             uuid primary key,
    name           text     not null,
    illustration   jsonb    null,
    portions_count smallint not null,
    instructions   text     not null,
    ingredients    jsonb    not null,
    tags           jsonb    not null
);

drop table if exists view_plans;
create table view_plans
(
    id    uuid primary key,
    meals jsonb not null
);
