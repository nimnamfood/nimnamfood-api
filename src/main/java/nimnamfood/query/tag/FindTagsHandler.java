package nimnamfood.query.tag;

import nimnamfood.model.Repositories;
import nimnamfood.model.tag.Tag;
import nimnamfood.query.QueryNormalizer;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;
import java.util.stream.Stream;

@Component()
public class FindTagsHandler implements QueryHandler<FindTags, List<TagSummary>> {
    @Override
    public List<TagSummary> execute(FindTags query) {
        Stream<Tag> sourceStream = Repositories.tags().getAll().stream();
        Stream<Tag> filteredStream = query.query == null ? sourceStream : sourceStream.filter(tag ->
                QueryNormalizer.normalize(tag.getName()).contains(QueryNormalizer.normalize(query.query)));

        return filteredStream
                .map(TagSummary::fromTag)
                .toList();
    }
}
