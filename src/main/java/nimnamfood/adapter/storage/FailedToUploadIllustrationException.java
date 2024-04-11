package nimnamfood.adapter.storage;

public class FailedToUploadIllustrationException extends RuntimeException {
    public FailedToUploadIllustrationException(String causeMessage) {
        super(causeMessage);
    }
}
