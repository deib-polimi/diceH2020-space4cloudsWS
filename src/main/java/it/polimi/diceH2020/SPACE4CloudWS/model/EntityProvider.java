package it.polimi.diceH2020.SPACE4CloudWS.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Provider")
public class EntityProvider {

	@Id
	@Column(name = "pId")
	String name;

	public EntityProvider() {
		super();
		System.out.println("");
	}

	public EntityProvider(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	@Override
	public boolean equals(Object provider){
		EntityProvider myProvider = (EntityProvider) provider;
		if( this.getName().equals(myProvider.name)){
			return true;
		}
		return false;
	}

}
