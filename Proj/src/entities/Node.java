package entities;

import java.util.LinkedList;

public class Node {
    private final String statement;
    private String value;
    private Node parent;
    private final LinkedList<Node> childs;
    private int id;
    private SourceLocation location;

    public Node(String statement) {
        this.statement = statement;
        this.childs = new LinkedList<>();
        this.parent = null;
        this.location = new SourceLocation();
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void addChild(Node node) {
        node.setParent(this);
        this.childs.add(node);
    }

    public String getValue() {
        return this.value;
    }

    public String getStatement() {
        return this.statement;
    }

    public LinkedList<Node> getChilds() {
        return this.childs;
    }

    public Node getParent() {
        return this.parent;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public SourceLocation getLocation() {
        return this.location;
    }

    public void setLocation(SourceLocation location) {
        this.location = location;
    }
}
