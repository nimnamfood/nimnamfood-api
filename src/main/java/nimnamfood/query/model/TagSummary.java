package nimnamfood.query.model;

public class TagSummary {
    public String name;

    public static TagSummary withName(String name) {
        TagSummary summary = new TagSummary();
        summary.name = name;
        return summary;
    }
}
