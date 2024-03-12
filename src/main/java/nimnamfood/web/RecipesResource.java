package nimnamfood.web;

import nimnamfood.model.Repositories;
import nimnamfood.model.ingredient.Ingredient;
import nimnamfood.model.ingredient.IngredientUnit;
import nimnamfood.model.recipe.Recipe;
import nimnamfood.model.recipe.RecipeIngredient;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.recipe.FindRecipes;
import nimnamfood.query.recipe.model.RecipeSearchSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@RestController
public class RecipesResource {
    private final QueryBus queryBus;

    @Autowired
    public RecipesResource(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @PostMapping("/recipes")
    public Future<ResponseEntity<Map<String, UUID>>> create() {
        final Ingredient ingredient = new Ingredient("ingredient", IngredientUnit.GRAM);
        final RecipeIngredient recipeIngredient = new RecipeIngredient(ingredient, 120, false);
        final Recipe recipe = Recipe.factory().create("recette", 1, List.of(recipeIngredient), "instructions", List.of(new Tag("végé")));
        Repositories.recipes().add(recipe);
        return CompletableFuture.completedFuture(new ResponseEntity<>(Collections.singletonMap("id", recipe.getId()), HttpStatus.CREATED));
    }

    @GetMapping("/recipes")
    public Future<List<RecipeSearchSummary>> get(@RequestParam(required = false, name = "q") String query) {
        return this.queryBus.dispatch(new FindRecipes(query));
    }
}
