package vtertre.ddd.event;

import java.util.List;

public interface EventBus {
    <T extends DomainEvent> void publish(List<T> events);
}
