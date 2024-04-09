package nimnamfood.command.illustration.validation;

import com.google.common.collect.Sets;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Set;

public class ValidFileValidator implements ConstraintValidator<ValidFile, MultipartFile> {
    private Set<String> contentTypes;
    private long maxBytesSize;

    @Override
    public void initialize(ValidFile constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.contentTypes = Sets.newHashSet(constraintAnnotation.contentTypes());
        this.maxBytesSize = constraintAnnotation.maxBytesSize();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        final Set<String> violations = Sets.newHashSet();
        final String contentType = file.getContentType();

        if (contentType == null || !this.contentTypes.contains(contentType)) {
            violations.add("must have a valid extension: {contentTypes}");
        }

        if (file.getSize() > this.maxBytesSize) {
            final String humanReadableMaxSize = humanReadableByteCountSI(this.maxBytesSize);
            violations.add("must not exceed " + humanReadableMaxSize);
        }

        context.disableDefaultConstraintViolation();
        violations.forEach(
                violation -> context.buildConstraintViolationWithTemplate(violation).addConstraintViolation());

        return violations.isEmpty();
    }

    private static String humanReadableByteCountSI(long size) {
        long bytes = size;
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
