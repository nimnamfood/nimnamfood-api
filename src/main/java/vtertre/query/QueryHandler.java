package vtertre.query;

import org.springframework.transaction.annotation.Transactional;
import vtertre.infrastructure.bus.MessageHandler;

public interface QueryHandler<TQuery extends Query<TResponse>, TResponse> extends MessageHandler<TQuery, TResponse> {
    @Transactional
    TResponse execute(TQuery query);
}
