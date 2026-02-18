package nimnamfood.query.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.ObjectMapperFactory;
import nimnamfood.query.recipe.model.RecipeIllustrationSummaryInspector;
import nimnamfood.query.recipe.model.RecipeIngredientSummaryPartInspector;
import nimnamfood.query.recipe.model.RecipeSummaryInspector;
import nimnamfood.query.recipe.model.RecipeTagSummaryPartInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipesViewTestHelper {
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

    public void insertIngredients(Ingredient... ingredients) {
        this.template.batchUpdate(
                "insert into view_part_recipe_ingredients values (:id, :name)",
                Arrays.stream(ingredients).map(i -> Map.of(
                        "id", i.getId(),
                        "name", i.getName()
                )).toArray(Map[]::new)
        );
    }

    public Optional<RecipeIngredientSummaryPartInspector> findIngredient(UUID id) {
        return this.client
                .sql("select * from view_part_recipe_ingredients where id = :id")
                .param("id", id)
                .query((resultSet, rowNum) -> new RecipeIngredientSummaryPartInspector(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name")
                ))
                .optional();
    }

    public void insertRecipes(Map<UUID, String> ingredientNames, Recipe... recipes) {
        final Map<UUID, String> ingredientsJson = Arrays.stream(recipes)
                .map(r -> {
                    final List<Map<String, String>> ingredientMaps = r.getIngredients().stream()
                            .map(ingredient -> Map.of(
                                    "id", ingredient.ingredientId().toString(),
                                    "name", ingredientNames.get(ingredient.ingredientId()),
                                    "quantity", Float.toString(ingredient.quantity()),
                                    "unit", ingredient.unit().toString()
                            ))
                            .toList();
                    try {
                        final String json = mapper.writeValueAsString(ingredientMaps);
                        return Map.entry(r.getId(), json);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        final Map<UUID, String> illustrationsJson = Arrays.stream(recipes)
                .filter(r -> r.getIllustrationId() != null)
                .map(r -> {
                    final Map<String, String> illustrationMap = Map.of("id", r.getIllustrationId().toString(), "url", "url:" + r.getIllustrationId());
                    try {
                        final String json = mapper.writeValueAsString(illustrationMap);
                        return Map.entry(r.getId(), json);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        final Set<Recipe> recipesWithTags = Arrays.stream(recipes).filter(r -> !r.getTagIds().isEmpty()).collect(Collectors.toSet());
        final Set<Recipe> recipesWithoutTags = Sets.difference(Sets.newHashSet(recipes), recipesWithTags);

        if (!recipesWithTags.isEmpty()) {
            recipesWithTags.stream().forEach(r -> {
                this.template.update("""
                                with selected_tags as (select * from view_part_recipe_tags where id in (:tagIds))
                                insert into view_recipes (id, name, illustration, portions_count, instructions, ingredients, tags)
                                select :id, :name, :illustration::json, :portionsCount, :instructions, :ingredients::jsonb, jsonb_agg(selected_tags.*)
                                from selected_tags
                                """,
                        new HashMap() {{
                            put("id", r.getId());
                            put("name", r.getName());
                            put("illustration", r.getIllustrationId() != null ? illustrationsJson.get(r.getId()) : null);
                            put("portionsCount", r.getPortionsCount());
                            put("instructions", r.getInstructions());
                            put("ingredients", ingredientsJson.get(r.getId()));
                            put("tagIds", r.getTagIds());
                        }}
                );
            });
        }
        if (!recipesWithoutTags.isEmpty()) {
            this.template.batchUpdate("""
                            insert into view_recipes (id, name, illustration, portions_count, instructions, ingredients, tags)
                            values(:id, :name, :illustration::json, :portionsCount, :instructions, :ingredients::jsonb, '[]'::jsonb)
                            """,
                    recipesWithoutTags.stream().map(r -> new HashMap() {{
                        put("id", r.getId());
                        put("name", r.getName());
                        put("illustration", r.getIllustrationId() != null ? illustrationsJson.get(r.getIllustrationId()) : null);
                        put("portionsCount", r.getPortionsCount());
                        put("instructions", r.getInstructions());
                        put("ingredients", ingredientsJson.get(r.getId()));
                    }}).toArray(Map[]::new)
            );
        }
    }

    public RecipeSummaryInspector findRecipe(UUID id) {
        return this.client
                .sql("select * from view_recipes where id = :id")
                .param("id", id)
                .query(resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }

                    try {
                        final String illustrationJson = resultSet.getString("illustration");
                        return new RecipeSummaryInspector(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("name"),
                                illustrationJson != null ? mapper.readValue(illustrationJson, RecipeIllustrationSummaryInspector.class) : null,
                                resultSet.getInt("portions_count"),
                                resultSet.getString("instructions"),
                                mapper.readValue(resultSet.getString("ingredients"), new TypeReference<>() {
                                }),
                                mapper.readValue(resultSet.getString("tags"), new TypeReference<>() {
                                })
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
