/*
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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfigurationsMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Matrix {
	private final Logger logger = Logger.getLogger(getClass());

	private ConcurrentHashMap<String,SolutionPerJob[]> matrix;

	private String matrixNVMString;

	private int numCells;

	private Map<Integer, String> mapForNotFailedRows;


	public Matrix(){
		matrix = new ConcurrentHashMap<>();
		mapForNotFailedRows = new HashMap<>();
	}

	public void put(String key,SolutionPerJob[] value ){
		matrix.put(key, value);
	}

	public List<SolutionPerJob> getAllSolutions(){
		List<SolutionPerJob> spjList = new ArrayList<>();
		for (Map.Entry<String,SolutionPerJob[]> matrixRow : matrix.entrySet()) {
			for(SolutionPerJob spj : matrixRow.getValue()){
				spjList.add(spj);
			}
		}
		return spjList;
	}

	public SolutionPerJob[] get(String key){
		return matrix.get(key);
	}

	public Set<Map.Entry<String,SolutionPerJob[]>> entrySet() {
		return matrix.entrySet();
	}

	public ConcurrentHashMap<String,SolutionPerJob[]> get(){
		return matrix;
	}

	public Iterable<Integer> getAllH(String row){
		return Arrays.stream(matrix.get(row)).map(SolutionPerJob::getNumberUsers).collect(Collectors.toList());
	}

	public int getHlow(String row){
		return matrix.get(row)[0].getJob().getHlow();
	}

	public int getHup(String row){
		int pos = matrix.get(row).length;
		return matrix.get(row)[pos-1].getJob().getHup();
	}

	public Iterable<Double> getAllCost(String row){
		restoreInitialHup();
		System.out.println("row"+row+" cost");
		return Arrays.stream(matrix.get(row)).map(spj->{System.out.println(spj.getCost());return spj.getCost();}).collect(Collectors.toList());//TODO delete
		//return Arrays.stream(matrix.get(row)).map(SolutionPerJob::getCost).collect(Collectors.toList());
	}

	public Iterable<Double> getAllPenalty(String row){
		restoreInitialHup();
		return Arrays.stream(matrix.get(row)).map(spj->{return spj.getJob().getHup() * spj.getJob().getJob_penalty() - spj.getNumberUsers();}).collect(Collectors.toList());
	}

	private void restoreInitialHup(){
		for(Map.Entry<String, SolutionPerJob[]> row : matrix.entrySet()){
			SolutionPerJob[] jobs = row.getValue();
			int spjHup = 0;
			for(int i=0;i<jobs.length;i++){
				if(jobs[i].getJob().getHup() >  spjHup){
					spjHup = jobs[i].getJob().getHup();
				}
			}
			for(int i=0;i<jobs.length;i++){
				JobClass job = jobs[i].getJob();
				job.setHup(spjHup);
				jobs[i].setAlfa(job.getJob_penalty() * job.getHup() * job.getHlow());
				jobs[i].setCost();
			}
		}
	}

	public boolean containsKey(String row){
		return matrix.containsKey(row);
	}

	public List<String> getAllSelectedVMid(String row){
		return Arrays.stream(matrix.get(row)).map(SolutionPerJob::getTypeVMselected).map(TypeVM::getId).collect(Collectors.toList());
	}

	public List<Double> getAllMtilde(String row,VMConfigurationsMap vmConfigurations){
		List<Double> mTildeSet = new ArrayList<>();
		for(String id : getAllSelectedVMid(row)){
			mTildeSet.add(vmConfigurations.getMapVMConfigurations().get(id).getMemory());
		}
		return mTildeSet;
	}

	public List<Double> getAllVtilde(String row,VMConfigurationsMap vmConfigurations){
		List<Double> vTildeSet = new ArrayList<>();
		for(String id : getAllSelectedVMid(row)){
			vTildeSet.add(vmConfigurations.getMapVMConfigurations().get(id).getCore());
		}
		return vTildeSet;
	}

	/**
	 *
	 * @param row class ID
	 * @param concurrencyLevel H of the given column
	 * @return SolutionPerJob
	 */
	public SolutionPerJob getCell(String row, int concurrencyLevel){
		return Arrays.stream(matrix.get(row)).filter(s->s.getNumberUsers()==concurrencyLevel).findFirst().get();
	}

	public String getID(String row){
		return  Arrays.stream(matrix.get(row)).findFirst().map(SolutionPerJob::getJob).map(JobClass::getId).get();
	}

	public List<Integer> getAllNu(String row){
		List<Integer> nuSet = new ArrayList<>();
		for(SolutionPerJob spj :  matrix.get(row)){
			nuSet.add(spj.getNumReservedVM()+spj.getNumOnDemandVM()+spj.getNumSpotVM());
		}
		return nuSet;
	}

	public int getNumRows(){
		return matrix.size();
	}

	/**
	 * negative cells
	 */
	public Matrix removeFailedSimulations(){


		Matrix matrixWithHoles = new Matrix();

		for (Map.Entry<String,SolutionPerJob[]> matrixRow : matrix.entrySet()){
			int i = (int) Arrays.stream(matrix.get(matrixRow.getKey())).map(SolutionPerJob::getNumberVM).filter(v->v>0).count();
			if(i != 0){
				SolutionPerJob[] rowWithHoles = new SolutionPerJob[i];
				i=0;
				for(SolutionPerJob spj : matrixRow.getValue()){
					if(spj.getNumberVM()>0){
						rowWithHoles[i] = spj;
						i++;
					}
				}
				matrixWithHoles.put(matrixRow.getKey(),  rowWithHoles);
			}else{
				//TODO
				logger.info("All Simulations of Matrix row "+matrix.get(matrixRow.getKey())+", have failed! ");
			}
		}
		return matrixWithHoles;
	}


	public int getNumCells(){
		numCells = 0;
		matrix.forEach((k,v)->{
			numCells += v.length;
		});
		return numCells;
	}

	public String asString(){
		matrixNVMString = new String();
		matrix.forEach((k,v)->{
			matrixNVMString += v[0].getJob().getId()+"\t| ";
			for(SolutionPerJob cell: v){
				matrixNVMString += " H:"+cell.getNumberUsers()+",nVM:"+cell.getNumberVM();
				if(cell.getFeasible()) matrixNVMString += ",F  \t|";
				else matrixNVMString += ",I  \t|";
			}
			matrixNVMString += "\n   \t| ";
			for(SolutionPerJob cell: v){
				matrixNVMString += " dur: "+cell.getDuration().intValue()+"  \t|";
			}
			matrixNVMString += "\n";
		});
		//adding title
		matrixNVMString = "Optimality Matrix(for solution"+matrix.entrySet().iterator().next().getValue()[0].getParentID()+"):\n" + matrixNVMString;
		return matrixNVMString;
	}

	public String getNotFailedRow(Integer key) {
		return mapForNotFailedRows.get(key);
	}

	public boolean containsNotFailedRow(String value){
		return mapForNotFailedRows.containsValue(value);
	}

	public int numNotFailedRows(){
		return mapForNotFailedRows.size();
	}

	public void addNotFailedRow(Integer key, String value) {
		mapForNotFailedRows.put(key, value);
	}

}
