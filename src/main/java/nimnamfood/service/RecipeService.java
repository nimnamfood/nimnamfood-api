package nimnamfood.service;

import nimnamfood.adapter.storage.FailedToUploadIllustrationException;
import nimnamfood.adapter.storage.MissingBlobException;
import nimnamfood.adapter.storage.StorageAdapter;
import nimnamfood.adapter.storage.BlobPublicUrl;
import nimnamfood.command.recipe.RecipeIngredientCommandPart;
import nimnamfood.model.Repositories;
import nimnamfood.model.recipe.RecipeIngredient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vtertre.ddd.MissingAggregateRootException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private final StorageAdapter storageAdapter;

    @Autowired
    public RecipeService(StorageAdapter storageAdapter) {
        this.storageAdapter = storageAdapter;
    }

    public void importIllustration(UUID illustrationId, MultipartFile file) {
        final Map<String, String> metadata = Collections.singletonMap("firebaseStorageDownloadTokens",
                illustrationId.toString());

        try {
            this.storageAdapter.create("pending/" + illustrationId + ".webp", file.getInputStream(),
                    file.getContentType(), metadata);
        } catch (IOException e) {
            throw new FailedToUploadIllustrationException(e.getMessage());
        }
    }

    public void replaceIllustration(UUID currentIllustrationId, UUID newIllustrationId) {
        this.activateIllustration(newIllustrationId);
        this.deleteIllustration(currentIllustrationId);
    }

    public void activateIllustration(UUID illustrationId) {
        final String sourceBlobName = "pending/" + illustrationId + ".webp";
        final String targetBlobName = "live/recipes/" + illustrationId + ".webp";

        if (!this.storageAdapter.exists(sourceBlobName)) {
            throw new MissingBlobException(illustrationId);
        }

        boolean copied = this.storageAdapter.copy(sourceBlobName, targetBlobName);

        if (copied) {
            this.storageAdapter.delete(sourceBlobName);
        }
    }

    public void deleteIllustration(UUID illustrationId) {
        this.storageAdapter.delete("live/recipes/" + illustrationId + ".webp");
    }

    public String illustrationUrl(UUID illustrationId) {
        final String stringIllustrationId = illustrationId.toString();
        final BlobPublicUrl url = this.storageAdapter.publicUrl("live/recipes/" + stringIllustrationId + ".webp")
                .withToken(stringIllustrationId);
        return url.toUrl();
    }

    public static Set<RecipeIngredient> recipeIngredientsFromCommand(Set<RecipeIngredientCommandPart> parts) {
        return parts
                .stream()
                .map(part -> {
                    final UUID ingredientId = UUID.fromString(part.ingredientId);

                    if (!Repositories.ingredients().exists(ingredientId)) {
                        throw new MissingAggregateRootException(ingredientId);
                    }

                    return new RecipeIngredient(
                            UUID.fromString(part.ingredientId), part.quantity, part.unit, part.quantityFixed);
                })
                .collect(Collectors.toSet());
    }

    public static Set<UUID> tagIdsFromCommand(Set<String> tagIds) {
        return tagIds.stream().map(stringUuid -> {
            final UUID tagId = UUID.fromString(stringUuid);

            if (!Repositories.tags().exists(tagId)) {
                throw new MissingAggregateRootException(tagId);
            }

            return tagId;
        }).collect(Collectors.toSet());
    }
}
