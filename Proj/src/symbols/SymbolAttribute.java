package symbols;

import java.util.ArrayList;

public class SymbolAttribute extends SymbolRow{

    protected final String name;
    protected final String dataType;
    protected ArrayList<Integer> dimensions; // variable has null
    protected final int declarationLine;
    protected boolean declared;
    protected boolean isFunction;
    protected boolean initialized;
    protected boolean mightNotBeInitialized;
    protected String register;
    protected boolean isReadOnly;

    public SymbolAttribute(String name, int id, String dataType, ArrayList<Integer> dimensions, int declarationLine, SymbolRow parentScope, boolean isFunction) {
        this.name = name;
        this.id = id;
        this.dataType = dataType;
        this.dimensions = dimensions;
        this.declarationLine = declarationLine;
        this.parentScope = parentScope;
        this.declared = false;
        this.isFunction = isFunction;
        this.initialized = false;
        this.mightNotBeInitialized = true;
        this.isReadOnly = false;
    }
}
