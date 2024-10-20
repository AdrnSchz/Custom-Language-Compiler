package symbols;

import entities.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class SymbolsTable {
    private final HashMap<String, ArrayList<SymbolRow>> symbols;

    public SymbolsTable() {
        symbols = new HashMap<>();
    }

    public void insert(SymbolRow symbolRow) {

        String name = Integer.toString(symbolRow.getId());
        if (symbolRow instanceof SymbolAttribute) {
            name = symbolRow.getName();
        }
        ArrayList<SymbolRow> symbolRowList = symbols.get(name);

        if (symbolRowList == null) {
            symbolRowList = new ArrayList<>();
            symbolRowList.add(symbolRow);
            symbols.put(name, symbolRowList);
        }
        else {
            if (lookUp(name, symbolRow.getId()) == null) {
                symbolRowList.add(symbolRow);
            }
        }

    }

    public SymbolRow lookUp(String name, int id) {

        ArrayList<SymbolRow> symbolRowList = symbols.get(name);
        if (symbolRowList == null) {
            return null;
        }

        for (SymbolRow symbolRow : symbolRowList) {
            if (symbolRow.getId() == id) {
                return symbolRow;
            }
        }
        return null;
    }

    // Only for non-SymbolAttributes
    public SymbolRow lookUp(int id) {
        return lookUp(Integer.toString(id), id);
    }

    public ArrayList<SymbolRow> getEntriesInScope(Node nameNode, Stack<Node> stack, boolean isFunction) {

        String name = nameNode.getValue();
        ArrayList<SymbolRow> entries = new ArrayList<>();
        ArrayList<SymbolRow> result = symbols.get(name);

        if (result == null) return entries;

        if (isFunction) {
            for (SymbolRow symbolRow : result) {
                if (symbolRow.isFunction()) {
                    entries.add(symbolRow);
                    nameNode.setId(symbolRow.getId());
                }
            }
            return entries;
        }
        SymbolRow scopeSymbolRow;
        for (int i = stack.size()-1; i >= 0; i--) {
            scopeSymbolRow = lookUp(stack.get(i).getValue() , stack.get(i).getId());
            if (scopeSymbolRow == null) {
                if (stack.get(i).getId() == 0) {
                    scopeSymbolRow = lookUp("start", 0);
                }
                else {
                    scopeSymbolRow = lookUp(stack.get(i).getId());
                }
            }

            for (SymbolRow symbolRow : result) {
                if (symbolRow.getParentScope().equals(scopeSymbolRow)) {
                    entries.add(symbolRow);
                    nameNode.setId(symbolRow.getId());
                }
            }

            if (scopeSymbolRow.isFunction() && !entries.isEmpty()) {
                return entries;
            }
            else if (scopeSymbolRow.isFunction() && entries.isEmpty()){
                for (SymbolRow symbolRow : result) {
                    if (!symbolRow.isFunction() && symbolRow.parentScope.getName().equals("start")) {
                        entries.add(symbolRow);
                        nameNode.setId(symbolRow.getId());
                    }
                }
                return entries;
            }
        }
        return entries;
    }
}
