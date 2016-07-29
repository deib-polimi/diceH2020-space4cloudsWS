package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfigurationsMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

public class Matrix {
	
	private ConcurrentHashMap<String,SolutionPerJob[]> matrix;
	
	private String matrixNVMString;
	
	private int numCells;
	
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
}