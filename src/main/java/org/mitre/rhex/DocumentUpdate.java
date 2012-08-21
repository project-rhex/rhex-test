package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private String targetText;

	public DocumentUpdate() {
		setProperty(DocumentPutPreTest.class, PROP_KEEP_DOCUMENT_BOOL, true);
	}

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
		return Collections.<Class<? extends TestUnit>> singletonList(DocumentPutPreTest.class); // 6.5.2.0
	}

	public void execute() throws TestException {
		TestUnit dependTest = getDependency(DocumentPutPreTest.class);
		if (dependTest == null) {
			// assertion failed: this should never be null
			log.error("Failed to retrieve prerequisite test");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test");
			return;
		}
		DocumentPutPreTest baseTest = (DocumentPutPreTest) dependTest;
		final URI baseURL = baseTest.getBaseURL();
		if (baseURL == null) {
			// check pre-conditions and setup
			log.error("Failed to retrieve prerequisite test results");
			setStatus(StatusEnumType.SKIPPED, "Failed to retrieve prerequisite test results: 6.5.2.0");
			return;
		}
		final String xmlContent;
		try {
			xmlContent = getContent(baseTest);
			if (xmlContent == null) {
				setStatus(StatusEnumType.SKIPPED, "Failed to fetch/update test document to perform test");
				return;
			}
			System.out.println("\nUpdated XML content=\n" + xmlContent);
		} catch (IOException e) {
			System.out.println("URL=" + baseURL);
			throw new TestException(e);
		} catch (JDOMException e) {
			System.out.println("URL=" + baseURL);
			throw new TestException(e);
		}

		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			HttpPut request = createRequest(baseURL, xmlContent);
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

			// next verify change was accepted
			if (validateContent(context, request, response, baseTest, baseURL))
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

	protected HttpPut createRequest(URI baseURL, String xmlContent) {
		HttpPut request = new HttpPut(baseURL);
		StringEntity entity = new StringEntity(xmlContent, ContentType.APPLICATION_XML);
		request.setEntity(entity);
		return request;
	}

	protected String getContent(DocumentPutPreTest baseTest) throws IOException, JDOMException {
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
		Document doc = baseTest.getDocument();
		if (doc == null) {
			log.error("Failed to retrieve document from prerequisite test");
			// setStatus(StatusEnumType.SKIPPED, "Failed to retrieve document from prerequisite test");
			return null;
		}
		final Element rootElement = doc.getRootElement();
		final Element effectiveTime = rootElement.getChild("effectiveTime", rootElement.getNamespace());
		// System.out.println("target effectiveTime=" + effectiveTime);
		if (effectiveTime != null) {
			final Element start = effectiveTime.getChild("start", rootElement.getNamespace());
			Attribute value = null;
			/*
			Look for effectiveTime field in one of 3 forms in input document:

			1. <effectiveTime>2011-06-27 04:00:00 +0000</effectiveTime>

			2. <effectiveTime>
		    	<start value="2005-02-09 03:00:00 -0500" />
  			   </effectiveTime>

			3. <effectiveTime>
     			  <start>2009-02-09 02:00:00 -0500</start>
       			  <end></end>
			   </effectiveTime>

			 */
			String text;
			if (start != null) {
				value = start.getAttribute("value");
				if (value != null) text = value.getValue(); // #2
				else text = start.getTextTrim(); // #3
			} else {
				text = effectiveTime.getTextTrim(); // #1
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
			if (start != null) {
				if (value != null) value.setValue(targetText);
				else start.setText(targetText);
			} else
				effectiveTime.setText(targetText);
			// System.out.println("targetText=" + effectiveTime.getText());
			XMLOutputter xo = new XMLOutputter();
			xo.setFormat(org.jdom.output.Format.getPrettyFormat());
			return xo.outputString(doc);
		} // TODO try alternative update strategy if applicable
		return null;
	}

	protected boolean validateContent(Context context, HttpPut request, HttpResponse response, DocumentPutPreTest baseTest, URI baseURL)
            throws JDOMException, IOException, TestException
    {
		int code = response.getStatusLine().getStatusCode();
		if (code != 200 || log.isDebugEnabled()) {
			dumpResponse(request, response);
		}
		// check return code
		assertEquals(200, code);

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
		String elementValue;
		if (start != null) {
			Attribute value = start.getAttribute("value");
			if (value != null) elementValue = value.getValue();
			else elementValue = start.getText();
		} else {
			// <effectiveTime>2011-06-27 04:00:00 +0000</effectiveTime>
			elementValue = effectiveTime.getText();
		}
        assertEquals(targetText, elementValue);
        return true;
	}

}
