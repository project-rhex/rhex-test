package org.mitre.hdata.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;

/**
 * @author Jason Mathews, MITRE Corp.
 *
 * Date: 2/22/12 4:46 PM
 */
public final class ClientHelper {

	/**
	 * Return content-type value with optional character encoding
	 * stripped off.
	 *
	 * @param entity HttpEntity returned from HttpResponse
	 * @return content-type from HTTP response header, null if not present
	 */
	@CheckForNull
	public static String getContentType(HttpEntity entity) {
		return getContentType(entity, false);
	}

	/**
	 * Return content-type value and optionally strip off the character encoding
	 * following the content MIME type.
	 *
	 * @param entity HttpEntity returned from HttpResponse
	 * @param includeEncoding Flag if true it include character encoding if provided
	 *                        otherwise strip encoding from the content type
	 * @return content-type from HTTP response header, null if not present
	 * @see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.17
	 */
	@CheckForNull
	public static String getContentType(HttpEntity entity, boolean includeEncoding) {
		if (entity == null) return null;
		Header header = entity.getContentType();
		if (header == null) return null;
		String type = header.getValue();
		if (type != null && !includeEncoding) {
			// remove character encoding from mime type
			// Content-Type: text/html; charset=ISO-8859-4
			int ind = type.indexOf(';');
			if (ind >= 0) type = type.substring(0, ind);
		}
		return type;
	}

	public static boolean isXmlContentType(String contentType) {
		if (contentType == null || contentType.startsWith("image/")) return false;
		if (TestUnit.MIME_APPLICATION_ATOM_XML.equals(contentType)) return true;
		if (contentType.endsWith("/xml")) return true; // e.g. text/xml, application/xml
		return false;
	}

}
