package stages.frontend;

import entities.Dictionary;
import entities.Token;
import entities.SourceLocation;
import exceptions.NoTokenLeftException;
import errors.types.UnknownTokenError;
import helpers.ZStringTokenizer;

import java.util.NoSuchElementException;

public class LexicalAnalyser {

    private static final String DELIMITERS = " ¡!¿?()%.,[]:&|\"'\n";
    private static final String SPECIAL_DELIMITERS = "+-*/<>#=";
    private final ZStringTokenizer tokenizer;
    private final Dictionary dictionary;
    private SourceLocation currLocation;

    public LexicalAnalyser(String srcCode) {
        this.dictionary = new Dictionary();
        this.tokenizer = new ZStringTokenizer(srcCode, DELIMITERS, SPECIAL_DELIMITERS);
        currLocation = new SourceLocation();
    }

    public Token nextToken() throws NoTokenLeftException, UnknownTokenError {
        try {
            String tokenValue = tokenizer.nextToken();

            currLocation.updateLine(tokenizer.getLineDiff());
            currLocation.updateColumn(tokenizer.getColumnDiff());

            String tokenType = analyseToken(tokenValue);

            return new Token(tokenValue, tokenType);
        }
        catch (NoSuchElementException e) {
            throw new NoTokenLeftException();
        }
    }

    public SourceLocation getLocation() {
        return currLocation;
    }

    public String getType(String tokenDescriptor) {
        Token.TokenType tokenType;
        if ((tokenType = dictionary.find(tokenDescriptor)) != null) {
            return String.valueOf(tokenType);
        }
        return tokenDescriptor;
    }

    private String analyseToken(String tokenValue) throws UnknownTokenError {
        Token.TokenType tokenType;
        if ((tokenType = dictionary.find(tokenValue)) != null) {
            return String.valueOf(tokenType);
        }
        else if (tokenValue.matches("^-?[0-9]+$")) {
            return String.valueOf(Token.TokenType.NUM_LIT);
        }
        else if (tokenValue.matches("^\"[\\s\\S]*\"$")) {
            return String.valueOf(Token.TokenType.STR_LIT);
        }
        else if (tokenValue.matches("^[+-]?([0-9]*[´])?[0-9]+$")) {
            return String.valueOf(Token.TokenType.DEC_LIT);
        }
        else if (tokenValue.matches("^'[\\s\\S]?'$")) {
            return String.valueOf(Token.TokenType.CHAR_LIT);
        }
        else if (tokenValue.matches("^[a-zA-Z]+[a-zA-Z0-9_]*$")) {
            return String.valueOf(Token.TokenType.NAME);
        }
        throw new UnknownTokenError(tokenValue, currLocation);
    }
}
