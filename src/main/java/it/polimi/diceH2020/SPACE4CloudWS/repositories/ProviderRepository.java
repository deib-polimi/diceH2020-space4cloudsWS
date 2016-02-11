package it.polimi.diceH2020.SPACE4CloudWS.repositories;

import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<EntityProvider, String> {

}
