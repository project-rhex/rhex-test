package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Test for document GET. This test assumes the patient document is an well-formed XML document.
 *
 * <pre>
 * 6.5 baseURL/sectionpath/documentname
 *
 * 6.5.1 GET
 *
 * This operation returns a representation of the document that is identified by documentname
 * within the section identified by sectionpath. The documentname is typically assigned by the
 * underlying system and is not guaranteed to be identical across two different systems.
 *
 * Status Code: 200
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 6/26/12 3:41 PM
 */
public class DocumentGet extends BaseXmlTest {

    private URI documentURL;

	private boolean dumped;

    @NonNull
    public String getId() {
        return "6.5.1.1";
    }

    @NonNull
    public String getName() {
        return "GET operation returns a representation of the document that is identified by documentname in URL";
    }

    @NonNull
    public List<Class<? extends TestUnit>> getDependencyClasses() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public void execute() throws TestException {
        final Context context = Loader.getInstance().getContext();
        documentURL = context.getPropertyAsURI("document.url");
        if (documentURL == null) {
            // check pre-conditions and setup
            log.error("Failed to specify valid document/url property in configuration");
            setStatus(StatusEnumType.SKIPPED, "Failed to specify valid document/url property in configuration");
            return;
        }
		String content = context.getString("document.content");
		if (StringUtils.isBlank(content)) {
			// check pre-conditions and setup
			log.error("Failed to specify valid document/content property in configuration");
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid document/content property in configuration");
			return;
		}
        HttpClient client = context.getHttpClient();
        HttpGet req = new HttpGet(documentURL);
        req.setHeader("Accept", MIME_APPLICATION_XML);
        if (log.isDebugEnabled()) {
            System.out.println("\nURL: " + req.getURI());
            for(Header header : req.getAllHeaders()) {
                System.out.println("  " + header.getName() + ": " + header.getValue());
            }
        }
		HttpResponse response = null;
        try {
            response = context.executeRequest(client, req);
            validateContent(context, req, response, content);
            setResponse(response);
            setStatus(StatusEnumType.SUCCESS);
        } catch (IOException e) {
			if (response != null) dumpResponse(req, response, false);
            throw new TestException(e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void validateContent(Context context, HttpGet req, HttpResponse response, String content)
			throws IOException, TestException
	{
        int code = response.getStatusLine().getStatusCode();

        if (code != 200 || log.isDebugEnabled()) {
            dumpResponse(req, response, false);
        }
        assertEquals(200, code);
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            // no document body
            dumpResponse(req, response, false);
            log.error("no BODY in response for document: " + documentURL.getPath());
            throw new TestException("encountered non-body in document response");
        }
        long len = entity.getContentLength();
        if (len <= 0) {
            dumpResponse(req, response, true);
            throw new TestException("Document content length=" + len + ", expecting len > 0");
        }
        final String contentType = ClientHelper.getContentType(entity);
        // expect content-type = text/xml OR application/xml OR application/atom+xml
        if (!ClientHelper.isXmlContentType(contentType)) {
            dumpResponse(req, response, true);
            throw new TestException("expected XML content in response body: type=" + contentType);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        entity.writeTo(bos);
        try {
			String bodyText = bos.toString("UTF-8");
			if (bodyText == null || !bodyText.contains(content)) {
				log.error("Content:\n\t{}", bodyText);
				throw new TestException("Failed to match expected content in response");
			}
			log.debug("XXX: expected content found in response:\n\t{}", bodyText); // success so far
            Document doc = getDefaultDocument(context, bos);
            if (keepDocument) setDocument(doc);
        } catch (JDOMException e) {
			if (!log.isDebugEnabled()) {
				dumpResponse(req, response, false);
				System.out.println("Response body:\n"  + bos.toString());
			}
            addWarning(e.getMessage());
            log.warn("", e);
        }
    }

    protected void dumpResponse(HttpRequestBase req, HttpResponse response, boolean dumpEntity) {
		if (!dumped) {
			dumped = true;
			if (!log.isDebugEnabled()) {
				System.out.println("\nURL: " + req.getURI());
			}
			super.dumpResponse(req, response, dumpEntity);
		}
    }

    public URI getDocumentURL() {
        return documentURL;
    }

}