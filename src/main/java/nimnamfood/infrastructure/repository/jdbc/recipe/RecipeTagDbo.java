package nimnamfood.infrastructure.repository.jdbc.recipe;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("recipe_tags")
public class RecipeTagDbo {
    @Id
    UUID tagId;

    public UUID getTagId() {
        return tagId;
    }

    public void setTagId(UUID tagId) {
        this.tagId = tagId;
    }
}
