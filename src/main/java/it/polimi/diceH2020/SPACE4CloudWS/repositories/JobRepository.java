package it.polimi.diceH2020.SPACE4CloudWS.repositories;

import it.polimi.diceH2020.SPACE4CloudWS.model.EntityJobClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<EntityJobClass, Integer> {

}
