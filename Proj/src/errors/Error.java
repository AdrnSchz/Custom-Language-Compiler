package errors;

import entities.SourceLocation;

public abstract class Error extends Throwable {

    public enum ErrorType {
        LEXICAL,
        SYNTAX,
        SEMANTIC,
        WARNING
    }

    private final ErrorType type;
    private final SourceLocation location;

    public Error(ErrorType errorType, SourceLocation srcLocation) {
        type = errorType;
        location = srcLocation;
    }

    public ErrorType getType() {
        return type;
    }

    public SourceLocation getLocation() {
        return location;
    }

    abstract public String getMessage();
}
