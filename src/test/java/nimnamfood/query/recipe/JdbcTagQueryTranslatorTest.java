package nimnamfood.query.recipe;

import nimnamfood.web.converter.DoesNotHaveTag;
import nimnamfood.web.converter.HasOneOfTags;
import nimnamfood.web.converter.HasTag;
import nimnamfood.web.converter.TagFilterQuery;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import vtertre.ddd.Tuple;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcTagQueryTranslatorTest {
    @Test
    void translatesAQueryWithoutFiltersToNull() {
        TagFilterQuery query = new TagFilterQuery(Collections.emptySet());

        Tuple<String, Map<String, Set<String>>> result = new JdbcTagQueryTranslator("").toQueryValue(query);

        assertThat(result).isNull();
    }

    @Test
    void translatesQueryWithInclusiveFiltersOnly() {
        TagFilterQuery query = new TagFilterQuery(Set.of(new HasTag("1"), new HasTag("2")));

        Tuple<String, Map<String, Set<String>>> result = new JdbcTagQueryTranslator("tags").toQueryValue(query);

        assertThat(result._1).isEqualTo("jsonb_exists_all(tags, array[:hasTagIds])");
        assertThat(result._2.get("hasTagIds")).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void translatesQueryWithExclusiveFiltersOnly() {
        TagFilterQuery query = new TagFilterQuery(Set.of(new DoesNotHaveTag("1"), new DoesNotHaveTag("2")));

        Tuple<String, Map<String, Set<String>>> result = new JdbcTagQueryTranslator("tags").toQueryValue(query);

        assertThat(result._1).isEqualTo("NOT (jsonb_exists_any(tags, array[:doesNotHaveTagIds]))");
        assertThat(result._2.get("doesNotHaveTagIds")).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void translatesQueryWithOneOrFilterOnly() {
        TagFilterQuery query = new TagFilterQuery(Set.of(new HasOneOfTags(Set.of("1", "2"))));

        Tuple<String, Map<String, Set<String>>> result = new JdbcTagQueryTranslator("tags").toQueryValue(query);

        assertThat(result._1).isEqualTo("jsonb_exists_any(tags, array[:hasOneOfTagIds0])");
        assertThat(result._2.get("hasOneOfTagIds0")).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void translatesQueryWithOrFiltersOnly() {
        TagFilterQuery query = new TagFilterQuery(Set.of(new HasOneOfTags(Set.of("1", "2")), new HasOneOfTags(Set.of("3", "4"))));

        Tuple<String, Map<String, Set<String>>> result = new JdbcTagQueryTranslator("tags").toQueryValue(query);

        assertThat(result._1).isEqualTo("jsonb_exists_any(tags, array[:hasOneOfTagIds0]) AND jsonb_exists_any(tags, array[:hasOneOfTagIds1])");
        assertThat(result._2).hasSize(2);
        assertThat(result._2).containsKeys("hasOneOfTagIds0", "hasOneOfTagIds1");
        assertThat(result._2.values()).anySatisfy(set -> assertThat(set).containsExactlyInAnyOrder("1", "2"));
        assertThat(result._2.values()).anySatisfy(set -> assertThat(set).containsExactlyInAnyOrder("3", "4"));
    }

    @Test
    void translatesQueryWithMultipleFilters() {
        TagFilterQuery query = new TagFilterQuery(Set.of(new HasTag("0"), new HasOneOfTags(Set.of("1", "2")), new DoesNotHaveTag("3"), new HasTag("4"), new DoesNotHaveTag("5"), new HasOneOfTags(Set.of("6", "7", "8"))));

        Tuple<String, Map<String, Set<String>>> result = new JdbcTagQueryTranslator("tags").toQueryValue(query);

        assertThat(result._1).isEqualTo("jsonb_exists_all(tags, array[:hasTagIds]) AND NOT (jsonb_exists_any(tags, array[:doesNotHaveTagIds])) AND jsonb_exists_any(tags, array[:hasOneOfTagIds0]) AND jsonb_exists_any(tags, array[:hasOneOfTagIds1])");
        assertThat(result._2.get("hasTagIds")).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactlyInAnyOrder("0", "4");
        assertThat(result._2.get("doesNotHaveTagIds")).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactlyInAnyOrder("5", "3");
        assertThat(result._2.entrySet())
                .filteredOn(e -> e.getKey().equals("hasOneOfTagIds0") || e.getKey().equals("hasOneOfTagIds1"))
                .hasSize(2)
                .anySatisfy(entry -> assertThat(entry.getValue()).containsExactlyInAnyOrder("1", "2"))
                .anySatisfy(entry -> assertThat(entry.getValue()).containsExactlyInAnyOrder("6", "7", "8"));
    }
}