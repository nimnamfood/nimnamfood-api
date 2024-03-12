package vtertre.ddd;

import java.util.UUID;

public class MissingAggregateRootException extends RuntimeException {
    public MissingAggregateRootException(UUID id) {
        super(String.format("AGGREGATE_ROOT_NOT_FOUND - %s", id.toString()));
    }
}
