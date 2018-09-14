package com.apple.wwrc.service.customer.resolver;

import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.model.response.EmployeeSearch;
import java.util.ArrayList;
import java.util.Arrays;
@SuppressWarnings({"squid:S00103","common-java:InsufficientCommentDensity","squid:S1166","squid:S109", "squid:S00112"})
public abstract class AbstractUserLookup {
    public abstract DSUser lookupFromEmail(String query, int page, int limit) throws Exception;
    public abstract DSUser lookupFromBadge(String query, int page, int limit) throws Exception;

    public EmployeeSearch makeSearchResponse(int page, int limit, DSUser dsUser) {
        if (dsUser == null) {
            return new EmployeeSearch(page, limit, new ArrayList<DSUser>());
        } else {
            return new EmployeeSearch(page, limit, new ArrayList<DSUser>(Arrays.asList(dsUser)));
        }
    }
    public void tearDown() {}
}
