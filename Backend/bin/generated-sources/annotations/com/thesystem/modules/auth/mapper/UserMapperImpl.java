package com.thesystem.modules.auth.mapper;

import com.thesystem.modules.auth.dto.UserResponse;
import com.thesystem.modules.auth.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-09T03:54:43+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UUID id = null;
        String email = null;
        Boolean emailVerified = null;

        id = user.getId();
        email = user.getEmail();
        emailVerified = user.getEmailVerified();

        UserResponse userResponse = new UserResponse( id, email, emailVerified );

        return userResponse;
    }
}
