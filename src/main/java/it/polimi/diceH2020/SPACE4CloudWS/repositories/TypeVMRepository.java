package it.polimi.diceH2020.SPACE4CloudWS.repositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;

@Repository
public interface TypeVMRepository extends JpaRepository<EntityTypeVM, EntityKey> {

	public List<EntityTypeVM> findByProvider(EntityProvider provider); 
}
