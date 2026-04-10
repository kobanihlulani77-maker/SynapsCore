package com.synapsecore.api.controller;

import com.synapsecore.auth.AuthSessionService;
import com.synapsecore.auth.dto.AuthSessionPasswordChangeRequest;
import com.synapsecore.auth.dto.AuthSessionRequest;
import com.synapsecore.auth.dto.AuthSessionResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/session")
@RequiredArgsConstructor
public class AuthController {

    private final AuthSessionService authSessionService;

    @GetMapping
    public AuthSessionResponse getSession(HttpSession session) {
        return authSessionService.getCurrentSession(session);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthSessionResponse signIn(@Valid @RequestBody AuthSessionRequest request, HttpSession session) {
        String tenantCode = request.tenantCode() == null ? null : request.tenantCode().trim();
        return authSessionService.signIn(session, tenantCode, request.username().trim(), request.password().trim());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public AuthSessionResponse signOut(HttpSession session) {
        return authSessionService.signOut(session);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.OK)
    public AuthSessionResponse changePassword(@Valid @RequestBody AuthSessionPasswordChangeRequest request,
                                              HttpSession session) {
        return authSessionService.changePassword(session, request.currentPassword().trim(), request.newPassword().trim());
    }
}
