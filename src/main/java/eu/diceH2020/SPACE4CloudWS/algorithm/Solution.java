package eu.diceH2020.SPACE4CloudWS.algorithm;

public class Solution {

	private int[] number_vm;

	private double[] simulated_time;
	private String[] typeVMselected;
	private int[] user;
	public Solution() {
	}

	public Solution(int numJobs) {
		this. number_vm = new int[numJobs];
		this.simulated_time = new double[numJobs];
		this.typeVMselected = new String[numJobs];
		this.user = new int[numJobs];
	}
	
	public int[] getNumber_vm() {
		return number_vm;
	}

	public double[] getSimulated_time() {
		return simulated_time;
	}

	public String[] getTypevmselected() {
		return typeVMselected;
	}
	
	public String getTypeVMselected(int pos) {
		return typeVMselected[pos];
	}

	public int[] getUser() {
		return user;
	}

	public void setNumber_vm(int[] number_vm) {
		this.number_vm = number_vm;
	}
	
	public void setNumber_vm(int pos, int nVM) {
		this.number_vm[pos] = nVM;
	}

	public void setSimulated_time(double[] simulated_time) {
		this.simulated_time = simulated_time;
	}

	public void setSimulated_time(int pos, double simulated_time) {
		this.simulated_time[pos] = simulated_time;
	}
	
	public void setTypeVMselected(String[] typevmselected) {
		this.typeVMselected = typevmselected;
	}
	
	public void setTypeVMselected(int pos, String typevmselected) {
		this.typeVMselected[pos] = typevmselected;
	}

	public void setUser(int[] user) {
		this.user = user;
	}

	public void setNumUsers(int pos, int numUsers) {
		this.user[pos] = numUsers;
	}
}