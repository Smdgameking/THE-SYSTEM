package com.thesystem.modules.user.mapper;

import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "timezone", source = "timezone")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "accountStatus", source = "accountStatus")
    @Mapping(target = "lastActiveAt", source = "lastActiveAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserProfileResponse toUserProfileResponse(UserProfile userProfile);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "timezone", source = "timezone")
    @Mapping(target = "locale", source = "locale")
    @Mapping(target = "country", source = "country")
    PublicUserResponse toPublicUserResponse(UserProfile userProfile);
}
