package vtertre.command;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

public class CommandValidatorTest {
    final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    @Test
    void validatesACommand() {
        CommandValidator validator = new CommandValidator(factory.getValidator());

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validator.validate(new FakeCommand("")));
    }

    @Test
    void providesErrorMessages() {
        CommandValidator validator = new CommandValidator(factory.getValidator());

        ValidationException e = catchThrowableOfType(() -> validator.validate(new FakeCommand("")),
                ValidationException.class);

        assertThat(e.messages()).isNotEmpty();
        assertThat(e.messages().getFirst()).isNotBlank();
    }

    @Test
    void validatesTheCommandBeforeCallingTheNextMiddleware() {
        CommandValidator validator = new CommandValidator(factory.getValidator());
        FakeMiddleware m = new FakeMiddleware();
        FakeCommand command = new FakeCommand("");

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validator.intercept(null, command, () -> m.apply(command)));

        assertThat(m.command).isNull();
    }

    @Test
    void callsTheNextMiddlewareWhenTheCommandIsValidAndPassesThrough() throws Exception {
        CommandValidator validator = new CommandValidator(factory.getValidator());
        FakeMiddleware m = new FakeMiddleware();
        FakeCommand command = new FakeCommand("name");

        CompletableFuture<String> result = validator.intercept(null, command, () -> m.apply(command));

        assertThat(m.command).isEqualTo(command);
        assertThat(result.get()).isEqualTo("fake middleware");
    }

    private record FakeCommand(@NotEmpty String name) implements Command<String> {
        private FakeCommand(String name) {
            this.name = name;
        }
    }

    private static class FakeMiddleware {
        Command<?> command;

        CompletableFuture<String> apply(Command<?> command) {
            this.command = command;
            return CompletableFuture.completedFuture("fake middleware");
        }
    }
}
