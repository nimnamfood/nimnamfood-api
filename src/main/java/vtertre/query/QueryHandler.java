package vtertre.query;

import vtertre.infrastructure.bus.MessageHandler;

public interface QueryHandler<TQuery extends Query<TResponse>, TResponse> extends MessageHandler<TQuery, TResponse> {
    TResponse execute(TQuery query);
}
