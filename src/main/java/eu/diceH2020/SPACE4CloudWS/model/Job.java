package eu.diceH2020.SPACE4CloudWS.model;

import javax.persistence.Entity;
import javax.persistence.Id;

//TODO This entity is unused. 
@Entity
public class Job {

	@Id
	private int idjob;

	/*
	 * @OneToMany(cascade = CascadeType.ALL, mappedBy = "job") private
	 * List<SpedFind> spedf;
	 */

	public int getIdjob() {
		return idjob;
	}

	public void setIdjob(int idjob) {
		this.idjob = idjob;
	}

	public Job() {
	}

	public Job(int idjob) {
		super();
		this.idjob = idjob;
	}

}