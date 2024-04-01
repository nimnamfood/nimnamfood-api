package nimnamfood.query.recipe.model;

import nimnamfood.query.tag.model.TagSummary;

import java.util.Set;
import java.util.UUID;

public class RecipeSearchSummary {
    public UUID id;
    public String name;
    public Set<TagSummary> tags;
}
