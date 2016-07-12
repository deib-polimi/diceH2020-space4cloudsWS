package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;


public class Matrix {
	
	private ConcurrentHashMap<String,SolutionPerJob[]> matrix;
	
	private String matrixNVMString;
	
	public Matrix(){
		matrix = new ConcurrentHashMap<>();
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
	
	public String asString(){
		matrixNVMString = new String();
		matrix.forEach((k,v)->{
			matrixNVMString += " "+v[0].getJob().getId()+" | ";
			for(SolutionPerJob cell: v){
				matrixNVMString += " H:"+cell.getNumberUsers()+",nVM:"+cell.getNumberVM();
				if(cell.getFeasible()) matrixNVMString += ",F\t|";
				else matrixNVMString += ",I\t|";
			}
			matrixNVMString += "\n   | ";
			for(SolutionPerJob cell: v){
				matrixNVMString += " dur: "+cell.getDuration().intValue()+"\t|";
			}
			matrixNVMString += "\n";
		});
		//adding title
		matrixNVMString = "Optimality Matrix(for solution"+matrix.entrySet().iterator().next().getValue()[0].getParentID()+" ):\n" + matrixNVMString;
		return matrixNVMString;
	}
	
}