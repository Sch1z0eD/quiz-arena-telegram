package com.quizarena.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionOrderTest {

    @Test
    void storageAndDisplayRoundTripForEveryStorageIndex() {
        OptionOrder order = OptionOrder.parse("2,0,3,1");
        // order[d] = storage shown at display slot d
        assertEquals(2, order.storageAt(0));
        assertEquals(0, order.storageAt(1));
        assertEquals(3, order.storageAt(2));
        assertEquals(1, order.storageAt(3));
        // the correct storage option resolves to a display slot, and that slot maps back to it
        for (int storage = 0; storage < 4; storage++) {
            int slot = order.displayOfCorrect(storage);
            assertEquals(storage, order.storageAt(slot), "displayed slot of correct must map back to the correct storage index");
        }
    }

    @Test
    void identityIsTransparent() {
        OptionOrder identity = OptionOrder.identity();
        for (int i = 0; i < 4; i++) {
            assertEquals(i, identity.storageAt(i));
            assertEquals(i, identity.displayOfCorrect(i));
        }
        assertEquals("0,1,2,3", identity.toCsv());
    }

    @Test
    void csvRoundTrips() {
        assertEquals("3,1,2,0", OptionOrder.parse("3,1,2,0").toCsv());
    }

    @Test
    void malformedOrMissingCsvFallsBackToIdentity() {
        assertEquals("0,1,2,3", OptionOrder.parse(null).toCsv());
        assertEquals("0,1,2,3", OptionOrder.parse("").toCsv());
        assertEquals("0,1,2,3", OptionOrder.parse("1,2,3").toCsv(), "wrong length");
        assertEquals("0,1,2,3", OptionOrder.parse("0,1,1,2").toCsv(), "not a permutation");
        assertEquals("0,1,2,3", OptionOrder.parse("0,1,2,9").toCsv(), "out of range");
        assertEquals("0,1,2,3", OptionOrder.parse("a,b,c,d").toCsv(), "non-numeric");
    }

    @Test
    void randomProducesAValidPermutation() {
        OptionOrder order = OptionOrder.random(new Random(42));
        boolean[] seen = new boolean[4];
        for (int slot = 0; slot < 4; slot++) {
            int storage = order.storageAt(slot);
            seen[storage] = true;
        }
        for (boolean s : seen) {
            assertEquals(true, s, "every storage index must appear exactly once");
        }
    }
}
