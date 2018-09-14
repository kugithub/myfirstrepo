package com.apple.wwrc.service.customer.model.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DSUserTest {

    @Test
    void testNullConstructor() {
        DSUser ghost = new DSUser();
        assertEquals(null, ghost.getBadgeID());
        assertEquals(null, ghost.getDsID());
        assertEquals(null, ghost.getEmail());
        assertEquals(null, ghost.getFirstName());
        assertEquals(null, ghost.getLastName());
        assertEquals(null, ghost.getFullName());
        assertEquals(null, ghost.getPhoto());
        assertEquals(0, ghost.getPtype());
    }
    @Test
    void testConstructor() {
        DSUser joe = new DSUser();
        joe.setBadgeID("111");
        joe.setDsID("123");
        joe.setEmail("joe@gmail.com");
        joe.setFirstName("Joe");
        joe.setLastName("Jonathan");
        joe.setFullName("Joe Jonathan");
        joe.setPhoto(new CustomerPhoto());
        joe.setPtype(0);
        
        assertEquals("111", joe.getBadgeID());
        assertEquals("123", joe.getDsID());
        assertEquals("joe@gmail.com", joe.getEmail());
        assertEquals("Joe", joe.getFirstName());
        assertEquals("Jonathan", joe.getLastName());
        assertEquals("Joe Jonathan", joe.getFullName());
        assertNotNull(joe.getPhoto());
        assertEquals(0, joe.getPtype());
    }
}
