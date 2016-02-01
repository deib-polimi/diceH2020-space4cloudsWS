package it.polimi.diceH2020.SPACE4CloudWS.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "JOBCLASS")
public class EntityJobClass {

	@Id
	private int idJob;

	public int getIdJob() {
		return idJob;
	}

	public void setIdJob(int idJob) {
		this.idJob = idJob;
	}

	public EntityJobClass() {
	}

	public EntityJobClass(int idJob) {
		super();
		this.idJob = idJob;
	}

}
