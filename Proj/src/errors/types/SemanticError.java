package errors.types;

import errors.Error;
import entities.SourceLocation;

public class SemanticError extends Error {
    private final String message;

    public SemanticError(String message, SourceLocation location) {
        super(ErrorType.SEMANTIC, location);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
