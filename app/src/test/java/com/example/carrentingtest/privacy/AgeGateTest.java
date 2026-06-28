package com.example.carrentingtest.privacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

public class AgeGateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 28);

    @Test
    public void parseIsoDob_validIso_returnsDate() {
        LocalDate result = AgeGate.parseIsoDob("1990-05-12");
        assertEquals(LocalDate.of(1990, 5, 12), result);
    }

    @Test
    public void parseIsoDob_invalidString_returnsNull() {
        assertNull(AgeGate.parseIsoDob("12/05/1990"));
        assertNull(AgeGate.parseIsoDob("not-a-date"));
        assertNull(AgeGate.parseIsoDob(""));
        assertNull(AgeGate.parseIsoDob(null));
    }

    @Test
    public void isAtLeastMinimumAge_exactlyEighteen_today_isAllowed() {
        LocalDate dob = TODAY.minusYears(18);
        assertTrue(AgeGate.isAtLeastMinimumAge(dob, TODAY));
    }

    @Test
    public void isAtLeastMinimumAge_dayBeforeEighteenth_blocked() {
        LocalDate dob = TODAY.minusYears(18).plusDays(1);
        assertFalse(AgeGate.isAtLeastMinimumAge(dob, TODAY));
    }

    @Test
    public void isAtLeastMinimumAge_well_overEighteen_isAllowed() {
        LocalDate dob = LocalDate.of(1980, 1, 1);
        assertTrue(AgeGate.isAtLeastMinimumAge(dob, TODAY));
    }

    @Test
    public void isAtLeastMinimumAge_futureDob_blocked() {
        LocalDate dob = TODAY.plusDays(1);
        assertFalse(AgeGate.isAtLeastMinimumAge(dob, TODAY));
    }
}
