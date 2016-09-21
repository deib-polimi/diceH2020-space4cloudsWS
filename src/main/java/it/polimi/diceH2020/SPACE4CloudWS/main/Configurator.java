/*
Copyright 2016 Michele Ciavotta
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
package it.polimi.diceH2020.SPACE4CloudWS.main;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.cache.CacheBuilder;

import it.polimi.diceH2020.SPACE4Cloud.shared.generators.SolutionGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.generatorsDataMultiProvider.InstanceDataMultiProviderGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.*;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ComponentScan("it.polimi.diceH2020.SPACE4CloudWS.services")
@EnableCaching
public class Configurator {
	public final static String CACHE_NAME = "cachedEval";
	public final static String CACHEMANAGER_NAME = "guavaCM";
	public final static String SPJ_KEYGENERATOR = "spjKeyGenerator";
	
	@Autowired 
	private DS4CSettings settings;

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
	public InstanceDataMultiProvider applData() {
		return new InstanceDataMultiProvider();
	}

	//
	@Bean
	@Profile("test")
	public InstanceDataMultiProvider applDataTest() {
		return InstanceDataMultiProviderGenerator.build();
	}

	@Bean
	@Profile("test")
	public Solution solution() {
		return SolutionGenerator.build();
	}

	// @Autowired
	// public void configJackson(Jackson2ObjectMapperBuilder
	// jackson2ObjectMapperBuilder) {
	// SimpleModule module = new SimpleModule();
	// module.addKeyDeserializer(TypeVMJobClassKey.class,
	// TypeVMJobClassKey.getDeserializer());
	// jackson2ObjectMapperBuilder.modules(module,new
	// Jdk8Module());//.configureAbsentsAsNulls(true));
	// //jackson2ObjectMapperBuilder.configure(new
	// ObjectMapper().registerModule(new
	// Jdk8Module()));//.configureAbsentsAsNulls(true));
	// }

	@Bean
	public Module java8Module() {
		return new Jdk8Module();
	}

	@Primary
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		registerModules(mapper);
		return mapper;
	}

	@Bean
	public SimpleModule customModule() {
		SimpleModule module = new SimpleModule();
		module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer());
		return module;
	}

	@Bean
	public Jdk8Module jdk8Module() {
		return new Jdk8Module().configureAbsentsAsNulls(true);
	}

	private void registerModules(ObjectMapper mapper) {
		mapper.registerModule(jdk8Module());
		mapper.registerModule(customModule());
	}

	@Primary
	@Bean
	public ObjectWriter writer(ObjectMapper mapper) {
		return mapper.writer();
	}

	@Primary
	@Bean
	public ObjectReader reader(ObjectMapper mapper) {
		return mapper.reader();
	}

	@Bean
	public CacheBuilder<Object, Object> builder(){
		CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
		CacheSettings cache = settings.getCache();
		builder.maximumSize(cache.getSize()).expireAfterAccess(cache.getDaysBeforeExpire(), TimeUnit.DAYS);
		if(cache.isRecordStats()) builder.recordStats();
		return builder;
	}
	
	@Bean(name=CACHEMANAGER_NAME)
	public CacheManager cacheManager() {
		GuavaCacheManager cacheManager = new GuavaCacheManager();
	    cacheManager.setCacheBuilder(builder());
	    cacheManager.setCacheNames(Arrays.asList(CACHE_NAME));
	    return cacheManager;
	}
	
	@Bean(name=SPJ_KEYGENERATOR)
	 public KeyGenerator spjKeyGen() {
	    return new KeyGenerator() {
	      @Override
	      public Object generate(Object o, Method method, Object... objects) {
	        StringBuilder sb = new StringBuilder();
	        sb.append(o.getClass().getName());
	        sb.append(method.getName());
	        for (Object obj : objects) {
	          sb.append(obj.toString());
	        }
	        
	        return sb.toString();
	      }
	    };
	  }

}
