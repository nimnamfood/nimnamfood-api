package nimnamfood.query.model;

public class CriteriaSummary {
    public String name;

    public static CriteriaSummary withName(String name) {
        CriteriaSummary summary = new CriteriaSummary();
        summary.name = name;
        return summary;
    }
}
