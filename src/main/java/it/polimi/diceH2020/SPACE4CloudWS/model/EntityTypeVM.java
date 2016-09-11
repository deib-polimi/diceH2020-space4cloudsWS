/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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