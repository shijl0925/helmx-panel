package com.helmx.tutorial.system.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResetPasswordRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setOldPassword("oldPass123");
        req.setNewPassword("newPass456");
        req.setConfirmPassword("newPass456");

        assertEquals("oldPass123", req.getOldPassword());
        assertEquals("newPass456", req.getNewPassword());
        assertEquals("newPass456", req.getConfirmPassword());
    }

    @Test
    void defaultConstructor_fieldsAreNull() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        assertNull(req.getOldPassword());
        assertNull(req.getNewPassword());
        assertNull(req.getConfirmPassword());
    }

    @Test
    void equals_sameFields_areEqual() {
        ResetPasswordRequest r1 = new ResetPasswordRequest();
        r1.setOldPassword("old");
        r1.setNewPassword("new");
        r1.setConfirmPassword("new");

        ResetPasswordRequest r2 = new ResetPasswordRequest();
        r2.setOldPassword("old");
        r2.setNewPassword("new");
        r2.setConfirmPassword("new");

        assertEquals(r1, r2);
    }

    @Test
    void equals_differentFields_notEqual() {
        ResetPasswordRequest r1 = new ResetPasswordRequest();
        r1.setNewPassword("aaa");

        ResetPasswordRequest r2 = new ResetPasswordRequest();
        r2.setNewPassword("bbb");

        assertNotEquals(r1, r2);
    }
}
