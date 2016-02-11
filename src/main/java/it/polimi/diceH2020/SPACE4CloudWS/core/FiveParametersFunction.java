package it.polimi.diceH2020.SPACE4CloudWS.core;

@FunctionalInterface
public interface FiveParametersFunction<One, Two, Three, Four, Five, Six> {
    Six apply(One one, Two two, Three three, Four four, Five five);
}
