package it.polimi.diceH2020.SPACE4CloudWS.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.SPACE4CloudWS.model.EntityJobClass;

@Repository
public interface JobRepository extends JpaRepository<EntityJobClass, Integer> {

}
