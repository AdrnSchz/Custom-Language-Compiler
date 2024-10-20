package errors.types;

import entities.Token;
import errors.Error;
import entities.SourceLocation;

public class UnknownTokenError extends Error {

    private static final String MESSAGE = "Unknown token '%s'";
    private final String token;

    public UnknownTokenError(String token, SourceLocation location) {
        super(ErrorType.LEXICAL, location);
        this.token = token;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE, token);
    }
}
