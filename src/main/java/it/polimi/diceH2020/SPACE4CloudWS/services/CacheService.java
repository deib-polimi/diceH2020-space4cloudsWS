package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Created by ciavotta on 15/02/16.
 */
@Service
public class CacheService {

    LoadingCache cache;

    private ResultCacheLoader loader;

    @Autowired
    public void CacheService(ResultCacheLoader loader) {
        this.loader = loader;
        cache = CacheBuilder.newBuilder().
                maximumSize(100).
                build(loader);
    }

    public Optional<BigDecimal> get(SolutionPerJob solutionPerJob) throws ExecutionException {
        return (Optional<BigDecimal>) cache.get(solutionPerJob);
    }


}
