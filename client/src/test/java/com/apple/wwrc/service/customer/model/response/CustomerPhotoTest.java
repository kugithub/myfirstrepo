package com.apple.wwrc.service.customer.model.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CustomerPhotoTest {

    @Test
    void testNullConstructor() {
        CustomerPhoto empty = new CustomerPhoto();
        assertEquals("", empty.getHighResPhoto());
        assertEquals("", empty.getHighResPhotoType());
        assertEquals("", empty.getLowResPhoto());
        assertEquals("", empty.getLowResPhotoType());
    }
    @Test
    void testConstructor() {
        CustomerPhoto empty = new CustomerPhoto("lowResPhoto", "lowResPhotoType", "highResPhoto", "highResPhotoType");
        assertEquals("highResPhoto", empty.getHighResPhoto());
        assertEquals("highResPhotoType", empty.getHighResPhotoType());
        assertEquals("lowResPhoto", empty.getLowResPhoto());
        assertEquals("lowResPhotoType", empty.getLowResPhotoType());
    }

}
