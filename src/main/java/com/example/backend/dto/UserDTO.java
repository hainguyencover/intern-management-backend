package com.example.backend.dto;

import java.io.Serializable;
import java.util.List;

public class UserDTO implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String name;
    private String password;
    private List<String> roles;

    public UserDTO() {
    }

    public UserDTO(Long id, String username, String email, String phone, String name) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}


