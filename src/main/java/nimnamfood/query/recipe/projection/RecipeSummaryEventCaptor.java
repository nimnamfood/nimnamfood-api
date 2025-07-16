package nimnamfood.query.recipe.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.query.recipe.model.RecipeIngredientSummary;
import nimnamfood.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import vtertre.ddd.event.DomainEvent;
import vtertre.ddd.event.EventCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class RecipeSummaryEventCaptor<TEvent extends DomainEvent> implements EventCaptor<TEvent> {
    private JdbcClient client;
    private RecipeService recipeService;
    private ObjectMapper mapper;

    @Override
    public void execute(TEvent event) {
        this.execute(event, this.client);
    }

    protected abstract void execute(TEvent event, JdbcClient client);

    protected String ingredientsJsonString(Set<RecipeIngredient> ingredients) {
        if (ingredients.isEmpty()) {
            return "[]";
        }

        final Map<UUID, String> ingredientNames = this.client
                .sql("SELECT id, name FROM view_part_recipe_ingredients WHERE id IN (:ingredientIds)")
                .param("ingredientIds", ingredients.stream().map(RecipeIngredient::ingredientId).collect(Collectors.toSet()))
                .query(resultSet -> {
                    final Map<UUID, String> results = Maps.newHashMap();
                    while (resultSet.next()) {
                        results.put(resultSet.getObject("id", UUID.class), resultSet.getString("name"));
                    }
                    return results;
                });


        final List<RecipeIngredientSummary> ingredientMaps = ingredients.stream()
                .map(ingredient -> new RecipeIngredientSummary(ingredient.ingredientId(),
                        ingredientNames.get(ingredient.ingredientId()), ingredient.quantity(), ingredient.unit(),
                        ingredient.quantityFixed()))
                .toList();

        try {
            return mapper.writeValueAsString(ingredientMaps);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String illustrationJsonString(UUID illustrationId) {
        final String illustrationUrl = this.recipeService.illustrationUrl(illustrationId);
        final Map<String, String> illustrationMap = Map.of("id", illustrationId.toString(), "url", illustrationUrl);

        try {
            return mapper.writeValueAsString(illustrationMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    public void setClient(JdbcClient client) {
        this.client = client;
    }

    @Autowired
    public void setRecipeService(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Autowired
    public void setMapper(@Qualifier("Jsonb") ObjectMapper mapper) {
        this.mapper = mapper;
    }
}
