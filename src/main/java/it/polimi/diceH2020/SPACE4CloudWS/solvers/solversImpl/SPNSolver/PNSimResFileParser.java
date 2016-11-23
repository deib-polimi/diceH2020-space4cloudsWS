/*
Copyright 2016 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class PNSimResFileParser {
    private File simResFile;

    PNSimResFileParser(File file) {
        simResFile = file;
    }

    Map<String, Double> parse () throws IOException {
        Map<String, Double> result = new HashMap<>();
        try (Stream<String> lines = Files.lines(simResFile.toPath())) {
            Pattern throughputRe = Pattern
                    .compile("Throughput of (?<name>\\w+) \\([\\d\\se.+-]+\\): [\\de.+-]+ <= X <= [\\de.+-]+");
            Pattern tokensRe = Pattern
                    .compile("Mean n\\.of tokens in (?<name>\\w+) \\([\\d\\se.+-]+\\): [\\de.+-]+ <= mu <= [\\de.+-]+");
            Pattern valuesRe = Pattern
                    .compile("Value (?<value>[\\de.+-]+) Mean Value (?<mean>[\\de.+-]+) Accuracy (?<accuracy>[\\de.+-]+)");
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                Matcher throughputMatcher = throughputRe.matcher(line);
                Matcher tokensMatcher = tokensRe.matcher(line);
                String label = null;
                if (throughputMatcher.find()) label = throughputMatcher.group("name");
                if (tokensMatcher.find()) label = tokensMatcher.group("name");
                if (label != null) {
                    line = iterator.next();
                    Matcher valuesMatcher = valuesRe.matcher(line);
                    if (valuesMatcher.find()) {
                        double average = Double.parseDouble(valuesMatcher.group("mean"));
                        result.put(label, average);
                    }
                }
            }
        }
        return result;
    }
}
