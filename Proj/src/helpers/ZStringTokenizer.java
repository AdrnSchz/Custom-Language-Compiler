package helpers;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class ZStringTokenizer extends StringTokenizer {

    private final String specialDelimiters;
    private String pendingToken;
    private String literalBuffer;
    private int lineDiff;
    private int prevTokenDiff;
    private int columnDiff;

    public ZStringTokenizer(String str, String delim, String specialDelim) {
        super(str, delim + specialDelim, true);
        specialDelimiters = specialDelim;
        pendingToken = null;
        literalBuffer = "";
        lineDiff = 0;
        prevTokenDiff = 0;
        columnDiff = 0;
    }

    public String nextToken() throws NoSuchElementException {

        if (pendingToken != null) {
            String token = pendingToken;
            pendingToken = null;
            return token;
        }

        String token = getNextToken();

        if (specialDelimiters.contains(token)) {

            String adjToken = getNextToken();

            if (adjToken.equals("=")) {
                return token + "=";
            }

            if (token.equals("+") && adjToken.equals("+")) {
                return "++";
            }

            if (token.equals("-") && adjToken.equals("-")) {
                return "--";
            }

            pendingToken = adjToken;
        }

        return token;
    }

    public int getColumnDiff() {
        return columnDiff;
    }

    public int getLineDiff() {
        int diff = lineDiff;
        lineDiff = 0;
        return diff;
    }

    private String getTokenOrLiteral() {

        String token;
        boolean literal = false, literalEnd = false;

        do {

            token = super.nextToken();

            if (token.contains("\"") || token.contains("'")) {
                literal = true;
                literalEnd = !literalBuffer.isEmpty();
            }

            if (literal) {
                literalBuffer += token;
            }

            if (literalEnd) {
                token = literalBuffer;
                literalBuffer = "";
            }

        } while (literal && !literalEnd);

        return token;
    }

    private String getNextToken() throws NoSuchElementException {

        String token = getTokenOrLiteral();

        columnDiff = prevTokenDiff;

        while (token.equals(" ") || token.equals("\n")) {
            if (token.equals("\n")) {
                lineDiff++;
                columnDiff = 0;
            }
            if (token.equals(" ")) columnDiff++;

            token = getTokenOrLiteral();
        }

        prevTokenDiff = token.length();
        return token;
    }
}
