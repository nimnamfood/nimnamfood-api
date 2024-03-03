package nimnamfood.query;

import nimnamfood.model.Repositories;
import nimnamfood.query.model.TagSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;

@Component()
public class FindAllTagsHandler implements QueryHandler<FindAllTags, List<TagSummary>> {
    @Override
    public List<TagSummary> execute(FindAllTags query) {
        return Repositories.tags().getAll()
                .stream()
                .map(TagSummary::fromTag)
                .toList();
    }
}
