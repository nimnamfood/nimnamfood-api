package nimnamfood.web;

import nimnamfood.query.FindAllCriteria;
import nimnamfood.query.model.CriteriaSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vtertre.query.QueryBus;

import java.util.List;
import java.util.concurrent.Future;

@RestController
public class CriteriaResource {
    private final QueryBus queryBus;

    @Autowired
    public CriteriaResource(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/criteria")
    public Future<List<CriteriaSummary>> getAll() {
        return this.queryBus.send(new FindAllCriteria());
    }
}
