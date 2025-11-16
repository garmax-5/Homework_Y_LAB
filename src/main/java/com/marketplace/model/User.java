package com.marketplace.model;

// Модель пользователя
public class User {
    public enum Role { ADMIN, USER }

    private Long id;
    private String userName;
    private String password;
    private Role role;

    public User(Long id, String userName, String password, Role role) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.role = role;
    }

    public User(String userName, String password, Role role) {
        this.id = null;
        this.userName = userName;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', role=%s}", id, userName, role);
    }
}
