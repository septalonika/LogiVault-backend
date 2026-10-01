package com.logivault.user;

import com.logivault.user.dto.UserResponse;
import com.logivault.user.dto.UserSummary;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserSummary toSummary(User user);

    UserResponse toResponse(User user);
}
