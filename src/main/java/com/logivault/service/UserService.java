package com.logivault.service;

import com.logivault.dto.PageResponse;
import com.logivault.dto.user.ChangePasswordRequest;
import com.logivault.dto.user.CreateUserRequest;
import com.logivault.dto.user.ResetPasswordRequest;
import com.logivault.dto.user.UpdateUserRequest;
import com.logivault.dto.user.UpdateUserStatusRequest;
import com.logivault.dto.user.UserResponse;
import com.logivault.entity.Role;
import com.logivault.entity.User;
import com.logivault.exception.BusinessException;
import com.logivault.exception.ErrorCode;
import com.logivault.mapper.UserMapper;
import com.logivault.repository.RefreshTokenRepository;
import com.logivault.repository.UserRepository;
import com.logivault.security.CurrentUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final Clock clock;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder, UserMapper userMapper, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    public UserResponse getCurrentUser() {
        return userMapper.toResponse(findCurrentUser());
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        User user = findCurrentUser();

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_OLD_PASSWORD);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "New password must differ from the old password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String q, Role role, Boolean active, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("email")), like)));
        }
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        return PageResponse.from(userRepository.findAll(spec, pageable), userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();
        try {
            // Flush now so a concurrent duplicate hits the unique index here, not at commit.
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findById(id);
        if (isSelf(user) && request.role() != user.getRole()) {
            throw new BusinessException(ErrorCode.SELF_MODIFICATION_NOT_ALLOWED);
        }
        user.setName(request.name().trim());
        user.setRole(request.role());
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UpdateUserStatusRequest request) {
        User user = findById(id);
        if (!request.active() && isSelf(user)) {
            throw new BusinessException(ErrorCode.SELF_MODIFICATION_NOT_ALLOWED);
        }
        user.setActive(request.active());
        if (!request.active()) {
            refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(clock));
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now(clock));
    }

    private boolean isSelf(User user) {
        return user.getId().equals(CurrentUser.id());
    }

    private User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private User findCurrentUser() {
        return userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
