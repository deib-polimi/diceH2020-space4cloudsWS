package it.polimi.diceH2020.SPACE4CloudWS.repositories;


import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeVMRepository extends JpaRepository<EntityTypeVM, EntityKey> {

	List<EntityTypeVM> findByProvider(EntityProvider provider);
}
