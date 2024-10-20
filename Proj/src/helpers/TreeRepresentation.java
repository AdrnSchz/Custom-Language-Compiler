package helpers;

import entities.Node;

import java.util.ArrayList;
import java.util.Collections;

public class TreeRepresentation {
    private int maxLevel = 0;
    private final ArrayList<Integer> maxNameLengthPerLevel;
    private final ArrayList<String> stringsPerLvl;


    public TreeRepresentation(Node root) {
        maxNameLengthPerLevel = new ArrayList<>();
        maxNameLengthPerLevel.add(0);
        stringsPerLvl = new ArrayList<>();
        maxLevel = 0;

        readAST(root, 0);

        for (Integer maxNameLength: maxNameLengthPerLevel)  {
            int stringLength = "|-".length() + maxNameLength + 3;
            String str = String.join("", Collections.nCopies(stringLength, " "));
            stringsPerLvl.add(str);
        }

        generateTree(root, 0);
    }

    private void fillIntermediateStrings() {

        for (int i = 0; i < stringsPerLvl.size(); i++) {
            String str = " ";

            if (stringsPerLvl.get(i).charAt(0) == '|') {
                str = "|";
            }
            int stringLength = "|-".length() + maxNameLengthPerLevel.get(i) + 5 - 1;
            str = str + String.join("", Collections.nCopies(stringLength, " "));
            stringsPerLvl.set(i, str);
        }
    }

    private void printLine() {
        for (String str : stringsPerLvl) {
            System.out.print(str);
        }
        System.out.println();
        fillIntermediateStrings();
    }

    private void removePotLines(Node node, int lvl) {

        if (node.getParent() == null ||
                node == node.getParent().getChilds().get(node.getParent().getChilds().size() - 1)) {
            int stringLength = "  ".length() + maxNameLengthPerLevel.get(lvl) + 5;
            stringsPerLvl.set(lvl, String.join("", Collections.nCopies(stringLength, " ")));
        }
        if (node.getParent() != null) {
            removePotLines(node.getParent(), lvl - 1);
        }
    }

    private void generateTree(Node node, int lvl) {

        int stringLength = maxNameLengthPerLevel.get(lvl)-node.getStatement().length();
        String str = "|-" + node.getStatement() + String.join("", Collections.nCopies(stringLength, " "));
        if (node.getChilds().size() != 0) {
            str = str + " ----";
        }
        else {
            str = str + "    ";
        }
        stringsPerLvl.set(lvl, str);

        if (node.getChilds().size() == 0) {
            printLine();
            removePotLines(node, lvl);
            printLine();
            printLine();
            return;
        }

        for (int i = 0; i < node.getChilds().size(); i++) {
            generateTree(node.getChilds().get(i), lvl+1);
        }
    }

    private void readAST(Node node, int lvl) {

        if (maxLevel < lvl) {
            maxLevel  = lvl;
            maxNameLengthPerLevel.add(node.getStatement().length());
        }
        else if (maxNameLengthPerLevel.get(lvl) < node.getStatement().length()) {
            maxNameLengthPerLevel.set(lvl, node.getStatement().length());
        }

        for (int i = 0; i < node.getChilds().size(); i++) {
            readAST(node.getChilds().get(i), lvl+1);
        }
    }
}










