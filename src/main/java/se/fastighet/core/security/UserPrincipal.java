package se.fastighet.core.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import se.fastighet.core.entity.User;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserPrincipal {

    private final User user;

    public UUID getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getName() {
        return user.getName();
    }

    public User.Role getRole() {
        return user.getRole();
    }

    public boolean isAdmin() {
        return user.getRole() == User.Role.ADMIN;
    }

    public boolean isBoardMember() {
        return user.getRole() == User.Role.BOARD_MEMBER || isAdmin();
    }

    public boolean isTechnician() {
        return user.getRole() == User.Role.TECHNICIAN;
    }

    public boolean isResident() {
        return user.getRole() == User.Role.RESIDENT;
    }
}
