package entities;

import java.util.HashMap;
import java.util.Map;

public class Dictionary {

    private final Map<String, Token.TokenType> dict;

    public Dictionary() {
        dict = new HashMap<>();
        populate();
    }

    public Token.TokenType find(String token) {
        return dict.get(token);
    }

    private void populate() {
        dict.put("fact", Token.TokenType.KEYWORD);
        dict.put(".", Token.TokenType.SEPARATOR);
        dict.put("=", Token.TokenType.OPERATOR);
        dict.put("+", Token.TokenType.OPERATOR);
        dict.put("-", Token.TokenType.OPERATOR);
        dict.put("*", Token.TokenType.OPERATOR);
        dict.put("/", Token.TokenType.OPERATOR);
        dict.put("%", Token.TokenType.OPERATOR);
        dict.put("fam", Token.TokenType.KEYWORD);
        dict.put("(", Token.TokenType.SEPARATOR);
        dict.put(")", Token.TokenType.SEPARATOR);
        dict.put("[", Token.TokenType.SEPARATOR);
        dict.put("]", Token.TokenType.SEPARATOR);
        dict.put(",", Token.TokenType.SEPARATOR);
        dict.put("bro", Token.TokenType.KEYWORD);
        dict.put("sis", Token.TokenType.KEYWORD);
        dict.put("bipolar", Token.TokenType.KEYWORD);
        dict.put("mainchar", Token.TokenType.KEYWORD);
        dict.put("ghosted", Token.TokenType.NULL_LIT);
        dict.put("pass", Token.TokenType.BOOL_LIT);
        dict.put("smash", Token.TokenType.BOOL_LIT);
        dict.put("CEO", Token.TokenType.KEYWORD);
        dict.put("¡", Token.TokenType.SEPARATOR);
        dict.put("!", Token.TokenType.SEPARATOR);
        dict.put("¿", Token.TokenType.SEPARATOR);
        dict.put("?", Token.TokenType.SEPARATOR);
        dict.put("zombie", Token.TokenType.KEYWORD);
        dict.put("throwback", Token.TokenType.KEYWORD);
        dict.put("vibe", Token.TokenType.KEYWORD);
        dict.put("check", Token.TokenType.KEYWORD);
        dict.put("then", Token.TokenType.KEYWORD);
        dict.put("like", Token.TokenType.KEYWORD);
        dict.put("whatever", Token.TokenType.KEYWORD);
        dict.put("and", Token.TokenType.OPERATOR);
        dict.put("or", Token.TokenType.OPERATOR);
        dict.put("<", Token.TokenType.OPERATOR);
        dict.put("<=", Token.TokenType.OPERATOR);
        dict.put(">", Token.TokenType.OPERATOR);
        dict.put(">=", Token.TokenType.OPERATOR);
        dict.put("==", Token.TokenType.OPERATOR);
        dict.put("#=", Token.TokenType.OPERATOR);
        dict.put("swipe", Token.TokenType.KEYWORD);
        dict.put("right", Token.TokenType.KEYWORD);
        dict.put(":", Token.TokenType.SEPARATOR);
        dict.put("left", Token.TokenType.KEYWORD);
        dict.put("periodt", Token.TokenType.KEYWORD);
        dict.put("4", Token.TokenType.KEYWORD);
        dict.put("++", Token.TokenType.OPERATOR);
        dict.put("--", Token.TokenType.OPERATOR);
        dict.put("+=", Token.TokenType.OPERATOR);
        dict.put("-=", Token.TokenType.OPERATOR);
        dict.put("*=", Token.TokenType.OPERATOR);
        dict.put("/=", Token.TokenType.OPERATOR);
        dict.put("&", Token.TokenType.OPERATOR);
        dict.put("|", Token.TokenType.OPERATOR);
        dict.put("#", Token.TokenType.OPERATOR);
    }
}