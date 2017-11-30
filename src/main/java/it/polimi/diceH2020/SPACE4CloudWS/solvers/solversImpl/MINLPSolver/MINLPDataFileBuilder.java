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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.MatrixHugeHoleException;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import lombok.Setter;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class MINLPDataFileBuilder {

   private List<String> lines;
   private InstanceDataMultiProvider data;
   private Matrix fullMatrix;

   protected Logger logger = Logger.getLogger(getClass());

   FileUtility fileUtility;


   public MINLPDataFileBuilder (InstanceDataMultiProvider instanceDataMultiProvider, Matrix matrixWithoutHoles) {
      data = instanceDataMultiProvider;
      fullMatrix = matrixWithoutHoles;
      lines = new LinkedList<>();
      fileUtility = new FileUtility();
   }

   public void createDataFile(File dataFile) throws MatrixHugeHoleException, IOException {
      Matrix matrix = fullMatrix.removeFailedSimulations();
      addHeader();
      addScalarParameter("nAM", matrix.getNumRows());

      addScalarParameter("N", data.getPrivateCloudParameters().getN());
      addDoubleParameter("V", data.getPrivateCloudParameters().getV());
      addDoubleParameter("M", data.getPrivateCloudParameters().getM());

      final boolean tail = (matrix.getNumRows() > 1);
      @SuppressWarnings("unchecked") Pair<Iterable<Integer>, Iterable<Double>>[] costOrPenaltiesPairs = tail ? new Pair[matrix.getNumRows()-1] : null;
      @SuppressWarnings("unchecked") Pair<Iterable<Integer>, Iterable<Double>>[] mTilde = tail ? new Pair[matrix.getNumRows()-1] : null;
      @SuppressWarnings("unchecked") Pair<Iterable<Integer>, Iterable<Double>>[] vTilde = tail ? new Pair[matrix.getNumRows()-1] : null;
      @SuppressWarnings("unchecked") Pair<Iterable<Integer>, Iterable<Integer>>[] nu = tail ? new Pair[matrix.getNumRows()-1] : null;

      Pair<Iterable<Integer>, Iterable<Double>> costOrPenaltiesFirst = null;
      Pair<Iterable<Integer>, Iterable<Double>> mTildeFirst = null;
      Pair<Iterable<Integer>, Iterable<Double>> vTildeFirst = null;
      Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;

      int i = 0;
      for (Entry<String, SolutionPerJob[]> row : matrix.entrySet()) {
         Iterable<Integer> rowH = matrix.getAllH(row.getKey());
         Iterable<Double> rowCostOrPenalty = matrix.getAllCost(row.getKey());
         Iterable<Double> rowMTilde = matrix.getAllMtilde(row.getKey(), data.getMapVMConfigurations());
         Iterable<Double> rowVTilde = matrix.getAllVtilde(row.getKey(), data.getMapVMConfigurations());
         Iterable<Integer> rowNu = matrix.getAllNu(row.getKey());

         Pair<Iterable<Integer>, Iterable<Double>> costOrPenalty = new ImmutablePair<>(rowH, rowCostOrPenalty);
         Pair<Iterable<Integer>, Iterable<Double>> thisMTilde = new ImmutablePair<>(rowH, rowMTilde);
         Pair<Iterable<Integer>, Iterable<Double>> thisVTilde = new ImmutablePair<>(rowH, rowVTilde);
         Pair<Iterable<Integer>, Iterable<Integer>> thisNu = new ImmutablePair<>(rowH, rowNu);

         if (i == 0) {
            costOrPenaltiesFirst = costOrPenalty;
            mTildeFirst = thisMTilde;
            vTildeFirst = thisVTilde;
            nuFirst = thisNu;
         } else if (tail) {
            int idx = i - 1;
            costOrPenaltiesPairs[idx] = costOrPenalty;
            mTilde[idx] = thisMTilde;
            vTilde[idx] = thisVTilde;
            nu[idx] = thisNu;
         }

         int writtenIndex = ++i;
         addIndexedSet("H", writtenIndex, rowH);
         fullMatrix.addNotFailedRow(writtenIndex, row.getKey());
      }

      final String parameterName = "bigC";
      addIndexedArrayParameter(parameterName, costOrPenaltiesFirst, costOrPenaltiesPairs);
      addIndexedArrayParameter("Mtilde", mTildeFirst, mTilde);
      addIndexedArrayParameter("Vtilde", vTildeFirst, vTilde);
      addIndexedArrayParameter("nu", nuFirst, nu);
      if(fileUtility == null)
         throw new RuntimeException("fileUtility is null");
      if(dataFile== null)
         throw new RuntimeException("dataFile is null");
      fileUtility.writeContentToFile(build(), dataFile);
   }

   MINLPDataFileBuilder addScalarParameter(String name, int value) {
      String currentLine = String.format(Locale.UK,"param %s := %d;", name, value);
      if(lines == null)
         throw new RuntimeException("lines should not be null");
      lines.add(currentLine);
      return this;
   }

   MINLPDataFileBuilder addDoubleParameter(String name, double value) {
      String currentLine = String.format("param %s := %.5f;", name, value);
      lines.add(currentLine);
      return this;
   }

   <N extends Number> MINLPDataFileBuilder addArrayParameter(String name, Iterable<N> values) {
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

    MINLPDataFileBuilder addIndexedSet(String name, int index, Iterable<Integer> elements) {
        String currentLine = String.format("set %s[%d] :=", name, index);
        lines.add(currentLine);
        elements.forEach(element -> lines.add(element.toString()));
        lines.add(";");
        return this;
    }

    //TODO 
    @SafeVarargs
    final <N extends Number> MINLPDataFileBuilder addIndexedArrayParameter(String name,
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
