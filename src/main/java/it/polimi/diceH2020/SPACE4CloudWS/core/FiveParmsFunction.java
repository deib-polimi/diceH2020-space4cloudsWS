package it.polimi.diceH2020.SPACE4CloudWS.core;

@FunctionalInterface
public interface FiveParmsFunction<One, Two, Three, Four, Five, Six> {
    public Six apply(One one, Two two, Three three, Four four, Five five);
}
