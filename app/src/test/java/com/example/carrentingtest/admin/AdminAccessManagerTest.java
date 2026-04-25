package com.example.carrentingtest.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.carrentingtest.domain.CompanyLifecycleStatus;
import com.example.carrentingtest.domain.UserLifecycleStatus;

import org.junit.Test;

public class AdminAccessManagerTest {
    @Test
    public void isOperationalUserStatus_allowsPendingUserWhenCompanyApproved() {
        assertTrue(AdminAccessManager.isOperationalUserStatus(
                UserLifecycleStatus.PENDING_COMPANY_APPROVAL,
                CompanyLifecycleStatus.APPROVED));
        assertEquals(UserLifecycleStatus.ACTIVE, AdminAccessManager.effectiveUserStatus(
                UserLifecycleStatus.PENDING_COMPANY_APPROVAL,
                CompanyLifecycleStatus.APPROVED));
    }

    @Test
    public void isOperationalUserStatus_deniesSuspendedUserEvenWhenCompanyApproved() {
        assertFalse(AdminAccessManager.isOperationalUserStatus(
                UserLifecycleStatus.SUSPENDED,
                CompanyLifecycleStatus.APPROVED));
    }
}
