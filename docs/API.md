# Nimnamfood API Reference

Nimnamfood is a daily meal assistant REST API built with Spring Boot. This document describes all available endpoints, request formats, response shapes, validation rules, and error handling — everything a frontend application needs to integrate with this API.

---

## Base URL

```
https://<your-host>
```

All endpoints are relative to the base URL. There is no global `/api/v1` prefix.

---

## General Conventions

- **Content-Type**: `application/json` for all JSON request bodies; `multipart/form-data` for file uploads.
- **IDs**: All resource IDs are UUID v4 strings (e.g. `"3fa85f64-5717-4562-b3fc-2c963f66afa6"`).
- **Pagination**: Controlled via `skip` (offset) and `limit` query parameters where supported.
- **Async**: All responses are resolved asynchronously server-side; behavior from the client's perspective is standard HTTP request/response.

---

## CORS

All origins configured via `CORS_ALLOWED_ORIGINS` are allowed. Allowed HTTP methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. All request headers are permitted.

---

## Error Responses

All error responses return a JSON body.

### 400 Bad Request — Validation Error

Returned when request body fields fail validation constraints.

```json
{
  "errors": ["name must not be blank", "portionsCount must be a positive number"]
}
```

### 400 Bad Request — Duplicate Resource

Returned when attempting to create a resource that already exists (e.g. duplicate ingredient or tag name).

```json
{
  "error": "..."
}
```

### 404 Not Found

Returned when a resource with the given UUID does not exist.

```json
{
  "error": "..."
}
```

---

## Endpoints

---

### Health Check

#### `GET /`

Returns the API health status.

**Request**: No parameters, no body.

**Response** `200 OK`:
```json
{
  "result": "ok"
}
```

---

### Ingredients

#### `GET /ingredients`

Search for ingredients by name with optional pagination.

**Query Parameters**:

| Name    | Type    | Required | Description                                                              |
|---------|---------|----------|--------------------------------------------------------------------------|
| `q`     | string  | No       | Free-text search string to filter ingredients by name (accent-insensitive) |
| `skip`  | integer | No       | Number of results to skip (offset). Minimum: `0`. Default: `0`          |
| `limit` | integer | No       | Maximum number of results to return. Range: `0–20`. Default: `0` (returns all up to max) |

**Response** `200 OK` — array of ingredient summaries:
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Flour",
    "unit": "GRAM"
  }
]
```

**Response Fields**:

| Field  | Type   | Description                            |
|--------|--------|----------------------------------------|
| `id`   | UUID   | Unique identifier of the ingredient    |
| `name` | string | Display name of the ingredient         |
| `unit` | string | Default unit — see [Ingredient Units](#ingredient-units) |

---

#### `POST /ingredients`

Create a new ingredient.

**Request Body** (`application/json`):
```json
{
  "name": "Flour",
  "unit": "GRAM"
}
```

**Fields**:

| Field  | Type   | Required | Validation          | Description                   |
|--------|--------|----------|---------------------|-------------------------------|
| `name` | string | Yes      | Not blank, must be unique | Display name of the ingredient |
| `unit` | string | Yes      | Must be a valid `IngredientUnit` enum value | Default measurement unit |

**Response** `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

---

#### `PUT /ingredients/{id}`

Update an existing ingredient's name and/or unit.

**Path Parameters**:

| Name | Type | Description                    |
|------|------|--------------------------------|
| `id` | UUID | The ingredient's UUID          |

**Request Body** (`application/json`):
```json
{
  "name": "Whole Wheat Flour",
  "unit": "GRAM"
}
```

**Fields**:

| Field  | Type   | Required | Validation          | Description                   |
|--------|--------|----------|---------------------|-------------------------------|
| `name` | string | Yes      | Not blank           | New name for the ingredient   |
| `unit` | string | Yes      | Valid `IngredientUnit` | New default unit           |

**Response** `204 No Content` — empty body on success.

---

### Ingredient Units

