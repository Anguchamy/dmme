package com.dmme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;

/**
 * Raised when the Meta Graph / Instagram Graph API returns a non-success response.
 * Carries the parsed {@code error} object fields when present.
 */
public class InstagramApiException extends RuntimeException {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpStatusCode httpStatus;
    private final String requestPath;
    private final String graphMessage;
    private final String graphType;
    private final Integer graphCode;
    private final Integer errorSubcode;
    private final String fbtraceId;

    public InstagramApiException(HttpStatusCode httpStatus, String requestPath,
                                 String graphMessage, String graphType,
                                 Integer graphCode, Integer errorSubcode, String fbtraceId) {
        super(graphMessage != null ? graphMessage : "Instagram Graph API request failed");
        this.httpStatus = httpStatus;
        this.requestPath = requestPath;
        this.graphMessage = graphMessage;
        this.graphType = graphType;
        this.graphCode = graphCode;
        this.errorSubcode = errorSubcode;
        this.fbtraceId = fbtraceId;
    }

    public HttpStatusCode getHttpStatus() {
        return httpStatus;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getGraphMessage() {
        return graphMessage;
    }

    public String getGraphType() {
        return graphType;
    }

    public Integer getGraphCode() {
        return graphCode;
    }

    public Integer getErrorSubcode() {
        return errorSubcode;
    }

    public String getFbtraceId() {
        return fbtraceId;
    }

    /** Token expiry, revocation, or other auth problems that require reconnecting Instagram. */
    public boolean isAuthProblem() {
        if ("OAuthException".equals(graphType)) {
            return true;
        }
        if (graphCode != null && (graphCode == 190 || graphCode == 102)) {
            return true;
        }
        return errorSubcode != null && errorSubcode == 463;
    }

    public static InstagramApiException parse(HttpStatusCode status, String path, String body) {
        String message = null;
        String type = null;
        Integer code = null;
        Integer subcode = null;
        String fbtraceId = null;

        if (body != null && !body.isBlank()) {
            try {
                JsonNode error = MAPPER.readTree(body).get("error");
                if (error != null && error.isObject()) {
                    if (error.hasNonNull("message")) {
                        message = error.get("message").asText();
                    }
                    if (error.hasNonNull("type")) {
                        type = error.get("type").asText();
                    }
                    if (error.has("code") && error.get("code").isNumber()) {
                        code = error.get("code").asInt();
                    }
                    if (error.has("error_subcode") && error.get("error_subcode").isNumber()) {
                        subcode = error.get("error_subcode").asInt();
                    }
                    if (error.hasNonNull("fbtrace_id")) {
                        fbtraceId = error.get("fbtrace_id").asText();
                    }
                }
            } catch (Exception ignored) {
                // keep whatever fields we already extracted
            }
        }

        return new InstagramApiException(status, path, message, type, code, subcode, fbtraceId);
    }
}
