package eu.diceH2020.SPACE4CloudWS.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Provider {

	@Id
	@Column(name = "pId")
	String name;

	public Provider() {
		super();
		System.out.println("");
	}

	public Provider(String name) {
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
		Provider myProvider = (Provider) provider;
		if( this.getName().equals(myProvider.name)){
			return true;
		}
		return false;
	}

}