#### `GET /units`

Returns all available ingredient unit values (the full enum set).

**Request**: No parameters, no body.

**Response** `200 OK` — set of unit strings:
```json
["GRAM", "MILLILITER", "TABLESPOON", "TEASPOON", "PIECE", "PINCH", "HANDFUL", "SLICE", "LEAF"]
```

**Available Units**:

| Value        | Description              |
|--------------|--------------------------|
| `GRAM`       | Grams (weight)           |
| `MILLILITER` | Millilitres (volume)     |
| `TABLESPOON` | Tablespoon               |
| `TEASPOON`   | Teaspoon                 |
| `PIECE`      | Individual piece / unit  |
| `PINCH`      | Pinch                    |
| `HANDFUL`    | Handful                  |
| `SLICE`      | Slice                    |
| `LEAF`       | Leaf                     |

---

### Tags

#### `GET /tags`

Search for tags by name.

**Query Parameters**:

| Name | Type   | Required | Description                                      |
|------|--------|----------|--------------------------------------------------|
| `q`  | string | No       | Free-text search string to filter tags by name   |

**Response** `200 OK` — array of tag summaries:
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Vegetarian"
  }
]
```

**Response Fields**:

| Field  | Type   | Description              |
|--------|--------|--------------------------|
| `id`   | UUID   | Unique identifier of the tag |
| `name` | string | Display name of the tag  |

---

#### `POST /tags`

Create a new tag.

**Request Body** (`application/json`):
```json
{
  "name": "Vegetarian"
}
```

**Fields**:

| Field  | Type   | Required | Validation            | Description         |
|--------|--------|----------|-----------------------|---------------------|
| `name` | string | Yes      | Not blank, must be unique | Name of the tag |

**Response** `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

---

### Recipes

#### `GET /recipes`

Search and list recipes with optional full-text search, tag filtering, and pagination.

**Query Parameters**:

| Name    | Type   | Required | Description                                                     |
|---------|--------|----------|-----------------------------------------------------------------|
| `q`     | string | No       | Full-text search query against recipe names and content (accent-insensitive) |
| `tags`  | string | No       | Tag filter expression (see [Tag Filter Syntax](#tag-filter-syntax)) |
| `skip`  | integer | No      | Number of results to skip. Minimum: `0`. Default: `0`          |
| `limit` | integer | No      | Max results to return. Range: `0–15`. Default: `0` (returns all up to max) |

##### Tag Filter Syntax

The `tags` parameter accepts a comma-separated list of filter tokens. Each token can be:

| Syntax             | Meaning                                        | Example                         |
|--------------------|------------------------------------------------|---------------------------------|
| `{tagName}`        | Recipe **must have** this tag                  | `tags=Vegetarian`               |
| `!{tagName}`       | Recipe **must NOT have** this tag              | `tags=!Meat`                    |
| `({a}\|{b}\|{c})`  | Recipe must have **at least one** of these tags | `tags=(Breakfast\|Brunch)`     |

Filters are combinable by separating them with commas:

```
tags=Vegetarian,!Meat,(Breakfast|Brunch)
```

Means: the recipe must be tagged `Vegetarian`, must not be tagged `Meat`, and must be tagged with at least one of `Breakfast` or `Brunch`.

**Response** `200 OK` — array of recipe search summaries:
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Pancakes",
    "illustrationUrl": "https://storage.googleapis.com/...",
    "tags": [
      {
        "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
        "name": "Breakfast"
      }
    ]
  }
]
```

**Response Fields**:

| Field              | Type        | Description                                            |
|--------------------|-------------|--------------------------------------------------------|
| `id`               | UUID        | Unique identifier of the recipe                        |
| `name`             | string      | Recipe name                                            |
| `illustrationUrl`  | string\|null | Public URL of the recipe illustration image, or `null` |
| `tags`             | array       | List of tags associated with the recipe                |
| `tags[].id`        | UUID        | Tag identifier                                         |
| `tags[].name`      | string      | Tag name                                               |

---

#### `GET /recipes/{id}`

Retrieve full details of a single recipe.

**Path Parameters**:

| Name | Type | Description         |
|------|------|---------------------|
| `id` | UUID | The recipe's UUID   |

**Response** `200 OK`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Pancakes",
  "illustration": {
    "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "url": "https://storage.googleapis.com/..."
  },
  "portionsCount": 4,
  "instructions": "Mix flour, eggs, and milk. Cook on a hot pan.",
  "ingredients": [
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "name": "Flour",
      "quantity": 200.0,
      "unit": "GRAM"
    }
  ],
  "tags": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "name": "Breakfast"
    }
  ]
}
```

