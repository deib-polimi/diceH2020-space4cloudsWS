package it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization;

import java.util.Iterator;

import org.springframework.stereotype.Service;

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
