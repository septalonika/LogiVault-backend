package com.logivault.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.logivault.dto.PageResponse;
import com.logivault.dto.user.ChangePasswordRequest;
import com.logivault.dto.user.CreateUserRequest;
import com.logivault.dto.user.ResetPasswordRequest;
import com.logivault.dto.user.UpdateUserRequest;
import com.logivault.dto.user.UpdateUserStatusRequest;
import com.logivault.dto.user.UserResponse;
import com.logivault.entity.Role;
import com.logivault.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Users", description = "Own profile and user management")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Current user profile")
    @ApiResponse(responseCode = "200", description = "Profile")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Operation(summary = "Change own password")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR or INVALID_OLD_PASSWORD")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List users (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Page of users")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) Role role,
                                            @RequestParam(required = false) Boolean active,
                                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userService.list(q, role, active, pageable);
    }

    @Operation(summary = "Create a user (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_EXISTS")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @Operation(summary = "Get a user (ADMIN)")
    @ApiResponse(responseCode = "200", description = "User")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Update name and role (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SELF_MODIFICATION_NOT_ALLOWED")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Activate or deactivate a user (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SELF_MODIFICATION_NOT_ALLOWED")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(id, request));
    }

    @Operation(summary = "Reset a user password (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Password reset")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
