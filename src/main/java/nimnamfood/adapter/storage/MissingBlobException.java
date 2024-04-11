package nimnamfood.adapter.storage;

import java.util.UUID;

public class MissingBlobException extends RuntimeException {
    public MissingBlobException(UUID id) {
        super(String.format("BLOB_NOT_FOUND - %s", id.toString()));
    }
}
