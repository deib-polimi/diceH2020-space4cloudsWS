package eu.diceH2020.SPACE4CloudWS.messages;

import java.util.Arrays;
import java.util.List;

import eu.diceH2020.SPACE4CloudWS.model.TypeVM;

public class InstanceData {

	private int[][] cM;
	private int[][] cR;
	private double[] D;
	private int gamma;
	private int[] HLow;
	private int[] HUp;
	private List<Integer> id_job;
	private double[] job_penalty;
	private double[] Mavg;
	private double[] Mmax;
	private double[] N;
	private int[] NM;
	private int[] NR;
	private String provider;
	private double[] R;
	private double[] Ravg;
	private double[] Rmax;

	private double[] SH1max;
	private double[] SHtypavg;
	private double[] SHtypmax;
	private double[] think;
//	private TypeVM tm;
	private List<String> typeVm;

	
	public InstanceData() {
	}

	public InstanceData(int gamma, List<String> typeVm, String provider, List<Integer> id_job, double[] think, int[][] cM, int[][] cR,
			double[] n, int[] hUp, int[] hLow, int[] nM, int[] nR, double[] mmax, double[] rmax, double[] mavg,
			double[] ravg, double[] d, double[] sH1max, double[] sHtypmax, double[] sHtypavg, double[] job_penalty,
			double[] r) {
		super();

		this.gamma = gamma;
		this.typeVm = typeVm;
		this.provider = provider;
		this.id_job = id_job;
		this.think = think;
		this.cM = cM;
		this.cR = cR;
		N = n;
		HUp = hUp;
		HLow = hLow;
		NM = nM;
		NR = nR;
		Mmax = mmax;
		Rmax = rmax;
		Mavg = mavg;
		Ravg = ravg;
		D = d;
		SH1max = sH1max;
		SHtypmax = sHtypmax;
		SHtypavg = sHtypavg;
		this.job_penalty = job_penalty;
		R = r;
	}

	
	public void clear(){
		//TODO implement cleansing method here
	}
	
	public int[][] getcM() {
		return cM;
	}
	public int getcM(int i, int j) {
		return cM[i][j];
	}

	public int[][] getcR() {
		return cR;
	}
	public int getcR(int i, int j) {
		return cR[i][j];
	}

	public double[] getD() {
		return D;
	}

	public double getD(int pos) {
		return D[pos];
	}
	
	public int getGamma() {
		return gamma;
	}

	public int[] getHLow() {
		return HLow;
	}

	public int getHLow(int pos) {
		return HLow[pos];
	}
	
	public int[] getHUp() {
		return HUp;
	}
	public int getHUp(int pos) {
		return HUp[pos];
	}
	
	public List<Integer> getId_job() {
		return id_job;
	}
	
	public int getNumberJobs(){
		return id_job.size();
	}
	
	public int getId_job(int pos) {
		return id_job.get(pos);
	}

	public double[] getJob_penalty() {
		return job_penalty;
	}
	public double getJob_penalty(int pos) {
		return job_penalty[pos];
	}

	public double[] getMavg() {
		return Mavg;
	}

	public double getMavg(int pos) {
		return Mavg[pos];
	}
	
	public double[] getMmax() {
		return Mmax;
	}
	
	public double getMmax(int pos) {
		return Mmax[pos];
	}
	
	public double[] getN() {
		return N;
	}

	public double getN(int pos) {
		return N[pos];
	}
	
	public int[] getNM() {
		return NM;
	}

	public int getNM(int pos) {
		return NM[pos];
	}
	
	public int[] getNR() {
		return NR;
	}

	public int getNR(int pos) {
		return NR[pos];
	}


	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public double[] getR() {
		return R;
	}

	public double getR(int pos) {
		return R[pos];
	}
	
	public double[] getRavg() {
		return Ravg;
	}

	public double getRavg(int pos) {
		return Ravg[pos];
	}
	
	public double[] getRmax() {
		return Rmax;
	}

