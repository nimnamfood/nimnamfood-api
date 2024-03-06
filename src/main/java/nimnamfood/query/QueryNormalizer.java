package nimnamfood.query;

import java.text.Normalizer;

public class QueryNormalizer {
    public static String normalize(String query) {
        return Normalizer
                .normalize(query.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }
}
