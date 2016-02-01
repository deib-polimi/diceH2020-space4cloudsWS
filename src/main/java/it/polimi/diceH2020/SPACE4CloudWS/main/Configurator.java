package it.polimi.diceH2020.SPACE4CloudWS.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceDataGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.SolutionPerJob;
import it.polimi.diceH2020.SPACE4Cloud.shared.TypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

@Configuration
public class Configurator {

	@Value("${pool.size:10}")
	private int poolSize;

	@Value("${queue.capacity:2}")
	private int queueCapacity;

	@Bean(name = "workExecutor")
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize(poolSize);
		taskExecutor.setQueueCapacity(queueCapacity);
		taskExecutor.afterPropertiesSet();
		return taskExecutor;
	}

	@Bean
	public States state() {
		return States.IDLE;
	}

	@Bean
	public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Bean
	@Profile("dev")
	public InstanceData applData() {
		return new InstanceData();
	}
//
	@Bean
	@Profile("test")
	public InstanceData applDataTest() {
		return InstanceDataGenerator.build();
	}

	@Bean
	@Profile("test")
	public Solution solution() {
		Solution sol = new Solution();
		SolutionPerJob sol1 = new SolutionPerJob();
		TypeVM t1 = new TypeVM();
		t1.setId("T2");
		t1.setEta(0.3);
		t1.setR(25);
		sol1.setTypeVMselected(t1);
		JobClass job1 = new JobClass();
		job1.setD(150.0);
		sol1.setJob(job1);
		
		
		sol1.setAlfa(1250.0);
		sol1.setBeta(125.0);
		sol1.setChanged(false);
		sol1.setCost(13.3);
		sol1.setDeltaBar(1.4);
		sol1.setFeasible(false);
		sol1.setNumberContainers(5);
		sol1.setNumberUsers(10);
		sol1.setNumberVM(15);
		sol1.setNumCores(3);
		sol1.setNumOnDemandVM(0);
		sol1.setNumReservedVM(11);
		sol1.setNumSpotVM(4);
		sol1.setPos(0);
		
		sol1.setRhoBar(1.1);
		sol1.setSigmaBar(0.3);
		sol1.setDuration(180.0);

		sol.getLstSolutions().add(sol1);

		SolutionPerJob sol2 = new SolutionPerJob();
        JobClass job2 = new JobClass();
        job2.setD(150.0);
        sol2.setJob(job2);
		sol2.setTypeVMselected(t1);
		sol2.setAlfa(749.5);
        sol2.setBeta(74.95);
        sol2.setChanged(false);
        sol2.setCost(35.0);
        sol2.setDeltaBar(1.4);
        sol2.setFeasible(false);
        sol2.setNumberContainers(13);
        sol2.setNumberUsers(10);
        sol2.setNumberVM(39);
        sol2.setNumCores(3);
        sol2.setNumOnDemandVM(3);
        sol2.setNumReservedVM(25);
        sol2.setNumSpotVM(11);
        sol2.setPos(1);
        sol2.setRhoBar(1.1);
        sol2.setSigmaBar(0.3);
        sol2.setDuration(150.0);

        sol.getLstSolutions().add(sol2);
        
		return sol;
	}

}
