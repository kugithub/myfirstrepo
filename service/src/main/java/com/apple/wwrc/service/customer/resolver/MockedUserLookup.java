package com.apple.wwrc.service.customer.resolver;

import com.apple.ist.ds2.pub.common.PersonTypeI;
import com.apple.wwrc.service.customer.model.response.DSUser;
import com.apple.wwrc.service.customer.model.response.CustomerPhoto;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;

public class MockedUserLookup extends AbstractUserLookup {

    //Read mocked image
    private static String mockedPic;

    public MockedUserLookup() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("mockedImage.txt");
            mockedPic = IOUtils.toString(is, Charsets.UTF_8.toString());
        } catch (IOException e) {
            mockedPic = e.toString();
        }
    }

    @Override
    public DSUser lookupFromBadge(String query, int page, int limit) throws Exception {
        return lookupFromEmail(query, page, limit);
    }

    @Override
    public DSUser lookupFromEmail(String query, int page, int limit) throws Exception {
        DSUser mockedUser;
        if (page == 0) {
            mockedUser = new DSUser("123456", "John", "Doe", "john_doe@acme.com", "654321", PersonTypeI.EMPLOYEE);
            CustomerPhoto p = new CustomerPhoto(mockedPic,"jpg","","");
            mockedUser.setPhoto(p);
        } else if (page == 1) {
            mockedUser = new DSUser("123456", "John", "Doe", "john_doe@acme.com", "654321", PersonTypeI.EMPLOYEE);
        } else {
            mockedUser = null;
        }
        return mockedUser;
    }
}
