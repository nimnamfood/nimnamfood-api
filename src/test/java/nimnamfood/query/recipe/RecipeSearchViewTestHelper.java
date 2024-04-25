package nimnamfood.query.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.ObjectMapperFactory;
import nimnamfood.query.recipe.model.RecipeSearchSummaryInspector;
import nimnamfood.query.recipe.model.RecipeTagSummaryPartInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeSearchViewTestHelper {
    @Autowired
    NamedParameterJdbcTemplate template;
    @Autowired
    JdbcClient client;

    private final ObjectMapper mapper = ObjectMapperFactory.withSnakeCasePropertyNamingStrategy();

    public void insertTags(Tag... tags) {
        this.template.batchUpdate(
                "insert into view_part_recipe_tags values (:id, :name)",
                Arrays.stream(tags).map(i -> Map.of(
                        "id", i.getId(),
                        "name", i.getName()
                )).toArray(Map[]::new)
        );
    }

    public Optional<RecipeTagSummaryPartInspector> findTag(UUID id) {
        return this.client
                .sql("select * from view_part_recipe_tags where id = :id")
                .param("id", id)
                .query((resultSet, rowNum) -> new RecipeTagSummaryPartInspector(resultSet.getObject("id", UUID.class),
                        resultSet.getString("name")))
                .optional();
    }

    public void insertRecipes(Recipe... recipes) {
        final Set<Recipe> recipesWithTags = Arrays.stream(recipes).filter(r -> !r.getTagIds().isEmpty()).collect(Collectors.toSet());
        final Set<Recipe> recipesWithoutTags = Sets.difference(Sets.newHashSet(recipes), recipesWithTags);
        if (!recipesWithTags.isEmpty()) {
            recipesWithTags.stream().forEach(r -> {
                this.template.update("""
                                with selected_tags as (select * from view_part_recipe_tags where id in (:tagIds))
                                insert into view_recipe_search (id, name, illustration_url, creation_date_time, tags)
                                select :id, :name, :illustrationUrl, :creationDateTime, jsonb_agg(selected_tags.*)
                                from selected_tags
                                """,
                        new HashMap() {{
                            put("id", r.getId());
                            put("name", r.getName());
                            put("illustrationUrl", r.getIllustrationId() != null ? "url:" + r.getIllustrationId() : null);
                            put("creationDateTime", r.getCreationDateTime());
                            put("tagIds", r.getTagIds());
                        }}
                );
            });
        }
        if (!recipesWithoutTags.isEmpty()) {
            this.template.batchUpdate("""
                            insert into view_recipe_search (id, name, illustration_url, creation_date_time, tags)
                            values(:id, :name, :illustrationUrl, :creationDateTime, '[]'::jsonb)
                            """,
                    recipesWithoutTags.stream().map(r -> new HashMap() {{
                        put("id", r.getId());
                        put("name", r.getName());
                        put("illustrationUrl", r.getIllustrationId() != null ? "url:" + r.getIllustrationId() : null);
                        put("creationDateTime", r.getCreationDateTime());
                    }}).toArray(Map[]::new)
            );
        }
    }

    public RecipeSearchSummaryInspector findRecipe(UUID id) {
        return this.client
                .sql("select * from view_recipe_search where id = :id")
                .param("id", id)
                .query(resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }

                    try {
                        return new RecipeSearchSummaryInspector(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("name"),
                                resultSet.getString("illustration_url"),
                                resultSet.getObject("creation_date_time", LocalDateTime.class),
                                mapper.readValue(resultSet.getString("tags"), new TypeReference<>() {
                                })
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
