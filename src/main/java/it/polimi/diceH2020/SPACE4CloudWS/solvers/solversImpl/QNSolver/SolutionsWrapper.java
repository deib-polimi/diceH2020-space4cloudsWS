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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver.generated.Solutions;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

class SolutionsWrapper {

    private Solutions wrappedSolutions;
    private Double meanValue = null;
    private Boolean failure = null;

    static SolutionsWrapper unMarshal(File file) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Solutions.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Solutions resultObject = (Solutions) jaxbUnmarshaller.unmarshal(file);
        return new SolutionsWrapper(resultObject);
    }

    private SolutionsWrapper(Solutions wrapped) {
        wrappedSolutions = wrapped;
    }

    double getMeanValue() {
        if (meanValue == null) {
            final String result = wrappedSolutions.getMeasure().get(0).getMeanValue();
            try {
                meanValue = Double.parseDouble(result);
            } catch (NumberFormatException e) {
                if (result.toLowerCase().contains("infinity")) {
                    meanValue = result.contains("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                } else {
                    meanValue = Double.NaN;
                }
            }
        }
        return meanValue;
    }

    boolean isFailed() {
        if (failure == null) {
            final String successful = wrappedSolutions.getMeasure().get(0).getSuccessful();
            failure = ! Boolean.parseBoolean(successful);
        }
        return failure;
    }
}
