package org.mitre.test;

import junit.framework.TestCase;
import org.apache.http.client.HttpClient;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mitre.test.Context;
import org.mitre.test.Loader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: MATHEWS
 * Date: 4/25/12 12:07 PM
 */
public class TestContext extends TestCase implements ErrorHandler {

	private final Context context = Loader.getInstance().getContext();

	public void testBaseUrl() throws URISyntaxException {
		final URI baseURL = context.getBaseURL();
		assertNotNull(baseURL);
		assertEquals(baseURL, context.getBaseURL(""));
		assertTrue(context.getBaseURL("section").toASCIIString().contains("/section"));
		assertTrue(context.getBaseURL("/section").toASCIIString().contains("/section"));
		assertNotNull(context.getString("baseURL"));
		assertNotNull(context.getPropertyAsURI("baseURL"));
	}

	public void testPropertyAsURI() {
		assertNull(context.getPropertyAsURI("propertyNotFound"));
		assertNull(context.getPropertyAsURI("invalidURL"));
	}

	public void testStringNotFound() {
		assertNull(context.getString("notfound"));
	}

	public void testPropertyAsFile() throws JDOMException, IOException {
		File file = context.getPropertyAsFile("profileDocumentFile");
		assertNotNull(file);
		assertTrue(file.exists());

		SAXBuilder builder = context.getBuilder(this);
		assertNotNull(builder);
		assertNotNull(builder.build(file));
	}

	public void testPropertyAsFileNotFound() {
		File file = context.getPropertyAsFile("propertyNotFound");
		assertNull(file);

		file = context.getPropertyAsFile("fileNotFound");
		assertNull(file);
	}

	public void testBuilder() {
		File file = context.getPropertyAsFile("profileDocumentFile");
		assertNotNull(file);
		assertTrue(file.exists());

		SAXBuilder builder = context.getValidatingBuilder(this);
		assertNotNull(builder);
		/*
		try {
			assertNotNull(builder.build(file));
		} catch (JDOMException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
		*/
	}

	public void testHttpClient() throws URISyntaxException, IOException {
		final HttpClient httpClient = context.getHttpClient();
		assertNotNull(httpClient);
		try {
			context.executeRequest(httpClient, null);
			fail("expected thrown exception here");
		} catch(IllegalArgumentException e) {
			// expected
		}
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		System.out.println("Warning: " + exception);
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		fail(exception.toString());
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		fail(exception.toString());
	}

}
