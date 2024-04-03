package vtertre.command;

import org.springframework.transaction.annotation.Transactional;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;
import vtertre.infrastructure.bus.MessageHandler;

import java.util.List;

public interface CommandHandler<TCommand extends Command<TResponse>, TResponse> extends MessageHandler<TCommand, TResponse> {
    @Transactional
    Tuple<TResponse, List<DomainEvent>> execute(TCommand command);
}
