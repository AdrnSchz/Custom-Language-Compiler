package entities;

public enum Register {

    ZERO("$zero", false),
    T0("$t0", true),
    T1("$t1", true),
    T2("$t2", true),
    T3("$t3", true),
    T4("$t4", true),
    T5("$t5", true),
    T6("$t6", true),
    T7("$t7", true),
    T8("$t8", true),
    T9("$t9", true),
    F0("$f0", true, true),
    F2("$f2", true, true),
    F4("$f4", true, true),
    F6("$f6", true, true),
    F8("$f8", true, true),
    F10("$f10", true, true),
    F12("$f12", true, true),
    F14("$f14", true, true),
    F16("$f16", true, true),
    F18("$f18", true, true),
    F20("$f20", true, true),
    F22("$f22", true, true),
    F24("$f24", true, true),
    F26("$f26", true, true),
    F28("$f28", true, true),
    F30("$f30", true, true),
    STACK_POINTER("$sp", false),
    FRAME_POINTER("$fp", false),
    RETURN_ADDRESS("$ra", false),
    RETURN_VALUE("$v0", false),
    DIVISION_REMAINDER("$hi", false),
    DIVISION_QUOTIENT("$lo", false);

    private final String name;
    private final boolean isTemporary;
    private final boolean isFloat;

    Register(String name, boolean isTemporary) {
        this.name = name;
        this.isTemporary = isTemporary;
        this.isFloat = false;
    }

    Register(String name, boolean isTemporary, boolean isFloat) {
        this.name = name;
        this.isTemporary = isTemporary;
        this.isFloat = isFloat;
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public boolean isFloat() {
        return isFloat;
    }

    public String toString() {
        return name;
    }
}
