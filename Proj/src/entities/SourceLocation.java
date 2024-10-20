package entities;

public class SourceLocation {

    private int lineNum;
    private int columnNum;

    public SourceLocation() {
        lineNum = 0;
        columnNum = 0;
    }

    public void updateLine(int diff) {
        columnNum = 0;
        lineNum += diff;
    }

    public void updateColumn(int diff) {
        columnNum += diff;
    }

    public int getLine() {
        return lineNum;
    }

    public int getColumn() {
        return columnNum;
    }
}
