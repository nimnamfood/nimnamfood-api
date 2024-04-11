package nimnamfood.adapter.storage;

import com.google.cloud.storage.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class StorageAdapterTest {
    Storage storage = Mockito.mock();
    String bucketName = "bucket";

    @Test
    void createsABlob() throws IOException {
        StorageAdapter adapter = new StorageAdapter(storage, bucketName);
        InputStream stream = Mockito.mock();
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);

        adapter.create("blob", stream, "content type", Collections.singletonMap("key", "value"));

        Mockito.verify(storage, Mockito.times(1))
                .createFrom(blobInfoCaptor.capture(), Mockito.eq(stream), Mockito.eq(Storage.BlobWriteOption.doesNotExist()));
        assertThat(blobInfoCaptor.getValue().getBucket()).isEqualTo("bucket");
        assertThat(blobInfoCaptor.getValue().getName()).isEqualTo("blob");
        assertThat(blobInfoCaptor.getValue().getContentType()).isEqualTo("content type");
        assertThat(blobInfoCaptor.getValue().getStorageClass()).isEqualTo(StorageClass.STANDARD);
        assertThat(blobInfoCaptor.getValue().getCacheControl()).isEqualTo("max-age=31536000, immutable");
        assertThat(blobInfoCaptor.getValue().getMetadata()).hasSize(1);
        assertThat(blobInfoCaptor.getValue().getMetadata().get("key")).isEqualTo("value");
    }

    @Test
    void generatesUrlObjectByName() {
        StorageAdapter adapter = new StorageAdapter(storage, bucketName);

        BlobPublicUrl url = adapter.publicUrl("blob");

        assertThat(url.blobName()).isEqualTo("blob");
        assertThat(url.bucketName()).isEqualTo("bucket");
    }

    @Test
    void checksIfABlobExistsByName() {
        StorageAdapter adapter = new StorageAdapter(storage, bucketName);
        Blob fakeBlob = Mockito.mock();
        ArgumentCaptor<BlobId> captor = ArgumentCaptor.forClass(BlobId.class);
        Mockito.when(storage.get(captor.capture())).thenReturn(fakeBlob);
        Mockito.when(fakeBlob.exists()).thenReturn(true);

        boolean result = adapter.exists("blob name");

        assertThat(result).isTrue();
        assertThat(captor.getValue().getBucket()).isEqualTo("bucket");
        assertThat(captor.getValue().getName()).isEqualTo("blob name");
    }

    @Test
    void deletesABlobByName() {
        StorageAdapter adapter = new StorageAdapter(storage, bucketName);
        ArgumentCaptor<BlobId> captor = ArgumentCaptor.forClass(BlobId.class);

        adapter.delete("blob name");

        Mockito.verify(storage, Mockito.times(1)).delete(captor.capture());
        assertThat(captor.getValue().getBucket()).isEqualTo("bucket");
        assertThat(captor.getValue().getName()).isEqualTo("blob name");
    }

    @Test
    void copiesABlob() {
        StorageAdapter adapter = new StorageAdapter(storage, bucketName);
        CopyWriter cw = Mockito.mock();
        Mockito.when(cw.isDone()).thenReturn(false);
        ArgumentCaptor<Storage.CopyRequest> captor = ArgumentCaptor.forClass(Storage.CopyRequest.class);
        Mockito.when(storage.copy(captor.capture())).thenReturn(cw);

        boolean result = adapter.copy("source", "target");

        assertThat(result).isFalse();
        assertThat(captor.getValue().getSource().getBucket()).isEqualTo("bucket");
        assertThat(captor.getValue().getSource().getName()).isEqualTo("source");
        assertThat(captor.getValue().getTarget().getBucket()).isEqualTo("bucket");
        assertThat(captor.getValue().getTarget().getName()).isEqualTo("target");
        assertThat(captor.getValue().getTargetOptions()).containsExactly(Storage.BlobTargetOption.doesNotExist());
    }
}