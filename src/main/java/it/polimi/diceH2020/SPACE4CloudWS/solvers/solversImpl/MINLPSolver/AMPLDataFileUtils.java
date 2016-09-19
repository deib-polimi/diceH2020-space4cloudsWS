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
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map.Entry;

class AMPLDataFileUtils {


	@SuppressWarnings("unchecked")
	static AMPLDataFileBuilder knapsackBuilder(InstanceDataMultiProvider data, Matrix matrixWithoutHoles) {
		boolean tail = false;
		Matrix matrix = matrixWithoutHoles.removeFailedSimulations();

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(matrix.getNumRows());
		builder.addScalarParameter("N", data.getPrivateCloudParameters().getN());
		builder.addDoubleParameter("V", data.getPrivateCloudParameters().getV());
		builder.addDoubleParameter("M", data.getPrivateCloudParameters().getM());

		Pair<Iterable<Integer>, Iterable<Double>>[] bigC = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] mTilde = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] vTilde = null;
		Pair<Iterable<Integer>, Iterable<Integer>>[] nu = null;

		if(matrix.getNumRows()>1){
			tail = true;

			bigC=new Pair[matrix.getNumRows()-1];
			mTilde=new Pair[matrix.getNumRows()-1];
			vTilde=new Pair[matrix.getNumRows()-1];
			nu = new Pair[matrix.getNumRows()-1];
		}

		Pair<Iterable<Integer>, Iterable<Double>> bigCFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> mTildeFirst=null;
		Pair<Iterable<Integer>, Iterable<Double>> vTildeFirst=null;
		Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;

		int i = 0;
		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
			Iterable<Integer> rowH = matrix.getAllH(row.getKey());
			Iterable<Double> rowCost = matrix.getAllCost(row.getKey());
			System.out.println(rowCost);
			Iterable<Double> rowMtilde = matrix.getAllMtilde(row.getKey(), data.getMapVMConfigurations());
			Iterable<Double> rowVtilde = matrix.getAllVtilde(row.getKey(), data.getMapVMConfigurations());
			Iterable<Integer> rowNu = matrix.getAllNu(row.getKey());

			if(i==0){
				bigCFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowCost);
				mTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				vTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				nuFirst = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
			}else if(tail){
				Pair<Iterable<Integer>, Iterable<Double>> c = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowCost);
				Pair<Iterable<Integer>, Iterable<Double>> m = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				Pair<Iterable<Integer>, Iterable<Double>> v = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				Pair<Iterable<Integer>, Iterable<Integer>> n = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
				//first received i, i=1

				bigC[i-1] = c;
				mTilde[i-1] = m;
				vTilde[i-1] = v;
				nu[i-1] = n;
			}
			builder.addIndexedSet("H", i+1, rowH);
			matrixWithoutHoles.addNotFailedRow(i+1, row.getKey());
			i++;
		}

//		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
//		}
		builder.addIndexedArrayParameter("bigC", bigCFirst, bigC);
		builder.addIndexedArrayParameter("nu", nuFirst, nu);
		builder.addIndexedArrayParameter("Mtilde", mTildeFirst, mTilde);
		builder.addIndexedArrayParameter("Vtilde", vTildeFirst, vTilde);
		return builder;
	}


	@SuppressWarnings("unchecked")
	static AMPLDataFileBuilder binPackingBuilder(InstanceDataMultiProvider data, Matrix matrixWithoutHoles) {
		boolean tail = false;
		Matrix matrix = matrixWithoutHoles.removeFailedSimulations();

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(matrix.getNumRows());
		builder.addScalarParameter("N", data.getPrivateCloudParameters().getN());
		builder.addDoubleParameter("V", data.getPrivateCloudParameters().getV());
		builder.addDoubleParameter("M", data.getPrivateCloudParameters().getM());
		builder.addDoubleParameter("bigE", data.getPrivateCloudParameters().getE());


		Pair<Iterable<Integer>, Iterable<Double>>[] bigP = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] mTilde = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] vTilde = null;
		Pair<Iterable<Integer>, Iterable<Integer>>[] nu = null;



		if(matrix.getNumRows()>1){
			tail = true;

			bigP=new Pair[matrix.getNumRows()-1];
			mTilde=new Pair[matrix.getNumRows()-1];
			vTilde=new Pair[matrix.getNumRows()-1];
			nu = new Pair[matrix.getNumRows()-1];
		}

		Pair<Iterable<Integer>, Iterable<Double>> bigPFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> mTildeFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> vTildeFirst = null;
		Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;

		int i = 0;
		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
			Iterable<Integer> rowH = matrix.getAllH(row.getKey());
			Iterable<Double> rowPenalty = matrix.getAllPenalty(row.getKey());
			Iterable<Double> rowMtilde = matrix.getAllMtilde(row.getKey(), data.getMapVMConfigurations());
			Iterable<Double> rowVtilde = matrix.getAllVtilde(row.getKey(), data.getMapVMConfigurations());
			Iterable<Integer> rowNu = matrix.getAllNu(row.getKey());

			if(i==0){
				bigPFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowPenalty);
				mTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				vTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				nuFirst = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
			}else if(tail){
				Pair<Iterable<Integer>, Iterable<Double>> p = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowPenalty);
				Pair<Iterable<Integer>, Iterable<Double>> m = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				Pair<Iterable<Integer>, Iterable<Double>> v = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				Pair<Iterable<Integer>, Iterable<Integer>> n = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
				//first received i, i=1
				bigP[i-1] = p;
				mTilde[i-1] = m;
				vTilde[i-1] = v;
				nu[i-1] = n;

			}
			builder.addIndexedSet("H", i+1, rowH);
			matrixWithoutHoles.addNotFailedRow(i+1, row.getKey());
			i++;
		}

//		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
//		}

		builder.addIndexedArrayParameter("bigP", bigPFirst, bigP);
		builder.addIndexedArrayParameter("Mtilde", mTildeFirst, mTilde);
		builder.addIndexedArrayParameter("Vtilde", vTildeFirst, vTilde);
		builder.addIndexedArrayParameter("nu", nuFirst, nu);
		return builder;
	}

}
