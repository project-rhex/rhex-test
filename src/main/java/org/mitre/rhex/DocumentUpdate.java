package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test for document PUT operation
 *
 * <pre>
 * 6.5 baseURL/sectionpath/documentname
 *
 * 6.5.2 PUT
 *
 * This operation is used to update a document by replacing it. The PUT operation
 * MUST NOT be used to create a new document; new documents MUST be created by
 * POSTing to the section. If the client attempts to create a new document this
 * way [via PUT], the server MUST return a 404.
 *
 * The content MUST conform to the media type identified by the document metadata
 * or the section content type. For media type application/xml, the document MUST
 * also conform to the XML schema that corresponds to the content type identified
 * by the document metadata or the section.
 *
 * If the parameter is incorrect or the content cannot be validated against
 * the correct media type or the XML schema identified by the content type
 * of this section, the server MUST return a status code of 400.
 *
 * If the request is successful, the new section document MUST show up in the
 * document feed for the section. The server returns a 200.
 *
 * Status Code: <B>200</B>, 400, 404
 * </pre>
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class DocumentUpdate extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(DocumentUpdate.class);

	private String targetText;

	@NonNull
	public String getId() {
		return "6.5.2.1";
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@NonNull
	public String getName() {
		return "PUT operation to update document";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList();
	}

	public void execute() throws TestException {
		final Context context = Loader.getInstance().getContext();
		final URI baseURL = context.getPropertyAsURI("updateDocumentUrl");
		if (baseURL == null) {
			// check pre-conditions and setup
			log.error("Failed to specify valid updateDocumentUrl property in configuration");
			setStatus(StatusEnumType.SKIPPED, "Failed to specify valid updateDocumentUrl property in configuration");
			return;
		}
		final String xmlContent;
		try {
			xmlContent = getContent(context, baseURL);
			if (xmlContent == null) {
				setStatus(StatusEnumType.SKIPPED, "Failed to fetch/update test document to perform test");
				return;
			}
			System.out.println("\nUpdated XML content=\n" + xmlContent);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (JDOMException e) {
			throw new TestException(e);
		}

		HttpClient client = context.getHttpClient();
		try {
			HttpPut request = new HttpPut(baseURL);
			StringEntity entity = new StringEntity(xmlContent, ContentType.APPLICATION_XML);
			request.setEntity(entity);
			/*
			HTTP Request:
			  - >> "PUT /records/1547/vital_signs/4f37e9a12a1002000400008b HTTP/1.1[\r][\n]"
			  - >> "Content-Length: 390[\r][\n]"
			  - >> "Content-Type: application/xml; charset=UTF-8[\r][\n]"
			  - >> "Host: hdata.herokuapp.com[\r][\n]"
			  - >> "Connection: Keep-Alive[\r][\n]"
			  - >> "User-Agent: Apache-HttpClient/4.1.3 (java 1.5)[\r][\n]"
 			  - >> "[\r][\n]"
			  - >> "<?xml version="1.0" encoding="UTF-8"?>[\r][\n]"
			  - >> "<vitalSign xmlns="urn:hl7-org:greencda:c32">[\r][\n]"
			  - >> "  <id>4f37e9a12a1002000400008b</id>[\r][\n]"
			  - >> "  <code code="60621009" codeSystem="2.16.840.1.113883.6.96">[\r][\n]"
			  - >> "    <originalText>BMI</originalText>[\r][\n]"
			  - >> "  </code>[\r][\n]"
			  - >> "  <status code="completed" />[\r][\n]"
			  - >> "  <effectiveTime>2011-06-BaseUrlNotFoundTest27 04:00:00 +0000</effectiveTime>[\r][\n]"
			  - >> "  <value amount="17.358024691358025" unit="" />[\r][\n]"
			  - >> "</vitalSign>[\r][\n]"
			  - >> "[\r][\n]"
			 */
			HttpResponse response = context.executeRequest(client, request);
            int code = response.getStatusLine().getStatusCode();
			if (code != 200 || log.isDebugEnabled()) {
				dumpResponse(request, response);
			}

			// check return code
			assertEquals(200, code);

			// next verify change was accepted
			if (validateContent(context, baseURL))
				setStatus(StatusEnumType.SUCCESS);
			else
				setStatus(StatusEnumType.FAILED);
		} catch (UnsupportedEncodingException e) {
			log.error("", e);
			addWarning(e.getMessage());
			setStatus(StatusEnumType.SKIPPED, "Unable to encode test document");
		} catch (IOException e) {
			throw new TestException(e);
		} catch (JDOMException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	private String getContent(Context context, URI baseURL) throws IOException, JDOMException {
		Document doc = getXmlDocument(context, baseURL);
		if (doc == null) return null;
		final Element rootElement = doc.getRootElement();
		final Element effectiveTime = rootElement.getChild("effectiveTime", rootElement.getNamespace());
		// System.out.println("target effectiveTime=" + effectiveTime);
		if (effectiveTime != null) {
			final Element start = effectiveTime.getChild("start", rootElement.getNamespace());
			/*
			look for effectiveTime field in either form in input document:

			<effectiveTime>2011-06-27 04:00:00 +0000</effectiveTime>

			<effectiveTime>
     			  <start>2009-02-09 02:00:00 -0500</start>
       			  <end></end>
			</effectiveTime>
			 */
			String text;
			if (start != null) {
				text = start.getTextTrim();
			} else {
				text = effectiveTime.getTextTrim();
			}
			System.out.println("target effectiveTime=" + text);
			if (StringUtils.isBlank(text)) {
				log.debug("Blank or empty time field");
				return null;
			}

			//Pattern p = Pattern.compile("(\\d\\d\\d\\d)-"); // 2010-06-27 04:00:00 +0000
			Pattern p = Pattern.compile("(\\S+) (\\d\\d):"); // 2010-06-27 *04*:00:00 +0000 -- select hour field
			Matcher m = p.matcher(text);
			if (!m.lookingAt()) {
				log.warn("Failed to match target date pattern");
				return null; // TODO try alternative update strategy if applicable
			}
			// System.out.println("match " + m.group(1));
			// increment the hour field
			try {
				targetText = String.format("%s %02d:%s",
					m.group(1),
					(Integer.parseInt(m.group(2)) + 1) % 24,
					text.substring(m.end()));
			} catch (NumberFormatException nfe) {
				log.debug("Failed to parse hour field", nfe);
				return null;
			}
			// increment the year field
			//targetText = (Integer.parseInt(m.group(1)) + 1) + text.substring(4);
			if (start != null)
				start.setText(targetText);
			else
				effectiveTime.setText(targetText);
			// System.out.println("targetText=" + effectiveTime.getText());
			XMLOutputter xo = new XMLOutputter();
			xo.setFormat(org.jdom.output.Format.getPrettyFormat());
			return xo.outputString(doc);
		} // TODO try alternative update strategy if applicable
		return null;
	}

	private boolean validateContent(Context context, URI baseURL)
            throws JDOMException, IOException, TestException
    {
		Document doc = getXmlDocument(context, baseURL);
		if (doc == null) return false;
		final Element rootElement = doc.getRootElement();
		final Element effectiveTime = rootElement.getChild("effectiveTime", rootElement.getNamespace());
		// final String effectiveTime = rootElement.getChildTextTrim("effectiveTime", rootElement.getNamespace());
		if (effectiveTime == null) {
            log.debug("Cannot find effectiveTime element");
            return false;
        }
        final Element start = effectiveTime.getChild("start", rootElement.getNamespace());
        String elementValue = start != null ? start.getText()
                : effectiveTime.getText();
        assertEquals(targetText, elementValue);
        return true;
	}

	@CheckForNull
	private Document getXmlDocument(Context context, URI baseURL)
			throws IOException, JDOMException
	{
		HttpClient client = context.getHttpClient();
		try {
			HttpGet req = new HttpGet(baseURL);
            // Accept definition -> http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
			req.setHeader("Accept", MIME_APPLICATION_XML);
			// req.setHeader("If-Modified-Since", "Tue, 28 Feb 2012 14:33:15 GMT");
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + req.getURI());
				for(Header header : req.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (code != 200 || log.isDebugEnabled()) {
                dumpResponse(req, response);
			}
			if (code != 200) {
				addWarning("Unexpected HTTP response: " + code);
				return null;
			}
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				addWarning("Expect XML in body of response");
				return null;
			}
			final String contentType = ClientHelper.getContentType(entity, false);
			// content-type = text/xml OR application/xml
			if (!MIME_TEXT_XML.equals(contentType) && !MIME_APPLICATION_XML.equals(contentType)) {
				addWarning("Expected supported XML content-type but was: " + contentType);
				return null;
			}
			long len = entity.getContentLength();
			// minimum length expected is 66 bytes or a negative number if unknown
			if (len <= 0) {
				addWarning("Expecting valid XML document; returned length was " + len);
				return null;
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			entity.writeTo(bos);
			if (log.isDebugEnabled()) {
				System.out.println("Content=\n" + bos.toString("UTF-8"));
			}
			/*
			expecting something like:

			<vitalSign xmlns="urn:hl7-org:greencda:c32">
				<id>4f37e9a12a1002000400008b</id>
				<code code="60621009" codeSystem="2.16.840.1.113883.6.96" ><originalText>BMI</originalText></code>
				  <status code="completed"/>
				<effectiveTime>2010-06-27 04:00:00 +0000</effectiveTime>
				<value amount="17.358024691358025" unit="" />
			</vitalSign>
			 */
			return getDefaultDocument(context, bos);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}
