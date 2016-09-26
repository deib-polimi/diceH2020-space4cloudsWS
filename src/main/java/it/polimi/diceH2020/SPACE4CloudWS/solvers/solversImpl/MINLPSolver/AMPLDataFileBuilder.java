/*
Copyright 2016 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class AMPLDataFileBuilder {

    private List<String> lines;

    AMPLDataFileBuilder(int numberOfClasses) {
        lines = new LinkedList<>();
        addHeader();
        addScalarParameter("nAM", numberOfClasses);
    }

    AMPLDataFileBuilder addScalarParameter(String name, int value) {
        String currentLine = String.format(Locale.UK,"param %s := %d;", name, value);
        lines.add(currentLine);
        return this;
    }

    AMPLDataFileBuilder addDoubleParameter(String name, double value) {
        String currentLine = String.format("param %s := %.5f;", name, value);
        lines.add(currentLine);
        return this;
    }

    <N extends Number> AMPLDataFileBuilder addArrayParameter(String name, Iterable<N> values) {
        String currentLine = String.format("param: %s :=", name);
        lines.add(currentLine);
        int idx = 1;
        for (N value: values) {
            if (value instanceof Double) {
                //UK to have . instead of , as separator
                currentLine = String.format(Locale.UK, "%d %f", idx++, value.doubleValue());
            } else if (value instanceof Integer) {
                currentLine = String.format("%d %d", idx++, value.intValue());
            }
            lines.add(currentLine);
        }
        lines.add(";");
        return this;
    }

    AMPLDataFileBuilder addIndexedSet(String name, int index, Iterable<Integer> elements) {
        String currentLine = String.format("set %s[%d] :=", name, index);
        lines.add(currentLine);
        elements.forEach(element -> lines.add(element.toString()));
        lines.add(";");
        return this;
    }

    //TODO 
    @SafeVarargs
    final <N extends Number> AMPLDataFileBuilder addIndexedArrayParameter(String name,
                                                                          Pair<Iterable<Integer>, Iterable<N>> head,
                                                                          Pair<Iterable<Integer>, Iterable<N>>... tail) {
        final String currentLine = String.format("param %s :=", name);
        lines.add(currentLine);
        int idx = 1;
        printIndexedTable(idx++, head);
        if(tail != null){
            for (Pair<Iterable<Integer>, Iterable<N>> pair: tail) {
                printIndexedTable(idx++, pair);
            }
        }
        lines.add(";");
        return this;
    }

    private <N extends Number> void printIndexedTable(int idx, Pair<Iterable<Integer>, Iterable<N>> pair) {
        String currentLine = String.format("  [%d, *] :=", idx);
        lines.add(currentLine);
        Iterator<Integer> first = pair.getKey().iterator();
        Iterator<N> second = pair.getValue().iterator();
        while (first.hasNext() && second.hasNext()) {
            Integer key = first.next();
            N value = second.next();
            if (value instanceof Double) {
                //UK to have . instead of , as separator
                currentLine = String.format(Locale.UK, "    %d %f", key, value.doubleValue());
            } else if (value instanceof Integer) {
                currentLine = String.format("    %d %d", key, value.intValue());
            }
            lines.add(currentLine);
        }
    }

    private void addHeader() {
        lines.add("data;");
    }

    private void addFooter() {}

    String build() throws IOException {
        addFooter();
        return String.join("\n", lines);
    }

}
