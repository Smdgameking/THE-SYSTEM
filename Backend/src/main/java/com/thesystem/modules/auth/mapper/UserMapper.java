package com.thesystem.modules.auth.mapper;

import com.thesystem.modules.auth.dto.UserResponse;
import com.thesystem.modules.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "emailVerified", source = "emailVerified")
    UserResponse toUserResponse(User user);
}
