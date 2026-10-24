package com.ems.user.dto;

public record CreateUserResponse(UserResponse user, boolean invitationSent) {
}