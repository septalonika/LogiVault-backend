package com.logivault.user;

import com.logivault.auth.RefreshTokenRepository;
import com.logivault.common.exception.BusinessException;
import com.logivault.common.exception.ErrorCode;
import com.logivault.common.security.CurrentUser;
import com.logivault.user.dto.ChangePasswordRequest;
import com.logivault.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

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

    private User findCurrentUser() {
        return userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
