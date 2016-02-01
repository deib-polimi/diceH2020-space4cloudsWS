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
