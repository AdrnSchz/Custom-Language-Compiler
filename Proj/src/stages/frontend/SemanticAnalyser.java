package stages.frontend;

import entities.*;
import errors.ErrorHandler;
import errors.types.SemanticError;
import errors.types.SemanticWarning;
import symbols.SymbolAttribute;
import symbols.SymbolRow;
import symbols.SymbolsTable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class SemanticAnalyser {
    private final Node parseTree;
    private final SymbolsTable symbolsTable;
    private final ErrorHandler errorHandler;
    private final Stack<Node> scopes = new Stack<>();
    private final ArrayList<Function> functions = new ArrayList<>();
    private boolean hasMain = false;

    public SemanticAnalyser(Node parseTree, SymbolsTable symbolsTable, ErrorHandler errorHandler) {
        this.parseTree = parseTree;
        this.symbolsTable = symbolsTable;
        this.errorHandler = errorHandler;
        analyse();
    }

    private void analyse() {
        Queue<Node> queue = new LinkedList<>(parseTree.getChilds());
        boolean hasMain = false;
        scopes.add(parseTree);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "globals":
                    queue.addAll(node.getChilds());
                    break;
                case "globals_decl":
                    boolean readOnly = node.getChilds().get(0).getChilds().get(0).getStatement().equals("fact");
                    analyseDeclaration(node.getChilds().get(1), readOnly);
                    queue.add(node.getChilds().get(1));
                    break;
                case "main_":
                    if (queue.isEmpty()) {
                        hasMain = true;
                        analyseMain(node);
                        break;
                    } else queue.add(node);
            }
        }

        if (!hasMain)
            errorHandler.report(new SemanticError("Missing CEO function", new SourceLocation()));
    }

    private void analyseDeclaration(Node node, boolean readOnly) {
        if (node.getChilds().get(0).getStatement().equals("arr_decl")) {
            analyseArrDecl(node.getChilds().get(0).getChilds(), readOnly);
        } else if (node.getChilds().get(2).getStatement().equals("func_decl")) {
            if (readOnly) {
                String error = "Function cannot be declared as constant";
                errorHandler.report(new SemanticError(error, node.getChilds().get(2).getLocation()));
                return;
            }
            analyseFuncDecl(node.getChilds().get(1), node.getChilds().get(2).getChilds());
        } else if (node.getChilds().get(2).getStatement().equals("func_or_var_decl")) {
            Node name = node.getChilds().get(1);
            Node declaration = node.getChilds().get(2).getChilds().get(0);

            switch (declaration.getStatement()) {
                case "var_decl_assign":
                    if (declaration.getChilds().size() == 3) {
                        boolean equalOp = declaration.getChilds().get(0).getStatement().equals("equal_assign_op");
                        analyseVarOp(name, declaration.getChilds().get(1).getChilds(), true, equalOp, readOnly);
                    } else {
                        checkVariable(name, true, false, false, readOnly);
                    }
                    break;
                case "func_decl":
                    if (readOnly) {
                        String error = "Function cannot be declared as constant";
                        errorHandler.report(new SemanticError(error, node.getChilds().get(2).getLocation()));
                        return;
                    }
                    analyseFuncDecl(name, declaration.getChilds());
                    break;
            }
        } else {
            String error = "Unknown declaration type: " + node.getStatement();
            errorHandler.report(new SemanticError(error, node.getLocation()));
        }
    }

    private void analyseArrDecl(LinkedList<Node> nodes, boolean readOnly) {
        Node name = nodes.get(3), assignation = nodes.get(4);
        ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(name, scopes, false);
        if (checkDuplicates(entries)) {
            String error = "Fam name: " + name.getValue() + " already exists in the same scope";
            errorHandler.report(new SemanticError(error, name.getLocation()));
            return;
        }

        if (entries.isEmpty()) {
            String error = "Fam: " + name.getValue() + ", does not exist";
            errorHandler.report(new SemanticError(error, name.getLocation()));
            return;
        }

        SymbolRow attributes = symbolsTable.lookUp(name.getValue(), name.getId());
        attributes.setDeclared(true);
        if (readOnly) attributes.setReadOnly(readOnly);

        if (assignation.getChilds().size() == 2) {
            attributes.setInitialized(true);

            Node arrAssignVal = assignation.getChilds().get(1);
            Datatype arrAssignType, type = new Datatype(attributes.getDataType(), attributes.getDimensions());

            if (arrAssignVal.getChilds().get(0).getStatement().equals("var_value"))
                arrAssignType = getVarValueType(arrAssignVal.getChilds().get(0));
            else
                arrAssignType = getArrListType(name, arrAssignVal.getChilds().get(1));

            if (arrAssignType.getType().equals("ERROR")) return;

            if (compareDataType(type , arrAssignType)) {
                String error = "Fam: " + name.getValue() + ", of type " + type.getMessage() + " is being assigned " + arrAssignType.getMessage();
                errorHandler.report(new SemanticError(error, name.getLocation()));
            }
        } else if (readOnly) {
            String error = "Fam: " + name.getValue() + ", is being declared as constant without being initialized";
            errorHandler.report(new SemanticWarning(error, name.getLocation()));
        }
    }

    private void analyseFuncDecl(Node name, LinkedList<Node> nodes) {
        ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(name, scopes, true);
        if (checkDuplicates(entries)) {
            String error = "Function name already exists in the same scope: " + name.getValue();
            errorHandler.report(new SemanticError(error, name.getLocation()));
            return;
        }

        scopes.add(name);
        SymbolRow attributes = symbolsTable.lookUp(name.getValue(), name.getId());
        attributes.setDeclared(true);

        Function function = new Function(name.getValue(), name.getId());
        if (nodes.get(1).getChilds().get(0).getStatement().equals("param_decl"))
            storeParams(nodes.get(1).getChilds().get(0).getChilds(), function);

        analyseFuncBody(name, nodes.getLast().getChilds().get(1).getChilds(), attributes);
        scopes.pop();
    }

    private void storeParams(LinkedList<Node> nodes, Function function) {
        Queue<Node> queue = new LinkedList<>(nodes);
        while (!queue.isEmpty()) {
            Node datatype = queue.poll(), paramName = queue.poll(), paramList = queue.poll();
            function.addParameter(new Datatype(datatype.getChilds().get(0).getValue(), null), paramName.getValue(), paramName.getId());

            ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(paramName, scopes, true);
            if (checkDuplicates(entries)) {
                String error = "Parameter name \"" + paramName.getValue() + "\" already exists in the same scope in function: " + function.getName();
                errorHandler.report(new SemanticError(error, paramName.getLocation()));
            }

            SymbolRow attributes = symbolsTable.lookUp(paramName.getValue(), paramName.getId());
            attributes.setDeclared(true);
            attributes.setInitialized(true);

            if (paramList.getChilds().size() == 2) queue.addAll(paramList.getChilds().get(1).getChilds());
        }
        functions.add(function);
    }

    private void analyseFuncBody(Node name, LinkedList<Node> nodes, SymbolRow attributes) {
        Queue<Node> queue = new LinkedList<>(nodes);
        boolean hasReturn = false;
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "name":
                    Node fun_var = queue.poll();

                    if (fun_var.getChilds().get(0).getStatement().equals("func_call")) {
                        ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(node, scopes, true);
                        if (entries.isEmpty()) {
                            String error = "Function: " + node.getValue() + " is not declared";
                            errorHandler.report(new SemanticError(error, node.getLocation()));
                            break;
                        }
                        checkFuncCallType(node, fun_var.getChilds().get(0).getChilds().get(1));
                    }
                    else if (fun_var.getChilds().get(0).getStatement().equals("var_assign")) {
                        boolean equalOp = fun_var.getChilds().get(0).getChilds().get(0).getChilds().get(0).getStatement().equals("equal_assign_op");
                        analyseVarOp(node, fun_var.getChilds().get(0).getChilds(), false, equalOp, false);
                    }
                    else {
                        Datatype varType = checkVarType(node), newType;
                        int dimensions = getArrPos(node, fun_var.getChilds().get(0));
                        if (dimensions == -1) break;

                        if (varType.getDimensions().size() == dimensions) newType = new Datatype(varType.getType(), null);
                        else {
                            ArrayList<Integer> dims = new ArrayList<>();
                            for (int i = varType.getDimensions().size() - 1; i >= dimensions; i--) {
                                dims.add(varType.getDimensions().get(i));
                            }
                            newType = new Datatype(varType.getType(), dims);
                        }

                        if (fun_var.getChilds().get(1).getChilds().size() == 2) {
                            Node arrAssignVal = fun_var.getChilds().get(1).getChilds().get(1);
                            Datatype arrAssignType;
                            if (arrAssignVal.getChilds().get(0).getStatement().equals("var_value"))
                                arrAssignType = getVarValueType(arrAssignVal);
                            else
                                arrAssignType = getArrListType(name, arrAssignVal.getChilds().get(1));

                            if (arrAssignType.getType().equals("ERROR")) break;
                            if (compareDataType(newType, arrAssignType)) {
                                String error = "Variable: " + node.getValue() + ", of type " + newType.getMessage() + " is being assigned " + arrAssignType.getMessage();
                                errorHandler.report(new SemanticError(error, node.getLocation()));
                            }
                        }
                    }
                    break;
                case "expression", "func_body":
                    queue.addAll(node.getChilds());
                    break;
                case "end":
                    if (node.getChilds().get(0).getStatement().equals("return_")) {
                        hasReturn = true;
                        if (attributes.getName().equals("CEO"))
                            continue;

                        Node throwback = node.getChilds().get(0).getChilds().get(0);
                        Node throwbackVal = node.getChilds().get(0).getChilds().get(1);

                        boolean negated = throwbackVal.getChilds().get(0).getChilds().get(0).getStatement().equals("#");

                        Datatype varType = getVarValueType(throwbackVal.getChilds().getLast());

                        if (negated && !varType.getType().equals("bipolar") && !varType.getType().equals("bool_lit")) {
                            String error = "Throwback value of type " + varType.getType() + " cannot be negated";
                            errorHandler.report(new SemanticError(error, throwbackVal.getLocation()));
                        }
                        if (compareDataType(new Datatype(attributes.getDataType(), attributes.getDimensions()), varType)) {
                            String error = "Function: " + attributes.getName() + " expects to throwback " +
                                    attributes.getDataType() + " and throwback " + varType.getType();
                            errorHandler.report(new SemanticError(error, throwback.getLocation()));
                        }
                    }
                    break;
                case "conditional":
                    analyseConditional(node.getChilds());
                    break;
                case "while_loop":
                    analyseWhileLoop(node.getChilds().getLast().getChilds().get(0));
                    break;
                case "for_loop":
                    analyseForLoop(node);
                    break;
                case "switch_":
                    analyseSwitch(node.getChilds());
                    break;
                case "datatype":
                    Node varName = queue.poll();
                    Node varDeclAssign = queue.poll();

                    if (varDeclAssign.getChilds().size() == 3) {
                        boolean equalOp = varDeclAssign.getChilds().get(0).getStatement().equals("equal_assign_op");
                            analyseVarOp(varName, varDeclAssign.getChilds().get(1).getChilds(), true, equalOp, false);
                    } else {
                        checkVariable(varName, true, false, false, false);
                    }

                    break;
                case "arr_decl":
                    analyseArrDecl(node.getChilds(), false);
                    break;
            }
        }

        if (!hasReturn && !attributes.getName().equals("CEO") && name.getValue() != null) {
            String error = "Missing throwback statement in function: " + attributes.getName();
            errorHandler.report(new SemanticError(error, name.getLocation()));
        } else if (hasReturn && attributes.getName().equals("CEO")) {
            String error = "CEO function cannot have a throwback statement";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }
    }


    private SymbolRow checkVariable(Node name, boolean isDeclaration, boolean equalOp, boolean isInitialization, boolean readOnly) {
        ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(name, scopes, false);
        if (checkDuplicates(entries)) {
            String error = "Variable name \"" + name.getValue() + "\" already exists in the same scope";
            errorHandler.report(new SemanticError(error, name.getLocation()));

            return null;
        }
        if (entries.isEmpty()) {
            String error = "Variable: " + name.getValue() + ", does not exist";
            errorHandler.report(new SemanticError(error, name.getLocation()));

            return null;
        }
        SymbolRow attributes = entries.get(0);

        if (attributes.isDeclared() && isDeclaration)  {
            String error = "Variable: " + name.getValue() + ", already declared";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        } else if (!attributes.isDeclared() && !isDeclaration && isInitialization) {
            String error = "Variable: " + name.getValue() + ", is being initialized without being declared";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        } else if (!attributes.isDeclared() && !isDeclaration && !isInitialization) {
            String error = "Variable: " + name.getValue() + ", is being used without being declared";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }
        else if ((attributes.isDeclared() || isDeclaration) && !attributes.isInitialized() && isInitialization && equalOp) {
            String error = "Variable: " + name.getValue() + ", is being operated without being initialized";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }

        if (isDeclaration) attributes.setDeclared(true);
        if (isInitialization) attributes.setInitialized(true);
        if (readOnly) attributes.setReadOnly(true);
        if (readOnly && !isInitialization) {
            String error = "Variable: " + name.getValue() + ", is being declared as a constant without being initialized";
            errorHandler.report(new SemanticWarning(error, name.getLocation()));
        }

        if (isInitialization && (symbolsTable.lookUp(scopes.peek().getId()) instanceof SymbolAttribute || isDeclaration))
            attributes.setMightNotBeInitialized(false);

        if (attributes.isDeclared() && attributes.getMightNotBeInitialized() && isInitialization && equalOp) {
            String error = "Variable: " + name.getValue() + ", might not be initialized";
            errorHandler.report(new SemanticWarning(error, name.getLocation()));
        }
        return attributes;
    }

    private void analyseVarOp(Node name, LinkedList<Node> nodes, boolean isDeclaration, boolean equalOp, boolean readOnly) {

        SymbolRow attributes = checkVariable(name, isDeclaration, equalOp, true, readOnly);
        if (attributes == null) return;

        if (!readOnly && attributes.isReadOnly()) {
            String error = "Variable: " + name.getValue() + " is a constant, you cannot assign it a value";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }
        Parameter param = new Parameter(new Datatype(attributes.getDataType(), attributes.getDimensions()), name.getValue(), name.getId());
        checkVarOpType(nodes, param);
    }

    private void analyseConditional(LinkedList<Node> nodes) {
        Queue<Node> queue = new LinkedList<>(nodes);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "if_", "elif":
                    scopes.add(node);
                    queue.add(analyseIf(node, node.getChilds(), getClosestFunction(node.getId())));
                    scopes.pop();
                    break;
                case "cond_else", "else_opt":
                    queue.add(node.getChilds().getLast());
                    break;
                case "else_":
                    scopes.add(node);
                    analyseFuncBody(node, node.getChilds().get(1).getChilds(), getClosestFunction(node.getId()));
                    scopes.pop();
                    break;
            }
        }
    }

    private Node analyseIf(Node name, LinkedList<Node> nodes, SymbolRow function) {
        LinkedList<Node> queue = new LinkedList<>(nodes);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "boolean_cond":
                    analyseCondition(node.getChilds());
                    break;
                case "func_body":
                    analyseFuncBody(name, node.getChilds(), function);
                    break;
                case "cond_else":
                    return node.getChilds().getLast();
            }
        }
        return new Node("skip");
    }

    private void analyseCondition(LinkedList<Node> nodes) {
        Queue<Node> queue = new LinkedList<>(nodes);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "name", "comparable_literal":
                    Node comparison = queue.poll();
                    Datatype type = new Datatype("ERROR", null);
                    if (node.getStatement().equals("name"))
                        type = checkVarType(node);
                    else if (node.getStatement().equals("comparable_literal"))
                        type = getLitType(node.getChilds().get(0));

                    if (!comparison.getChilds().getFirst().getStatement().equals("epsilon"))
                        checkVarOpType(comparison.getChilds().getLast().getChilds(), new Parameter(type, node.getValue(), node.getId()));
                    else if (comparison.getChilds().getFirst().getStatement().equals("epsilon") && !type.getType().equals("bool_lit")) {
                        String error = "Condition must be a bipolar expression";
                        errorHandler.report(new SemanticError(error, node.getLocation()));
                    }
                    break;
                case "condition", "boolean_cond", "nest_cond":
                    queue.addAll(node.getChilds());
                    break;
            }
        }
    }

    private void analyseWhileLoop(Node node) {
        scopes.add(node);
        if (node.getStatement().equals("while_")) {
            analyseCondition(node.getChilds().get(2).getChilds());
            analyseFuncBody(node, node.getChilds().get(5).getChilds(), getClosestFunction(node.getId()));
        }
        else if (node.getStatement().equals("do_while")) {
            analyseFuncBody(node, node.getChilds().get(1).getChilds(), getClosestFunction(node.getId()));
            analyseCondition(node.getChilds().get(6).getChilds());
        }
        scopes.pop();
    }

    private void analyseForLoop(Node node) {
        scopes.add(node);
        for (Node child : node.getChilds()) {
            switch (child.getStatement()) {
                case "for_decl":
                    Node datatype = child.getChilds().get(0), name = child.getChilds().get(1), declaration = child.getChilds().getLast();
                    boolean equalOp = declaration.getChilds().get(0).getStatement().equals("equal_assign_op");
                    boolean isDeclaration = datatype.getChilds().get(0).getStatement().equals("datatype");

                    if (declaration.getChilds().size() == 3)
                        analyseVarOp(name, declaration.getChilds().get(1).getChilds(), isDeclaration, equalOp, false);
                    else checkVariable(name, isDeclaration, false, false, false);
                    break;
                case "boolean_cond":
                    analyseCondition(child.getChilds());
                    break;
                case "for_iterator":
                    Node varName = child.getChilds().get(0), change = child.getChilds().getLast();
                    Datatype type = checkVarType(varName);

                    if (type.getType().equals("bipolar")) {
                        String error = "Variable " + varName.getValue() + " is of type bipolar, so cannot be operated";
                        errorHandler.report(new SemanticError(error, varName.getLocation()));
                    } else if (change.getChilds().getLast().getStatement().equals("var_value")) {
                        Datatype type2 = getVarValueType(change.getChilds().getLast());
                        if (compareDataType(type, type2)) {
                            String error = "Variable: " + varName.getValue() + ", of type " + type + " is being operated with " + type2;
                            errorHandler.report(new SemanticError(error, varName.getLocation()));
                        }
                    }
                    break;
                case "func_body":
                    analyseFuncBody(node, child.getChilds(), getClosestFunction(node.getId()));
                    break;
            }
        }
        scopes.pop();
    }

    private void analyseSwitch(LinkedList<Node> nodes) {
        Queue<Node> queue = new LinkedList<>(nodes);
        Datatype type = new Datatype("ERROR", null), caseType;

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "name":
                    type = checkVarType(node);
                    break;
                case "case_":
                    scopes.add(node);

                    caseType = getLitType(node.getChilds().get(1).getChilds().get(0).getChilds().get(0));
                    if (compareDataType(type, caseType)) {
                        String error = "Trying to compare: " + caseType + " with " + type;
                        errorHandler.report(new SemanticError(error, node.getChilds().get(1).getChilds().get(0).getChilds().get(0).getLocation()));
                    }
                    if (node.getChilds().get(3).getChilds().get(0).getStatement().equals("epsilon")) {
                        String error = "Case cannot be empty";
                        errorHandler.report(new SemanticError(error, node.getLocation()));
                    } else
                        analyseFuncBody(node, node.getChilds().get(3).getChilds().get(0).getChilds(), getClosestFunction(node.getId()));
                    scopes.pop();
                    queue.add(node.getChilds().getLast().getChilds().get(0));
                    break;
                case "default_":
                    scopes.add(node);
                    if (node.getChilds().get(0).getStatement().equals("left"))
                        analyseFuncBody(node, node.getChilds().get(2).getChilds().get(0).getChilds(), getClosestFunction(node.getId()));
                    scopes.pop();
                    break;
            }
        }
    }

    private void analyseMain(Node node) {
        Node name = node.getChilds().get(0);
        ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(name, scopes, true);
        if (checkDuplicates(entries)) {
            String error = "Function name CEO already exists in the same scope";
            errorHandler.report(new SemanticError(error, name.getLocation()));
            return;
        }

        if (entries.isEmpty()) {
            hasMain = false;
            return;
        }

        scopes.add(name);
        SymbolRow attributes = symbolsTable.lookUp(name.getValue(), name.getId());
        attributes.setDeclared(true);

        analyseFuncBody(name, node.getChilds().get(2).getChilds(), attributes);
        scopes.pop();
    }

    private boolean checkDuplicates(ArrayList<SymbolRow> entries) {
        for (int i = 0; i < entries.size() - 1; i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                if (entries.get(i).getParentScope().getId() == entries.get(j).getParentScope().getId())
                    return true;
            }
        }
        return false;
    }

    private Datatype getVarValueType(Node node) {
        if (node.getChilds().getLast().getStatement().equals("literal")) {
            return getLitType(node.getChilds().getLast().getChilds().get(0).getChilds().get(0));
        } else {
            Node name = node.getChilds().get(0);
            if (node.getChilds().getLast().getChilds().size() == 1) {
                Node arrFunc = node.getChilds().getLast().getChilds().get(0);
                if (arrFunc.getStatement().equals("epsilon") || arrFunc.getChilds().get(0).getStatement().equals("epsilon"))
                    return checkVarType(name);
                else {
                    Datatype varType = checkVarType(name);

                    int dimensions = getArrPos(name, arrFunc);
                    ArrayList<Integer> dims = new ArrayList<>();
                    if (dimensions == -1) return new Datatype("ERROR", null);

                    if (varType.getDimensions().size() == dimensions) return new Datatype(varType.getType(), null);
                    for (int i = varType.getDimensions().size() - 1; i >= dimensions; i--) {
                        dims.add(varType.getDimensions().get(i));
                    }
                    return new Datatype(varType.getType(), dims);
                }
            } else {
                Node valueList = node.getChilds().getLast().getChilds().get(1);

                ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(name, scopes, true);
                if (entries.isEmpty()) {
                    String error = "Function: " + name.getValue() + " is not declared";
                    errorHandler.report(new SemanticError(error, name.getLocation()));

                    return new Datatype("ERROR", null);
                }
                SymbolRow attributes = entries.get(0);
                checkFuncCallType(name, valueList);

                return new Datatype(attributes.getDataType(), attributes.getDimensions());
            }
        }
    }
    private Datatype getLitType(Node node) {
            if (!node.getStatement().equals("str_lit")) {
                return new Datatype(node.getStatement(), null);
            } else {
                ArrayList<Integer> dimension = new ArrayList<>();
                dimension.add(node.getValue().length() - 2);

                return new Datatype("mainchar", dimension);
            }
    }

    private void checkVarOpType(LinkedList<Node> nodes, Parameter parameter) {
        ArrayList<Datatype> types = new ArrayList<>();
        Queue<Node> queue = new LinkedList<>(nodes);
        boolean negated;
        Node location = new Node("location");
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "neg":
                    negated = node.getChilds().get(0).getStatement().equals("#");
                    if (negated && !parameter.getDatatype().getType().equals("bipolar")) {
                        String error = "Variable , " + parameter.getName() + ", of type " + parameter.getDatatype().getMessage() + " cannot be negated";
                        errorHandler.report(new SemanticError(error, node.getChilds().get(0).getLocation()));
                        return;
                    }
                    break;
                case "var_op_val", "var_op", "assignation":
                    queue.addAll(node.getChilds());
                    break;
                case "var_value":
                    types.add(getVarValueType(node));
                    if (node.getChilds().get(0).getChilds().isEmpty()) location = node.getChilds().get(0);
                    else location = node.getChilds().get(0).getChilds().get(0).getChilds().get(0);
                    break;
                case "nested_op":
                    if (node.getChilds().size() == 2) queue.add(node.getChilds().get(1));
                    break;
            }
        }

        Datatype type, expectedType = parameter.getDatatype();
        if (types.size() == 1) {
            if (compareDataType(expectedType, types.get(0))) {
                String error = "Variable \"" + parameter.getName() + "\" expects " +
                        parameter.getDatatype().getMessage() + " and receives " + types.get(0).getMessage();
                errorHandler.report(new SemanticError(error, location.getLocation()));
            }
        }
        else {
            type = types.get(0);
            if (type.getType().equals("str_lit") || type.getType().equals("null_lit") || type.getType().equals("bool_lit")) {
                String error = type + " cannot be operated";
                errorHandler.report(new SemanticError(error, location.getLocation()));

                return;
            }

            for (int j = 1; j < types.size(); j++) {
                if (types.get(j).getType().equals("str_lit") || types.get(j).getType().equals("null_lit") || types.get(j).getType().equals("bool_lit")) {
                    String error = types.get(j).getType() + " cannot be operated";
                    errorHandler.report(new SemanticError(error, location.getLocation()));

                    return;
                }
                if (getTypeRank(types.get(j).getType()) > getTypeRank(type.getType())) type = types.get(j);
            }

            if (compareDataType(expectedType, type)) {
                String error = "Variable \"" + parameter.getName() + "\" expects " +
                        parameter.getDatatype().getMessage() + " and receives " + types.get(0).getMessage();
                errorHandler.report(new SemanticError(error, location.getLocation()));
            }
        }
    }

    private void checkFuncCallType(Node name, Node valueList) {
        Function function = getFunction(name.getValue());
        Queue<Node> queue = new LinkedList<>();
        queue.add(valueList);
        int i = 0;

        if (function == null) return;

        while (!queue.isEmpty()) {
            Node node = queue.poll();

            if (node.getStatement().equals("value_list") || node.getStatement().equals("var_list_more")) {
                if (node.getChilds().size() == 1) break;
                queue.addAll(node.getChilds());
            } else if (node.getStatement().equals("var_op")) {
                if (i < function.getParameters().size())
                    checkVarOpType(node.getChilds(), function.getParameters().get(i));

                i++;
            }
        }
        if (i > function.getParameters().size() || i < function.getParameters().size()) {
            String error = "Function: " + function.getName() + " expects " + function.getParameters().size() +
                    " parameters and receives " + i;
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }
    }

    private Datatype checkVarType(Node name) {
        ArrayList<SymbolRow> entries = symbolsTable.getEntriesInScope(name, scopes, false);

        if (entries.isEmpty()) {
            String error = "Variable: " + name.getValue() + ", does not exist";
            errorHandler.report(new SemanticError(error, name.getLocation()));

            return new Datatype("ERROR", null);
        }
        SymbolRow attributes = entries.get(0);
        if (!attributes.isDeclared()) {
            String error = "Variable: " + name.getValue() + ", used but not declared";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }
        else if (!attributes.isInitialized()) {
            String error = "Variable: " + name.getValue() + ", used but not initialized";
            errorHandler.report(new SemanticError(error, name.getLocation()));
        }
        return new Datatype(attributes.getDataType(), attributes.getDimensions());
    }

    private boolean compareDataType(Datatype type1, Datatype type2) {
        return !(checkDimensions(type1.getDimensions(), type2.getDimensions()) &&
                checkType(type1.getType(), type2.getType()));
    }

    private boolean checkDimensions(ArrayList<Integer> dimensions1, ArrayList<Integer> dimensions2) {
        if (dimensions1 == null && dimensions2 == null) return true;
        else if (dimensions1 == null || dimensions2 == null) return false;
        else if (dimensions1.size() != dimensions2.size()) return false;
        else {
            for (int i = 0; i < dimensions1.size(); i++) {
                if (dimensions1.get(i) < dimensions2.get(i)) return false;
            }
        }

        return true;
    }

    private boolean checkType(String type1, String type2) {
        if ((type1.equals("bool_lit") || type1.equals("bipolar")) && (type2.equals("bool_lit") || type2.equals("bipolar")))
            return true;
        else if ((type1.equals("null_lit") || type1.equals("zombie")) && (type2.equals("null_lit") || type2.equals("zombie")))
            return true;
        else if ((type1.equals("num_lit") || type1.equals("char_lit") || type1.equals("bro") || type1.equals("mainchar")) &&
                (type2.equals("num_lit") || type2.equals("char_lit") || type2.equals("bro") || type2.equals("mainchar")))
            return true;
        else if ((type1.equals("dec_lit") || type1.equals("sis")) &&
                (type2.equals("dec_lit") || type2.equals("num_lit") || type2.equals("char_lit") || type2.equals("sis")  || type2.equals("bro")) || type2.equals("mainchar"))
            return true;

        return false;
    }
    private int getTypeRank(String type) {
        return switch (type) {
            case "char_lit", "mainchar" -> 0;
            case "num_lit", "bro" -> 1;
            case "sis", "dec_lit" -> 2;
            case "twin" -> 3;
            default -> -1;
        };
    }
    private Function getFunction(String name) {
        for (Function function : functions) {
            if (function.getName().equals(name)) return function;
        }
        return null;
    }

    private SymbolRow getClosestFunction(int id) {
        SymbolRow symbolRow = symbolsTable.lookUp(id);

        while (!(symbolRow instanceof SymbolAttribute) || !symbolRow.getParentScope().getName().equals("start")) {
            if (symbolRow.isFunction()) return symbolRow;
            symbolRow = symbolRow.getParentScope();
        }

        return symbolRow;
    }

    private int getArrPos(Node name, Node arrPos) {
        int dimensions = 0;
        Queue<Node> queue = new LinkedList<>(arrPos.getChilds());
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            switch (node.getStatement()) {
                case "arr_pos":
                    queue.addAll(node.getChilds());
                    break;
                case "var_value":
                    Datatype type = getVarValueType(node);

                    if (type.getType().equals("ERROR")) return -1;
                    if (type.getDimensions() != null) {
                        String error = "Fam: " + name.getValue() + ", is being assigned a fam as position";
                        errorHandler.report(new SemanticError(error, name.getLocation()));
                        return -1;
                    } else if (!type.getType().equals("num_lit") && !type.getType().equals("bro")) {
                        String error = "Fam: " + name.getValue() + ", is being assigned " + type.getType() + " as position";
                        errorHandler.report(new SemanticError(error, name.getLocation()));
                        return -1;
                    } else {
                        dimensions++;
                    }
                    break;
            }
        }
        return dimensions;
    }

    private Datatype getArrListType(Node name, Node node) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        ArrayList<Datatype> types = new ArrayList<>();
        while (!queue.isEmpty()) {
            Node arrNode = queue.poll();
            switch (arrNode.getStatement()) {
                case "arr_value":
                    Datatype type;
                    if (arrNode.getChilds().get(0).getStatement().equals("neg")) {
                        boolean negated = arrNode.getChilds().get(0).getChilds().get(0).getStatement().equals("#");
                        type = getVarValueType(arrNode.getChilds().get(1));

                        if (type.getType().equals("ERROR")) return new Datatype("ERROR", null);

                        if (negated && type.getDimensions() != null) {
                            String error = "A fam cannot be negated";
                            errorHandler.report(new SemanticError(error, node.getLocation()));
                        } else if (negated && !type.getType().equals("bipolar") && !type.getType().equals("bool_lit")) {
                            String error = "A value of type " + type + " cannot be negated";
                            errorHandler.report(new SemanticError(error, node.getLocation()));
                        }
                    } else {
                        type = getArrListType(name, arrNode.getChilds().get(1));

                        if (type.getType().equals("ERROR")) return new Datatype("ERROR", null);
                    }
                    types.add(type);
                    break;
                case "arr_list":
                    queue.add(arrNode.getChilds().get(0));
                    queue.addAll(arrNode.getChilds().getLast().getChilds());
                    break;
            }
        }

        Datatype type = types.get(0);
        for (int i = 1; i < types.size(); i++) {
            if (compareDataType(type, types.get(i))) {
                String error = "Fam is being assigned values of different types or dimensions";
                errorHandler.report(new SemanticError(error, node.getLocation()));
                return new Datatype("ERROR", null);
            }
        }

        if (type.getDimensions() == null)  {
            ArrayList<Integer> dims = new ArrayList<>();
            dims.add(types.size());
            return new Datatype(type.getType(), dims);
        }
        type.getDimensions().add(0, types.size());
        return new Datatype(type.getType(), type.getDimensions());
    }
}
