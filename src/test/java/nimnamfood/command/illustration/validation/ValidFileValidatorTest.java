package nimnamfood.command.illustration.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class ValidFileValidatorTest {
    ConstraintValidatorContext context = Mockito.mock();
    ConstraintValidatorContext.ConstraintViolationBuilder builder = Mockito.mock();

    @BeforeEach
    void setUp() {
        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.any())).thenReturn(builder);
    }

    @Test
    void rejectsAFileWithInvalidContentType() {
        ValidFile annotation = Mockito.mock();
        String[] fakeContentTypes = {"text/plain"};
        Mockito.when(annotation.contentTypes()).thenReturn(fakeContentTypes);
        Mockito.when(annotation.maxBytesSize()).thenReturn(100000L);
        ValidFileValidator validator = new ValidFileValidator();
        validator.initialize(annotation);
        MultipartFile file = Mockito.mock();
        Mockito.when(file.getContentType()).thenReturn("image/webp");

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
        Mockito.verify(context, Mockito.times(1))
                .buildConstraintViolationWithTemplate("must have a valid extension: {contentTypes}");
    }

    @Test
    void rejectsAFileWithInvalidSize() {
        ValidFile annotation = Mockito.mock();
        String[] fakeContentTypes = {"text/plain", "image/webp"};
        Mockito.when(annotation.contentTypes()).thenReturn(fakeContentTypes);
        Mockito.when(annotation.maxBytesSize()).thenReturn(100L);
        ValidFileValidator validator = new ValidFileValidator();
        validator.initialize(annotation);
        MultipartFile file = Mockito.mock();
        Mockito.when(file.getContentType()).thenReturn("image/webp");
        Mockito.when(file.getSize()).thenReturn(150L);

        boolean result = validator.isValid(file, context);

        assertThat(result).isFalse();
        Mockito.verify(context, Mockito.times(1))
                .buildConstraintViolationWithTemplate("must not exceed 100 B");
    }
}