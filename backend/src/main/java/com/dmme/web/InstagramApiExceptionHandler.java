package com.dmme.web;

import com.dmme.service.InstagramApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class InstagramApiExceptionHandler {

    @ExceptionHandler(InstagramApiException.class)
    public ResponseEntity<Map<String, Object>> handleInstagramApi(InstagramApiException ex) {
        if (ex.isAuthProblem()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", "instagram_reauth_required");
            body.put("message",
                    "Your Instagram connection is no longer valid. Please reconnect your Instagram account in Settings.");
            if (ex.getGraphCode() != null) {
                body.put("graphCode", ex.getGraphCode());
            }
            if (ex.getErrorSubcode() != null) {
                body.put("graphSubcode", ex.getErrorSubcode());
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        String message = ex.getGraphMessage() != null
                ? ex.getGraphMessage()
                : "Instagram Graph API request failed";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", message));
    }
}
