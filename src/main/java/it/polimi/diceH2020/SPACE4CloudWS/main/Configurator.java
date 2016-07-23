package it.polimi.diceH2020.SPACE4CloudWS.main;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.cache.CacheBuilder;
import it.polimi.diceH2020.SPACE4Cloud.shared.generators.InstanceDataGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.generators.SolutionGenerator;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
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

//	@Autowired
//	public void configJackson(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
//		SimpleModule module = new SimpleModule();
//		module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer());
//		jackson2ObjectMapperBuilder.modules(module,new Jdk8Module());//.configureAbsentsAsNulls(true));
//		//jackson2ObjectMapperBuilder.configure(new ObjectMapper().registerModule(new Jdk8Module()));//.configureAbsentsAsNulls(true));
//	}
	
	@Bean
	public Module java8Module(){
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
	public SimpleModule customModule(){
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
	public CacheManager cacheManager() {
		GuavaCacheManager guavaCacheManager =  new GuavaCacheManager("cachedEval");
		guavaCacheManager.setCacheBuilder(CacheBuilder.newBuilder());
		return guavaCacheManager;
	}

}
