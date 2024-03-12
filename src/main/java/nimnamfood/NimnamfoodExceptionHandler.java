package nimnamfood;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import vtertre.command.ValidationException;
import vtertre.ddd.MissingAggregateRootException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class NimnamfoodExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, List<String>>> validationExceptionHandler(ValidationException exception, WebRequest request) {
        return new ResponseEntity<>(
                Collections.singletonMap("errors", exception.messages()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingAggregateRootException.class)
    public ResponseEntity<Map<String, String>> validationExceptionHandler(MissingAggregateRootException exception, WebRequest request) {
        return new ResponseEntity<>(
                Collections.singletonMap("error", exception.getMessage()), HttpStatus.NOT_FOUND);
    }
}