	public double getRmax(int pos) {
		return Rmax[pos];
	}
	
	public double[] getSH1max() {
		return SH1max;
	}

	public double getSH1max(int pos) {
		return SH1max[pos];
	}
	
	public double[] getSHtypavg() {
		return SHtypavg;
	}

	public double getSHtypavg(int pos) {
		return SHtypavg[pos];
	}
	
	public double[] getSHtypmax() {
		return SHtypmax;
	}
	
	public double getSHtypmax(int pos) {
		return SHtypmax[pos];
	}

	public double[] getThink() {
		return think;
	}

	public double getThink(int pos) {
		return think[pos];
	}

	public List<String> getTypeVm() {
		return typeVm;
	}

	public String getTypeVm(int pos) {
		return typeVm.get(pos);
	}
	
	public void setcM(int[][] cM) {
		this.cM = cM;
	}

	public void setcR(int[][] cR) {
		this.cR = cR;
	}

	public void setD(double[] d) {
		D = d;
	}

	public void setGamma(int gamma) {
		this.gamma = gamma;
	}

	public void setHLow(int[] hLow) {
		HLow = hLow;
	}
	public void setHLow(int pos, int i) {
		HLow[pos] = i;
	}

	public void setHUp(int[] hUp) {
		HUp = hUp;
	}
	public void setHUp(int pos, int i) {
		HUp[pos] = i;
	}
	
	public void incHUp(int pos, int incValue){
		HUp[pos] = HUp[pos]+incValue;
	}

	public void setId_job(List<Integer> id_job) {
		this.id_job = id_job;
	}

	public void setJob_penalty(double[] job_penalty) {
		this.job_penalty = job_penalty;
	}

	public void setMavg(double[] mavg) {
		Mavg = mavg;
	}

	public void setMmax(double[] mmax) {
		Mmax = mmax;
	}

	public void setN(double[] n) {
		N = n;
	}

	public void setNM(int[] nM) {
		NM = nM;
	}

	public void setNR(int[] nR) {
		NR = nR;
	}


	public void setR(double[] r) {
		R = r;
	}

	public void setRavg(double[] ravg) {
		Ravg = ravg;
	}

	public void setRmax(double[] rmax) {
		Rmax = rmax;
	}

	public void setSH1max(double[] sH1max) {
		SH1max = sH1max;
	}

	public void setSHtypavg(double[] sHtypavg) {
		SHtypavg = sHtypavg;
	}

	public void setSHtypmax(double[] sHtypmax) {
		SHtypmax = sHtypmax;
	}

	public void setThink(double[] think) {
		this.think = think;
	}

	public void setTypeVm(List<String> typeVm) {
		this.typeVm = typeVm;
	}

	@Override
	public String toString() {
		return "App [gamma=" + gamma + ", typeVm=" + typeVm.toString() + ", provider=" + provider + ", id_job="
				+ id_job.toString() + ", think=" + Arrays.toString(think) + ", cM=" + Arrays.toString(cM)
				+ ", cR=" + Arrays.toString(cR) + ", N=" + Arrays.toString(N) + ", HUp=" + Arrays.toString(HUp)
				+ ", HLow=" + Arrays.toString(HLow) + ", NM=" + Arrays.toString(NM) + ", NR=" + Arrays.toString(NR)
				+ ", Mmax=" + Arrays.toString(Mmax) + ", Rmax=" + Arrays.toString(Rmax) + ", Mavg="
				+ Arrays.toString(Mavg) + ", Ravg=" + Arrays.toString(Ravg) + ", D=" + Arrays.toString(D) + ", SH1max="
				+ Arrays.toString(SH1max) + ", SHtypmax=" + Arrays.toString(SHtypmax) + ", SHtypavg="
				+ Arrays.toString(SHtypavg) + ", job_penalty=" + Arrays.toString(job_penalty) + ", R="
				+ Arrays.toString(R) + "]";
	}

	public int getNumberTypeVM() {
		return this.typeVm.size();
	}

}