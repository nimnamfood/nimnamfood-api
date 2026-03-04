package nimnamfood.infrastructure.repository.jdbc.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import nimnamfood.infrastructure.repository.RecipeTagFilterRequirement;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.recipe.RecipeRepository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import vtertre.infrastructure.persistence.jdbc.JdbcRepositoryWithUuid;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class RecipeJdbcRepository extends JdbcRepositoryWithUuid<Recipe, RecipeDbo> implements RecipeRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public RecipeJdbcRepository(RecipeJdbcCrudRepository jdbcCrudRepository, JdbcAggregateTemplate jdbcAggregateTemplate,
                                NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(jdbcCrudRepository, jdbcAggregateTemplate);
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public RecipeDbo toDbo(Recipe recipe) {
        final RecipeDbo dbo = new RecipeDbo();
        dbo.setId(recipe.getId());
        dbo.name = recipe.getName();
        dbo.illustrationId = recipe.getIllustrationId();
        dbo.portionsCount = recipe.getPortionsCount();
        dbo.instructions = recipe.getInstructions();
        dbo.creationDateTime = recipe.getCreationDateTime();
        dbo.ingredients = recipe.getIngredients().stream()
                .map(RecipeJdbcRepository::recipeIngredientDbo).collect(Collectors.toSet());
        dbo.tags = recipe.getTagIds().stream().map(tagId -> {
            final RecipeTagDbo recipeTagDbo = new RecipeTagDbo();
            recipeTagDbo.tagId = tagId;
            return recipeTagDbo;
        }).collect(Collectors.toSet());
        return dbo;
    }

    private static RecipeIngredientDbo recipeIngredientDbo(RecipeIngredient recipeIngredient) {
        final RecipeIngredientDbo recipeIngredientDbo = new RecipeIngredientDbo();
        recipeIngredientDbo.id = recipeIngredient.getId();
        recipeIngredientDbo.ingredientId = recipeIngredient.ingredientId();
        recipeIngredientDbo.quantity = recipeIngredient.quantity();
        recipeIngredientDbo.unit = recipeIngredient.unit();
        return recipeIngredientDbo;
    }

    @Override
    public Iterable<Recipe> getAllById(Set<UUID> recipeIds) {
        return StreamSupport
                .stream(this.jdbcCrudRepository.findAllById(recipeIds).spliterator(), false)
                .map(RecipeDbo::asAggregateRoot)
                .toList();
    }

    @Override
    public Set<UUID> findIdsByTagFilterRequirement(RecipeTagFilterRequirement requirement) {
        final List<String> clauses = Lists.newArrayList();
        final var params = new MapSqlParameterSource();

        if (!requirement.requiredTagsIds().isEmpty()) {
            clauses.add("(SELECT COUNT(DISTINCT rt.tag_id) FROM recipe_tags rt WHERE rt.recipe_id = r.id AND rt.tag_id IN (:required)) = :requiredCount");
            params.addValue("required", requirement.requiredTagsIds());
            params.addValue("requiredCount", requirement.requiredTagsIds().size());
        }

        if (!requirement.excludedTagIds().isEmpty()) {
            clauses.add("NOT EXISTS (SELECT 1 FROM recipe_tags rt WHERE rt.recipe_id = r.id AND rt.tag_id IN (:excluded))");
            params.addValue("excluded", requirement.excludedTagIds());
        }

        if (!requirement.oneOfTagsIdsCombinations().isEmpty()) {
            IntStream.range(0, requirement.oneOfTagsIdsCombinations().size()).forEach(index -> {
                final var paramName = "oneOf" + index;
                clauses.add("EXISTS (SELECT 1 FROM recipe_tags rt WHERE rt.recipe_id = r.id AND rt.tag_id IN (:" + paramName + "))");
                params.addValue(paramName, requirement.oneOfTagsIdsCombinations().get(index));
            });
        }

        final var sql = clauses.isEmpty()
                ? "SELECT r.id FROM recipes r"
                : "SELECT r.id FROM recipes r WHERE " + String.join(" AND ", clauses);

        final var results = this.namedParameterJdbcTemplate.queryForList(sql, params, UUID.class);
        return Sets.newHashSet(results);
    }
}
