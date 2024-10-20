package entities;

public class Token {

    public enum TokenType {
        KEYWORD,
        OPERATOR,
        SEPARATOR,
        NAME,
        NUM_LIT,
        STR_LIT,
        DEC_LIT,
        CHAR_LIT,
        NULL_LIT,
        BOOL_LIT
    }

    private final TokenType type;
    private final String value;

    public Token(String value, String type) {
        this.value = value;
        this.type = TokenType.valueOf(type.toUpperCase());
    }

    public String getType() {
        return type.toString();
    }

    public String getValue() {
        return value;
    }
}
