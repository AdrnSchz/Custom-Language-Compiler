package errors.types;

import entities.Token;
import errors.Error;
import entities.SourceLocation;

public class UnexpectedTokenError extends Error {

    private static final String MESSAGE = "Unexpected token '%s', expected '%s'";
    private final Token token;
    private final String expectedType;

    public UnexpectedTokenError(Token token, String expectedType, SourceLocation location) {
        super(ErrorType.SYNTAX, location);
        this.token = token;
        this.expectedType = expectedType;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE, token.getValue(), expectedType);
    }
}
