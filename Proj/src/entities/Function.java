package entities;

import java.util.ArrayList;
import java.util.List;

public class Function {
    private final String name;
    private final int id;
    private final List<Parameter> parameters;

    public Function(String name, int id) {
        this.name = name;
        this.id = id;
        this.parameters = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void addParameter(Datatype type, String name, int id) {
        parameters.add(new Parameter(type, name, id));
    }

    public List<Parameter> getParameters() {
        return parameters;
    }
}
