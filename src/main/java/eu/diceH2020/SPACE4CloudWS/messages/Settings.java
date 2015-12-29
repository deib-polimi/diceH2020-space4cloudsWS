package eu.diceH2020.SPACE4CloudWS.messages;

public class Settings {

	public Settings(double accuracy, int cycles) {
		super();
		this.accuracy = accuracy;
		this.cycles = cycles;
	}
	private double accuracy;
	private int cycles;
	/**
	 * @return the accuracy
	 */
	public double getAccuracy() {
		return accuracy;
	}
	/**
	 * @param accuracy the accuracy to set
	 */
	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}
	/**
	 * @return the cycles
	 */
	public int getCycles() {
		return cycles;
	}
	/**
	 * @param cycles the cycles to set
	 */
	public void setCycles(int cycles) {
		this.cycles = cycles;
	}
	
	
}
