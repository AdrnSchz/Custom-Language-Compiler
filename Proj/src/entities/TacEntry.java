package entities;

public class TacEntry {

    public enum Operation {
        ADD,
        SUB,
        MUL,
        DIV,
        MOD,
        NEG,
        AND,
        OR,
        COMPARISON,
        STARTER,
        ENDER,
        CALL,
        DECLARE,
        LITERAL,
        EQUAL,
        PARAM,
        FUNC_DEC,
        PARAM_DEC,
        RETURN
    }

    private final String arg1;
    private final String arg2;
    private final String res;
    private final Operation op;
    private final String tag;

    public TacEntry(String res, String arg1, String arg2, Operation op, String tag) {
        this.res = res;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.op = op;
        this.tag = tag;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public String getRes() {
        return res;
    }

    public Operation getOp() {
        return op;
    }

    public String getTag() {
        return tag;
    }

}
