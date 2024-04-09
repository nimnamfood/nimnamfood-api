package nimnamfood.adapter;

public class FailedToUploadIllustration extends RuntimeException {
    public FailedToUploadIllustration(String causeMessage) {
        super(causeMessage);
    }
}
