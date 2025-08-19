package com.example.dividend.model;

import com.example.dividend.persist.entity.MemberEntity;
import lombok.Data;

import java.util.List;

public class Auth {

    @Data
    public static class SignIn {
        private String username;
        private String password;

    }

    @Data
    public static class SignUp {
        private String username;
        private String password;
        private List<String> roles;

        public MemberEntity toEntity() {
            return MemberEntity.builder()
                    .username(this.username)
                    .password(this.password)
                    .roles(this.roles)
                    .build();
        }
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String username;
        private List<String> roles;
        boolean enabled;
        boolean accountNonExpired;
        boolean accountNonLocked;
        boolean credentialsNonExpired;

        public static UserResponse from(MemberEntity e) {
            UserResponse response = new UserResponse();
            response.setId(e.getId());
            response.setUsername(e.getUsername());
            response.setRoles(e.getRoles());
            response.setEnabled(e.isEnabled());
            response.setAccountNonExpired(e.isAccountNonExpired());
            response.setAccountNonLocked(e.isAccountNonLocked());
            response.setCredentialsNonExpired(e.isCredentialsNonExpired());
            return response;
        }
    }

}
