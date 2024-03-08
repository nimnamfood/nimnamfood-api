package vtertre.command;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Order(1)
@Component
public class CommandValidator implements CommandMiddleware {
    private final Validator validator;

    @Autowired
    public CommandValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public <T> CompletableFuture<T> intercept(CommandBus commandBus, Command<T> command, Supplier<CompletableFuture<T>> nextMiddleware) {
        this.validate(command);
        return nextMiddleware.get();
    }

    public void validate(Command<?> command) {
        Set<ConstraintViolation<Command<?>>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ValidationException(toMessages(violations));
        }
    }

    private List<String> toMessages(Set<ConstraintViolation<Command<?>>> violations) {
        return violations.stream().map(
                violation -> violation.getPropertyPath() + " " + violation.getMessage()).collect(Collectors.toList());
    }
}
