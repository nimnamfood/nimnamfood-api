package nimnamfood.web;

import nimnamfood.command.CreateTagCommand;
import nimnamfood.query.tag.FindTags;
import nimnamfood.query.tag.model.TagSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vtertre.command.CommandBus;
import vtertre.query.QueryBus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@RestController
public class TagsResource {
    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Autowired
    public TagsResource(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @GetMapping("/tags")
    public Future<List<TagSummary>> get(@RequestParam(required = false, name = "q") String query) {
        return this.queryBus.send(new FindTags(query));
    }

    @PostMapping("/tags")
    public Future<Map<String, UUID>> create(@RequestBody CreateTagCommand command) {
        return this.commandBus
                .send(command)
                .thenApply(result -> Collections.singletonMap("id", result));
    }
}
