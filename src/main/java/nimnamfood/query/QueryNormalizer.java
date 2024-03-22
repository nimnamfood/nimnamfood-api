package nimnamfood.query;

import java.text.Normalizer;

public class QueryNormalizer {
    public static String normalize(String query) {
        return Normalizer
                .normalize(query.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }

    public static boolean partialMatch(String string1, String string2) {
        return normalize(string1).contains(normalize(string2));
    }
}
