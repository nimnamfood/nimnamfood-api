create table plans
(
    id         uuid primary key,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table meals
(
    id         uuid primary key,
    plan_id    uuid    not null references plans (id) on delete cascade,
    meal_index integer not null,
    recipe_id  uuid    not null,

    constraint uk_plan_index unique (plan_id, meal_index)
);

create table view_plans
(
    id    uuid primary key,
    meals jsonb not null
);

