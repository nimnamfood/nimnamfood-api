package nimnamfood.query.tag;

import nimnamfood.model.Repositories;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;

@Component()
public class FindTagsHandler implements QueryHandler<FindTags, List<TagSummary>> {
    @Override
    public List<TagSummary> execute(FindTags query) {
        return Repositories.tags()
                .getAll(
                        tag -> query.query == null || QueryNormalizer.partialMatch(tag.getName(), query.query),
                        query.limit(), query.skip())
                .stream()
                .map(TagSummary::fromTag)
                .toList();
    }
}
