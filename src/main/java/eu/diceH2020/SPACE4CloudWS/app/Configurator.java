package eu.diceH2020.SPACE4CloudWS.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import eu.diceH2020.SPACE4CloudWS.stateMachine.States;
import eu.diceH2020.SPACE4Cloud_messages.InstanceData;

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

	@Bean
	@Profile("test")
	public InstanceData applDataTest() {
		int gamma = 1;
		List<String> typeVm = Arrays.asList( "T1", "T2" );
		String provider = "Amazon";
		List<Integer> id_job = Arrays.asList( 10, 11 ); // numJobs = 2
		double[] think = { 0.5, 0.10 }; // check
		int[][] cM = { { 3, 4 }, { 1, 2 } };
		int[][] cR = { { 1, 2 }, { 3, 4 } };
		double[] n = { 0.1, 0.5 };
		int[] hUp = { 10, 10 };
		int[] hLow = { 5, 5 };
		int[] nM = { 2, 2 };
		int[] nR = { 1, 1 };
		double[] mmax = { 1.5, 2.1 };
		double[] rmax = { 1.2, 3.2 };
		double[] mavg = { 3.1, 0.1 };
		double[] ravg = { 2.1, 0.2 };
		double[] d = { 0.8, 1.2 };
		double[] sH1max = { 1.1, 0.9 };
		double[] sHtypmax = { 0.5, 2.1 };
		double[] sHtypavg = { 0.7, 0.6 };
		double[] job_penalty = { 0.2, 2.1 };
		double[] r = { 2.2, 1.1 };
		return new InstanceData(gamma, typeVm, provider, id_job, think, cM, cR, n, hUp, hLow, nM, nR, mmax, rmax, mavg,
				ravg, d, sH1max, sHtypmax, sHtypavg, job_penalty, r);
	}

	
	
	
}
