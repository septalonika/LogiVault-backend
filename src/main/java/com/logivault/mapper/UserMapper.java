package com.logivault.mapper;

import com.logivault.dto.user.UserResponse;
import com.logivault.dto.user.UserSummary;
import com.logivault.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserSummary toSummary(User user);

    UserResponse toResponse(User user);
}
