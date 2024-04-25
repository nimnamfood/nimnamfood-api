alter table view_part_recipe_search_tags rename to view_part_recipe_tags;

create table view_part_recipe_ingredients (
    id uuid primary key,
    name text not null
);

insert into view_part_recipe_ingredients (id, name)
select id, name from ingredients;

create table view_recipes (
    id uuid primary key,
    name text not null,
    illustration json null,
    portions_count smallint not null,
    instructions text not null,
    ingredients jsonb not null,
    tags jsonb not null
);

insert into view_recipes (id, name, illustration, portions_count, instructions, ingredients, tags)
select
    r.id,
    r.name,
    case when r.illustration_id is not null then json_build_object('id', r.illustration_id, 'url', 'https://firebasestorage.googleapis.com/v0/b/${storage-bucket}/o/live%2Frecipes%2F' || r.illustration_id::text || '.webp?alt=media&token=' || r.illustration_id::text) else null end,
    r.portions_count,
    r.instructions,
    coalesce(
        (select jsonb_agg(jsonb_build_object('id', i.id, 'name', i.name, 'quantity', ri.quantity, 'unit', ri.unit, 'quantity_fixed', ri.quantity_fixed))
        from recipe_ingredients ri
            left join ingredients i on ri.ingredient_id = i.id
        where ri.recipe_id = r.id),
        '[]'::jsonb
    ),
    coalesce(
        (select jsonb_agg(t.*)
        from recipe_tags rt
            left join tags t on rt.tag_id = t.id
        where rt.recipe_id = r.id),
        '[]'::jsonb
    )
from recipes r;