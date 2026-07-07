# Plan 007: Validate uploaded illustrations by content (WebP magic bytes), not by client header

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat ad32e0f..HEAD -- src/main/java/nimnamfood/command/illustration src/test/java/nimnamfood/command/illustration`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `ad32e0f`, 2026-07-06

## Why this matters

`POST /illustrations` accepts a multipart file that ends up publicly served
from Google Cloud Storage. The only content check is
`file.getContentType()` — a **client-supplied header**. Anyone can send
arbitrary bytes with `Content-Type: image/webp` and they will be stored and
served as a `.webp` blob. The blast radius is contained (the blob is stored
with the declared allowlisted content type, so browsers treat it as an image),
but the bucket still accepts arbitrary non-image payloads up to 100 KB that
your own frontend will then fail to render. Checking the WebP container's
magic bytes closes this cheaply and catches honest mistakes (wrong file
picked) with a proper 400 instead of a broken image later.

## Current state

- `src/main/java/nimnamfood/command/illustration/validation/ValidFileValidator.java`
  — jakarta `ConstraintValidator<ValidFile, MultipartFile>`. Relevant part
  (lines 23-42):

```java
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
```

- `src/main/java/nimnamfood/command/illustration/validation/ValidFile.java` —
  the constraint annotation (has `contentTypes` and `maxBytesSize` members).
- Usage — `src/main/java/nimnamfood/command/illustration/ImportIllustrationCommand.java:11-13`:

```java
@NotNull
@ValidFile(maxBytesSize = 100_000L, contentTypes = {"image/webp"})
public MultipartFile file;
```

- Existing tests:
  `src/test/java/nimnamfood/command/illustration/validation/ValidFileValidatorTest.java`
  — read it before starting; it shows how the validator is constructed and how
  `MultipartFile` is faked (Spring's `MockMultipartFile` or a hand fake).
  New tests follow its structure exactly.
- WebP format fact (for the implementation): a WebP file starts with the RIFF
  container header — bytes 0-3 are ASCII `RIFF`, bytes 4-7 are the
  little-endian file size, bytes 8-11 are ASCII `WEBP`. Checking those 12
  bytes is the standard signature test.
- The app is a WebP-only pipeline (blobs are stored as `.webp` — see
  `RecipeService.importIllustration`), so a WebP-specific check is appropriate;
  no generic content-detection library is needed (and adding Apache Tika for
  one signature was considered and rejected — dependency cost exceeds value).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw -q compile` | exit 0 |
| Validator tests (no Docker) | `./mvnw test -Dtest=ValidFileValidatorTest` | BUILD SUCCESS |
| Full suite (needs Docker) | `./mvnw verify` | BUILD SUCCESS |

## Scope

**In scope**:
- `src/main/java/nimnamfood/command/illustration/validation/ValidFileValidator.java`
- `src/test/java/nimnamfood/command/illustration/validation/ValidFileValidatorTest.java`

**Out of scope** (do NOT touch):
- `ValidFile.java` — no new annotation members needed; the magic-byte check is
  implied by content type `image/webp` being in the allowlist.
- `StorageAdapter` / `RecipeService` — upload plumbing unchanged.
- Image *decoding* (verifying the full file is a valid decodable image) —
  rejected as overkill; signature check only.

## Git workflow

- Branch: `advisor/007-validate-upload-magic-bytes`
- Commit style: French imperative, lowercase. Suggested: `valider la signature webp des illustrations`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add the signature check to the validator

In `ValidFileValidator.isValid`, after the existing content-type check, add a
signature check that runs only when the declared content type is
`image/webp` (currently the only allowlisted type):

```java
if (contentType != null && contentType.equals("image/webp") && !hasWebpSignature(file)) {
    violations.add("must be a valid webp file");
}
```

with:

```java
private static boolean hasWebpSignature(MultipartFile file) {
    try (InputStream stream = file.getInputStream()) {
        final byte[] header = stream.readNBytes(12);
        return header.length == 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
    } catch (IOException e) {
        return false;
    }
}
```

Note: `MultipartFile.getInputStream()` can be called again later by
`RecipeService.importIllustration` (Spring buffers multipart content in
memory/temp file), so reading 12 bytes here does not consume the upload.

**Verify**: `./mvnw -q compile` → exit 0

### Step 2: Tests

Extend `ValidFileValidatorTest` (match its existing construction/fake style):

1. valid WebP header (12+ bytes: `"RIFF" + 4 arbitrary bytes + "WEBP" + payload`) with content type `image/webp` → valid.
2. declared `image/webp` but body starts with e.g. `<html>` → invalid, violation message contains "webp".
3. declared `image/webp` but body shorter than 12 bytes → invalid.
4. previously passing cases (wrong content type, oversized file) still behave the same.

Build the byte arrays inline in the tests — no fixture files with real images
needed for a signature check.

**Verify**: `./mvnw test -Dtest=ValidFileValidatorTest` → BUILD SUCCESS, includes 3+ new tests

### Step 3: Full suite

`ImportIllustrationCommandHandlerTest` and `StorageAdapterTest` must still
pass — if they construct fake uploads that now fail validation, fix the TEST
fixtures to carry a valid signature (that is expected fallout, not scope creep).

**Verify**: `./mvnw verify` → BUILD SUCCESS (Docker required)

## Test plan

Covered in Step 2; pattern is the existing `ValidFileValidatorTest`.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -n "WEBP" src/main/java/nimnamfood/command/illustration/validation/ValidFileValidator.java` returns at least 1 match
- [ ] `./mvnw test -Dtest=ValidFileValidatorTest` exits 0
- [ ] `./mvnw verify` exits 0 (Docker required)
- [ ] `git status` shows no modified files outside the in-scope list (test fixtures under `src/test/java/nimnamfood/command/illustration/` are allowed per Step 3)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `ValidFile` is used elsewhere with non-WebP content types
  (`grep -rn "@ValidFile" src/main/java`) — the WebP-specific check would then
  be wrong for those sites; report before generalizing.
- Reading `getInputStream()` in the validator empties the stream for the
  handler (an integration test starts failing on upload) — the buffering
  assumption would be false for this Spring version; report.

## Maintenance notes

- If other image formats are ever allowlisted (PNG/JPEG), extend
  `hasWebpSignature` into a per-content-type signature map — at that point
  reconsider Apache Tika.
- Reviewer should scrutinize: the check must be conditional on the *declared*
  type being webp so that the "wrong content type" violation (not the webp
  violation) fires for other types — two violations for one wrong file is
  acceptable, but the content-type violation must never be masked.
