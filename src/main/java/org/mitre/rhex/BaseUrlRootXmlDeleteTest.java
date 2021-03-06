package org.mitre.rhex;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.mitre.test.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * 6.3 baseURL/root.xml
 *
 * 6.3.2 POST, PUT, DELETE These operations MUST NOT be implemented.
 *
 * Status Code: 405
 *
 * @author Jason Mathews, MITRE Corp.
 * Date: 2/20/12 10:45 AM
 */
public class BaseUrlRootXmlDeleteTest extends BaseTest {

	@NonNull
	public String getId() {
		return "6.3.2.3";
	}

	@Override
	public boolean isRequired() {
		return true; // MUST
	}

	@NonNull
	public String getName() {
		return "baseURL/root.xml DELETE operation MUST NOT be implemented. Returns 405 status";
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
			HttpDelete req = new HttpDelete(baseURL);
			if (log.isDebugEnabled()) {
				System.out.println("\nDELETE URL: " + req.getURI());
			}
			HttpResponse response = context.executeRequest(client, req);
			int code = response.getStatusLine().getStatusCode();
			if (code != 405 || log.isDebugEnabled()) {
				if (!log.isDebugEnabled()) {
					System.out.println("URL: " + req.getURI());
				}
				dumpResponse(req, response, code == 200);
			}
			assertEquals(405, code);
			setStatus(StatusEnumType.SUCCESS);
		} catch (IOException e) {
			throw new TestException(e);
		} catch (URISyntaxException e) {
			throw new TestException(e);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

}
