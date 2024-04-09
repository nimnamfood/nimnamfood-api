package nimnamfood.command.illustration;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import nimnamfood.adapter.FailedToUploadIllustration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vtertre.command.CommandHandler;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class ImportIllustrationCommandHandler implements CommandHandler<ImportIllustrationCommand, UUID> {
    private final Storage storage;
    private final String bucketName;

    public ImportIllustrationCommandHandler(@Autowired @Lazy Storage storage,
                                            @Value("${nimnamfood.storage.bucket}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    @Override
    public Tuple<UUID, List<DomainEvent>> execute(ImportIllustrationCommand command) {
        final UUID fileId = UUID.randomUUID();
        final String filename = fileId + "." + StringUtils.getFilenameExtension(command.file.getOriginalFilename());

        final BlobId blobId = BlobId.of(this.bucketName, "pending/" + filename);
        final BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(command.file.getContentType())
                .setStorageClass(StorageClass.STANDARD)
                .setCacheControl("max-age=31536000, immutable")
                .build();

        try {
            this.storage.createFrom(blobInfo, command.file.getInputStream());
        } catch (IOException e) {
            throw new FailedToUploadIllustration(e.getMessage());
        }

        return Tuple.of(fileId, Collections.emptyList());
    }
}
