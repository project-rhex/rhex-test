package org.mitre.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jason Mathews, MITRE Corp.
 *
 * Date: 2/22/12 4:46 PM
 */
public final class ClientHelper {

    private static final Logger log = LoggerFactory.getLogger(ClientHelper.class);

    /**
     * Dump HTTP Response status code and headers
     *
     * @param req Optional Http request may be null
     * @param response Http Response, never null
     */
    public static void dumpResponse(HttpRequestBase req, HttpResponse response) {
        dumpResponse(req, response, false);
    }

    /**
     * Dump HTTP Response status code and headers
     *
     * @param req Optional Http request may be null
     * @param response Http Response, never null
     * @param dumpEntity True if want to dump the response body otherwise body entity is ignored
     */
    public static void dumpResponse(HttpRequestBase req, HttpResponse response, boolean dumpEntity) {
        if (req != null)
            System.out.printf("%s Response %s%n", req.getMethod(), response.getStatusLine());
        for (Header header : response.getAllHeaders()) {
            String name = header.getName();
            // suppress set-cookie header in output unless debug enabled
            if (log.isDebugEnabled() || !"Set-Cookie".equals(name))
                System.out.println("\t" + name + ": " + header.getValue());
        }
        if (dumpEntity) {
            HttpEntity entity = response.getEntity();
            if (entity != null)
                try {
                    System.out.println("----------------------------------------");
                    String bodyText = EntityUtils.toString(entity);
                    if (bodyText.length() > 70)
                        System.out.println("Response body:");
                    else
                        System.out.print("Response body: ");
                    System.out.println(bodyText);
                } catch(Exception e) {
                    log.warn("", e);
                }
            else
                System.out.println("XXX: No body");
        }
    }

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
		// NOTE: similar to EntityUtils.getContentMimeType(HttpEntity)
		if (entity == null) return null;
		Header header = entity.getContentType();
		if (header == null) return null;
		String type = header.getValue();
		if (type != null) {
			if (!includeEncoding) {
				// remove character encoding from mime type
				// Content-Type = text/html; charset=ISO-8859-4 => text/html
				int ind = type.indexOf(';');
				if (ind >= 0) type = type.substring(0, ind);
			}
			type = StringUtils.trimToNull(type);
		}
		return type;
	}

	public static boolean isXmlContentType(String contentType) {
		if (contentType == null || contentType.indexOf('/') <= 0) return false;
		if (contentType.endsWith("+xml")) return true; // e.g. application/atom+xml, application/rdf+xml, image/svg+xml, etc.
		//if (TestUnit.MIME_APPLICATION_ATOM_XML.equals(contentType)) return true;
		if (contentType.endsWith("/xml")) return true; // e.g. text/xml, application/xml
		// REVIEW: missing any other XML mime types that may appear in section ATOM feeds ?
		return false;
	}

}
