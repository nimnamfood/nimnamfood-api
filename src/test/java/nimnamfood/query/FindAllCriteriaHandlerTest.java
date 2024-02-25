package nimnamfood.query;

import nimnamfood.infrastructure.repository.memory.WithMemoryRepositories;
import nimnamfood.model.Repositories;
import nimnamfood.query.model.CriteriaSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({WithMemoryRepositories.class})
public class FindAllCriteriaHandlerTest {

    @Test
    void returnsAnEmptyListOfCriteria() {
        FindAllCriteriaHandler handler = new FindAllCriteriaHandler();

        List<CriteriaSummary> result = handler.execute(new FindAllCriteria());

        assertThat(result).hasSize(0);
    }

    @Test
    void returnsANonEmptyListOfCriteria() {
        FindAllCriteriaHandler handler = new FindAllCriteriaHandler();
        Repositories.criteria().add("Rapide");
        Repositories.criteria().add("Végé");

        List<CriteriaSummary> result = handler.execute(new FindAllCriteria());

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().name).isEqualTo("Rapide");
        assertThat(result.getLast().name).isEqualTo("Végé");
    }
}
