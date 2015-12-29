package eu.diceH2020.SPACE4CloudWS.jpaRepositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.diceH2020.SPACE4CloudWS.model.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {

}
