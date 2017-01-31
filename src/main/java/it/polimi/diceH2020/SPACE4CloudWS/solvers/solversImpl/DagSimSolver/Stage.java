/*
Copyright 2017 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.DagSimSolver;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;

@Accessors(chain = true)
@Setter(AccessLevel.PACKAGE)
class Stage {
    private String name;
    private Integer tasks;
    private Distribution distribution;

    @Setter(AccessLevel.NONE)
    private List<String> predecessors = new LinkedList<>();

    @Setter(AccessLevel.NONE)
    private List<String> successors = new LinkedList<>();

    Stage addPredecessor(String name) {
        predecessors.add(name);
        return this;
    }

    Stage addSuccessor(String name) {
        successors.add(name);
        return this;
    }

    private String formatList(List<String> list) {
        String result = "{}";
        if (! list.isEmpty()) {
            String elements = String.join("\", \"", list);
            result = String.format("{ \"%s\" }", elements);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("{ name = \"%s\", tasks = \"%d\", distr = %s, pre = %s, post = %s }",
                name, tasks, distribution.toString(), formatList(predecessors), formatList(successors));
    }
}
