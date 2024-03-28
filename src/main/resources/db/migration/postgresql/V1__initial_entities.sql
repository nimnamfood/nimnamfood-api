create table tags (
    id uuid primary key,
    name text not null
);

create table ingredients (
    id uuid primary key,
    name text not null,
    unit text not null
);

create table recipe_tags (
    recipe_id uuid not null,
    tag_id uuid not null
);

create table recipe_ingredients (
    recipe_id uuid not null,
    ingredient_id uuid not null,
    quantity numeric not null,
    unit text not null,
    quantity_fixed boolean not null
);

create table recipes (
    id uuid primary key,
    name text not null,
    portions_count smallint not null,
    instructions text not null
);