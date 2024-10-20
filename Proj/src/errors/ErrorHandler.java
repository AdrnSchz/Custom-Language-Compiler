package errors;

import entities.SourceLocation;

public class ErrorHandler implements ErrorListener {

    private final StringBuilder lexicalErrors;
    private final StringBuilder syntaxErrors;
    private final StringBuilder semanticErrors;
    private final StringBuilder semanticWarnings;

    public ErrorHandler() {
        lexicalErrors = new StringBuilder();
        syntaxErrors = new StringBuilder();
        semanticErrors = new StringBuilder();
        semanticWarnings = new StringBuilder();
    }

    @Override
    public void report(Error err) {

        switch (err.getType()) {
            case LEXICAL -> logLexicalErr(err);
            case SYNTAX -> logSyntaxErr(err);
            case SEMANTIC -> logSemanticErr(err);
            case WARNING -> logSemanticWarning(err);
        }
    }

    public String getErrorWall() {
        String errors = lexicalErrors.toString() + syntaxErrors + semanticErrors + semanticWarnings;
        if (errors.isEmpty()) {
            return "Stan 🎉🎉🎉";
        } else {
            return errors;
        }
    }

    private void logLexicalErr(Error err) {

        if (lexicalErrors.isEmpty()) {
            lexicalErrors.append("------------------------------ Too cringe ✖_✖------------------------------\n\n");
        }

        lexicalErrors.append(formatLocation(err.getLocation()));
        lexicalErrors.append(" -> ");
        lexicalErrors.append(err.getMessage());
        lexicalErrors.append("\n\n");
    }

    private void logSyntaxErr(Error err) {

        if (syntaxErrors.isEmpty()) {
            syntaxErrors.append("------------------------------ Ohh, your canceled :) ------------------------------\n\n");
        }

        syntaxErrors.append(formatLocation(err.getLocation()));
        syntaxErrors.append(" -> ");
        syntaxErrors.append(err.getMessage());
        syntaxErrors.append("\n\n");
    }

    private void logSemanticErr(Error err) {

        if (semanticErrors.isEmpty()) {
            semanticErrors.append("------------------------------ What a boomer ¬_¬ ------------------------------\n\n");
        }

        semanticErrors.append(formatLocation(err.getLocation()));
        semanticErrors.append(" -> ");
        semanticErrors.append(err.getMessage());
        semanticErrors.append("\n\n");
    }

    private void logSemanticWarning(Error err) {

        if (semanticWarnings.isEmpty()) {
            semanticWarnings.append("------------------------------ A little sus ◔_◔ ------------------------------\n\n");
        }

        semanticWarnings.append(formatLocation(err.getLocation()));
        semanticWarnings.append(" -> ");
        semanticWarnings.append(err.getMessage());
        semanticWarnings.append("\n\n");
    }

    private String formatLocation(SourceLocation location) {
        return String.format("Line %d | Column %d", location.getLine(), location.getColumn());
    }

    public void abort() {
        System.exit(1);
    }
}
