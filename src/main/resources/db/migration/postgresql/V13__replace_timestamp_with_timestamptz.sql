ALTER TABLE recipes
    ALTER COLUMN creation_date_time
        TYPE timestamptz
        USING creation_date_time AT TIME ZONE 'Europe/Berlin';

ALTER TABLE plans
    ALTER COLUMN created_at
        TYPE timestamptz
        USING created_at AT TIME ZONE 'Europe/Berlin';

ALTER TABLE plans
    ALTER COLUMN updated_at
        TYPE timestamptz
        USING updated_at AT TIME ZONE 'Europe/Berlin';

ALTER TABLE view_recipe_search
    ALTER COLUMN creation_date_time
        TYPE timestamptz
        USING creation_date_time AT TIME ZONE 'Europe/Berlin';

ALTER TABLE view_plan_search
    ALTER COLUMN created_at
        TYPE timestamptz
        USING created_at AT TIME ZONE 'Europe/Berlin';