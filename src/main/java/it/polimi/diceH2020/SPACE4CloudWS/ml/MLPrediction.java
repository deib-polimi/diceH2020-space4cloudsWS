package it.polimi.diceH2020.SPACE4CloudWS.ml;

import lombok.Data;

@Data
public class MLPrediction {
	double deadline;
	double chi_c;
	double chi_h;
	double chi_0;
	//M,V depends on the selected VM that depends on h, xi cannot be cached
	
	public MLPrediction(double deadline,double chi_c,double chi_h,double chi_0){
		this.deadline = deadline;
		this.chi_c = chi_c;
		this.chi_h = chi_h;
		this.chi_0 = chi_0;
	}
}
