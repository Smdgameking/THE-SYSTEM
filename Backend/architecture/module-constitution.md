# Module Constitution

## Module Structure

Every module follows this exact package structure:

```
modules/<module-name>/
    controller/     # REST controllers
    service/        # Service interfaces and implementations
    repository/     # Spring Data JPA repositories
    entity/         # JPA entities
    dto/            # Data Transfer Objects
    mapper/         # MapStruct mappers
    validator/      # Validation logic
    exception/      # Module-specific exceptions and handlers
    events/         # Domain events
```

## Module Dependencies

- Modules must NOT depend on other modules' implementations
- Modules communicate through interfaces or domain events
- Shared code goes in `shared/` package
- Common utilities go in `common/` package

## Module Registration

- Each module's packages must be registered in `@SpringBootApplication` scan
- Use `@ComponentScan` for module-specific components if needed
- Keep module boundaries clean

## Module Testing

- Unit tests in `<module>/service/` and `<module>/controller/`
- Integration tests in `<module>/controller/` or separate test classes
- Test coverage minimum: 80%

## Module Documentation

Every module must have `docs/<module-name>/README.md` containing:
- Purpose
- Database Tables
- API Endpoints
- Business Rules
- Relationships
- Events
- Future Roadmap

## Module Evolution

- Modules can be extracted to separate services later
- Design for eventual extraction
- Avoid cross-module database joins
- Use events for cross-module communication
