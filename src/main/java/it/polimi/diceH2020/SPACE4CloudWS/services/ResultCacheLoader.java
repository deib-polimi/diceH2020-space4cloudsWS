package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.google.common.cache.CacheLoader;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Created by ciavotta on 15/02/16.
 */
@Component
public class ResultCacheLoader extends CacheLoader<SolutionPerJob, Optional<BigDecimal>> {

    @Override
    public Optional<BigDecimal> load(SolutionPerJob key) throws Exception {
        return null;
    }
}
