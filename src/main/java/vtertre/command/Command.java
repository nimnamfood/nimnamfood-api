package vtertre.command;

import vtertre.infrastructure.bus.Message;

public interface Command<TResponse> extends Message<TResponse> {
}
