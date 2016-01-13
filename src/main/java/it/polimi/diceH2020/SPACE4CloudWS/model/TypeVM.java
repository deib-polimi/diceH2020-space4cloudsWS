package it.polimi.diceH2020.SPACE4CloudWS.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@IdClass(Key.class)
public class TypeVM {

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public TypeVM() {
	}

	@Id
	private String type;

	@Id
	@ManyToOne
	@JoinColumn(name = "pId")
	private Provider provider;

	private int core;

	public int getNumCores() {
		return core;
	}

	public void setCore(int core) {
		this.core = core;
	}

	private double deltabar;

	public TypeVM(String type) {
		super();
		this.type = type;
	}

	// cost reserved
	private double rhobar;

	// cost
	private double sigmabar;

	public double getDeltabar() {
		return deltabar;
	}

	public void setDeltabar(double deltabar) {
		this.deltabar = deltabar;
	}

	public double getRhoBar() {
		return rhobar;
	}

	public void setRhobar(double rhobar) {
		this.rhobar = rhobar;
	}

	public double getSigmaBar() {
		return sigmabar;
	}

	public void setSigmabar(double sigmabar) {
		this.sigmabar = sigmabar;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}