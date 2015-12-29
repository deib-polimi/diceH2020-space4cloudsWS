package eu.diceH2020.SPACE4CloudWS.jpaRepositories;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.diceH2020.SPACE4CloudWS.model.Key;
import eu.diceH2020.SPACE4CloudWS.model.Provider;
import eu.diceH2020.SPACE4CloudWS.model.TypeVM;

@Repository
public interface TypeVMRepository extends JpaRepository<TypeVM, Key> {

	public List<TypeVM> findByProvider(Provider provider); 
}
