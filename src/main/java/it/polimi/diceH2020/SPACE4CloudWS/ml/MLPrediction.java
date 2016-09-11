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
