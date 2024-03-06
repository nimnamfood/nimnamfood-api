package nimnamfood.query.tag;

import nimnamfood.query.tag.model.TagSummary;
import vtertre.query.Query;

import java.util.List;

public class FindTags implements Query<List<TagSummary>> {
    public final String query;

    public FindTags() {
        this.query = null;
    }

    public FindTags(String query) {
        this.query = query;
    }
}
