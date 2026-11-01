package com.smartinventory.model;

import com.smartinventory.model.enums.UserRole;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain entity representing a system user/operator.
 */
public class User {

    private Integer userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private UserRole role = UserRole.CASHIER;
    private boolean active = true;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
    }

    public User(String username, String passwordHash, String fullName, String email, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role != null ? role : UserRole.CASHIER;
        this.active = true;
    }

    public User(Integer userId, String username, String passwordHash, String fullName, String email, UserRole role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role != null ? role : UserRole.CASHIER;
        this.active = active;
    }

    public User(Integer userId, String username, String passwordHash, String fullName, String email, UserRole role, boolean active, Timestamp createdAt, Timestamp updatedAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.role = role != null ? role : UserRole.CASHIER;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role != null ? role : UserRole.CASHIER;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isManagerOrAdmin() {
        return this.role == UserRole.ADMIN || this.role == UserRole.MANAGER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    @Override
    public String toString() {
        return fullName + " (" + username + " - " + role + ")";
    }
}
