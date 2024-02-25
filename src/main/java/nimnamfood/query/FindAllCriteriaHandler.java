package nimnamfood.query;

import nimnamfood.model.Repositories;
import nimnamfood.query.model.CriteriaSummary;
import org.springframework.stereotype.Component;
import vtertre.query.QueryHandler;

import java.util.List;

@Component()
public class FindAllCriteriaHandler implements QueryHandler<FindAllCriteria, List<CriteriaSummary>> {
    @Override
    public List<CriteriaSummary> execute(FindAllCriteria query) {
        return Repositories.criteria().getAll()
                .stream()
                .map(CriteriaSummary::withName)
                .toList();
    }
}
