package com.glassstore.identity.dto;

import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String employeeCode;
    private String branchName;
    private Set<String> roles;
    private String primaryRole;
    private String defaultRoute;
}
