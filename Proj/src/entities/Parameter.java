package entities;

public class Parameter {
    private final Datatype type;
    private final String name;
    private final int id;

    public Parameter(Datatype type, String name, int id) {
        this.type = type;
        this.name = name;
        this.id = id;
    }

    public Datatype getDatatype() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
