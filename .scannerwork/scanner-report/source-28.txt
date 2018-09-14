package com.apple.wwrc.service.customer.model.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class EmployeeSearchTest {

    @Test
    void testNullConstructor() {
        EmployeeSearch result = new EmployeeSearch();
        assertEquals(null, result.getEmployees());
        assertEquals(0, result.getLimit());
        assertEquals(0, result.getPage());
    }
    @Test
    void testConstructor() {
        EmployeeSearch result = new EmployeeSearch();
        result.setPage(0);
        result.setLimit(1);
        result.setEmployees(new ArrayList<DSUser>());

        assertEquals(0, result.getEmployees().size());
        assertEquals(1, result.getLimit());
        assertEquals(0, result.getPage());
    }
}
