package entities;

import java.util.ArrayList;

public class Datatype {
        private final String type;
        private final ArrayList<Integer> dimensions;

        public Datatype(String type, ArrayList<Integer> dimensions) {
            this.type = type;
            this.dimensions = dimensions;
        }

        public String getType() {
            return type;
        }

        public ArrayList<Integer> getDimensions() {
            return dimensions;
        }

        public String getMessage() {
            StringBuilder message = new StringBuilder();
            if (dimensions != null) {
                for (int i = 0; i < dimensions.size(); i++) {
                    message.append("fam ").append(dimensions.get(i).intValue()).append(" ");
                }
            }
            message.append(type);
            return message.toString();
        }
}
