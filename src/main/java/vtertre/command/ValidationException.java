package vtertre.command;

import com.google.common.collect.Lists;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> messages = Lists.newArrayList();

    public ValidationException(List<String> messages) {
        this.messages.addAll(messages);
    }

    public List<String> messages() {
        return messages;
    }
}
