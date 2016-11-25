/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

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
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.AMPLModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.MatrixHugeHoleException;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map.Entry;

class AMPLDataFileBuilderBuilder {

	private InstanceDataMultiProvider data;
	private Matrix fullMatrix;
	private AMPLModel model;

	AMPLDataFileBuilderBuilder (InstanceDataMultiProvider instanceDataMultiProvider,
								Matrix matrixWithoutHoles, AMPLModel modelType) {
		data = instanceDataMultiProvider;
		fullMatrix = matrixWithoutHoles;
		model = modelType;
	}

	AMPLDataFileBuilder populateBuilder() throws MatrixHugeHoleException {
		Matrix matrix = fullMatrix.removeFailedSimulations();
		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(matrix.getNumRows());

		builder.addScalarParameter("N", data.getPrivateCloudParameters().getN());
		builder.addDoubleParameter("V", data.getPrivateCloudParameters().getV());
		builder.addDoubleParameter("M", data.getPrivateCloudParameters().getM());
		if (model == AMPLModel.BIN_PACKING) {
			builder.addDoubleParameter("bigE", data.getPrivateCloudParameters().getE());
		}

		final boolean tail = (matrix.getNumRows() > 1);
		Pair<Iterable<Integer>, Iterable<Double>>[] costOrPenaltiesPairs = tail ? new Pair[matrix.getNumRows()-1] : null;
		Pair<Iterable<Integer>, Iterable<Double>>[] mTilde = tail ? new Pair[matrix.getNumRows()-1] : null;
		Pair<Iterable<Integer>, Iterable<Double>>[] vTilde = tail ? new Pair[matrix.getNumRows()-1] : null;
		Pair<Iterable<Integer>, Iterable<Integer>>[] nu = tail ? new Pair[matrix.getNumRows()-1] : null;

		Pair<Iterable<Integer>, Iterable<Double>> costOrPenaltiesFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> mTildeFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> vTildeFirst = null;
		Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;

		int i = 0;
		for (Entry<String, SolutionPerJob[]> row : matrix.entrySet()) {
			Iterable<Integer> rowH = matrix.getAllH(row.getKey());
			Iterable<Double> rowCostOrPenalty = model == AMPLModel.KNAPSACK
					? matrix.getAllCost(row.getKey())
					: matrix.getAllPenalty(row.getKey());
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
			builder.addIndexedSet("H", writtenIndex, rowH);
			fullMatrix.addNotFailedRow(writtenIndex, row.getKey());
		}

		final String parameterName = (model == AMPLModel.KNAPSACK) ? "bigC" : "bigP";
		builder.addIndexedArrayParameter(parameterName, costOrPenaltiesFirst, costOrPenaltiesPairs);
		builder.addIndexedArrayParameter("Mtilde", mTildeFirst, mTilde);
		builder.addIndexedArrayParameter("Vtilde", vTildeFirst, vTilde);
		builder.addIndexedArrayParameter("nu", nuFirst, nu);

		return builder;
	}

}
