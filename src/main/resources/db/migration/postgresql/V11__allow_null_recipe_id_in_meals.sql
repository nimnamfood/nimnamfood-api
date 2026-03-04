alter table meals alter column recipe_id drop not null;
alter table meals drop constraint uk_plan_index;
