package com.example.carrentingtest.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UserLifecycleStatusTest {
    @Test
    public void from_treatsApprovedAsActive() {
        assertEquals(UserLifecycleStatus.ACTIVE, UserLifecycleStatus.from("approved", UserRole.ADMIN));
        assertEquals(UserLifecycleStatus.ACTIVE, UserLifecycleStatus.from("APPROVED", UserRole.ADMIN));
    }
}
