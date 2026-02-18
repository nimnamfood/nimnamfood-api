alter table recipe_ingredients drop column quantity_fixed;

update view_recipes
set ingredients = (
    select coalesce(jsonb_agg(ingredient - 'quantity_fixed'), '[]'::jsonb)
    from jsonb_array_elements(ingredients) as ingredient
);
