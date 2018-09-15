package com.apple.wwrc.service.customer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Mockingclass.class)
public class MockTestExample {

	@Test
	public void powerMockito_MockingAConstructor() throws Exception {

		Mockingclass testClass = mock(Mockingclass.class);

		when(testClass.listSize()).thenReturn(40);
		Assert.assertTrue("Size is ",testClass.listSize() == 40);
	}
	@Test
	public void test_mock_static_method() {
		PowerMockito.mockStatic(Mockingclass.class);
		when(Mockingclass.getString()).thenReturn("Hello!");

	}

	@Test
	public void test_private_static_method() throws Exception {
//		PowerMockito.mockStatic(Mockingclass.class);
		PowerMockito.spy(Mockingclass.class);
		PowerMockito.doReturn("abc").when(Mockingclass.class, "callPrivateMethod");

		String retrieved = Mockingclass.privateStaticMethodTest();

		Assert.assertNotNull(retrieved);
		assertEquals(retrieved, "abc");
	}

	//Generic test methods

	@Test
	public void testMoreThanOneReturnValue()  {
		Iterator<String> i = mock(Iterator.class);
		when(i.next()).thenReturn("Mockito").thenReturn("example");
		String result= i.next()+" "+i.next();
		assertEquals("Mockito example", result);
	}

	@Test
	public void test_with_arguments(){
		Comparable c=mock(Comparable.class);
		when(c.compareTo("Test")).thenReturn(1);
		assertEquals(1,c.compareTo("Test"));
	}

	@Test
	public void testReturnValueDependentOnMethodParameter()  {
		Comparable<String> c= mock(Comparable.class);
		when(c.compareTo("Mockito")).thenReturn(1);
		when(c.compareTo("PowerMock")).thenReturn(2);
		assertEquals(1, c.compareTo("Mockito"));
	}

	@Test
	public void test_with_unspecified_arguments(){
		Comparable c=mock(Comparable.class);
		when(c.compareTo(anyInt())).thenReturn(-1);
		assertEquals(-1,c.compareTo(5));
	}

	@Test
	public void test_with_throw()
			throws IOException {
		OutputStream mock =mock(OutputStream.class);
		OutputStreamWriter osw  =new OutputStreamWriter(mock);
		PowerMockito.doThrow(new IOException()).when(mock).close();
		osw.close();
	}

}