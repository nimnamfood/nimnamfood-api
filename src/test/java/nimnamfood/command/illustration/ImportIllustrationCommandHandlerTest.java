package nimnamfood.command.illustration;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import nimnamfood.adapter.FailedToUploadIllustration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;
import vtertre.ddd.Tuple;
import vtertre.ddd.event.DomainEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ImportIllustrationCommandHandlerTest {
    Storage storage = Mockito.mock();
    String bucketName = "bucket";

    @Test
    void uploadsAndReturnsFileId() throws IOException {
        InputStream stream = Mockito.mock();
        MultipartFile file = Mockito.mock();
        Mockito.when(file.getContentType()).thenReturn("content type");
        Mockito.when(file.getOriginalFilename()).thenReturn("test.webp");
        Mockito.when(file.getInputStream()).thenReturn(stream);
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);

        ImportIllustrationCommandHandler handler = new ImportIllustrationCommandHandler(storage, bucketName);
        ImportIllustrationCommand command = ImportIllustrationCommand.withFile(file);

        Tuple<UUID, List<DomainEvent>> result = handler.execute(command);

        assertThat(result._1).isNotNull();
        assertThat(result._2).isEmpty();
        Mockito.verify(storage, Mockito.times(1))
                .createFrom(blobInfoCaptor.capture(), Mockito.eq(stream));
        assertThat(blobInfoCaptor.getValue().getBucket()).isEqualTo("bucket");
        assertThat(blobInfoCaptor.getValue().getName()).isEqualTo("pending/" + result._1 + ".webp");
        assertThat(blobInfoCaptor.getValue().getContentType()).isEqualTo("content type");
        assertThat(blobInfoCaptor.getValue().getStorageClass()).isEqualTo(StorageClass.STANDARD);
        assertThat(blobInfoCaptor.getValue().getCacheControl()).isEqualTo("max-age=31536000, immutable");
    }

    @Test
    void throwsAnExceptionWhenUploadFails() throws IOException {
        MultipartFile file = Mockito.mock();
        Mockito.when(file.getInputStream()).thenReturn(Mockito.mock());
        Mockito.doThrow(IOException.class).when(storage)
                .createFrom(Mockito.any(BlobInfo.class), Mockito.any(InputStream.class));

        ImportIllustrationCommandHandler handler = new ImportIllustrationCommandHandler(storage, bucketName);
        ImportIllustrationCommand command = ImportIllustrationCommand.withFile(file);

        assertThatExceptionOfType(FailedToUploadIllustration.class).isThrownBy(() -> handler.execute(command));
    }
}