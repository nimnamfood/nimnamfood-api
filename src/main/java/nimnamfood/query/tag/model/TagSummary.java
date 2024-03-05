package nimnamfood.query.tag.model;

import nimnamfood.model.tag.Tag;

import java.util.UUID;

public class TagSummary {
    public UUID id;
    public String name;

    public static TagSummary fromTag(Tag tag) {
        TagSummary summary = new TagSummary();
        summary.id = tag.getId();
        summary.name = tag.getName();
        return summary;
    }
}
