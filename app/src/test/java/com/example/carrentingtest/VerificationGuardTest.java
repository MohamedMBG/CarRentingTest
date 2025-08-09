package com.example.carrentingtest;

import static org.junit.Assert.*;

import com.example.carrentingtest.verification.VerificationGuard;
import org.junit.Test;

public class VerificationGuardTest {
    @Test
    public void canBook_onlyWhenVerified() {
        assertFalse(VerificationGuard.canBook(null));
        assertFalse(VerificationGuard.canBook("UNVERIFIED"));
        assertFalse(VerificationGuard.canBook("PENDING"));
        assertTrue(VerificationGuard.canBook("VERIFIED"));
    }
}


