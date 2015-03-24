package com.stevezeidner.movementgauge;

import com.stevezeidner.movementgauge.core.Utility;

import junit.framework.TestCase;


public class UtilityTest extends TestCase {

    public void testRoundingANumber() {
        float lotsOfDecimals = 5.6789238234923f;
        float expected = 5.679f;
        float actual = Utility.round(lotsOfDecimals, 3);
        assertEquals(expected, actual);
    }

    public void testRandomNumFallsBetweenLimits() {
        int numIterations = 30;
        int i = 0;
        int low = 3;
        int high = 56;
        while (i < numIterations) {
            int rand = Utility.randomBetween(low, high);
            assertTrue(rand >= low && rand <= high);
            i++;
        }
    }

    public void testTotalAcceleration() {
        float x = 1.5f;
        float y = 3.7f;
        float z = 6.2f;
        float expected = 7.37427962583464826398574235482869871113557793289521f;
        assertEquals(expected, Utility.totalAcceleration(x, y, z));
    }
}
