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
package it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization;

import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class WrapperDispatcher  extends QueueHandler<SpjWrapperGivenHandN> {

	public synchronized void dequeue(SpjOptimizerGivenH spjH){ //TODO this synch or the Q?both?
		for (Iterator<SpjWrapperGivenHandN> iterator = jobsQueue.iterator(); iterator.hasNext();) {
			SpjWrapperGivenHandN wrapper = iterator.next();
			if(wrapper.getHandler().equals(spjH)){
				iterator.remove();
			}
		}
	}

}
