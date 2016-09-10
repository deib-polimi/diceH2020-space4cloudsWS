package it.polimi.diceH2020.SPACE4CloudWS.model;

import javax.persistence.*;

@Entity
@IdClass(EntityKey.class)
@Table(name = "TYPEVM")
public class EntityTypeVM {

	@Id
	private String type;
	@Id
	@ManyToOne
	@JoinColumn(name = "pId")
	private EntityProvider provider;
	private double core;
	private double memory;
	private double deltabar;
	// cost reserved
	private double rhobar;
	// cost
	private double sigmabar;

	public EntityTypeVM() {
	}

	public EntityTypeVM(String type) {
		super();
		this.type = type;
	}

	public EntityProvider getProvider() {
		return provider;
	}

	public void setProvider(EntityProvider provider) {
		this.provider = provider;
	}

	public double getNumCores() {
		return core;
	}

	public void setCore(double core) {
		this.core = core;
	}
	
	public double getMemory() {
		return memory;
	}

	public void setMemory(double memory) {
		this.memory = memory;
	}

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