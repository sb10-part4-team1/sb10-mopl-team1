package com.sb10.mopl.auth.controller;

import com.sb10.mopl.auth.controller.api.CsrfTokenControllerApiDocs;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfTokenController implements CsrfTokenControllerApiDocs {

  @Override
  @GetMapping("/api/auth/csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    csrfToken.getToken();
    return ResponseEntity.noContent().build();
  }
}
