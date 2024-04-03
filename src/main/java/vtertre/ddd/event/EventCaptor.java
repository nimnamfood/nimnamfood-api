package vtertre.ddd.event;

import com.google.common.reflect.TypeToken;

public interface EventCaptor<TEvent extends DomainEvent> {
    void execute(TEvent event);

    default Class<TEvent> eventType() {
        return (Class<TEvent>) new TypeToken<TEvent>(getClass()) {
        }.getRawType();
    }
}
