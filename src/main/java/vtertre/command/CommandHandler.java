package vtertre.command;

import vtertre.infrastructure.bus.MessageHandler;

public interface CommandHandler<TCommand extends Command<TResponse>, TResponse> extends MessageHandler<TCommand, TResponse> {
    TResponse execute(TCommand command);
}
