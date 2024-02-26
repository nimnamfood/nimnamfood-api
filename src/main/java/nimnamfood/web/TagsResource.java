package nimnamfood.web;

import nimnamfood.query.FindAllTags;
import nimnamfood.query.model.TagSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.List;
import java.util.concurrent.Future;

@RestController
public class TagsResource {
    private final QueryBus queryBus;

    @Autowired
    public TagsResource(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/tags")
    public Future<List<TagSummary>> getAll() {
        return this.queryBus.send(new FindAllTags());
    }
}
