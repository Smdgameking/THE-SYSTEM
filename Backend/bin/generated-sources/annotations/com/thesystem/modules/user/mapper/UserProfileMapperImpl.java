package com.thesystem.modules.user.mapper;

import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.entity.UserProfile;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-09T04:49:22+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class UserProfileMapperImpl implements UserProfileMapper {

    @Override
    public UserProfileResponse toUserProfileResponse(UserProfile userProfile) {
        if ( userProfile == null ) {
            return null;
        }

        UUID id = null;
        UUID userId = null;
        String username = null;
        String displayName = null;
        String bio = null;
        String avatarUrl = null;
        String timezone = null;
        String locale = null;
        String country = null;
        String accountStatus = null;
        Instant lastActiveAt = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = userProfile.getId();
        userId = userProfile.getUserId();
        username = userProfile.getUsername();
        displayName = userProfile.getDisplayName();
        bio = userProfile.getBio();
        avatarUrl = userProfile.getAvatarUrl();
        timezone = userProfile.getTimezone();
        locale = userProfile.getLocale();
        country = userProfile.getCountry();
        accountStatus = userProfile.getAccountStatus();
        lastActiveAt = userProfile.getLastActiveAt();
        createdAt = userProfile.getCreatedAt();
        updatedAt = userProfile.getUpdatedAt();

        UserProfileResponse userProfileResponse = new UserProfileResponse( id, userId, username, displayName, bio, avatarUrl, timezone, locale, country, accountStatus, lastActiveAt, createdAt, updatedAt );

        return userProfileResponse;
    }

    @Override
    public PublicUserResponse toPublicUserResponse(UserProfile userProfile) {
        if ( userProfile == null ) {
            return null;
        }

        UUID id = null;
        String username = null;
        String displayName = null;
        String bio = null;
        String avatarUrl = null;
        String timezone = null;
        String locale = null;
        String country = null;

        id = userProfile.getId();
        username = userProfile.getUsername();
        displayName = userProfile.getDisplayName();
        bio = userProfile.getBio();
        avatarUrl = userProfile.getAvatarUrl();
        timezone = userProfile.getTimezone();
        locale = userProfile.getLocale();
        country = userProfile.getCountry();

        PublicUserResponse publicUserResponse = new PublicUserResponse( id, username, displayName, bio, avatarUrl, timezone, locale, country );

        return publicUserResponse;
    }
}
