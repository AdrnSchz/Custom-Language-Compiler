package stages.backend;

import entities.Register;
import entities.TacEntry;
import helpers.BiMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TargetCodeGenerator {

    private static final String INIT_POINTERS = "\tli   $fp, 2147483644\n\tmove $sp, $fp\n";
    private static final String SP_MOVE_DOWN = "\tsub  $sp, $sp, %s\n";
    private static final String SP_MOVE_UP = "\taddi $sp, $sp, %s\n";
    private static final String ASSIGNMENT_ARR_GLOBAL = "\t.data\n%s: .%s   0 : %s\n%s_size: .word   %s\n\t.text\n";
    private static final String ASSIGNMENT_GLOBAL = "\t.data\n%s: .%s   0\n\t.text\n";
    private static final String ASSIGNMENT_FLOAT = "\t.data\n%s: .float   %s\n\t.text\n";
    private static final String ADDR_TO_REG = "\tla   %s, %s\n";
    private static final String REG_TO_MEM = "\tsw   %s, %d(%s)\n";
    private static final String REG_TO_MEM_LABEL = "\tsw   %s, %s\n";
    private static final String FREG_TO_MEM = "\ts.s  %s, %d(%s)\n";
    private static final String FREG_TO_MEM_LABEL = "\ts.s  %s, %s\n";
    private static final String REG_TO_REG = "\tmove %s, %s\n";
    private static final String FREG_TO_FREG = "\tmov.s %s, %s\n";
    private static final String REG_TO_FREG = "\tmtc1 %s, %s\n";
    private static final String FREG_TO_REG = "\tmfc1 %s, %s\n";
    private static final String MEM_TO_REG = "\tlw   %s, %d(%s)\n";
    private static final String MEM_TO_REG_LABEL = "\tlw   %s, %s\n";
    private static final String MEM_TO_FREG = "\tl.s  %s, %d(%s)\n";
    private static final String MEM_TO_FREG_LABEL = "\tl.s  %s, %s\n";
    private static final String INT_TO_FLOAT = "\tcvt.s.w %s, %s\n";
    private static final String FLOAT_TO_INT = "\tcvt.w.s %s, %s\n";
    private static final String LOAD_LIT = "\tli   %s, %s\n";
    private static final String LOAD_FLOAT = "\tl.s   %s, %s\n";
    private static final String ADDITION = "\tadd  %s, %s, %s\n";
    private static final String ADDITION_FLOAT = "\tadd.s %s, %s, %s\n";
    private static final String SUBTRACTION = "\tsub  %s, %s, %s\n";
    private static final String SUBTRACTION_FLOAT = "\tsub.s %s, %s, %s\n";
    private static final String MULTIPLICATION = "\tmul  %s, %s, %s\n";
    private static final String MULTIPLICATION_FLOAT = "\tmul.s %s, %s, %s\n";
    private static final String DIVISION = "\tdiv  %s, %s\n";
    private static final String MOVE_FROM_LO = "\tmflo %s\n";
    private static final String DIVISION_FLOAT = "\tdiv.s %s, %s, %s\n";
    private static final String ABSOLUTE_FLOAT = "\tabs.s %s, %s\n";
    private static final String AND = "\tand  %s, %s, %s\n";
    private static final String OR = "\tor   %s, %s, %s\n";
    private static final String NOT = "\tnot  %s, %s\n";
    private static final String NOT_FLOAT = "\tneg.s  %s, %s\n";
    private static final String LABEL = "%s:\n";
    private static final String COMPARE_LESS_EQUAL = "\tc.le.s %s, %s\n";
    private static final String COMPARE_LESS = "\tc.lt.s %s, %s\n";
    private static final String COMPARE_EQUAL = "\tc.eq.s %s, %s\n";
    private static final String MOVE_FALSE = "\tmovf.s %s, %s\n";
    private static final String MOVE_TRUE = "\tmovt.s %s, %s\n";
    private static final String GOTO = "\tj    %s\n";
    private static final String CALL = "\tjal  %s\n";
    private static final String RETURN = "\tjr   %s\n";
    private static final String BRANCH_IF_GT0 = "\tbgtz %s, %s\n";
    private static final String EXIT = "\tli   $v0, 10\n\tsyscall\n";

    private final StringBuilder code;
    private final BiMap<String, Register> registerAssociation;
    private final HashMap<String, Integer[]> stackAssociation;
    private final HashMap<String, String> globalAssociation;
    private final HashMap<String, Boolean> floatAssociation;
    private final LinkedList<Register> registers;
    private final LinkedList<Register> floatRegisters;
    private final List<String> pendingParameters;
    private final LinkedList<Integer> statementIdStack;
    private final HashMap<Integer, LinkedList<String[]>> statementCheckBranching;
    private final HashMap<Integer, LinkedList<TacEntry>> statementCheckConditions;
    private boolean storeConditions;
    private int statementLabelCount;
    private int stackPointer;
    private boolean globalContext;

    public TargetCodeGenerator() {
        this.code = new StringBuilder();
        this.registerAssociation = new BiMap<>();
        this.stackAssociation = new HashMap<>();
        this.globalAssociation = new HashMap<>();
        this.floatAssociation = new HashMap<>();
        this.registers = new LinkedList<>();
        this.floatRegisters = new LinkedList<>();
        this.pendingParameters = new ArrayList<>();
        this.statementIdStack = new LinkedList<>();
        this.statementCheckBranching = new HashMap<>();
        this.statementCheckConditions = new HashMap<>();

        this.globalContext = true;
        this.statementLabelCount = 0;
        this.stackPointer = 0;
        this.storeConditions = false;

        for (Register reg : Register.values()) {
            if (reg.isTemporary()) {
                if (reg.isFloat()) {
                    floatRegisters.add(reg);
                }
                else {
                    registers.add(reg);
                }
            }
        }

        registerAssociation.putByKey("rr", Register.RETURN_VALUE);

        code.append(String.format(ASSIGNMENT_FLOAT, "TRUE", "1"));
        code.append(String.format(ASSIGNMENT_FLOAT, "FALSE", "0"));
        code.append(INIT_POINTERS);
    }

    public void generate(List<TacEntry> tacList, String outputFile) {

        for (TacEntry entry : tacList) {

            if (storeConditions && (entry.getOp() != TacEntry.Operation.STARTER && entry.getOp() != TacEntry.Operation.ENDER)) {
                statementCheckConditions.get(statementLabelCount).add(entry);
                continue;
            }

            processEntry(entry);
        }

        code.append(EXIT);

        File file = new File(outputFile);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(code.toString());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processEntry(TacEntry operation) {

        switch (operation.getOp()) {
            case DECLARE:
                if (globalContext) {
                    declareGlobal(operation.getRes(), operation.getArg1(), operation.getArg2(), operation.getTag());
                }
                else {
                    declareLocal(operation.getRes(), operation.getArg1(), operation.getArg2());
                }
                break;
            case LITERAL:
                setLiteral(operation.getRes(), operation.getArg1());
                break;
            case EQUAL:
                equality(operation.getRes(), operation.getArg1());
                break;
            case NEG:
                negation(operation.getRes(), operation.getArg1());
                break;
            case ADD:
                addition(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case SUB:
                subtraction(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case MUL:
                multiplication(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case DIV:
                division(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case MOD:
                modulo(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case AND:
                and(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case OR:
                or(operation.getRes(), operation.getArg1(), operation.getArg2());
                break;
            case COMPARISON:
                comparison(operation.getRes(), operation.getArg1(), operation.getArg2(), operation.getTag());
                break;
            case STARTER:
                addStatement(operation.getTag(), operation.getArg1());
                break;
            case ENDER:
                setStatementEnd(operation.getTag(), operation.getArg1(), operation.getArg2());
                break;
            case FUNC_DEC:
                declareFunction(operation.getArg1(), operation.getTag());
                break;
            case PARAM_DEC:
                popParameter(operation.getRes(), operation.getArg1());
                break;
            case PARAM:
                pushParameter(operation.getRes());
                break;
            case RETURN:
                returnFunction();
                break;
            case CALL:
                callFunction(operation.getTag());
                break;
        }
    }

    private void declareGlobal(String dest, String datatype, String arrSize, String name) {

        switch (datatype) {
            case "char" -> datatype = "byte";
            case "int" -> datatype = "word";
            case "float" -> datatype = "float";
        }

        if (arrSize == null) {
            if (datatype.equals("float")) floatAssociation.put(dest, true);
            globalAssociation.put(dest, name + "_");
            code.append(String.format(ASSIGNMENT_GLOBAL, name + "_", datatype));
        }
        else {

            int size = Integer.parseInt(arrSize);

            for (int i = 0; i < size; i++) {
                String reg = String.format("(%d)%s", i, dest);
                if (datatype.equals("float")) floatAssociation.put(reg, true);
                globalAssociation.put(reg, name + i + "_");
                code.append(String.format(ASSIGNMENT_GLOBAL, name + i + "_", datatype));
            }
        }
    }

    private void declareLocal(String dest, String datatype, String arrSize) {

        if (arrSize == null) {
            if (datatype.equals("float")) floatAssociation.put(dest, true);
            stackAssociation.put(dest, new Integer[]{ stackPointer - 4, stackPointer - 8 });

            code.append(String.format(SP_MOVE_DOWN, 4));
            stackPointer -= 4;
        }
        else {
            int size = Integer.parseInt(arrSize);

            for (int i = 0; i < size; i++) {

                String reg = String.format("(%d)%s", i, dest);
                int startOffset = (stackPointer - 4) * (i + 1);

                if (datatype.equals("float")) floatAssociation.put(reg, true);
                stackAssociation.put(reg, new Integer[]{ startOffset, startOffset - 4 });
            }

            code.append(String.format(SP_MOVE_DOWN, size * 4));
            stackPointer -= size * 4;
        }
    }

    private void setLiteral(String dest, String literal) {

        Register destRegister;

        try {
            Integer.parseInt(literal);
            destRegister = associateRegister(dest);
        } catch (NumberFormatException e) {
            destRegister = associateRegister(dest, true);
            floatAssociation.put(dest, true);
        }

        if (destRegister.isFloat()) {
            code.append(String.format(ASSIGNMENT_FLOAT, dest, literal));
            code.append(String.format(LOAD_FLOAT, destRegister, dest));
        }
        else {
            code.append(String.format(LOAD_LIT, destRegister, literal));
        }

        declareLocal(dest, "", null);
    }

    private void equality(String dest, String src) {

        Register srcRegister = associateRegister(src);
        Register destRegister = associateRegister(dest);

        equality(destRegister, srcRegister);
    }

    private void equality(Register destRegister, Register srcRegister) {

        if (destRegister.isFloat() && srcRegister.isFloat()) {
            code.append(String.format(FREG_TO_FREG, destRegister, srcRegister));
        }
        else if (destRegister.isFloat() && !srcRegister.isFloat()) {
            code.append(String.format(REG_TO_FREG, srcRegister, destRegister));
            code.append(String.format(INT_TO_FLOAT, destRegister, destRegister));
        }
        else if (!destRegister.isFloat() && srcRegister.isFloat()) {
            code.append(String.format(FLOAT_TO_INT, srcRegister, srcRegister));
            code.append(String.format(FREG_TO_REG, destRegister, srcRegister));
        }
        else {
            code.append(String.format(REG_TO_REG, destRegister, srcRegister));
        }
    }

    private void negation(String dest, String src) {

        Register srcRegister = associateRegister(src);
        Register destRegister = associateRegister(dest);

        if (destRegister.isFloat() && srcRegister.isFloat()) {
            code.append(String.format(NOT_FLOAT, destRegister, srcRegister));
        }
        else if (destRegister.isFloat() && !srcRegister.isFloat()) {
            equality(destRegister, srcRegister);
            code.append(String.format(NOT_FLOAT, destRegister, destRegister));
        }
        else if (!destRegister.isFloat() && srcRegister.isFloat()) {
            equality(destRegister, srcRegister);
            code.append(String.format(NOT, destRegister, destRegister));
        }
        else {
            code.append(String.format(NOT, destRegister, srcRegister));
        }
    }

    private void addition(String dest, String operand1, String operand2) {

        Register destReg = associateRegister(dest);
        Register operand1Reg = associateRegister(operand1);
        Register operand2Reg = associateRegister(operand2);

        Register auxReg;

        if (operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            code.append(String.format(ADDITION_FLOAT, auxReg, operand1Reg, operand2Reg));
        }
        else if (operand1Reg.isFloat() && !operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand2Reg);
            code.append(String.format(ADDITION_FLOAT, auxReg, operand1Reg, auxReg));
        }
        else if (!operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand1Reg);
            code.append(String.format(ADDITION_FLOAT, auxReg, operand2Reg, auxReg));
        }
        else {
            auxReg = getRegister(false, false);
            code.append(String.format(ADDITION, auxReg, operand1Reg, operand2Reg));
            equality(destReg, auxReg);
        }

        if (auxReg.isFloat()) {
            replaceRegister(auxReg, destReg);
            floatAssociation.put(dest, true);
        }
    }

    private void subtraction(String dest, String operand1, String operand2) {

        Register destReg = associateRegister(dest);
        Register operand1Reg = associateRegister(operand1);
        Register operand2Reg = associateRegister(operand2);

        Register auxReg;

        if (operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            code.append(String.format(SUBTRACTION_FLOAT, auxReg, operand1Reg, operand2Reg));
        }
        else if (operand1Reg.isFloat() && !operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand2Reg);
            code.append(String.format(SUBTRACTION_FLOAT, auxReg, operand1Reg, auxReg));
        }
        else if (!operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand1Reg);
            code.append(String.format(SUBTRACTION_FLOAT, auxReg, operand2Reg, auxReg));
        }
        else {
            auxReg = getRegister(false, false);
            code.append(String.format(SUBTRACTION, auxReg, operand1Reg, operand2Reg));
            equality(destReg, auxReg);
        }

        if (auxReg.isFloat()) {
            replaceRegister(auxReg, destReg);
            floatAssociation.put(dest, true);
        }
    }

    private void multiplication(String dest, String operand1, String operand2) {

        Register destReg = associateRegister(dest);
        Register operand1Reg = associateRegister(operand1);
        Register operand2Reg = associateRegister(operand2);

        Register auxReg;

        if (operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            code.append(String.format(MULTIPLICATION_FLOAT, auxReg, operand1Reg, operand2Reg));
        }
        else if (operand1Reg.isFloat() && !operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand2Reg);
            code.append(String.format(MULTIPLICATION_FLOAT, auxReg, operand1Reg, auxReg));
        }
        else if (!operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand1Reg);
            code.append(String.format(MULTIPLICATION_FLOAT, auxReg, operand2Reg, auxReg));
        }
        else {
            auxReg = getRegister(false, false);
            code.append(String.format(MULTIPLICATION, auxReg, operand1Reg, operand2Reg));
            equality(destReg, auxReg);
        }

        if (auxReg.isFloat()) {
            replaceRegister(auxReg, destReg);
            floatAssociation.put(dest, true);
        }
    }

    private void division(String dest, String operand1, String operand2) {

        Register destReg = associateRegister(dest);
        Register operand1Reg = associateRegister(operand1);
        Register operand2Reg = associateRegister(operand2);

        Register auxReg;

        if (operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            code.append(String.format(DIVISION_FLOAT, auxReg, operand1Reg, operand2Reg));
        }
        else if (operand1Reg.isFloat() && !operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand2Reg);
            code.append(String.format(DIVISION_FLOAT, auxReg, operand1Reg, auxReg));
        }
        else if (!operand1Reg.isFloat() && operand2Reg.isFloat()) {
            auxReg = getRegister(true, true);
            equality(auxReg, operand1Reg);
            code.append(String.format(DIVISION_FLOAT, auxReg, operand2Reg, auxReg));
        }
        else {
            auxReg = getRegister(false, false);
            code.append(String.format(DIVISION, operand1Reg, operand2Reg));
            code.append(String.format(MOVE_FROM_LO, auxReg));
            equality(destReg, auxReg);
        }

        if (auxReg.isFloat()) {
            replaceRegister(auxReg, destReg);
            floatAssociation.put(dest, true);
        }
    }

    private void modulo(String dest, String operand1, String operand2) {

        Register destReg = associateRegister(dest);
        Register operand1Reg = associateRegister(operand1);
        Register operand2Reg = associateRegister(operand2);

        Register auxReg1, auxReg2, auxReg3;

        // DIV = operand1Reg / operand2Reg
        // MOD = operand2Reg * (DIV - abs(DIV))

        if (operand1Reg.isFloat() && operand2Reg.isFloat()) {

            auxReg1 = getRegister(true, true);
            auxReg2 = getRegister(true, false);

            code.append(String.format(DIVISION_FLOAT, auxReg1, operand1Reg, operand2Reg));
            code.append(String.format(ABSOLUTE_FLOAT, auxReg2, auxReg1));
            code.append(String.format(SUBTRACTION_FLOAT, auxReg1, auxReg1, auxReg2));
            code.append(String.format(MULTIPLICATION_FLOAT, auxReg1, auxReg1, operand2Reg));
        }
        else if (operand1Reg.isFloat() && !operand2Reg.isFloat()) {

            auxReg1 = getRegister(true, true);
            auxReg2 = getRegister(true, true);
            auxReg3 = getRegister(true, false);

            equality(auxReg1, operand2Reg);

            code.append(String.format(DIVISION_FLOAT, auxReg2, operand1Reg, auxReg1));
            code.append(String.format(ABSOLUTE_FLOAT, auxReg3, auxReg2));
            code.append(String.format(SUBTRACTION_FLOAT, auxReg2, auxReg2, auxReg3));
            code.append(String.format(MULTIPLICATION_FLOAT, auxReg1, auxReg1, auxReg2));
        }
        else if (!operand1Reg.isFloat() && operand2Reg.isFloat()) {

            auxReg1 = getRegister(true, true);
            auxReg2 = getRegister(true, true);
            auxReg3 = getRegister(true, false);

            equality(auxReg1, operand1Reg);

            code.append(String.format(DIVISION_FLOAT, auxReg2, auxReg1, operand2Reg));
            code.append(String.format(ABSOLUTE_FLOAT, auxReg3, auxReg2));
            code.append(String.format(SUBTRACTION_FLOAT, auxReg2, auxReg2, auxReg3));
            code.append(String.format(MULTIPLICATION_FLOAT, auxReg1, operand2Reg, auxReg2));
        }
        else {
            auxReg1 = getRegister(false, false);
            code.append(String.format(DIVISION, operand1Reg, operand2Reg));
            code.append(String.format(REG_TO_REG, auxReg1, Register.DIVISION_REMAINDER));
            equality(destReg, auxReg1);
        }

        if (auxReg1.isFloat()) {
            replaceRegister(auxReg1, destReg);
            floatAssociation.put(dest, true);
        }
    }

    private void and(String dest, String arg1, String arg2) {

        Register destReg = associateRegister(dest);
        Register reg1 = associateRegister(arg1);
        Register reg2 = associateRegister(arg2);

        code.append(String.format(AND, destReg, reg1, reg2));
    }

    private void or(String dest, String arg1, String arg2) {

        Register destReg = associateRegister(dest);
        Register reg1 = associateRegister(arg1);
        Register reg2 = associateRegister(arg2);

        code.append(String.format(OR, destReg, reg1, reg2));
    }

    private void comparison(String dest, String arg1, String arg2, String operator) {

        Register destReg = associateRegister(dest);
        Register reg1 = associateRegister(arg1);
        Register reg2 = associateRegister(arg2);

        Register auxReg1 = getRegister(true, true);
        Register auxReg2 = getRegister(true, false);

        equality(auxReg1, reg1);
        equality(auxReg2, reg2);

        switch (operator) {
            case "==", "#=" -> code.append(String.format(COMPARE_EQUAL, auxReg1, auxReg2));
            case "<=" -> code.append(String.format(COMPARE_LESS_EQUAL, auxReg1, auxReg2));
            case ">=" -> code.append(String.format(COMPARE_LESS_EQUAL, auxReg2, auxReg1));
            case "<" -> code.append(String.format(COMPARE_LESS, auxReg1, auxReg2));
            case ">" -> code.append(String.format(COMPARE_LESS, auxReg2, auxReg1));
        }

        code.append(String.format(LOAD_FLOAT, auxReg1, "FALSE"));
        code.append(String.format(LOAD_FLOAT, auxReg2, "TRUE"));

        if (operator.equals("#=")) {
            code.append(String.format(MOVE_FALSE, auxReg1, auxReg2));
        }
        else {
            code.append(String.format(MOVE_TRUE, auxReg1, auxReg2));
        }

        equality(destReg, auxReg1);
    }

    private void addStatement(String type, String conditionReg) {

        storeConditions = false;

        switch (type) {
            case "if" -> handleIf(conditionReg);
            case "elif" -> handleElseIf(conditionReg);
            case "else" -> handleElse();
            case "conditions" -> handleConditions();
            case "while" -> handleWhile(conditionReg);
            case "do_while" -> handleDoWhile();
            case "for" -> handleFor(conditionReg);
        }
    }

    private void handleIf(String conditionReg) {

        cleanRegisters();

        code.append(String.format(GOTO, "check" + statementLabelCount));

        code.append(String.format(LABEL, "if" + statementLabelCount));

        LinkedList<String[]> checkInstructions = new LinkedList<>();
        statementCheckBranching.put(statementLabelCount, checkInstructions);

        checkInstructions.add(new String[]{ BRANCH_IF_GT0, "if", String.valueOf(statementLabelCount), conditionReg });

        statementIdStack.addFirst(statementLabelCount);
        statementLabelCount++;
    }

    private void handleElseIf(String conditionReg) {

        code.append(String.format(LABEL, "elif" + statementLabelCount));

        int statementNestId = statementIdStack.peek();
        LinkedList<String[]> checkInstructions = statementCheckBranching.get(statementNestId);

        checkInstructions.add(new String[]{ BRANCH_IF_GT0, "elif", String.valueOf(statementLabelCount), conditionReg });

        statementLabelCount++;
    }

    private void handleElse() {

        code.append(String.format(LABEL, "else" + statementLabelCount));

        int statementNestId = statementIdStack.peek();
        LinkedList<String[]> checkInstructions = statementCheckBranching.get(statementNestId);

        checkInstructions.add(new String[]{ GOTO, "else", String.valueOf(statementLabelCount)});

        statementLabelCount++;
    }

    private void handleConditions() {
        storeConditions = true;
        statementCheckConditions.put(statementLabelCount, new LinkedList<>());
    }


    private void handleWhile(String conditionReg) {

        cleanRegisters();

        code.append(String.format(GOTO, "check" + statementLabelCount));

        code.append(String.format(LABEL, "while" + statementLabelCount));

        LinkedList<String[]> checkInstructions = new LinkedList<>();
        statementCheckBranching.put(statementLabelCount, checkInstructions);

        checkInstructions.add(new String[]{ BRANCH_IF_GT0, "while", String.valueOf(statementLabelCount), conditionReg });

        statementIdStack.addFirst(statementLabelCount);
        statementLabelCount++;
    }

    private void handleDoWhile() {

        code.append(String.format(LABEL, "dowhile" + statementLabelCount));

        LinkedList<String[]> checkInstructions = new LinkedList<>();
        statementCheckBranching.put(statementLabelCount, checkInstructions);

        checkInstructions.add(new String[]{ BRANCH_IF_GT0, "dowhile", String.valueOf(statementLabelCount)});

        statementIdStack.addFirst(statementLabelCount);
        statementLabelCount++;
    }

    private void handleFor(String conditionReg) {

        cleanRegisters();

        code.append(String.format(GOTO, "check" + statementLabelCount));

        code.append(String.format(LABEL, "for" + statementLabelCount));

        LinkedList<String[]> checkInstructions = new LinkedList<>();
        statementCheckBranching.put(statementLabelCount, checkInstructions);

        checkInstructions.add(new String[]{ BRANCH_IF_GT0, "for", String.valueOf(statementLabelCount), conditionReg });

        statementIdStack.addFirst(statementLabelCount);
        statementLabelCount++;
    }

    @SuppressWarnings("ConstantConditions")
    private void setStatementEnd(String type, String doWhileConditionReg, String nextStatement) {

        int statementNestId = statementIdStack.peek();

        switch (type) {
            case "if", "elif", "else" -> {
                cleanRegisters();
                code.append(String.format(GOTO, "continue" + statementNestId));
            }
        }

        if (nextStatement != null) return;

        cleanRegisters();

        code.append(String.format(LABEL, "check" + statementNestId));

        LinkedList<String[]> checkInstructions = statementCheckBranching.remove(statementNestId);

        for (String[] instruction : checkInstructions) {

            LinkedList<TacEntry> conditionEntries = statementCheckConditions.remove(Integer.parseInt(instruction[2]));

            if (conditionEntries != null) {

                for (TacEntry tacEntry : conditionEntries) {
                    processEntry(tacEntry);
                }
            }
        }

        for (String[] instruction : checkInstructions) {

            instruction[1] += instruction[2];

            if (instruction[0].equals(BRANCH_IF_GT0)) {

                Register reg;
                if (doWhileConditionReg != null) {
                    reg = associateRegister(doWhileConditionReg);
                }
                else {
                    reg = associateRegister(instruction[3]);
                }

                if (reg.isFloat()) {
                    Register auxReg = getRegister(false, false);
                    equality(auxReg, reg);
                    code.append(String.format(BRANCH_IF_GT0, auxReg, instruction[1]));
                }
                else {
                    code.append(String.format(BRANCH_IF_GT0, reg, instruction[1]));
                }
            }
            else {
                code.append(String.format(GOTO, instruction[1]));
            }
        }

        code.append(String.format(LABEL, "continue" + statementNestId));

        statementIdStack.pop();
    }

    private void cleanRegisters() {
        for (Map.Entry<String, Register> register : registerAssociation.entrySet()) {
            if (register.getValue() != Register.RETURN_VALUE) persistRegister(register.getValue());
        }
        removeRegisterAssociations();
    }

    private void declareFunction(String datatype, String name) {

        if (globalContext) {
            globalContext = false;
            cleanRegisters();
            code.append(String.format(GOTO, "_CEO"));
        }

        code.append(String.format(LABEL, "_" + name));

        if (!name.equals("CEO")) {
            code.append(String.format(REG_TO_MEM, Register.RETURN_ADDRESS, 0, Register.FRAME_POINTER));
        }

        stackPointer = 0;
    }

    private void pushParameter(String paramRegister) {
        pendingParameters.add(paramRegister);
    }

    private void popParameter(String destRegister, String datatype) {

        int start = stackPointer - 4;
        int end = start - 4;

        stackPointer = start;

        stackAssociation.put(destRegister, new Integer[]{ start, end });

        if (datatype.equals("float")) floatAssociation.put(destRegister, true);
    }

    private void callFunction(String name) {

        for (Map.Entry<String, Register> register : registerAssociation.entrySet()) {
            if (register.getValue() != Register.RETURN_VALUE) persistRegister(register.getValue());
        }

        code.append(String.format(REG_TO_MEM, Register.FRAME_POINTER, -4, Register.STACK_POINTER));

        // Leave space also for return address, stored at function start
        code.append(String.format(SP_MOVE_DOWN, 8));

        code.append(String.format(REG_TO_REG, Register.FRAME_POINTER, Register.STACK_POINTER));

        for (String paramRegister : pendingParameters) {

            Register reg = associateRegister(paramRegister);

            if (reg.isFloat()) {
                Register auxReg = getRegister(false, false);
                code.append(String.format(FREG_TO_REG, auxReg, reg));
                code.append(String.format(REG_TO_MEM, auxReg, -4, Register.STACK_POINTER));
            }
            else {
                code.append(String.format(REG_TO_MEM, reg, -4, Register.STACK_POINTER));
            }

            code.append(String.format(SP_MOVE_DOWN, 4));
        }

        code.append(String.format(CALL, "_" + name));

        removeRegisterAssociations();
        pendingParameters.clear();
    }

    private void removeRegisterAssociations() {
        registerAssociation.clear();
        registerAssociation.put("rr", Register.RETURN_VALUE);
    }

    private void returnFunction() {

        code.append(String.format(REG_TO_REG, Register.STACK_POINTER, Register.FRAME_POINTER));
        code.append(String.format(SP_MOVE_UP, 8));

        code.append(String.format(MEM_TO_REG, Register.RETURN_ADDRESS, 0, Register.FRAME_POINTER));
        code.append(String.format(MEM_TO_REG, Register.FRAME_POINTER, 4, Register.FRAME_POINTER));

        code.append(String.format(RETURN, Register.RETURN_ADDRESS));

        removeRegisterAssociations();
    }

    private void replaceRegister(Register newReg, Register oldReg) {
        String tacRegister = registerAssociation.removeByValue(oldReg);
        registerAssociation.putByValue(newReg, tacRegister);
    }

    private Register associateRegister(String tacRegister, boolean floatRegister) {

        /*
        // For future version: array access

        if (tacRegister.charAt(0) == '(' && tacRegister.charAt(1) == 'r') {
            String indexTacReg = tacRegister.substring(tacRegister.indexOf("(") + 1);
            indexTacReg = indexTacReg.substring(0, indexTacReg.indexOf(")"));
            String tacReg = indexTacReg.substring(indexTacReg.indexOf(")") + 1);

            Register indexReg = associateRegister(indexTacReg, false);

            Integer frameOffset = stackAssociation.get("(0)" + tacReg)[0];

            Register auxReg = getRegister(false, false);

            code.append(String.format(LOAD_LIT, auxReg, 4));
            code.append(String.format(MULTIPLICATION, auxReg, indexReg, auxReg));
            code.append(String.format(ADDITION, auxReg, auxReg, Register.FRAME_POINTER));
        }
         */

        Register register = registerAssociation.getByKey(tacRegister);

        if (register == null) {

            floatRegister = floatRegister || floatAssociation.get(tacRegister) != null;

            register = getRegister(floatRegister, true);

            registerAssociation.putByValue(register, tacRegister);

            String globalLabel = globalAssociation.get(tacRegister);
            Integer[] boundaries = stackAssociation.get(tacRegister);

            if (boundaries != null) {
                loadFromStack(register, tacRegister);
            }
            else if (globalLabel != null) {
                loadFromGlobal(register, globalLabel);
            }
        }

        return register;
    }

    private Register associateRegister(String tacRegister) {
        return associateRegister(tacRegister, false);
    }

    private Register getRegister(boolean floatRegister, boolean queueAgain) {

        LinkedList<Register> registers;

        if (floatRegister) {
            registers = this.floatRegisters;
        }
        else {
            registers = this.registers;
        }

        Register register = registers.poll();

        persistRegister(register);
        registerAssociation.removeByValue(register);

        if (queueAgain) {
            registers.add(register);
        }
        else {
            registers.addFirst(register);
        }

        return register;
    }

    private void loadFromStack(Register register, String tacRegister) {

        Integer[] boundaries = stackAssociation.get(tacRegister);
        int framePointerOffset = boundaries[0];

        if (register.isFloat()) {
            code.append(String.format(MEM_TO_FREG, register, framePointerOffset, Register.FRAME_POINTER));
        }
        else {
            code.append(String.format(MEM_TO_REG, register, framePointerOffset,  Register.FRAME_POINTER));
        }
    }

    private void loadFromGlobal(Register register, String globalLabel) {

        if (register.isFloat()) {
            code.append(String.format(MEM_TO_FREG_LABEL, register, globalLabel));
        }
        else {
            code.append(String.format(MEM_TO_REG_LABEL, register, globalLabel));
        }
    }

    void persistRegister(Register register) {

        String tacRegister = registerAssociation.getByValue(register);

        if (tacRegister == null) return;

        String globalLabel = globalAssociation.get(tacRegister);
        Integer[] boundaries = stackAssociation.get(tacRegister);

        if (boundaries != null) {
            storeToStack(register, tacRegister);
        }
        else if (globalLabel != null) {
            storeToGlobal(register, globalLabel);
        }
    }

    private void storeToStack(Register register, String tacRegister) {

        Integer[] boundaries =  stackAssociation.get(tacRegister);
        int framePointerOffset = boundaries[0];

        if (register.isFloat()) {
            code.append(String.format(FREG_TO_MEM, register, framePointerOffset, Register.FRAME_POINTER));
        }
        else {
            code.append(String.format(REG_TO_MEM, register, framePointerOffset,  Register.FRAME_POINTER));
        }
    }

    private void storeToGlobal(Register register, String globalLabel) {

        if (register.isFloat()) {
            code.append(String.format(FREG_TO_MEM_LABEL, register, globalLabel));
        }
        else {
            code.append(String.format(REG_TO_MEM_LABEL, register, globalLabel));
        }
    }
}