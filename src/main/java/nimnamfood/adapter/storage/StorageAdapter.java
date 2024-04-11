package nimnamfood.adapter.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class StorageAdapter {
    private final Storage storage;
    private final String bucketName;

    public StorageAdapter(@Autowired Storage storage,
                          @Value("${nimnamfood.storage.bucket}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    public void create(String blobName, InputStream content, String contentType,
                       Map<String, String> metadata) throws IOException {
        final BlobId blobId = this.blobId(blobName);
        final BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setStorageClass(StorageClass.STANDARD)
                .setCacheControl("max-age=31536000, immutable")
                .setMetadata(metadata)
                .build();

        this.storage.createFrom(blobInfo, content, Storage.BlobWriteOption.doesNotExist());
    }

    public BlobPublicUrl publicUrl(String blobName) {
        return new BlobPublicUrl(this.bucketName, blobName);
    }

    public boolean exists(String blobName) {
        final BlobId blobId = this.blobId(blobName);
        return this.storage.get(blobId).exists();
    }

    public boolean copy(String sourceName, String targetName) {
        final BlobId sourceId = this.blobId(sourceName);
        final BlobId targetId = this.blobId(targetName);
        final Storage.CopyRequest copyRequest = Storage.CopyRequest
                .newBuilder()
                .setSource(sourceId)
                .setTarget(targetId, Storage.BlobTargetOption.doesNotExist())
                .build();

        return this.storage.copy(copyRequest).isDone();
    }

    public void delete(String blobName) {
        final BlobId blobId = this.blobId(blobName);
        this.storage.delete(blobId);
    }

    private BlobId blobId(String blobName) {
        return BlobId.of(this.bucketName, blobName);
    }
}
