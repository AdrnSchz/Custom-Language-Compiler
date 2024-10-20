package stages.backend;

import entities.Node;
import entities.TacEntry;
import symbols.SymbolAttribute;
import symbols.SymbolsTable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class IntermediateCodeGenerator {
    private final Node parseTree;
    private final SymbolsTable symbolsTable;
    private List<TacEntry> tacList = new ArrayList<>();
    private int countRegisters = 0;
    private String prevReg;
    private TargetCodeGenerator tcg;

    public IntermediateCodeGenerator(Node parseTree, SymbolsTable symbolsTable, String outputFile) {
        this.parseTree = parseTree;
        this.symbolsTable = symbolsTable;
        LinkedList<Node> init = parseTree.getChilds();
        Node root = init.get(0);
        generateTacList(parseTree);

        tcg = new TargetCodeGenerator();
        tcg.generate(tacList, outputFile);
    }

    private String generateRegister() {
        countRegisters++;
        return "r" + countRegisters;
    }

    private void generateTacList(Node currNode) {
        String state = currNode.getStatement();

        switch (state) {
            case "declaration":
                globalsDeclaration(currNode);
                break;
            case "expression":
                switch (currNode.getChilds().get(0).getStatement()) {
                    case "name" -> funcCallOrVarAssign(currNode);
                    case "datatype" -> varDeclaration(currNode);
                    case "arr_decl" -> arrDeclaration(currNode.getChilds().get(0));
                    case "conditional" -> conditional(currNode.getChilds().get(0));
                    case "while_loop" -> whileLoop(currNode.getChilds().get(0));
                    case "for_loop" -> forHandle(currNode.getChilds().get(0));
                    case "switch_" -> switchHandle(currNode.getChilds().get(0));
                }
                break;
            case "var_decl_assign":
            case "arr_assign":
            case "assignation":
                assignation(currNode);
                break;
            case "main_":
                mainDec(currNode);
                break;
            case "return_":
                returnFunc(currNode);
                break;
            default:
                for (Node child : currNode.getChilds()) {
                    generateTacList(child);
                }
        }
    }

    private void mainDec(Node currNode) {
        //<main_> ::= CEO ¡<func_body>!
        addFuncDec(null, "CEO");
        generateTacList(currNode.getChilds().get(2));
    }

    private void arrDeclaration(Node currNode) {
        //fam <arr_dim> <datatype> name <arr_assign> .
        List<Node> arrChildren = currNode.getChilds();

        String size2 = getArrDimension(arrChildren.get(1));
        String datatype = getDatatypeSize(arrChildren.get(2));

        //Declare array
        String reg = addDeclaration(arrChildren.get(3).getValue(), datatype, size2);
        symbolsTable.lookUp(arrChildren.get(3).getValue(), arrChildren.get(3).getId()).setRegister(reg);

        //Assign values
        prevReg = reg;
        generateTacList(arrChildren.get(4));
    }

    private void varDeclaration(Node currNode) {
        //<datatype> name <var_decl_assign>
        List<Node> varChildren = currNode.getChilds();

        String datatype = getDatatypeSize(varChildren.get(0));
        String name = varChildren.get(1).getValue();

        //Declare variable and store it in symbols table
        String varReg = addDeclaration(name, datatype, null);
        symbolsTable.lookUp(varChildren.get(1).getValue(), varChildren.get(1).getId()).setRegister(varReg);
        prevReg = varReg;

        //Explore assignation
        generateTacList(varChildren.get(2));
    }
    private void funcCallOrVarAssign(Node currNode) {
        //name <func_or_var>
        List<Node> children = currNode.getChilds();

        //<func_call> | <var_assign> | <arr_pos> <arr_assign>
        List<Node> funcOrVar = children.get(1).getChilds();

        switch (funcOrVar.get(0).getStatement()) {
            case "func_call" -> {
                //¿<value_list>?.
                List<Node> funcCall = funcOrVar.get(0).getChilds();
                evaluateParams(children.get(0).getValue(), funcCall.get(1));
                addFuncCall(children.get(0).getValue());
            }
            case "var_assign" -> {
                prevReg = symbolsTable.lookUp(children.get(0).getValue(), children.get(0).getId()).getRegister();
                generateTacList(funcOrVar.get(0));
            }
            case "arr_pos" -> {
                String offset = getArrPos(funcOrVar.get(0));
                String arr = symbolsTable.lookUp(children.get(0).getValue(), children.get(0).getId()).getRegister();
                prevReg = "(" + offset + ")" + arr;
                generateTacList(funcOrVar.get(1));
            }
        }
    }

    private void forHandle(Node currNode) {
        TacEntry.Operation operationFor = null;
        TacEntry.Operation operationFor2 = null;
        String varValueFor = null;

        // <for_loop> ::= 4 ¿<for_decl>. <boolean_cond>. <for_iterator>? ¡<func_body>!
        List<Node> children = currNode.getChilds();

        //<for_decl>::= <for_var_type> name <var_decl_assign>
        Node for_decl = children.get(2);
        List<Node> childrenVarType = for_decl.getChilds().get(0).getChilds();

        // <for_var_type> ::= <datatype> | E
        if(!(childrenVarType.get(0).getStatement().equals("epsilon"))) {
            String datatype = getDatatypeSize(childrenVarType.get(0));
            // Declare 4 variable
            String varReg = addDeclaration(for_decl.getChilds().get(1).getValue(), datatype, null);
            symbolsTable.lookUp(for_decl.getChilds().get(1).getValue(), for_decl.getChilds().get(1).getId()).setRegister(varReg);
            prevReg = varReg;
        } else {
            //4 variable already declared
            prevReg = symbolsTable.lookUp(for_decl.getChilds().get(1).getValue(), for_decl.getChilds().get(1).getId()).getRegister();
        }
        // <var_decl_assign>
        generateTacList(for_decl.getChilds().get(2));

        // <boolean_cond>
        addStarter(evaluateBooleanCond(children.get(3)), "for");

        //<for_iterator> ::= name <it_change>
        Node for_iterator = children.get(5);
        String valueFor2 = symbolsTable.lookUp(for_iterator.getChilds().get(0).getValue(), for_iterator.getChilds().get(0).getId()).getRegister();

        //<it_change> ::= ++ | -- | <equal_assign_op> <var_value>
        Node it_change = for_iterator.getChilds().get(1);
        if (it_change.getChilds().get(0).getStatement().equals("++") || it_change.getChilds().get(0).getStatement().equals("--")) {
            operationFor = evaluateForOp(it_change.getChilds().get(0).getStatement());
        } else {
            operationFor2 = evaluateEqualOp(it_change.getChilds().get(0).getStatement());
            varValueFor = varValue(it_change.getChilds().get(1));

        }
        //<func_body>
        generateTacList(children.get(8));
        if (it_change.getChilds().get(0).getStatement().equals("++") || it_change.getChilds().get(0).getStatement().equals("--")) {
            // es ++ o --
            String litRx = addLoadLiteral("1");
            addEqualAssign(valueFor2, operationFor, litRx);
        } else {
            addEqualAssign(valueFor2, operationFor2, varValueFor);
        }
        addEnder(null, null, "for");
    }

    private void whileLoop(Node currNode) {
        // vibe <while_opt>
        List<Node> children = currNode.getChilds();

        if (children.get(1).getChilds().get(0).getStatement().equals("while_")) {
            while_(children.get(1).getChilds().get(0));
        }
        else {
            doWhile(children.get(1).getChilds().get(0));
        }
    }

    private void while_(Node currNode) {
        // check ¿<boolean_cond>? ¡<func_body>!
        List<Node> children = currNode.getChilds();

        addStarter(evaluateBooleanCond(children.get(2)), "while");
        generateTacList(children.get(5));
        addEnder(null, null, "while");
    }

    private void doWhile(Node currNode) {
        // ¡<func_body>! then check ¿<boolean_cond>?
        List<Node> children = currNode.getChilds();

        addStarter(null, "do_while");
        generateTacList(children.get(1));
        addEnder(evaluateBooleanCond(children.get(6)), null, "do_while");
    }

    private void conditional(Node currNode) {
        // <if_> <cond_else>
        Node condElseNode = currNode.getChilds().get(1);

        ifCases(currNode.getChilds().get(0));

        while (condElseNode != null && !condElseNode.getChilds().get(0).getStatement().equals("epsilon")) {

            condElseNode = ifCases(condElseNode.getChilds().get(1).getChilds().get(0));
        }
    }

    private Node ifCases(Node currNode) {
        // <if_> ::= like ¿<boolean_cond>? ¡<func_body>!
        // <elif> ::= like ¿<boolean_cond>? ¡<func_body>! <cond_else>
        // <else_> ::= ¡<func_body>!
        List<Node> children = currNode.getChilds();
        Node funcBodyNode;

        if (children.get(0).getStatement().equals("like")) {
            addStarter(evaluateBooleanCond(children.get(2)), currNode.getStatement().replace("_",""));
            funcBodyNode = children.get(5);
        }
        else {
            addStarter(null, currNode.getStatement().replace("_",""));
            funcBodyNode = children.get(1);
        }
        String nextTag = null;

        if (currNode.getStatement().equals("if_")) {
            if (!currNode.getParent().getChilds().get(1).getChilds().get(0).getStatement().equals("epsilon")) {
                nextTag = "elif";
            }
        }
        else if (currNode.getStatement().equals("elif")) {
            if (!children.get(7).getChilds().get(0).getStatement().equals("epsilon")) {
                nextTag = children.get(7).getChilds().get(1).getChilds().get(0).getStatement().replace("_","");
            }
        }

        generateTacList(funcBodyNode);
        addEnder(null, nextTag, currNode.getStatement().replace("_",""));

        if (children.size() == 8) {
            // elif
            return children.get(7);
        }
        return null;
    }

    private void returnFunc(Node currNode) {
        //throwback <return_value> .
        //<neg> <var_value>
        List<Node> returnNode = currNode.getChilds().get(1).getChilds();

        if (!returnNode.get(1).getChilds().get(0).getStatement().equals("literal")) {
            String returnValue = varValue(returnNode.get(1));
            returnValue = evaluateNeg(returnValue, returnNode.get(0));
            addStoreValue(returnValue, "rr");
        }
        else if (!returnNode.get(1).getChilds().get(0).getChilds().get(0).getChilds().get(0).getStatement().equals("null_lit")) {
            String returnValue = varValue(returnNode.get(1));
            returnValue = evaluateNeg(returnValue, returnNode.get(0));
            addStoreValue(returnValue, "rr");
        }
        addReturn();
    }

    private void switchHandle(Node currNode) {
        //<switch_> ::= swipe ¿name? ¡<case_> <default_>!
        List<Node> children = currNode.getChilds();
        String value = symbolsTable.lookUp(children.get(2).getValue(), children.get(2).getId()).getRegister();
        SymbolAttribute symbolAttribute = (SymbolAttribute) (symbolsTable.lookUp(children.get(2).getValue(), children.get(2).getId()));
        boolean hasDefaultCase = !children.get(6).getChilds().get(0).getStatement().equals("epsilon");
        evaluateCase(children.get(5), value, "if", hasDefaultCase);

        //<default_> ::= left: <case_body> | E
        List<Node> default_ = children.get(6).getChilds();
        if(!(default_.get(0).getStatement().equals("epsilon"))) {
            addStarter("else");
            evaluateCaseBody(default_.get(2));
            addEnder(null, null, "else");
        }
    }

    private void evaluateCase(Node currNode, String valueRx, String ifType, boolean hasDefaultCase) {
        //<case_> ::=  right <literal> : <case_body> <nested_case>
        addStarter("conditions");
        List<Node> case_ = currNode.getChilds();
        String literal = getLiteral(case_.get(1));
        String litRX = addLoadLiteral(literal);
        String resRX = addComparison(litRX, valueRx, "==");
        addStarter(resRX, ifType);
        evaluateCaseBody(case_.get(3));

        if(!case_.get(4).getChilds().get(0).getStatement().equals("epsilon")) {
            addEnder(null, "elif", ifType);
            evaluateCase(case_.get(4).getChilds().get(0), valueRx, "elif", hasDefaultCase);

        } else {
            if (hasDefaultCase) {
                addEnder(null, "else", ifType);
            }
            else {
                addEnder(null, null, ifType);
            }
        }
    }

    private void evaluateCaseBody(Node currNode) {
        //<case_body> ::= <func_body>  <break_> | E
        List<Node> case_body = currNode.getChilds();
        if(!(case_body.get(0).getChilds().get(0).getStatement().equals("epsilon"))) {
            generateTacList(case_body.get(0));
        }
    }

    private void globalsDeclaration(Node currNode) {
        //<arr_decl> | zombie name <func_decl> | <datatype> name <func_or_var_decl>
        List<Node> children = currNode.getChilds();
        String name = children.get(1).getValue();
        List<Node> funcDecChildren;
        List<Node> funcImpl;

        switch (children.get(0).getStatement()) {
            case "arr_decl" -> arrDeclaration(children.get(0));
            case "zombie" -> {
                //Declare void function
                addFuncDec(null, name);
                //¿ <func_param> ?  <func_impl>
                funcDecChildren = children.get(2).getChilds();

                //Add parameters if exist
                if (!funcDecChildren.get(1).getChilds().get(0).getStatement().equals("epsilon")) {
                    evaluateParamDec(funcDecChildren.get(1).getChilds().get(0));
                }

                //¡<func_body>! | .
                funcImpl = funcDecChildren.get(3).getChilds();
                if (!funcImpl.get(0).getStatement().equals(".")) {
                    generateTacList(funcImpl.get(1));
                }
            }
            case "datatype" -> {
                //<var_decl_assign> | <func_decl>
                List<Node> funcOrVarChildren = children.get(2).getChilds();

                if (funcOrVarChildren.get(0).getStatement().equals("var_decl_assign")) {
                    varDeclaration(currNode);
                } else {
                    String datatype = getDatatypeSize(children.get(0));
                    //Declare function
                    addFuncDec(datatype, name);
                    //¿ <func_param> ?  <func_impl>
                    funcDecChildren = funcOrVarChildren.get(0).getChilds();

                    //Add parameters if exist
                    if (!funcDecChildren.get(1).getChilds().get(0).getStatement().equals("epsilon")) {
                        evaluateParamDec(funcDecChildren.get(1).getChilds().get(0));
                    }

                    //¡<func_body>! | .
                    funcImpl = funcDecChildren.get(3).getChilds();
                    //Explore body if exists
                    if (!funcImpl.get(0).getStatement().equals(".")) {
                        generateTacList(funcImpl.get(1));
                    }
                }
            }
        }
    }

    private void evaluateParamDec(Node paramDec) {
        //<datatype> name <param_list>
        List<Node> params = paramDec.getChilds();

        String size = getDatatypeSize(params.get(0));
        String paramReg = addParamDec(params.get(1).getValue(), size);
        symbolsTable.lookUp(params.get(1).getValue(), params.get(1).getId()).setRegister(paramReg);

        if (!params.get(2).getChilds().get(0).getStatement().equals("epsilon")) {
            //, <param_decl> | E
            evaluateParamDec(params.get(2).getChilds().get(1));
        }
    }

    private void assignation(Node currNode) {
        //= <var_op> . | <equal_assign_op>  <var_op> . | = <arr_assign_val> . | E | .
        List<Node> children = currNode.getChilds();

        if (!children.get(0).getStatement().equals("epsilon") && !children.get(0).getStatement().equals(".")) {
            if (children.get(0).getStatement().equals("=")) {
                //Variable operation case
                if (children.get(1).getStatement().equals("var_op")) {
                    //<neg> <var_op_val>
                    List<Node> varOpChildren = children.get(1).getChilds();

                    String source = evaluateVarOpValue(varOpChildren.get(1));
                    source = evaluateNeg(source, varOpChildren.get(0));
                    addStoreValue(source, prevReg);
                }
                //Array assignation value
                else {
                    //[ <arr_list> ] | <var_value>
                    List<Node> arrAssignChildren = children.get(1).getChilds();
                    if (arrAssignChildren.get(0).getStatement().equals("var_value")) {
                        String source = varValue(arrAssignChildren.get(0));
                        addStoreValue(source, prevReg);
                    }
                    else {
                        assignArrValues(arrAssignChildren.get(1), 0);
                    }
                }
            }
            //Equal assign operation case
            else {
                //Get operation
                TacEntry.Operation op = evaluateEqualOp(children.get(0).getChilds().get(0).getValue());
                //<neg> <var_op_val>
                List<Node> varOpChildren = children.get(1).getChilds();

                String reg = evaluateVarOpValue(varOpChildren.get(1));
                reg = evaluateNeg(reg, varOpChildren.get(0));
                addEqualAssign(prevReg, op, reg);
            }
        }
    }

    private void assignArrValues(Node arrList, int offset) {
        //<arr_value> <arr_value_list>
        List<Node> arrListChildren = arrList.getChilds();

        //Get array value
        //<neg> <var_value>
        String source = varValue(arrListChildren.get(0).getChilds().get(1));
        source = evaluateNeg(source, arrListChildren.get(0).getChilds().get(0));

        //Store value in position of arr
        String arrPos = "(" + offset + ")" + prevReg;
        addStoreValue(source, arrPos);

        //Continue if more assigns
        //, <arr_list> | E
        if (arrListChildren.get(1).getChilds().get(0).getStatement().equals(",")) {
            assignArrValues(arrListChildren.get(1).getChilds().get(1), offset + 1);
        }
    }

    private TacEntry.Operation evaluateEqualOp(String operation) {
        return switch (operation) {
            case "+=" -> TacEntry.Operation.ADD;
            case "-=" -> TacEntry.Operation.SUB;
            case "*=" -> TacEntry.Operation.MUL;
            default -> TacEntry.Operation.DIV;
        };
    }

    private TacEntry.Operation evaluateForOp(String operation) {
        return switch (operation) {
            case "++" -> TacEntry.Operation.ADD;
            default -> TacEntry.Operation.SUB;
        };
    }

    private String evaluateVarOpValue(Node currNode) {
        //<var_value> <nested_op> | ( <var_op> )
        List<Node> children = currNode.getChilds();
        String dest = null;

        if (!children.get(0).getStatement().equals("(")) {
            dest = varValue(children.get(0));
            dest = nestedOp(dest, children.get(1));
        }
        return dest;
    }

    private String nestedOp(String arg1, Node currNode) {
        //<operator> <var_op_val> | E
        List<Node> children = currNode.getChilds();

        if (children.get(0).getStatement().equals("epsilon")) {
            return arg1;
        }

        TacEntry.Operation operation = getOperation(children.get(0));
        if (operation.equals(TacEntry.Operation.ADD) || operation.equals(TacEntry.Operation.SUB)) {
            String arg2 = evaluateVarOpValue(children.get(1));
            return addOperation(arg1, arg2, operation);
        }
        else {
            String reg = varValue(children.get(1).getChilds().get(0));
            String dest = addOperation(arg1, reg, operation);

            if (!children.get(1).getChilds().get(1).getChilds().get(0).getStatement().equals("epsilon")) {
                dest = nestedOp(dest, children.get(1).getChilds().get(1));
                return dest;
            }
            return dest;
        }
    }

    private String varValue(Node currNode) {
        //name <arr_or_func> | <literal>
        List<Node> children = currNode.getChilds();
        String dest;

        if (children.get(0).getStatement().equals("name")) {
            //Get arr_or_func child: <arr_pos> | ¿<value_list>? | E
            Node arrOrFunc = children.get(1).getChilds().get(0);
            String name = children.get(0).getValue();

            if (arrOrFunc.getStatement().equals("arr_pos")){
                String offset = getArrPos(arrOrFunc);
                String arr = symbolsTable.lookUp(name, children.get(0).getId()).getRegister();
                dest = "(" + offset + ")" + arr;
            }
            else if (arrOrFunc.getStatement().equals("¿")) {
                //Value list of parameters
                Node params = children.get(1).getChilds().get(1);
                evaluateParams(name, params);
                addFuncCall(name);
                dest = "rr";
            }
            else {
                dest = symbolsTable.lookUp(children.get(0).getValue(), children.get(0).getId()).getRegister();
            }
        }
        else {
            String literal = getLiteral(children.get(0));
            dest = addLoadLiteral(literal);
        }
        return dest;
    }

    private String getLiteral(Node literal) {
        //<literal> ::= <comparable_literal> | <non_comparable_literal>
        List<Node> litChildren = literal.getChilds();

        //<non_comparable_literal> ::= str_lit | null_lit
        //<comparable_literal> ::=  numeric_literal | decimal_literal | boolean_literal | char_literal
        if (litChildren.get(0).getChilds().get(0).getStatement().equals("char_lit")) {
            String character = litChildren.get(0).getChilds().get(0).getValue();
            return String.valueOf((int) character.charAt(1));
        }
        return litChildren.get(0).getChilds().get(0).getValue();
    }

    private String getArrPos(Node currNode) {
        //[ <var_value> ]
        List<Node> children = currNode.getChilds();
        return varValue(children.get(1));
    }

    private String evaluateVarOp(Node currNode) {
        // <neg> <var_op_val>
        String nFinalResult = evaluateVarOpValue(currNode.getChilds().get(1));
        return evaluateNeg(nFinalResult, currNode.getChilds().get(0));
    }

    private String evaluateBooleanCond(Node currNode) {
        //<neg> <condition> <nest_cond>
        List<Node> children = currNode.getChilds();
        String nFinalResult;

        if (!currNode.getParent().getParent().getStatement().equals("boolean_cond")) {
            addStarter("conditions");
        }

        if (children.get(2).getChilds().get(0).getStatement().equals("&")) {
            nFinalResult = addAnd(evaluateCondition(children.get(1)), evaluateBooleanCond(children.get(2).getChilds().get(1)));
        }
        else if (children.get(2).getChilds().get(0).getStatement().equals("|")) {
            nFinalResult = addOr(evaluateCondition(children.get(1)), evaluateBooleanCond(children.get(2).getChilds().get(1)));
        }
        else { //equals epsilon
            nFinalResult = evaluateCondition(children.get(1));
        }

        return evaluateNeg(nFinalResult, children.get(0));
    }

    private String evaluateCondition(Node currNode) {
        //name <comparison> | <comparable_literal> <comparison>| (<condition>)
        List<Node> children = currNode.getChilds();
        String firstOperand;

        if (children.get(0).getStatement().equals("name")) {
            firstOperand = symbolsTable.lookUp(children.get(0).getValue(), children.get(0).getId()).getRegister();
        }
        else if (children.get(0).getStatement().equals("comparable_literal")) {
            firstOperand = addLoadLiteral(children.get(0).getChilds().get(0).getValue());
        }
        else {
            return evaluateCondition(children.get(1));
        }

        if (children.get(1).getChilds().get(0).getStatement().equals("epsilon")) {
            return firstOperand;
        }

        String comparative = children.get(1).getChilds().get(0).getChilds().get(0).getStatement();
        return addComparison(firstOperand, evaluateVarOp(children.get(1).getChilds().get(1)), comparative);
    }

    private void evaluateParams(String funcName, Node currNode) {
        //<var_op> <var_list_more> | E
        List<Node> children = currNode.getChilds();

        if (!children.get(0).getStatement().equals("epsilon")) {
            String source = evaluateVarOpValue(children.get(0).getChilds().get(1));
            source = evaluateNeg(source, children.get(0).getChilds().get(0));
            addParam(funcName, source);

            //Nested parameters
            if (!children.get(1).getChilds().get(0).getStatement().equals("epsilon")) {
                //, <value_list> | E
                evaluateParams(funcName, children.get(1).getChilds().get(1));
            }
        }
    }

    private String evaluateNeg(String source, Node neg) {
        //# | E
        List<Node> children = neg.getChilds();
        if (children.get(0).getStatement().equals("#"))
            source = addNegation(source);
        return source;
    }

    private TacEntry.Operation getOperation(Node currNode) {
        //+ | - | <high_priority_operator>
        List<Node> children = currNode.getChilds();

        String operation = children.get(0).getStatement();
        if (operation.equals("+"))
            return TacEntry.Operation.ADD;
        else if (operation.equals("-"))
            return TacEntry.Operation.SUB;
        else {
            //* | / | %
            operation = children.get(0).getChilds().get(0).getStatement();
            if (operation.equals("*"))
                return TacEntry.Operation.MUL;
            if (operation.equals("/"))
                return TacEntry.Operation.DIV;
            else
                return TacEntry.Operation.MOD;
        }
    }

    private String getArrDimension(Node currNode) {
        //numeric_literal <arr_arr>
        List<Node> children = currNode.getChilds();
        return children.get(0).getValue();
    }

    private String getDatatypeSize(Node currNode) {
        return switch (currNode.getChilds().get(0).getValue()) {
            case "bro" -> "int";
            case "sis" -> "float";
            default -> "char";
        };
    }

    private String addAnd(String param1, String param2) {
        String register = generateRegister();
        tacList.add(new TacEntry(
                register,
                param1,
                param2,
                TacEntry.Operation.AND,
                null
        ));
        return register;
    }

    private String addOr(String param1, String param2) {
        String register = generateRegister();
        tacList.add(new TacEntry(
                register,
                param1,
                param2,
                TacEntry.Operation.OR,
                null
        ));
        return register;
    }

    private String addComparison(String param1, String param2, String tag) {
        String register = generateRegister();
        tacList.add(new TacEntry(
                register,
                param1,
                param2,
                TacEntry.Operation.COMPARISON,
                tag
        ));
        return register;
    }

    private String addDeclaration(String label, String datatype, String size2) {
        String register = generateRegister();
        tacList.add(new TacEntry(
                register,
                datatype,
                size2,
                TacEntry.Operation.DECLARE,
                label
        ));
        return register;
    }

    private void addStarter(String booleanResult, String tag) {
        tacList.add(new TacEntry(
                null,
                booleanResult,
                null,
                TacEntry.Operation.STARTER,
                tag
        ));
    }

    private void addStarter(String tag) {
        tacList.add(new TacEntry(
                null,
                null,
                null,
                TacEntry.Operation.STARTER,
                tag
        ));
    }

    private void addEnder(String booleanResult, String nextStatementTag, String tag) {
        tacList.add(new TacEntry(
                null,
                booleanResult,
                nextStatementTag,
                TacEntry.Operation.ENDER,
                tag
        ));
    }

    private void addFuncDec(String datatype, String funcName) {
        tacList.add(new TacEntry(
                null,
                datatype,
                null,
                TacEntry.Operation.FUNC_DEC,
                funcName
        ));
    }

    private String addLoadLiteral(String literal) {
        //Replace boolean literals
        switch (literal) {
            case "smash" -> literal = "1";
            case "pass" -> literal = "0";
        }

        //Reformat float literals
        literal = literal.replace("´", ".");

        String reg = generateRegister();
        tacList.add(new TacEntry(
                reg,
                literal,
                null,
                TacEntry.Operation.LITERAL,
                null
        ));
        return reg;
    }

    private void addStoreValue(String source, String dest) {
        tacList.add(new TacEntry(
                dest,
                source,
                null,
                TacEntry.Operation.EQUAL,
                null
        ));
    }

    private String addNegation(String source) {
        String dest = generateRegister();
        tacList.add(new TacEntry(
                dest,
                source,
                null,
                TacEntry.Operation.NEG,
                null
        ));
        return dest;
    }

    private String addOperation(String arg1, String arg2, TacEntry.Operation operation) {
        String dest = generateRegister();
        tacList.add(new TacEntry(
                dest,
                arg1,
                arg2,
                operation,
                null
        ));
        return dest;
    }

    private void addFuncCall(String funcName) {
        tacList.add(new TacEntry(
                null,
                null,
                null,
                TacEntry.Operation.CALL,
                funcName
        ));
    }

    private void addParam(String funcName, String param) {
        tacList.add(new TacEntry(
                param,
                null,
                null,
                TacEntry.Operation.PARAM,
                funcName
        ));
    }

    private void addReturn() {
        tacList.add(new TacEntry(
                null,
                null,
                null,
                TacEntry.Operation.RETURN,
                null
        ));
    }
    private void addConditional(String content1, String content2, String symbol,  TacEntry.Operation operation) {
        tacList.add(new TacEntry(
                null,
                content1,
                content2,
                operation,
                symbol
        ));
    }

    private String addParamDec(String param, String size) {
        String dest = generateRegister();
        tacList.add(new TacEntry(
                dest,
                size,
                null,
                TacEntry.Operation.PARAM_DEC,
                param
        ));
        return dest;
    }

    private void addEqualAssign(String dest, TacEntry.Operation op, String value) {
        String temp = generateRegister();
        tacList.add(new TacEntry(
                temp,
                dest,
                value,
                op,
                null
        ));
        addStoreValue(temp, dest);
    }
}
