package it.polimi.diceH2020.SPACE4CloudWS.main;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.cache.CacheBuilder;

import it.polimi.diceH2020.SPACE4Cloud.shared.generators.InstanceDataGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.generators.SolutionGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ComponentScan("it.polimi.diceH2020.SPACE4CloudWS.services")
@EnableCaching
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
		return SolutionGenerator.build();
	}

	@Autowired(required = true)
	public void configeJackson(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
		SimpleModule module = new SimpleModule();
		module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer());
		jackson2ObjectMapperBuilder.modules(module);
	}
	
	@Bean
	public CacheManager cacheManager() {
	 GuavaCacheManager guavaCacheManager =  new GuavaCacheManager("cachedEval");
	 guavaCacheManager.setCacheBuilder(CacheBuilder.newBuilder());
	 return guavaCacheManager;
	}
	

}
