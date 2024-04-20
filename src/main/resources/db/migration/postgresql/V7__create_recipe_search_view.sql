create table view_part_recipe_search_tags (
    id uuid primary key,
    name text not null
);

insert into view_part_recipe_search_tags (id, name)
select id, name from tags;

create table view_recipe_search (
    id uuid primary key,
    name text not null,
    illustration_url text null,
    creation_date_time timestamp not null,
    tags jsonb not null
);

insert into view_recipe_search (id, name, illustration_url, creation_date_time, tags)
select
    r.id,
    r.name,
    'https://firebasestorage.googleapis.com/v0/b/${storage-bucket}/o/live%2Frecipes%2F' || r.illustration_id::text || '.webp?alt=media&token=' || r.illustration_id::text,
    r.creation_date_time,
    coalesce((select jsonb_agg(t.*) from recipe_tags rt left join tags t on rt.tag_id = t.id where rt.recipe_id = r.id), '[]'::jsonb)
from recipes r;