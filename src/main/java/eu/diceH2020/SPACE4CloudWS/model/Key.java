package eu.diceH2020.SPACE4CloudWS.model;

import java.io.Serializable;

public class Key implements Serializable {

	private static final long serialVersionUID = -5386973188931712971L;
	private Provider provider;

	private String type;

	public Key(){
		
	}
	
	public Key(String type, Provider provider) {
		super();
		this.setType(type);
		this.setProvider(provider);
	}

	public Key(String type, String name) {
		super();
		this.setType(type);
		this.setProvider(new Provider(name));
	}

	/**
	 * @return the provider
	 */
	public Provider getProvider() {
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
	public void setProvider(Provider provider) {
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
        if(obj != null && obj instanceof Key) {
            Key s = (Key)obj;
            return type.equals(s.type) && provider.equals(s.provider);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (type + provider.getName()).hashCode();
    }
	
	
}
