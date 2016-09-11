/*
Copyright 2016 Michele Ciavotta

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

import java.io.Serializable;

public class EntityKey implements Serializable {

	private static final long serialVersionUID = -5386973188931712971L;
	private EntityProvider provider;

	private String type;

	public EntityKey(){

	}

	public EntityKey(String type, EntityProvider provider) {
		super();
		this.setType(type);
		this.setProvider(provider);
	}

	public EntityKey(String type, String name) {
		super();
		this.setType(type);
		this.setProvider(new EntityProvider(name));
	}

	/**
	 * @return the provider
	 */
	public EntityProvider getProvider() {
		return provider;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param provider the provider to set
	 */
	public void setProvider(EntityProvider provider) {
		this.provider = provider;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj instanceof EntityKey) {
			EntityKey s = (EntityKey)obj;
			return type.equals(s.type) && provider.equals(s.provider);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (type + provider.getName()).hashCode();
	}

}
