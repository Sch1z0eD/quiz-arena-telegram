package com.quizarena.service;

import com.quizarena.domain.OptionOrder;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class OptionShuffler {

    private final Random random = new Random();

    public OptionOrder next() {
        return OptionOrder.random(random);
    }
}
