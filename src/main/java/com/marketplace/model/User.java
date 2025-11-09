package com.marketplace.model;

// Модель пользователя
public class User {
    public enum Role { ADMIN, USER }

    private final long id;
    private final String userName;
    private final String password;
    private final Role role;

    public User(long id, String userName, String password, Role role) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.role = role;
    }

    public long getId() { return id; }
    public String getUserName() { return userName; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }

    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', role=%s}", id, userName, role);
    }
}
