package vtertre.command;

import org.springframework.transaction.annotation.Transactional;
import vtertre.infrastructure.bus.MessageHandler;

public interface CommandHandler<TCommand extends Command<TResponse>, TResponse> extends MessageHandler<TCommand, TResponse> {
    @Transactional
    TResponse execute(TCommand command);
}