**Response Fields**:

| Field                       | Type        | Description                                              |
|-----------------------------|-------------|----------------------------------------------------------|
| `id`                        | UUID        | Unique identifier of the recipe                          |
| `name`                      | string      | Recipe name                                              |
| `illustration`              | object\|null | Illustration info, or `null` if none uploaded            |
| `illustration.id`           | UUID        | Illustration UUID                                        |
| `illustration.url`          | string      | Public URL of the illustration image                     |
| `portionsCount`             | integer     | Number of portions this recipe yields                    |
| `instructions`              | string      | Step-by-step cooking instructions                        |
| `ingredients`               | array       | List of ingredients used in the recipe                   |
| `ingredients[].id`          | UUID        | The ingredient's global UUID                             |
| `ingredients[].name`        | string      | Ingredient name                                          |
| `ingredients[].quantity`    | float       | Amount of the ingredient needed for this recipe          |
| `ingredients[].unit`        | string      | Unit for the quantity — see [Ingredient Units](#ingredient-units) |
| `tags`                      | array       | Tags associated with this recipe                         |
| `tags[].id`                 | UUID        | Tag identifier                                           |
| `tags[].name`               | string      | Tag name                                                 |

Returns `404 Not Found` if no recipe exists with the given UUID.

---

#### `POST /recipes`

Create a new recipe.

**Request Body** (`application/json`):
```json
{
  "name": "Pancakes",
  "illustrationId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "portionsCount": 4,
  "instructions": "Mix flour, eggs, and milk. Cook on a hot pan.",
  "tagIds": [
    "7c9e6679-7425-40de-944b-e07fc1f90ae7"
  ],
  "ingredients": [
    {
      "ingredientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "quantity": 200.0,
      "unit": "GRAM"
    }
  ]
}
```

**Fields**:

| Field            | Type    | Required | Validation                                   | Description                                      |
|------------------|---------|----------|----------------------------------------------|--------------------------------------------------|
| `name`           | string  | Yes      | Not blank                                    | Recipe name                                      |
| `illustrationId` | UUID    | No       | Valid UUID if provided                       | UUID returned from `POST /illustrations`         |
| `portionsCount`  | integer | Yes      | Not null, positive (≥ 1)                     | Number of portions this recipe makes             |
| `instructions`   | string  | Yes      | Not blank                                    | Cooking instructions text                        |
| `tagIds`         | array   | Yes      | Not null; each element must be a valid UUID v4 | Set of tag UUIDs to associate with the recipe  |
| `ingredients`    | array   | Yes      | Not null; each element must be valid         | Set of ingredient entries                        |

**Ingredient entry** (`ingredients[]`):

| Field          | Type    | Required | Validation           | Description                            |
|----------------|---------|----------|----------------------|----------------------------------------|
| `ingredientId` | UUID    | Yes      | Valid UUID           | UUID of an existing ingredient         |
| `quantity`     | float   | Yes      | Not null, positive   | Amount of the ingredient               |
| `unit`         | string  | Yes      | Valid `IngredientUnit` | Unit for the quantity (can differ from the ingredient's default unit) |

**Response** `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

---

#### `PUT /recipes/{id}`

Replace all fields of an existing recipe. This is a full update — all fields must be provided.

**Path Parameters**:

| Name | Type | Description       |
|------|------|-------------------|
| `id` | UUID | The recipe's UUID |

**Request Body** (`application/json`):

Same structure as `POST /recipes`:

```json
{
  "name": "Fluffy Pancakes",
  "illustrationId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "portionsCount": 6,
  "instructions": "Updated instructions here.",
  "tagIds": [
    "7c9e6679-7425-40de-944b-e07fc1f90ae7"
  ],
  "ingredients": [
    {
      "ingredientId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "quantity": 250.0,
      "unit": "GRAM"
    }
  ]
}
```

**Fields**:

| Field            | Type    | Required | Validation                                   | Description                                   |
|------------------|---------|----------|----------------------------------------------|-----------------------------------------------|
| `name`           | string  | Yes      | Not blank                                    | Recipe name                                   |
| `illustrationId` | UUID    | No       | Valid UUID if provided                       | UUID returned from `POST /illustrations`      |
| `portionsCount`  | integer | Yes      | Not null, positive (≥ 1)                     | Number of portions                            |
| `instructions`   | string  | Yes      | Not blank                                    | Cooking instructions                          |
| `tagIds`         | array   | Yes      | Not null; each must be a valid UUID v4       | Full replacement set of tag UUIDs             |
| `ingredients`    | array   | Yes      | Not null, not empty; each element must be valid | Full replacement set of ingredients        |

**Response** `204 No Content` — empty body on success.

Returns `404 Not Found` if no recipe exists with the given UUID.

---

### Illustrations

#### `POST /illustrations`

Upload an illustration image to be used on a recipe. The returned UUID can then be passed as `illustrationId` when creating or updating a recipe.

**Request**: `multipart/form-data`

| Field  | Type | Required | Validation                                     | Description                    |
|--------|------|----------|------------------------------------------------|--------------------------------|
| `file` | file | Yes      | Content-Type must be `image/webp`; max size: **100 KB** | WebP image file to upload |

**Example** (using `curl`):
```bash
curl -X POST https://<host>/illustrations \
  -F "file=@photo.webp;type=image/webp"
```

**Response** `201 Created`:
```json
{
  "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

Use the returned `id` as `illustrationId` in recipe create/update requests.

**Validation Errors** `400 Bad Request`:
- File content-type is not `image/webp`
- File exceeds 100 KB (100,000 bytes)

---

## Typical Frontend Workflows

### Creating a recipe with an illustration

1. **Upload the image**: `POST /illustrations` with a WebP file → receive `illustrationId`
2. **Ensure tags exist**: `GET /tags?q=...` to search, or `POST /tags` to create new ones → collect tag UUIDs
3. **Ensure ingredients exist**: `GET /ingredients?q=...` to search, or `POST /ingredients` to create → collect ingredient UUIDs
4. **Create the recipe**: `POST /recipes` with `illustrationId`, tag UUIDs, ingredient UUIDs, and other fields → receive `recipeId`

### Browsing and filtering recipes

1. **Load all tags**: `GET /tags` (no query) to populate filter UI
2. **Search with filters**: `GET /recipes?q=pancakes&tags=Vegetarian,!Meat&skip=0&limit=15`
3. **Open a recipe**: `GET /recipes/{id}` to get full details

### Editing a recipe

1. **Fetch current data**: `GET /recipes/{id}`
2. *(Optional)* **Upload a new illustration**: `POST /illustrations` → new `illustrationId`
3. **Submit full update**: `PUT /recipes/{id}` with the complete updated body (all fields required)

---

## Data Model Summary

```
Tag
  id:   UUID
  name: string (unique)

Ingredient
  id:   UUID
  name: string (unique)
  unit: IngredientUnit

Recipe
  id:             UUID
  name:           string
  portionsCount:  integer
  instructions:   string
  illustration:   { id: UUID, url: string } | null
  ingredients:    [{ id, name, quantity, unit }]
  tags:           [{ id, name }]
```