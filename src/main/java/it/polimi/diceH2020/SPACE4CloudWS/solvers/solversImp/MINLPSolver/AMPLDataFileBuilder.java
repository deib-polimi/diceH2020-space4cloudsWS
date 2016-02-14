package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class AMPLDataFileBuilder {

    private List<String> lines;

    public AMPLDataFileBuilder(int numberOfClasses) {
        lines = new LinkedList<>();
        addHeader();
        setScalarParameter("nAM", numberOfClasses);
    }

    public AMPLDataFileBuilder setScalarParameter(String name, Integer value) {
        String currentLine = String.format("param %s := %d;", name, value);
        lines.add(currentLine);
        return this;
    }

	public <N extends Number> AMPLDataFileBuilder setArrayParameter(String name, Iterable<N> values) {
        String currentLine = String.format("param: %s :=", name);
        lines.add(currentLine);
        int idx = 1;
        for (N value: values) {
            if (value instanceof Double) {
                currentLine = String.format("%d %f", idx++, value.doubleValue());
            } else if (value instanceof Integer) {
                currentLine = String.format("%d %d", idx++, value.intValue());
            }
            lines.add(currentLine);
        }
        lines.add(";");
        return this;
    }

    private void addHeader() {
        lines.add("data;");
    }

    private void addFooter() {}

    public String build() throws IOException {
        addFooter();
        return String.join("\n", lines);
    }

}
