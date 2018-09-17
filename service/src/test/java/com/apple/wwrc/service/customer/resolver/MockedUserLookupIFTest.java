package com.apple.wwrc.service.customer.resolver;

import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.model.response.EmployeeSearch;
import com.apple.wwrc.service.customer.resolver.MockedUserLookup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MockedUserLookupIFTest {

    @Test
    public void testDSLookupReturnMockUser() throws Exception {
        AbstractUserLookup lookup = new MockedUserLookup();
        DSUser u1 = lookup.lookupFromEmail("1234", 0, 10);
        DSUser u2 = lookup.lookupFromBadge("1234", 1, 10);
        DSUser u3 = lookup.lookupFromBadge("1234", 3, 10);
        EmployeeSearch response1 = lookup.makeSearchResponse(0, 10, u1);
        EmployeeSearch response2 = lookup.makeSearchResponse(1, 10, u2);
        EmployeeSearch response3 = lookup.makeSearchResponse(3, 10, u3);
        assertTrue(response1.getEmployees().size() == 1);
        System.out.println(response2);
        System.out.println(response3);
    }
}