# Coding Standards

## Language

- Java 21 LTS
- Use modern Java features (records, sealed classes, pattern matching)
- No Lombok in shared/common code (prefer records)

## Naming

- Classes: PascalCase
- Methods: camelCase
- Variables: camelCase
- Constants: UPPER_SNAKE_CASE
- Packages: lowercase, no underscores

## Code Organization

- One class per file
- File name matches class name
- Inner classes only for small, tightly-coupled helpers
- Max file length: 500 lines (prefer smaller)

## Comments

- Javadoc for public APIs
- No redundant comments
- No commented-out code
- Use `// TODO:` only with issue reference

## Error Handling

- Never swallow exceptions
- Always log errors with context
- Use `BusinessException` for business errors
- Use standard Spring exceptions for infrastructure errors

## Immutability

- Prefer immutable objects
- Use `final` for fields that don't change
- Use records for DTOs
- Avoid setters in DTOs

## Null Safety

- Use `Optional` for nullable return values
- Never return `null` from methods
- Validate inputs at boundaries
- Use `@NonNull` and `@Nullable` annotations

## Formatting

- 4 spaces indentation
- UTF-8 encoding
- LF line endings
- Trailing newline at end of files
- No trailing whitespace
