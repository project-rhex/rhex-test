package org.mitre.hdata.test.tests;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.mitre.hdata.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlRootXmlPost extends BaseXmlTest {

	private static final Logger log = LoggerFactory.getLogger(BaseUrlRootXmlPost.class);

	@NonNull
	public String getId() {
		return "6.3.2.1";
	}

	@Override
	public boolean isRequired() {
		return true; // MUST
	}

	@NonNull
	public String getName() {
		return "baseURL/root.xml POST operation MUST NOT be implemented. Returns 405 status";
	}

	@NonNull
	public List<Class<? extends TestUnit>> getDependencyClasses() {
		return Collections.emptyList(); // none
	}

	public void execute() throws TestException {
		final Context context = Loader.getInstance().getContext();
		HttpClient client = context.getHttpClient();
		try {
			URI baseURL = context.getBaseURL("root.xml");
			if (log.isDebugEnabled()) System.out.println("\nPOST URL: " + baseURL);
			/*
			HttpUriRequest req = new HttpGet(baseURL);
			req.setHeader("Accept", "text/xml");
			// req.setHeader("If-Modified-Since", "Tue, 28 Feb 2012 14:33:15 GMT");
			if (log.isDebugEnabled()) {
				System.out.println("\nURL: " + req.getURI());
				for(Header header : req.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			HttpResponse response = client.execute(req);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("GET Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			if  (code != 200) {
				setStatus(StatusEnumType.SKIPPED, "Failed to verify record exists");
				return;
			}
			*/

			/*
			expecting:

			 <?xml version="1.0" encoding="UTF-8"?>
			 <root xmlns="http://projecthdata.org/hdata/schemas/2009/06/core" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
				 ...
				 <extensions>
				 </extensions>
				 <sections>
				 </sections>
			 </root>
			 */

			/*
			// TODO: what is HTTP form parameter name for a potential POST
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final HttpEntity entity = response.getEntity();
			entity.writeTo(bos);
			Document document = getDefaultDocument(context, bos);
			Element root = document.getRootElement();
			final Namespace ns = Namespace.getNamespace(NAMESPACE_HDATA_SCHEMAS_2009_06_CORE);
			Element id = root.getChild("id", ns);
			Element lastModified = root.getChild("lastModified", ns);
			if (lastModified != null) {
				lastModified.setText(getDateFormatter().format(new Date()));
			} else {
				lastModified = new Element("lastModified", ns);
				lastModified.setText(getDateFormatter().format(new Date()));
				root.addContent(lastModified);
			}
			XMLOutputter xo = new XMLOutputter();
			xo.setFormat(org.jdom.output.Format.getPrettyFormat());
			String xmlContent = xo.outputString(document);
			// System.out.println("XXX: new XML\n" + xmlContent);
			HttpPost httppost = new HttpPost(baseURL);
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(2);
			if (id != null) formParams.add(new BasicNameValuePair("id", id.getText()));
			formParams.add(new BasicNameValuePair("data", xmlContent));
			httppost.setEntity(new UrlEncodedFormEntity(formParams));
			*/

			/*
			local test:
			baseURL = new URI("http://localhost:8000/form.html");
			System.out.println("\nPOST URL: " + baseURL);
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(1);
			formParams.add(new BasicNameValuePair("data", "bar"));
			HttpPost httppost = new HttpPost(baseURL);
			httppost.setEntity(new UrlEncodedFormEntity(formParams));
			client = new DefaultHttpClient();
			*/

			HttpPost httppost = new HttpPost(baseURL);
			HttpResponse response = client.execute(httppost);
			int code = response.getStatusLine().getStatusCode();
			if (log.isDebugEnabled()) {
				System.out.println("POST Response status=" + code);
				for (Header header : response.getAllHeaders()) {
					System.out.println("\t" + header.getName() + ": " + header.getValue());
				}
			}
			assertEquals(405, code);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (URISyntaxException e) {
			throw new TestException(e);
		//} catch (JDOMException e) {
			//throw new TestException(e);
		} finally {
			// REVIEW: if HttpClient not shared with other tests
			client.getConnectionManager().shutdown();
		}
	}

}
