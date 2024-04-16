package vtertre.ddd.event;

import com.google.common.reflect.TypeToken;
import org.springframework.transaction.annotation.Transactional;

public interface EventCaptor<TEvent extends DomainEvent> {
    @Transactional
    void execute(TEvent event);

    default Class<TEvent> eventType() {
        return (Class<TEvent>) new TypeToken<TEvent>(getClass()) {
        }.getRawType();
    }
}
