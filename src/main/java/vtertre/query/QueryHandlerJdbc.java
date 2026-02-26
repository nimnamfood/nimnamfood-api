package vtertre.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;

public abstract class QueryHandlerJdbc<TQuery extends Query<TResponse>, TResponse> implements QueryHandler<TQuery, TResponse> {
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public TResponse execute(TQuery query) {
        return this.execute(query, this.namedParameterJdbcTemplate);
    }

    protected HashMap<String, Object> baseLimitOffsetParams(TQuery query) {
        var params = new HashMap<String, Object>();
        params.put("limit", query.limit() > 0 ? query.limit() : null);
        params.put("offset", query.skip() > -1 ? query.skip() : 0);
        return params;
    }

    protected abstract TResponse execute(TQuery query, NamedParameterJdbcTemplate jdbcTemplate);

    protected static String appendLimitAndOffset(String sqlQuery) {
        return sqlQuery + " LIMIT :limit OFFSET :offset ";
    }

    @Autowired
    public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedJdbcTemplate;
    }
}
