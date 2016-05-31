package it.polimi.diceH2020.SPACE4CloudWS.FineGrainedLogicForOptimization;


public class ChannelInfo{

	private ReactorConsumer consumer;
	private States state;
	
	public ChannelInfo(ReactorConsumer consumer){
		this.consumer = consumer;
		this.state = States.READY;
	}
	
	public States getState(){
		return state;
	}
	
	public void setState(States state){
		this.state = state;
	}
	
	public ReactorConsumer getConsumer(){
		return consumer;
	}
}

