package com.quizarena.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Per-round permutation of the four answer options. {@code order[d]} is the storage index of the option
 * shown in display slot {@code d}. Captured at the start of a round and persisted in Redis so reveal and
 * scoring stay consistent even across a state restart. Parsing falls back to identity for missing or
 * malformed values so rounds started before this was introduced score in storage order, as they were shown.
 */
public final class OptionOrder {

    private static final int SIZE = 4;

    private final int[] order;

    private OptionOrder(int[] order) {
        this.order = order;
    }

    public static OptionOrder identity() {
        return new OptionOrder(new int[]{0, 1, 2, 3});
    }

    public static OptionOrder random(Random random) {
        List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(slots, random);
        int[] order = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            order[i] = slots.get(i);
        }
        return new OptionOrder(order);
    }

    public static OptionOrder parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return identity();
        }
        String[] parts = csv.split(",");
        if (parts.length != SIZE) {
            return identity();
        }
        int[] order = new int[SIZE];
        boolean[] seen = new boolean[SIZE];
        try {
            for (int i = 0; i < SIZE; i++) {
                int value = Integer.parseInt(parts[i].trim());
                if (value < 0 || value >= SIZE || seen[value]) {
                    return identity();
                }
                seen[value] = true;
                order[i] = value;
            }
        } catch (NumberFormatException e) {
            return identity();
        }
        return new OptionOrder(order);
    }

    public String toCsv() {
        return order[0] + "," + order[1] + "," + order[2] + "," + order[3];
    }

    public int storageAt(int displaySlot) {
        if (displaySlot < 0 || displaySlot >= SIZE) {
            return -1;
        }
        return order[displaySlot];
    }

    public int displayOfCorrect(int storageCorrect) {
        for (int slot = 0; slot < SIZE; slot++) {
            if (order[slot] == storageCorrect) {
                return slot;
            }
        }
        return -1;
    }
}
