package symbols;

import java.util.ArrayList;

public class SymbolRow {

    protected SymbolRow parentScope;
    protected int id;

    public SymbolRow() {
    }

    public SymbolRow(SymbolRow parentScope, int id) {
        this.parentScope = parentScope;
        this.id = id;
    }

    public SymbolRow getParentScope() {
        return parentScope;
    }
    //SymbolAttribute methods
    public String getName() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.name;
    }

    public int getId() {
        return this.id;
    }

    public String getDataType() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.dataType;
    }

    public ArrayList<Integer> getDimensions() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.dimensions;
    }

    public boolean isFunction() {
        try {
            SymbolAttribute symbolAttribute = (SymbolAttribute) this;
            return symbolAttribute.isFunction;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public boolean isDeclared() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.declared;
    }

    public void setDeclared(boolean declared) {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        symbolAttribute.declared = declared;
    }

    public boolean isInitialized() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.initialized;
    }

    public void setInitialized(boolean initialized) {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        symbolAttribute.initialized = initialized;
    }

    public String getRegister() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.register;
    }

    public void setRegister(String register) {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        symbolAttribute.register = register;
    }

    public boolean getMightNotBeInitialized() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.mightNotBeInitialized;
    }

    public void setMightNotBeInitialized(boolean mightNotBeInitialized) {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        symbolAttribute.mightNotBeInitialized = mightNotBeInitialized;
    }

    public boolean isReadOnly() {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        return symbolAttribute.isReadOnly;
    }

    public void setReadOnly(boolean isReadOnly) {
        SymbolAttribute symbolAttribute = (SymbolAttribute) this;
        symbolAttribute.isReadOnly = isReadOnly;
    }
}

