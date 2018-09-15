package com.apple.wwrc.service.customer;

import com.apple.ist.retail.pos.encrypt.util.EncodeDecodeUtil;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.service.customer.exception.InvalidInputException;
import com.apple.wwrc.service.customer.model.response.EmployeeSearch;
import com.apple.wwrc.service.customer.resolver.DirectoryServiceResolver;
import com.apple.wwrc.service.customer.resolver.JamaicaProxy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
class CustomerServiceTest {

    private static CustomerService service;
    @BeforeAll
    public static void setupBeforeTest() throws Exception {
        System.setProperty("com.apple.wwrc.db.jdbcUrl","jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=falkar.corp.apple.com)(PORT=1871))(CONNECT_DATA=(SERVICE_NAME=nexus2d)))");
        System.setProperty("com.apple.wwrc.db.user", "nexus_pod_user");
        System.setProperty("com.apple.wwrc.db.password",  EncodeDecodeUtil.encode("podnnexususerpassword"));
        System.setProperty("com.apple.wwrc.db.type", "ORACLE");
        System.setProperty("wwrc.pos.ssl.config", "src/test/resources/test_ssl_properties");
        Properties env = System.getProperties();
        env.setProperty("bootStrapIp", "ws://localhost:9000/rcm");
        env.setProperty("id_groups", "POS");
        env.setProperty("RPC", "false");//Got to be false to avoid loading fake keystore --> fake keystore will fail the Jamaica-Security

        //Bypass dependency to Jamaica-Security in Rio Test
        JamaicaProxy mockedProxy = PowerMockito.mock(JamaicaProxy.class);
        PowerMockito.doReturn(-1).when(mockedProxy).getEncryptionType(Matchers.anyString());
        PowerMockito.doReturn("i6v9unmmmddx2i9r").when(mockedProxy).decryptText(Matchers.anyString(),Matchers.eq(-1));
        DirectoryServiceResolver mockedResolver = PowerMockito.spy(new DirectoryServiceResolver());
        PowerMockito.doReturn(mockedProxy).when(mockedResolver).jamaicaProxy();
        service = PowerMockito.spy(new CustomerService());
        PowerMockito.doReturn(mockedResolver).when(service).createLookupResolver();
        service.onStart();
    }

    @AfterAll
    public static void tearDownAfterTest() throws Exception {
        service.onStop();
    }

    @Test
    void testEmailLookup() throws Exception {
        EmployeeSearch user_1 = service.employeeLookup("npoolsappasit@apple.com", 0, 10);
        assertNotNull(user_1);
        assertEquals(10, user_1.getLimit());
        assertEquals(0, user_1.getPage());
        assertEquals(1, user_1.getEmployees().size());
        assertEquals("973645158", user_1.getEmployees().get(0).getDsID());
        System.out.println(user_1);
        
        EmployeeSearch user_2 = service.employeeLookup("willis_lee@apple.com", 0, 10);
        assertNotNull(user_2);
        assertEquals(10, user_2.getLimit());
        assertEquals(0, user_2.getPage());
        assertEquals(1, user_2.getEmployees().size());
        assertEquals("269574376", user_2.getEmployees().get(0).getDsID());
        System.out.println(user_2);
        
        EmployeeSearch user_3 = service.employeeLookup("bacon@apple.com", 0, 10);
        assertNotNull(user_3);
        assertEquals(10, user_3.getLimit());
        assertEquals(0, user_3.getPage());
        assertEquals(1, user_3.getEmployees().size());
        assertEquals("206419612", user_3.getEmployees().get(0).getDsID());
        System.out.println(user_3);
    }

    @Test
    void testBadgeLookup() throws Exception {
        EmployeeSearch user_1 = service.employeeLookup("532800", 0, 10);
        assertNotNull(user_1);
        assertEquals(10, user_1.getLimit());
        assertEquals(0, user_1.getPage());
        assertEquals(1, user_1.getEmployees().size());
        assertEquals("973645158", user_1.getEmployees().get(0).getDsID());
        System.out.println(user_1);
    }
    
    @Test
    void testValidateEmailLookup() throws Exception {
        service.validateEmailLookup("valid@email.com");
        service.validateEmailLookup("valid@apc.com.au");
        assertThrows(InvalidInputException.class, () -> service.validateEmailLookup("123456"));
        assertThrows(InvalidInputException.class, () -> service.validateEmailLookup("invalid@google@com"));
        assertThrows(InvalidInputException.class, () -> service.validateEmailLookup("invalid @ google.com"));
    }
    @Test
    void validateEmployeeLookupInput() throws Exception {
        service.validateEmployeeLookupInput("valid@email.com", 0, 10);
        service.validateEmployeeLookupInput("1234567890", 0, 10);
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("valid@email.com", -1, 10));
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("valid@email.com", 0, 0));
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("valid@email.com", -1, 10));
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("valid@email.com", 1, -10));
    }

    @Test
    void testHandleInvalidInput() throws Exception {
        //Detection
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("", 0, 1));
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("123456", -1, 10));
        assertThrows(InvalidInputException.class, () -> service.validateEmployeeLookupInput("123456", 1, -10));
        
        //Handle as NotFound
        assertEquals(0, service.employeeLookup("", 0, 1).getEmployees().size());
        assertEquals(0, service.employeeLookup("123456", -1, 10).getEmployees().size());
        assertEquals(0, service.employeeLookup("abc@apple.com", 1, -10).getEmployees().size());
    }

    @Test
    void testThrowExceptionOnError() throws Exception {
        //Given
        DirectoryServiceResolver mocked_ = PowerMockito.spy(new DirectoryServiceResolver());
        PowerMockito.doThrow(new Exception("Internal Error: It is a Test!")).when(mocked_).findPerson(Matchers.any(), Matchers.any());

        CustomerService serviceUnderTest = PowerMockito.spy(new CustomerService());
        PowerMockito.doReturn(mocked_).when(serviceUnderTest).createLookupResolver();
        serviceUnderTest.onStart();

        //When
        assertThrows(FrameworkException.class, () -> serviceUnderTest.employeeLookup("invalid@email.com", 0, 10));
    }
    
    @Test
    void testHandleNotFound() throws Exception {
        //Given
        JamaicaProxy mockedProxy = PowerMockito.mock(JamaicaProxy.class);
        PowerMockito.doReturn(-1).when(mockedProxy).getEncryptionType(Matchers.anyString());
        PowerMockito.doReturn("i6v9unmmmddx2i9r").when(mockedProxy).decryptText(Matchers.anyString(),Matchers.eq(-1));
        
        DirectoryServiceResolver mockedResolver = Mockito.spy(new DirectoryServiceResolver());
        PowerMockito.doReturn(mockedProxy).when(mockedResolver).jamaicaProxy();
        Mockito.when(mockedResolver.findPerson(Matchers.any(), Matchers.any())).thenReturn(new Long(-1));

        CustomerService serviceUnderTest = new CustomerService();
        serviceUnderTest = Mockito.spy(serviceUnderTest);
        Mockito.when(serviceUnderTest.createLookupResolver()).thenReturn(mockedResolver);
        serviceUnderTest.onStart();

        //When
        try {
            EmployeeSearch response = serviceUnderTest.employeeLookup("NOT_FOUND@email.com", 0, 10);
            assertEquals(0, response.getEmployees().size());
        } finally {
            serviceUnderTest.onStop();
        }
    }
}