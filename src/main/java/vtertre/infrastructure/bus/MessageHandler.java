package vtertre.infrastructure.bus;

import com.google.common.reflect.TypeToken;

public interface MessageHandler<TMessage extends Message<TResponse>, TResponse> {
    default Class<TMessage> messageType() {
        return (Class<TMessage>) new TypeToken<TMessage>(getClass()) {
        }.getRawType();
    }
}
