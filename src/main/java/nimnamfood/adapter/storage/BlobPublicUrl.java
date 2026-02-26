package nimnamfood.adapter.storage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BlobPublicUrl {
    private final String bucketName;
    private final String blobName;
    private String token;

    public BlobPublicUrl(String bucketName, String blobName) {
        this.bucketName = bucketName;
        this.blobName = blobName;
    }

    public BlobPublicUrl withToken(String token) {
        this.token = token;
        return this;
    }

    public String toUrl() {
        final String encodedBlobName = URLEncoder.encode(this.blobName, StandardCharsets.UTF_8);
        final var base = "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media"
                .formatted(this.bucketName, encodedBlobName);
        return this.token != null ? base + "&token=" + this.token : base;
    }

    public String bucketName() {
        return bucketName;
    }

    public String blobName() {
        return blobName;
    }

    public String token() {
        return token;
    }
}
