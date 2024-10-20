package stages.frontend;

import entities.Node;
import entities.Token;
import errors.ErrorHandler;
import errors.ErrorListener;
import errors.types.UnexpectedTokenError;
import exceptions.NoTokenLeftException;
import errors.types.UnknownTokenError;
import stages.backend.IntermediateCodeGenerator;
import symbols.SymbolAttribute;
import symbols.SymbolRow;
import symbols.SymbolsTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class SyntaxAnalyser {
    private final LexicalAnalyser lexicalAnalyser;
    private final SemanticAnalyser semanticAnalyser;
    private IntermediateCodeGenerator intermediateCodeGenerator;
    private final ErrorListener errorHandler;
    private final ErrorHandler errHandler;
    private final SymbolsTable symbolsTable;
    private Token currToken;
    private Token prevToken;
    private final HashMap<String, String[]> first;
    private final HashMap<String, String[]> follow;
    private final Node parseTree;
    private final Stack<SymbolRow> scopeStack;
    private int currId;

    public SyntaxAnalyser(String srcCode, ErrorHandler errHandler, String outputFile) {

        this.errHandler = errHandler;
        errorHandler = errHandler;
        symbolsTable = new SymbolsTable();

        lexicalAnalyser = new LexicalAnalyser(srcCode);

        first = new HashMap<>();
        follow = new HashMap<>();

        // Parse input
        generateFirst();
        generateFollow();

        nextToken();

        parseTree = new Node("start");
        parseTree.setParent(null);

        scopeStack = new Stack<>();

        currId = 0;
        SymbolAttribute symbolAttribute =
                new SymbolAttribute(parseTree.getStatement(), currId++, null, null, 0, null, true);
        symbolsTable.insert(symbolAttribute);
        scopeStack.push(symbolAttribute);

        start();
        //TreeRepresentation treeRepresentation = new TreeRepresentation(parseTree);
        semanticAnalyser = new SemanticAnalyser(parseTree, symbolsTable, errHandler);

        //Stop execution if errors were found
        if (!errHandler.getErrorWall().equals("Stan 🎉🎉🎉"))
            return;

        intermediateCodeGenerator = new IntermediateCodeGenerator(parseTree, symbolsTable, outputFile);
    }

    private void generateFirst() {
        first.put("start", new String[]{"fact", "fam", "bro", "sis", "bipolar", "mainchar", "zombie", "CEO"});
        first.put("globals", new String[]{"fact","fam", "bro", "sis", "bipolar", "mainchar", "zombie", "epsilon"});
        first.put("globals_decl", new String[]{"fact","fam", "bro", "sis", "bipolar", "mainchar", "zombie"});
        first.put("declaration", new String[]{"fam", "bro", "sis", "bipolar", "mainchar", "zombie"});
        first.put("func_or_var_decl", new String[]{"var_decl_assign", "¿"});
        first.put("constant", new String[]{"fact", "epsilon"});
        first.put("var_decl_assign", new String[]{"=", ".", "+=", "-=", "*=", "/="});
        first.put("var_op", new String[]{"name", "(", "num_lit", "#", "str_lit", "null_lit", "dec_lit", "bool_lit", "char_lit"});
        first.put("var_op_val", new String[]{"name","(", "num_lit", "str_lit", "null_lit", "dec_lit", "bool_lit", "char_lit"});
        first.put("nested_op", new String[]{"+","-", "*", "/", "%", "epsilon"});
        first.put("operator", new String[]{"+","-", "*", "/", "%"});
        first.put("high_priority_operator", new String[]{"*", "/", "%"});
        first.put("arr_decl", new String[]{"fam"});
        first.put("arr_dim", new String[]{"num_lit"});
        first.put("arr_arr", new String[]{"num_lit", "epsilon"});
        first.put("arr_assign", new String[]{"=", "epsilon"});
        first.put("arr_assign_val", new String[]{"name", "(", "num_lit", "[", "str_lit", "null_lit", "dec_lit", "bool_lit", "char_lit"});
        first.put("arr_list", new String[]{"name", "(", "num_lit", "[", "#", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("arr_value", new String[]{"name", "(", "num_lit", "[", "#", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("arr_value_list", new String[]{",", "epsilon"});
        first.put("var_value", new String[]{"name", "(", "num_lit", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("arr_or_func", new String[]{"[", "¿", "epsilon"});
        first.put("arr_pos", new String[]{"["});
        first.put("neg", new String[]{"#", "epsilon"});
        first.put("datatype", new String[]{"bro", "sis", "bipolar", "mainchar"});
        first.put("literal", new String[]{"(", "num_lit", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("non_comparable_literal", new String[]{"str_lit", "null_lit"});
        first.put("comparable_literal", new String[]{"num_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("main_", new String[]{"CEO"});
        first.put("func_decl", new String[]{"¿"});
        first.put("func_impl", new String[]{".", "¡"});
        first.put("func_param", new String[]{"bro", "sis", "bipolar", "mainchar", "epsilon"});
        first.put("param_decl", new String[]{"bro", "sis", "bipolar", "mainchar"});
        first.put("param_list", new String[]{",", "epsilon"});
        first.put("func_body", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "throwback", "vibe", "like","swipe", "4", "epsilon"});
        first.put("expression", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "vibe", "like","swipe", "4"});
        first.put("func_or_var", new String[]{"=", "[", "¿", "+=", "-=", "*=", "/=", "epsilon"});
        first.put("end", new String[]{"throwback", "epsilon"});
        first.put("return_", new String[]{"throwback"});
        first.put("return_value", new String[]{"name", "(", "num_lit", "#", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("func_call", new String[]{"¿"});
        first.put("value_list", new String[]{"name", "(", "num_lit", "#", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit", "epsilon"});
        first.put("value_list_more", new String[]{",", "epsilon"});
        first.put("var_assign", new String[]{"=", "+=", "-=", "*=", "/="});
        first.put("assignation", new String[]{"=", "+=", "-=", "*=", "/="});
        first.put("equal_assign_op", new String[]{"+=", "-=", "*=", "/="});
        first.put("while_loop", new String[]{"vibe"});
        first.put("while_opt", new String[]{"¡", "check"});
        first.put("while_", new String[]{"check"});
        first.put("do_while", new String[]{"¡"});
        first.put("conditional", new String[]{"like"});
        first.put("if_", new String[]{"like"});
        first.put("cond_else", new String[]{"whatever", "epsilon"});
        first.put("else_opt", new String[]{"like", "else_"});
        first.put("elif", new String[]{"like"});
        first.put("else_", new String[]{"¡"});
        first.put("boolean_cond", new String[]{"name", "(", "num_lit", "#", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("condition", new String[]{"name", "(", "num_lit", "dec_lit","bool_lit", "char_lit"});
        first.put("nest_cond", new String[]{"&", "|", "epsilon"});
        first.put("comparison", new String[]{"<", "<=", ">", ">=", "==", "#=", "epsilon"});
        first.put("comparison_op", new String[]{"<", "<=", ">", ">=", "==", "#="});
        first.put("switch_", new String[]{"swipe"});
        first.put("case_", new String[]{"right"});
        first.put("nested_case", new String[]{"case_", "epsilon"});
        first.put("default_", new String[]{"left", "epsilon"});
        first.put("case_body", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "throwback", "vibe", "like", "swipe", "periodt", "4", "epsilon"});
        first.put("break_", new String[]{"periodt", "epsilon"});
        first.put("for_loop", new String[]{"4"});
        first.put("for_decl", new String[]{"name", "bro", "sis", "bipolar", "mainchar"});
        first.put("for_var_type", new String[]{"bro", "sis", "bipolar", "mainchar", "epsilon"});
        first.put("for_iterator", new String[]{"name"});
        first.put("it_change", new String[]{"+=", "-=", "*=", "/=", "++", "--"});
    }

    private void generateFollow() {
        follow.put("start", new String[]{"dollar"});
        follow.put("globals", new String[]{"CEO"});
        follow.put("globals_decl", new String[]{"zombie", "fact","fam", "bro", "sis", "bipolar", "mainchar", "CEO"});
        follow.put("declaration", new String[]{"zombie", "fact", "fam", "bro", "sis", "bipolar", "mainchar", "CEO"});
        follow.put("func_or_var_decl", new String[]{"zombie", "fact", "fam", "bro", "sis", "bipolar", "mainchar", "CEO"});
        follow.put("constant", new String[]{"zombie", "fam", "bro", "sis", "bipolar", "mainchar"});
        follow.put("var_decl_assign", new String[]{"zombie", "name", "fact", "(", "fam", "num_lit", "#", "bro", "sis", "bipolar", "mainchar", "dec_lit","bool_lit", "char_lit", "CEO", "!", "throwback", "vibe", "like", "swipe", "case_", "periodt", "4"});
        follow.put("var_op", new String[]{".", ")", ",", "?", "&", "|"});
        follow.put("var_op_val", new String[]{".", ")", ",", "?", "&", "|"});
        follow.put("nested_op", new String[]{".", ")", ",", "?", "&", "|"});
        follow.put("operator", new String[]{"name", "(", "num_lit", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        follow.put("high_priority_operator", new String[]{"name", "(", "num_lit", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        follow.put("arr_decl", new String[]{"zombie", "fact", "fam", "bro", "sis", "bipolar", "mainchar", "CEO"});
        follow.put("arr_dim", new String[]{"bro", "sis", "bipolar", "mainchar"});
        follow.put("arr_arr", new String[]{"bro", "sis", "bipolar", "mainchar"});
        follow.put("arr_assign", new String[]{"name", "fam", ".", "bro", "sis", "bipolar", "mainchar", "¡", "throwback", "vibe", "like", "swipe", "periodt", "4"});
        follow.put("arr_assign_val", new String[]{"name", "fam", ".", "bro", "sis", "bipolar", "mainchar", "¡", "throwback", "vibe", "like", "swipe", "periodt", "4"});
        follow.put("arr_list", new String[]{"]"});
        follow.put("arr_value", new String[]{"]", ","});
        follow.put("arr_value_list", new String[]{"]"});
        follow.put("var_value", new String[]{"name", ".", "fam", ")", "+", "-", "*", "/", "%","]", ",", "?", "&", "bro", "sis", "bipolar", "mainchar", "!", "?", "throwback", "vibe", "like", "&", "|", "swipe", "periodt", "4"});
        follow.put("arr_or_func", new String[]{"name", ".", ")", "+", "-", "*", "/", "%", "fam", "]", ",", "bro", "sis", "bipolar", "mainchar", "!", "?", "throwback", "vibe", "like", "&", "|", "swipe", "case_", "periodt", "4"});
        follow.put("arr_pos", new String[]{"name", ".", "=", "fam", ")", "+", "-", "*", "/", "%","]", ",", "?", "bro", "sis", "bipolar", "mainchar", "!", "throwback", "vibe", "like", "&", "|", "swipe", "case_", "periodt", "4"});
        follow.put("neg", new String[]{"name", "(", "num_lit", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        follow.put("datatype", new String[]{"name"});
        follow.put("literal", new String[]{"name", ".", "fam", ")", "+", "-", "*", "/", "%", "]", ",", "bro", "sis", "bipolar", "mainchar", "!", "?", "throwback", "vibe", "like", "&", "|", "swipe", "periodt", "4"});
        follow.put("non_comparable_literal", new String[]{"name", ".", "fam", ")", "+", "-", "*", "/", "%", "]", ",", "bro", "sis", "bipolar", "mainchar", "!", "?", "throwback", "vibe", "like", "&", "|", "swipe", "case_", ":", "periodt", "4"});
        follow.put("comparable_literal", new String[]{"name", ".", "fam", ")", "+", "-", "*", "/", "%", "]", ",", "bro", "sis", "bipolar", "mainchar", "!", "?", "throwback", "vibe", "like", "&", "|", "<", "<=", ">", ">=", "==", "#=", "swipe", "case_", ":", "periodt", "4"});
        follow.put("main_", new String[]{"dollar"});
        follow.put("func_decl", new String[]{"zombie", "fact", "fam", "bro", "sis", "bipolar", "mainchar", "CEO"});
        follow.put("func_impl", new String[]{"zombie", "fact", "fam", "bro", "sis", "bipolar", "mainchar", "CEO"});
        follow.put("func_param", new String[]{"?"});
        follow.put("param_decl", new String[]{"?"});
        follow.put("param_list", new String[]{"?"});
        follow.put("func_body", new String[]{"!"});
        follow.put("expression", new String[]{"name", "fam", "bro", "sis",  "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("func_or_var", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("end", new String[]{"!"});
        follow.put("return_", new String[]{"!"});
        follow.put("return_value", new String[]{"."});
        follow.put("func_call", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback", "vibe", "like", "swipe", "periodt", "4"});
        follow.put("value_list", new String[]{"?"});
        follow.put("value_list_more", new String[]{"?"});
        follow.put("var_assign", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback", "vibe", "like", "swipe", "periodt", "4"});
        follow.put("assignation", new String[]{"."});
        follow.put("equal_assign_op", new String[]{"name", "(", "num_lit", "#", "str_lit", "null_lit", "dec_lit", "bool_lit", "char_lit"});
        follow.put("while_loop", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("while_opt", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("while_", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("do_while", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("conditional", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("if_", new String[]{"name", "fam", "bro", "sis",  "bipolar", "mainchar", "!", "throwback","vibe", "like", "whatever", "swipe", "case_", "periodt", "4"});
        follow.put("cond_else", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("else_opt", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("elif", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("else_", new String[]{});
        follow.put("boolean_cond", new String[]{".", "?"});
        follow.put("condition", new String[]{".", ")", "?", "&", "|"});
        follow.put("nest_cond", new String[]{".", "?"});
        follow.put("comparison", new String[]{".", ")", "?", "&", "|"});
        follow.put("comparison_op", new String[]{"name", "(", "num_lit", "#", "str_lit", "null_lit", "dec_lit","bool_lit", "char_lit"});
        follow.put("switch_", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback","vibe", "like", "swipe", "periodt", "4"});
        follow.put("case_", new String[]{});
        follow.put("nested_case", new String[]{});
        follow.put("default_", new String[]{"!"});
        follow.put("case_body", new String[]{"!", "case_", "periodt"});
        follow.put("break_", new String[]{"!", "case_", "periodt"});
        follow.put("for_loop", new String[]{"name", "fam", "bro", "sis", "bipolar", "mainchar", "!", "throwback", "vibe", "like", "swipe", "periodt", "4"});
        follow.put("for_decl", new String[]{"name", "(", "num_lit", "#", "dec_lit", "bool_lit", "char_lit"});
        follow.put("for_var_type", new String[]{"name"});
        follow.put("for_iterator", new String[]{"?"});
        follow.put("it_change", new String[]{"?"});
    }

    private Token checkNextToken() {
        try {
            return lexicalAnalyser.nextToken();
        }
        catch (NoTokenLeftException ignored) {
        }
        catch (UnknownTokenError e) {
            errorHandler.report(e);
            nextToken();
        }
        return null;
    }

    private void nextToken() {
        try {
            if (currToken != null)
                prevToken = currToken;
            currToken = lexicalAnalyser.nextToken();
        }
        catch (NoTokenLeftException ignored) {}
        catch (UnknownTokenError e) {
            errorHandler.report(e);
            nextToken();
        }
    }

    private static String[] mergeArrays(String[] array1, String[] array2) {
        if (array2 == null) return array1;

        int mergedLength = array1.length + array2.length;

        String[] mergedArray = new String[mergedLength];

        System.arraycopy(array1, 0, mergedArray, 0, array1.length);

        System.arraycopy(array2, 0, mergedArray, array1.length, array2.length);

        return mergedArray;
    }

    //Finds the First Set of terminals of a non-terminal (considering the follow function)
    private String[] first(String nonTerminal) {
        String[] firstSet = first.get(nonTerminal);
        String[] followSet = follow.get(nonTerminal);

        for (String terminal : firstSet) {
            if (terminal.equals("epsilon")) {
                firstSet = mergeArrays(firstSet, followSet);
            }
        }
        return firstSet;
    }

    public boolean checkMatch(Token currToken, String s) {
        if (!currToken.getType().equals(s.toUpperCase())) {
            if(currToken.getValue().equals(s))
                return true;
            //Account for keyword 4 being a num_lit
            return currToken.getValue().equals("4") && s.equals("num_lit");
        }
        return true;
    }

    public boolean checkMatch(Token currToken, String[] values) {
        for (String value : values) {
            if (checkMatch(currToken, value)) return true;
        }
        return false;
    }

    public void match(Token currToken, String s, Node parent) {

        if (checkMatch(currToken, s)) {
            addTerminal(s, parent);
        }
        // Error recovery
        else {
            //One symbol lookahead
            nextToken();

            if (checkMatch(this.currToken, s)) {
                //Skip to next token (assuming right one was received)
                errorHandler.report(new UnexpectedTokenError(prevToken, s, lexicalAnalyser.getLocation()));
                addTerminal(s, parent);
                return;
            }

            //Report error and stop execution
            errorHandler.report(new UnexpectedTokenError(currToken, s, lexicalAnalyser.getLocation()));
            System.out.println(errHandler.getErrorWall());
            errorHandler.abort();
        }
    }

    private void addTerminal(String terminal, Node parent) {
        Node newNode = new Node(terminal);
        newNode.setLocation(lexicalAnalyser.getLocation());
        newNode.setValue(currToken.getValue());
        parent.addChild(newNode);

        addToSymbolTable(terminal, parent, newNode);

        nextToken();
    }

    private void addToSymbolTable(String s, Node parent, Node node) {
        if (s.equals("¡")) {
            if (parent.getStatement().equals("main_")) {
                Node CEONode = parent.getChilds().get(0);
                CEONode.setId(currId);
                SymbolAttribute symbolAttribute =
                        new SymbolAttribute("CEO", currId++, "zombie", null, 1, scopeStack.peek(), true);
                scopeStack.push(symbolAttribute);
                symbolsTable.insert(symbolAttribute);
            }
            else if (parent.getStatement().equals("while_") || parent.getStatement().equals("do_while") ||
                     parent.getStatement().equals("if_") || parent.getStatement().equals("elif") ||
                     parent.getStatement().equals("else_")) {

                parent.setId(currId);
                SymbolRow symbolRow = new SymbolRow(scopeStack.peek(), currId++);
                scopeStack.push(symbolRow);
                symbolsTable.insert(symbolRow);
            }
        }
        else if (node.getStatement().equals(":")) {
            parent.setId(currId);
            SymbolRow symbolRow = new SymbolRow(scopeStack.peek(), currId++);
            scopeStack.push(symbolRow);
            symbolsTable.insert(symbolRow);
        }
        else if ((node.getStatement().equals("right")) && !parent.getParent().getStatement().equals("switch_")
                || node.getStatement().equals("left") || s.equals("!")) {
            scopeStack.pop();
        }
        else if (node.getStatement().equals("name") && parent.getChilds().get(0).getStatement().equals("zombie")) {

                SymbolAttribute symbolAttribute =
                        new SymbolAttribute(node.getValue(), currId, "zombie", null, 1, scopeStack.peek(), true);
                scopeStack.push(symbolAttribute);
                symbolsTable.insert(symbolAttribute);
                node.setId(currId++);
        }
        else if (s.equals("¿") && parent.getStatement().equals("func_decl") && parent.getParent().getStatement().equals("func_or_var_decl")) {

            Node declarationNode = parent.getParent().getParent();
            String datatype = declarationNode.getChilds().get(0).getChilds().get(0).getStatement();
            Node nameNode = declarationNode.getChilds().get(1);

            SymbolAttribute symbolAttribute =
                    new SymbolAttribute(nameNode.getValue(), currId, datatype, null, 1, scopeStack.peek(), true);
            scopeStack.push(symbolAttribute);
            symbolsTable.insert(symbolAttribute);
            nameNode.setId(currId++);
        }
        else if (parent.getStatement().equals("var_decl_assign") && parent.getParent().getStatement().equals("func_or_var_decl")
                && parent.getChilds().get(0).equals(node)) {
                //variable

            String datatype = parent.getParent().getParent().getChilds().get(0).getChilds().get(0).getStatement();
            Node nameNode = parent.getParent().getParent().getChilds().get(1);

            SymbolAttribute symbolAttribute =
                    new SymbolAttribute(nameNode.getValue(), currId, datatype, null, 1, scopeStack.peek(), false);
            symbolsTable.insert(symbolAttribute);
            nameNode.setId(currId++);
        }
        else if (node.getStatement().equals("name") && (parent.getStatement().equals("expression")
                && parent.getChilds().get(0).getStatement().equals("datatype") || parent.getStatement().equals("param_decl"))) {

            String datatype = parent.getChilds().get(0).getChilds().get(0).getStatement();
            SymbolAttribute symbolAttribute =
                    new SymbolAttribute(node.getValue(), currId, datatype, null, 1, scopeStack.peek(), false);
            symbolsTable.insert(symbolAttribute);
            node.setId(currId++);
        }
        else if (node.getStatement().equals("name") && parent.getStatement().equals("for_decl")) {

            //for loop scope
            parent.getParent().setId(currId);
            SymbolRow symbolRow = new SymbolRow(scopeStack.peek(), currId++);
            scopeStack.push(symbolRow);
            symbolsTable.insert(symbolRow);

            if (!parent.getChilds().get(0).getChilds().get(0).getStatement().equals("epsilon")) {
                String datatype = parent.getChilds().get(0).getChilds().get(0).getChilds().get(0).getStatement();
                SymbolAttribute symbolAttribute =
                        new SymbolAttribute(node.getValue(), currId, datatype, null, 1, scopeStack.peek(), false);
                symbolsTable.insert(symbolAttribute);
                node.setId(currId++);
                System.out.println();
            }

        }
        else if (node.getStatement().equals("name") && parent.getStatement().equals("arr_decl")) {

            String datatype = parent.getChilds().get(2).getChilds().get(0).getStatement();
            ArrayList<Integer> dimensions = new ArrayList<>();

            Node arr_dimNode = parent.getChilds().get(1);
            boolean exit = false;
            while (!exit) {
                if (arr_dimNode.getChilds().size() == 0) {
                    exit = true;
                }
                else { // (arr_dimNode.getChilds().get(0).getStatement().equals("num_lit"))
                    dimensions.add(Integer.valueOf(arr_dimNode.getChilds().get(0).getValue()));
                    arr_dimNode = arr_dimNode.getChilds().get(1).getChilds().get(0);

                }
            }

            SymbolAttribute symbolAttribute =
                    new SymbolAttribute(node.getValue(), currId, datatype, dimensions, 1, scopeStack.peek(), false);
            symbolsTable.insert(symbolAttribute);
            node.setId(currId++);
        }

    }

    private void start() {
        this.parseTree.addChild(new Node("globals"));
        globals(this.parseTree.getChilds().getFirst());

        this.parseTree.addChild(new Node("main_"));
        main_(this.parseTree.getChilds().getLast());
    }

    private void globals(Node parent) {
        if (checkMatch(currToken, first("globals_decl"))) {
            Node declNode = new Node("globals_decl");
            parent.addChild(declNode);
            globals_decl(declNode);

            Node globNode = new Node("globals");
            parent.addChild(globNode);
            globals(globNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void globals_decl(Node parent) {
        Node constNode = new Node("constant");
        parent.addChild(constNode);
        constant(constNode);

        Node declNode = new Node("declaration");
        parent.addChild(declNode);
        declaration(declNode);
    }

    private void declaration(Node parent) {
        Node currNode;
        if (checkMatch(currToken, first("arr_decl"))) {
            currNode = new Node("arr_decl");
            parent.addChild(currNode);
            arr_decl(currNode);
        }
        else if (checkMatch(currToken, "zombie")) {
            match(currToken, "zombie", parent);
            match(currToken, "name", parent);

            currNode = new Node("func_decl");
            parent.addChild(currNode);
            func_decl(currNode);
        }
        else {
            currNode = new Node("datatype");
            parent.addChild(currNode);
            datatype(currNode);

            match(currToken, "name", parent);

            Node funcVarNode = new Node("func_or_var_decl");
            parent.addChild(funcVarNode);
            func_or_var_decl(funcVarNode);
        }
    }

    private void func_or_var_decl(Node parent) {
        Node currNode;
        if (checkMatch(currToken, first("var_decl_assign"))) {
            currNode = new Node("var_decl_assign");
            parent.addChild(currNode);
            var_decl_assign(currNode);
        }
        else {
            currNode = new Node("func_decl");
            parent.addChild(currNode);
            func_decl(currNode);
        }
    }

    private void constant(Node parent) {
        if (checkMatch(currToken, "fact")) {
            match(currToken, "fact", parent);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void var_decl_assign(Node parent) {
        if (checkMatch(currToken, "=")) {
            match(currToken, "=", parent);

            Node currNode = new Node("var_op");
            parent.addChild(currNode);
            var_op(currNode);
        }
        else if (checkMatch(currToken, first("equal_assign_op"))) {
            Node eqAssignNode = new Node("equal_assign_op");
            parent.addChild(eqAssignNode);
            equal_assign_op(eqAssignNode);

            Node currNode = new Node("var_op");
            parent.addChild(currNode);
            var_op(currNode);
        }
        match(currToken, ".", parent);
    }

    private void var_op(Node parent) {
        Node negNode = new Node("neg");
        parent.addChild(negNode);
        neg(negNode);

        Node varNode = new Node("var_op_val");
        parent.addChild(varNode);
        var_op_val(varNode);
    }
    private void var_op_val(Node parent) {
        if (checkMatch(currToken, first("var_value"))) {
            Node varNode = new Node("var_value");
            parent.addChild(varNode);
            var_value(varNode);

            Node nestedNode = new Node("nested_op");
            parent.addChild(nestedNode);
            nested_op(nestedNode);
        }
        else {
            match(currToken, "(", parent);

            Node varNode = new Node("var_op");
            parent.addChild(varNode);
            var_op(varNode);

            match(currToken, ")", parent);
        }
    }

    private void nested_op(Node parent) {
        if (checkMatch(currToken, first("operator"))) {
            Node operatorNode = new Node("operator");
            parent.addChild(operatorNode);
            operator(operatorNode);

            Node varNode = new Node("var_op_val");
            parent.addChild(varNode);
            var_op_val(varNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void operator(Node parent) {
        if (checkMatch(currToken, "+")) {
            match(currToken, "+", parent);
        }
        else if (checkMatch(currToken, "-")) {
            match(currToken, "-", parent);
        }
        else {
            Node highPriorityNode = new Node("high_priority_operator");
            parent.addChild(highPriorityNode);
            high_priority_operator(highPriorityNode);
        }
    }

    private void high_priority_operator(Node parent) {
        if (checkMatch(currToken, "*")) {
            match(currToken, "*", parent);
        }
        else if (checkMatch(currToken, "/")) {
            match(currToken, "/", parent);
        }
        else {
            match(currToken, "%", parent);
        }
    }

    private void arr_decl(Node parent) {
        match(currToken, "fam", parent);

        Node dimNode = new Node("arr_dim");
        parent.addChild(dimNode);
        arr_dim(dimNode);

        Node dataNode = new Node("datatype");
        parent.addChild(dataNode);
        datatype(dataNode);

        match(currToken, "name", parent);

        Node assignNode = new Node("arr_assign");
        parent.addChild(assignNode);
        arr_assign(assignNode);

        match(currToken, ".", parent);
    }

    private void arr_dim(Node parent) {
        match(currToken, "num_lit", parent);

        Node arrNode = new Node("arr_arr");
        parent.addChild(arrNode);
        arr_arr(arrNode);
    }

    private void arr_arr(Node parent) {
        if (checkMatch(currToken, first("arr_dim"))) {
            Node dimNode = new Node("arr_dim");
            parent.addChild(dimNode);
            arr_dim(dimNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void arr_assign(Node parent) {
        if (checkMatch(currToken, "=")) {
            match(currToken, "=", parent);

            Node assignValNode = new Node("arr_assign_val");
            parent.addChild(assignValNode);
            arr_assign_val(assignValNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void arr_assign_val(Node parent) {
        if (checkMatch(currToken, "[")) {
            match(currToken, "[", parent);

            Node listNode = new Node("arr_list");
            parent.addChild(listNode);
            arr_list(listNode);

            match(currToken, "]", parent);
        }
        else{
            Node varValueNode = new Node("var_value");
            parent.addChild(varValueNode);
            var_value(varValueNode);
        }
    }

    private void arr_list(Node parent) {
        Node arrValueNode = new Node("arr_value");
        parent.addChild(arrValueNode);
        arr_value(arrValueNode);

        Node arrValueListNode = new Node("arr_value_list");
        parent.addChild(arrValueListNode);
        arr_value_list(arrValueListNode);
    }

    private void arr_value(Node parent) {
        if (checkMatch(currToken, first("neg"))) {
            Node negNode = new Node("neg");
            parent.addChild(negNode);
            neg(negNode);

            Node varValueNode = new Node("var_value");
            parent.addChild(varValueNode);
            var_value(varValueNode);
        }
        else{
            match(currToken, "[", parent);

            Node listNode = new Node("arr_list");
            parent.addChild(listNode);
            arr_list(listNode);

            match(currToken, "]", parent);
        }
    }

    private void arr_value_list(Node parent) {
        if (checkMatch(currToken, ",")) {
            match(currToken, ",", parent);

            Node arrListNode = new Node("arr_list");
            parent.addChild(arrListNode);
            arr_list(arrListNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void var_value(Node parent) {
        if (checkMatch(currToken, first("literal"))) {
            Node litNode = new Node("literal");
            parent.addChild(litNode);
            literal(litNode);
        }
        else{
            match(currToken, "name", parent);

            Node arrFuncNode = new Node("arr_or_func");
            parent.addChild(arrFuncNode);
            arr_or_func(arrFuncNode);
        }
    }

    private void arr_or_func(Node parent) {
        if (checkMatch(currToken, first("arr_pos"))) {
            Node arrPosNode = new Node("arr_pos");
            parent.addChild(arrPosNode);
            arr_pos(arrPosNode);

            return;
        }
        else if (checkMatch(currToken, "¿")) {
            match(currToken, "¿", parent);

            Node valueListNode = new Node("value_list");
            parent.addChild(valueListNode);
            value_list(valueListNode);

            match(currToken, "?", parent);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void arr_pos(Node parent) {
        match(currToken, "[", parent);

        Node varValueNode = new Node("var_value");
        parent.addChild(varValueNode);
        var_value(varValueNode);

        match(currToken, "]", parent);
    }

    private void neg(Node parent) {
        if (checkMatch(currToken, "#")) {
            match(currToken, "#", parent);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void datatype(Node parent) {
        if (checkMatch(currToken, "bro")) {
            match(currToken, "bro", parent);
        }
        else if (checkMatch(currToken, "sis")) {
            match(currToken, "sis", parent);
        }
        else if (checkMatch(currToken, "bipolar")) {
            match(currToken, "bipolar", parent);
        }
        else {
            match(currToken, "mainchar", parent);
        }
    }

    private void literal(Node parent) {
        if (checkMatch(currToken, first("comparable_literal"))) {
            Node compLitNode = new Node("comparable_literal");
            parent.addChild(compLitNode);
            comparable_literal(compLitNode);
        }
        else{
            Node nonCompLitNode = new Node("non_comparable_literal");
            parent.addChild(nonCompLitNode);
            non_comparable_literal(nonCompLitNode);
        }
    }

    private void non_comparable_literal(Node parent) {
        if (checkMatch(currToken, "str_lit")) {
            match(currToken, "str_lit", parent);
        }
        else {
            match(currToken, "null_lit", parent);
        }
    }

    private void comparable_literal(Node parent) {
        if (checkMatch(currToken, "num_lit")) {
            match(currToken, "num_lit", parent);
        }
        else if (checkMatch(currToken, "dec_lit")) {
            match(currToken, "dec_lit", parent);
        }
        else if (checkMatch(currToken, "bool_lit")) {
            match(currToken, "bool_lit", parent);
        }
        else {
            match(currToken, "char_lit", parent);
        }
    }

    private void main_(Node parent) {
        match(currToken, "CEO", parent);
        match(currToken, "¡", parent);

        Node funcBodyNode = new Node("func_body");
        parent.addChild(funcBodyNode);
        func_body(funcBodyNode);

        match(currToken, "!", parent);
    }

    private void func_decl(Node parent) {
        match(currToken, "¿", parent);

        Node funcParamNode = new Node("func_param");
        parent.addChild(funcParamNode);
        func_param(funcParamNode);

        match(currToken, "?", parent);

        Node funcImplNode = new Node("func_impl");
        parent.addChild(funcImplNode);
        func_impl(funcImplNode);
    }

    private void func_impl(Node parent) {
        if (checkMatch(currToken, "¡")) {
            match(currToken, "¡", parent);

            Node funcBodyNode = new Node("func_body");
            parent.addChild(funcBodyNode);
            func_body(funcBodyNode);

            match(currToken, "!", parent);
        }
        else {
            match(currToken, ".", parent);
        }
    }

    private void func_param(Node parent) {
        if  (checkMatch(currToken, first("param_decl"))) {
            Node paramDeclNode = new Node("param_decl");
            parent.addChild(paramDeclNode);
            param_decl(paramDeclNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void param_decl(Node parent) {
        Node datatypeNode = new Node("datatype");
        parent.addChild(datatypeNode);
        datatype(datatypeNode);

        match(currToken, "name", parent);

        Node paramListNode = new Node("param_list");
        parent.addChild(paramListNode);
        param_list(paramListNode);
    }

    private void param_list(Node parent) {
        if (checkMatch(currToken, ",")) {
            match(currToken, ",", parent);
            Node paramDecNode = new Node("param_decl");
            parent.addChild(paramDecNode);
            param_decl(paramDecNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void func_body(Node parent) {
        if (checkMatch(currToken, first("expression"))) {
            Node expressionNode = new Node("expression");
            parent.addChild(expressionNode);
            expression(expressionNode);

            Node funcBodyNode = new Node("func_body");
            parent.addChild(funcBodyNode);
            func_body(funcBodyNode);

            return;
        }
        else if (checkMatch(currToken, first("end"))) {
            Node endNode = new Node("end");
            parent.addChild(endNode);
            end(endNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void expression(Node parent) {
        if (checkMatch(currToken, first("conditional"))) {
            Node conditionalNode = new Node("conditional");
            parent.addChild(conditionalNode);
            conditional(conditionalNode);
        }
        else if (checkMatch(currToken, first("while_loop"))) {
            Node whileLoopNode = new Node("while_loop");
            parent.addChild(whileLoopNode);
            while_loop(whileLoopNode);
        }
        else if (checkMatch(currToken, first("for_loop"))) {
            Node forLoopNode = new Node("for_loop");
            parent.addChild(forLoopNode);
            for_loop(forLoopNode);
        }
        else if (checkMatch(currToken, first("switch_"))) {
            Node switchNode = new Node("switch_");
            parent.addChild(switchNode);
            switch_(switchNode);
        }
        else if (checkMatch(currToken, "name")) {
            match(currToken, "name", parent);

            Node funcVarNode = new Node("func_or_var");
            parent.addChild(funcVarNode);
            func_or_var(funcVarNode);
        }
        else if (checkMatch(currToken, first("datatype"))) {
            Node datatypeNode = new Node("datatype");
            parent.addChild(datatypeNode);
            datatype(datatypeNode);

            match(currToken, "name", parent);

            Node varDeclNode = new Node("var_decl_assign");
            parent.addChild(varDeclNode);
            var_decl_assign(varDeclNode);
        }
        else if (checkMatch(currToken, first("arr_decl"))) {
            Node arrDeclNode = new Node("arr_decl");
            parent.addChild(arrDeclNode);
            arr_decl(arrDeclNode);
        }
        else {
            parent.addChild(new Node("epsilon"));
        }
    }

    private void func_or_var(Node parent) {
        if (checkMatch(currToken, first("func_call"))) {
            Node funcNode = new Node("func_call");
            parent.addChild(funcNode);
            func_call(funcNode);
        }
        else if (checkMatch(currToken, first("arr_pos"))){
            Node arrPosNode = new Node("arr_pos");
            parent.addChild(arrPosNode);
            arr_pos(arrPosNode);

            Node arrAssignNode = new Node("arr_assign");
            parent.addChild(arrAssignNode);
            arr_assign(arrAssignNode);
        }
        else {
            Node varNode = new Node("var_assign");
            parent.addChild(varNode);
            var_assign(varNode);
        }
    }
    private void end(Node parent) {
        if (checkMatch(currToken, first("return_"))) {
            Node returnNode = new Node("return_");
            parent.addChild(returnNode);
            return_(returnNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void return_(Node parent) {
        match(currToken, "throwback", parent);

        Node returnValueNode = new Node("return_value");
        parent.addChild(returnValueNode);
        return_value(returnValueNode);

        match(currToken, ".", parent);
    }

    private void return_value(Node parent) {
        Node negNode = new Node("neg");
        parent.addChild(negNode);
        neg(negNode);

        Node varValueNode = new Node("var_value");
        parent.addChild(varValueNode);
        var_value(varValueNode);
    }

    private void func_call(Node parent) {
        match(currToken, "¿", parent);

        Node valueListNode = new Node("value_list");
        parent.addChild(valueListNode);
        value_list(valueListNode);

        match(currToken, "?", parent);
        match(currToken, ".", parent);
    }

    private void value_list(Node parent) {
        if (checkMatch(currToken, first("var_op"))) {
            Node varOpNode = new Node("var_op");
            parent.addChild(varOpNode);
            var_op(varOpNode);

            Node varListMoreNode = new Node("var_list_more");
            parent.addChild(varListMoreNode);
            var_list_more(varListMoreNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void var_list_more(Node parent) {
        if (checkMatch(currToken, ",")) {
            match(currToken, ",", parent);

            Node varListNode = new Node("value_list");
            parent.addChild(varListNode);
            value_list(varListNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void var_assign(Node parent) {
        Node assignationNode = new Node("assignation");
        parent.addChild(assignationNode);
        assignation(assignationNode);

        match(currToken, ".", parent);
    }

    private void assignation(Node parent) {
        if (checkMatch(currToken, "=")) {
            match(currToken, "=", parent);

            Node varOpNode = new Node("var_op");
            parent.addChild(varOpNode);
            var_op(varOpNode);
        }
        else {
            Node equalAssignOpNode = new Node("equal_assign_op");
            parent.addChild(equalAssignOpNode);
            equal_assign_op(equalAssignOpNode);

            Node varOpNode = new Node("var_op");
            parent.addChild(varOpNode);
            var_op(varOpNode);
        }
    }

    private void equal_assign_op(Node parent) {
        if (checkMatch(currToken, "+=")) {
            match(currToken, "+=", parent);
        }
        else if (checkMatch(currToken, "-=")) {
            match(currToken, "-=", parent);
        }
        else if (checkMatch(currToken, "*=")) {
            match(currToken, "*=", parent);
        }
        else {
            match(currToken, "/=", parent);
        }
    }

    private void while_loop(Node parent) {
        match(currToken, "vibe", parent);

        Node whileNode = new Node("while_opt");
        parent.addChild(whileNode);
        while_opt(whileNode);
    }

    private void while_opt(Node parent) {
        if (checkMatch(currToken, first("while_"))) {
            Node whileNode = new Node("while_");
            parent.addChild(whileNode);
            while_(whileNode);
        }
        else {
            Node doWhileNode = new Node("do_while");
            parent.addChild(doWhileNode);
            do_while(doWhileNode);
        }
    }

    private void while_(Node parent) {
        match(currToken, "check", parent);
        match(currToken, "¿", parent);

        Node booleanCondNode = new Node("boolean_cond");
        parent.addChild(booleanCondNode);
        boolean_cond(booleanCondNode);

        match(currToken, "?", parent);
        match(currToken, "¡", parent);

        Node funcBodyNode = new Node("func_body");
        parent.addChild(funcBodyNode);
        func_body(funcBodyNode);

        match(currToken, "!", parent);
    }

    private void do_while(Node parent) {
        match(currToken, "¡", parent);

        Node funcBodyNode = new Node("func_body");
        parent.addChild(funcBodyNode);
        func_body(funcBodyNode);

        match(currToken, "!", parent);
        match(currToken, "then", parent);
        match(currToken, "check", parent);
        match(currToken, "¿", parent);

        Node booleanCondNode = new Node("boolean_cond");
        parent.addChild(booleanCondNode);
        boolean_cond(booleanCondNode);

        match(currToken, "?", parent);
    }

    private void conditional(Node parent) {
        Node ifNode = new Node("if_");
        parent.addChild(ifNode);
        if_(ifNode);

        Node elseNode = new Node("cond_else");
        parent.addChild(elseNode);
        cond_else(elseNode);
    }

    private void if_(Node parent) {
        match(currToken, "like", parent);
        match(currToken, "¿", parent);

        Node booleanCondNode = new Node("boolean_cond");
        parent.addChild(booleanCondNode);
        boolean_cond(booleanCondNode);

        match(currToken, "?", parent);
        match(currToken, "¡", parent);

        Node funcBodyNode = new Node("func_body");
        parent.addChild(funcBodyNode);
        func_body(funcBodyNode);

        match(currToken, "!", parent);
    }

    private void cond_else(Node parent) {
        if (checkMatch(currToken, "whatever")) {
            match(currToken, "whatever", parent);

            Node elseNode = new Node("else_opt");
            parent.addChild(elseNode);
            else_opt(elseNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void else_opt(Node parent) {
        if (checkMatch(currToken, first("elif"))) {
            Node elifNode = new Node("elif");
            parent.addChild(elifNode);
            elif(elifNode);
        }
        else {
            Node elseNode = new Node("else_");
            parent.addChild(elseNode);
            else_(elseNode);
        }
    }

    private void elif(Node parent) {
        match(currToken, "like", parent);
        match(currToken, "¿", parent);

        Node booleanCondNode = new Node("boolean_cond");
        parent.addChild(booleanCondNode);
        boolean_cond(booleanCondNode);

        match(currToken, "?", parent);
        match(currToken, "¡", parent);

        Node funcBodyNode = new Node("func_body");
        parent.addChild(funcBodyNode);
        func_body(funcBodyNode);

        match(currToken, "!", parent);

        Node condElseNode = new Node("cond_else");
        parent.addChild(condElseNode);
        cond_else(condElseNode);

    }

    private void else_(Node parent) {
        match(currToken, "¡", parent);

        Node currNode = new Node("func_body");
        parent.addChild(currNode);
        func_body(currNode);

        match(currToken, "!", parent);
    }

    private void boolean_cond(Node parent) {
        Node negNode = new Node("neg");
        parent.addChild(negNode);
        neg(negNode);

        Node condNode = new Node("condition");
        parent.addChild(condNode);
        condition(condNode);

        Node nestedCondNode = new Node("nest_cond");
        parent.addChild(nestedCondNode);
        nest_cond(nestedCondNode);
    }

    private void condition(Node parent) {
        if (checkMatch(currToken, "name")) {
            match(currToken, "name", parent);

            Node comparisonNode = new Node("comparison");
            parent.addChild(comparisonNode);
            comparison(comparisonNode);
        }
        else if (checkMatch(currToken, first("comparable_literal"))) {
            Node literalNode = new Node("comparable_literal");
            parent.addChild(literalNode);
            comparable_literal(literalNode);

            Node comparisonNode = new Node("comparison");
            parent.addChild(comparisonNode);
            comparison(comparisonNode);
        }
        else {
            match(currToken, "(", parent);

            Node currNode = new Node("condition");
            parent.addChild(currNode);
            condition(currNode);

            match(currToken, ")", parent);
        }
    }

    private void nest_cond(Node parent) {
        if (checkMatch(currToken, "&")) {
            match(currToken, "&", parent);

            Node booleanNode = new Node("boolean_cond");
            parent.addChild(booleanNode);
            boolean_cond(booleanNode);

            return;
        }
        else if (checkMatch(currToken, "|")) {
            match(currToken, "|", parent);

            Node booleanNode = new Node("boolean_cond");
            parent.addChild(booleanNode);
            boolean_cond(booleanNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void comparison(Node parent) {
        if (checkMatch(currToken, first("comparison_op"))) {
            Node compOpNode = new Node("comparison_op");
            parent.addChild(compOpNode);
            comparison_op(compOpNode);

            Node varOpNode = new Node("var_op");
            parent.addChild(varOpNode);
            var_op(varOpNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void comparison_op(Node parent) {
        if (checkMatch(currToken, "<")) {
            match(currToken, "<", parent);
        }
        else if (checkMatch(currToken, "<=")) {
            match(currToken, "<=", parent);
        }
        else if (checkMatch(currToken, ">")) {
            match(currToken, ">", parent);
        }
        else if (checkMatch(currToken, ">=")) {
            match(currToken, ">=", parent);
        }
        else if (checkMatch(currToken, "==")) {
            match(currToken, "==", parent);
        }
        else{
            match(currToken, "#=", parent);
        }
    }

    private void switch_(Node parent) {
        match(currToken, "swipe", parent);
        match(currToken, "¿", parent);
        match(currToken, "name", parent);
        match(currToken, "?", parent);
        match(currToken, "¡", parent);

        Node caseNode = new Node("case_");
        parent.addChild(caseNode);
        case_(caseNode);

        Node defaultNode = new Node("default_");
        parent.addChild(defaultNode);
        default_(defaultNode);

        match(currToken, "!", parent);
    }

    private void case_(Node parent) {
        match(currToken, "right", parent);

        Node litNode = new Node("literal");
        parent.addChild(litNode);
        literal(litNode);

        match(currToken, ":", parent);

        Node caseNode = new Node("case_body");
        parent.addChild(caseNode);
        case_body(caseNode);

        Node nestCaseNode = new Node("nested_case");
        parent.addChild(nestCaseNode);
        nested_case(nestCaseNode);
    }

    private void nested_case(Node parent) {
        if (checkMatch(currToken, first("case_"))) {
            Node caseNode = new Node("case_");
            parent.addChild(caseNode);
            case_(caseNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void default_(Node parent) {
        if (checkMatch(currToken, "left")) {
            match(currToken, "left", parent);
            match(currToken, ":", parent);

            Node currNode = new Node("case_body");
            parent.addChild(currNode);
            case_body(currNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void case_body(Node parent) {
        if (checkMatch(currToken, first("func_body"))) {
            Node funcNode = new Node("func_body");
            parent.addChild(funcNode);
            func_body(funcNode);

            Node breakNode = new Node("break_");
            parent.addChild(breakNode);
            break_(breakNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void break_(Node parent) {
        if (checkMatch(currToken, "periodt")) {
            match(currToken, "periodt", parent);
            match(currToken, ".", parent);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void for_loop(Node parent) {
        match(currToken, "4", parent);
        match(currToken, "¿", parent);

        Node forDecNode = new Node("for_decl");
        parent.addChild(forDecNode);
        for_decl(forDecNode);

        Node boolNode = new Node("boolean_cond");
        parent.addChild(boolNode);
        boolean_cond(boolNode);

        match(currToken, ".", parent);

        Node forItNode = new Node("for_iterator");
        parent.addChild(forItNode);
        for_iterator(forItNode);

        match(currToken, "?", parent);
        match(currToken, "¡", parent);

        Node forBodyNode = new Node("func_body");
        parent.addChild(forBodyNode);
        func_body(forBodyNode);

        match(currToken, "!", parent);
    }

    private void for_decl(Node parent) {
        Node forNode = new Node("for_var_type");
        parent.addChild(forNode);
        for_var_type(forNode);

        match(currToken, "name", parent);

        Node varNode = new Node("var_decl_assign");
        parent.addChild(varNode);
        var_decl_assign(varNode);
    }

    private void for_var_type(Node parent) {
        if (checkMatch(currToken, first("datatype"))) {
            Node currNode = new Node("datatype");
            parent.addChild(currNode);
            datatype(currNode);

            return;
        }
        parent.addChild(new Node("epsilon"));
    }

    private void for_iterator(Node parent) {
        match(currToken, "name", parent);

        Node currNode = new Node("it_change");
        parent.addChild(currNode);
        it_change(currNode);
    }

    private void it_change(Node parent) {
        if (checkMatch(currToken, "++")) {
            match(currToken, "++", parent);
        }
        else if (checkMatch(currToken, "--")) {
            match(currToken, "--", parent);
        }
        else {
            Node equalNode = new Node("equal_assign_op");
            parent.addChild(equalNode);
            equal_assign_op(equalNode);

            Node varNode = new Node("var_value");
            parent.addChild(varNode);
            var_value(varNode);
        }
    }
}
