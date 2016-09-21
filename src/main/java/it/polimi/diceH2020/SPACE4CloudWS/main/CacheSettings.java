package it.polimi.diceH2020.SPACE4CloudWS.main;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix="cache")
public class CacheSettings {
		private Integer size = 1000;
		private Integer daysBeforeExpire = 5;
		private boolean recordStats = false;
}
