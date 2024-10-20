package errors.types;

import entities.SourceLocation;
import errors.Error;

public class SemanticWarning extends Error {
    private final String message;

    public SemanticWarning(String message, SourceLocation location) {
        super(Error.ErrorType.WARNING, location);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
