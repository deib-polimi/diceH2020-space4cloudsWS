package it.polimi.diceH2020.SPACE4CloudWS.model;

import javax.persistence.Entity;
import javax.persistence.Id;

//TODO This entity is unused. 
@Entity
public class Job {

	@Id
	private int idJob;

	/*
	 * @OneToMany(cascade = CascadeType.ALL, mappedBy = "job") private
	 * List<SpedFind> spedf;
	 */

	public int getIdJob() {
		return idJob;
	}

	public void setIdJob(int idJob) {
		this.idJob = idJob;
	}

	public Job() {
	}

	public Job(int idJob) {
		super();
		this.idJob = idJob;
	}

}
