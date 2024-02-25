package nimnamfood.model;

import org.springframework.beans.factory.InitializingBean;

public abstract class Repositories implements InitializingBean {
    private static Repositories INSTANCE;

    protected abstract CriteriaRepository getCriteria();

    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    public static void initialize(Repositories instance) {
        Repositories.INSTANCE = instance;
    }

    public static CriteriaRepository criteria() {
        return INSTANCE.getCriteria();
    }
}
