package nimnamfood.web.converter;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringToTagFilterQueryConverterTest {
    @Test
    void canConvertAnEmptyString() {
        StringToTagFilterQueryConverter converter = new StringToTagFilterQueryConverter();

        TagFilterQuery query = converter.convert("");

        assertThat(query.values()).isEmpty();
    }

    @Test
    void canConvertASingleValue() {
        StringToTagFilterQueryConverter converter = new StringToTagFilterQueryConverter();

        TagFilterQuery query = converter.convert("1aE-4");

        assertThat(query.values()).hasSize(1);
        assertThat(query.values()).first().isExactlyInstanceOf(HasTag.class);
        assertThat(query.values()).first().extracting(f -> f.value).isEqualTo("1aE-4");
    }

    @Test
    void canConvertAListOfCommaSeparatedValues() {
        StringToTagFilterQueryConverter converter = new StringToTagFilterQueryConverter();

        TagFilterQuery query = converter.convert("1aE-4,0-Z");

        assertThat(query.values()).hasSize(2);
        assertThat(query.values()).anyMatch(f -> f.value.equals("1aE-4"));
        assertThat(query.values()).anyMatch(f -> f.value.equals("0-Z"));
    }

    @Test
    void canConvertExcludingValues() {
        StringToTagFilterQueryConverter converter = new StringToTagFilterQueryConverter();

        TagFilterQuery query = converter.convert("!0-Z");

        assertThat(query.values()).hasSize(1);
        assertThat(query.values()).first().isExactlyInstanceOf(DoesNotHaveTag.class)
                .extracting(f -> f.value).isEqualTo("0-Z");
    }

    @Test
    void canConvertOrValues() {
        StringToTagFilterQueryConverter converter = new StringToTagFilterQueryConverter();

        TagFilterQuery query = converter.convert("(1e-A|0-Z)");

        assertThat(query.values()).hasSize(1);
        assertThat(query.values()).first().isExactlyInstanceOf(HasOneOfTags.class)
                .asInstanceOf(InstanceOfAssertFactories.type(HasOneOfTags.class))
                .satisfies(f -> {
                    assertThat(f.value()).hasSize(2);
                    assertThat(f.value()).containsExactlyInAnyOrder("1e-A", "0-Z");
                });
    }

    @Test
    void canConvertComplexValues() {
        StringToTagFilterQueryConverter converter = new StringToTagFilterQueryConverter();

        TagFilterQuery query = converter.convert("!0-1-2,1-aA,(1e-A|0-Z),!8,1,(1|2),a-a");

        assertThat(query.values()).hasSize(7);
        assertThat(query.values()).anyMatch(f -> f.value.equals("0-1-2"));
        assertThat(query.values()).anyMatch(f -> f.value.equals("1-aA"));
        assertThat(query.values()).anyMatch(f -> f.value.equals("8"));
        assertThat(query.values()).anyMatch(f -> f.value.equals("1"));
        assertThat(query.values()).anyMatch(f -> f.value.equals("a-a"));
        assertThat(query.values()).filteredOn(f -> f.getClass().equals(HasOneOfTags.class))
                .hasSize(2)
                .anySatisfy(f -> assertThat(((HasOneOfTags) f).value()).containsExactlyInAnyOrder("1e-A", "0-Z"))
                .anySatisfy(f -> assertThat(((HasOneOfTags) f).value()).containsExactlyInAnyOrder("1", "2"));
    }
}
