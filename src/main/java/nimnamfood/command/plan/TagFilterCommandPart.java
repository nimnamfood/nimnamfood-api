package nimnamfood.command.plan;

import org.hibernate.validator.constraints.UUID;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TagFilterCommandPart {
    public Set<@UUID String> has = Collections.emptySet();

    public Set<@UUID String> doesNotHave = Collections.emptySet();

    public List<Set<@UUID String>> hasOneOf = Collections.emptyList();
}
