package nimnamfood.query.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nimnamfood.query.recipe.model.IllustrationSummary;
import nimnamfood.query.recipe.model.RecipeIngredientSummary;
import nimnamfood.query.recipe.model.RecipeSummary;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import vtertre.ddd.MissingAggregateRootException;
import vtertre.query.QueryHandlerJdbc;

import java.util.Set;
import java.util.UUID;

@Component
public class GetRecipeHandler extends QueryHandlerJdbc<GetRecipe, RecipeSummary> {
    private final ObjectMapper mapper;
    private final TypeReference<Set<RecipeIngredientSummary>> ingredientsTypeReference = new TypeReference<>() {
    };
    private final TypeReference<Set<TagSummary>> tagsTypeReference = new TypeReference<>() {
    };

    @Autowired
    public GetRecipeHandler(@Qualifier("Jsonb") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RecipeSummary execute(GetRecipe query, NamedParameterJdbcTemplate template) {
        final String sqlQuery = """
                SELECT id, name, illustration, portions_count, instructions, tags, ingredients
                FROM view_recipes
                WHERE id = :recipeId
                """;

        return template.query(sqlQuery, new MapSqlParameterSource("recipeId", query.id), resultSet -> {
            if (!resultSet.next()) {
                throw new MissingAggregateRootException(query.id);
            }

            final String illustrationJson = resultSet.getString("illustration");

            try {
                return new RecipeSummary(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("name"),
                        illustrationJson != null ? mapper.readValue(illustrationJson, IllustrationSummary.class) : null,
                        resultSet.getInt("portions_count"),
                        resultSet.getString("instructions"),
                        mapper.readValue(resultSet.getString("ingredients"), ingredientsTypeReference),
                        mapper.readValue(resultSet.getString("tags"), tagsTypeReference)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
