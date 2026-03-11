create table view_plan_search
(
    id         uuid primary key,
    created_at timestamp not null
);

insert into view_plan_search (id, created_at)
select id, created_at
from plans;