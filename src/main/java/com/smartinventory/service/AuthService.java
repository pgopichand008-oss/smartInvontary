package com.smartinventory.service;

import com.smartinventory.dao.UserDAO;
import com.smartinventory.exception.AuthenticationException;
import com.smartinventory.exception.DatabaseException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.User;
import com.smartinventory.util.PasswordUtil;
import com.smartinventory.util.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service Layer handling Authentication and Credential Verification.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user with username and password.
     *
     * @param username username entered by operator
     * @param password plaintext password
     * @return authenticated User object
     * @throws AuthenticationException on invalid credentials or inactive account
     */
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password is required.");
        }

        try {
            Optional<User> userOpt = userDAO.findByUsername(username.trim());

            if (!userOpt.isPresent()) {
                logger.warn("Authentication failed: User '{}' not found.", username.trim());
                throw new AuthenticationException("Invalid username or password.");
            }

            User user = userOpt.get();

            if (!user.isActive()) {
                logger.warn("Authentication blocked: User '{}' is deactivated.", username.trim());
                throw new AuthenticationException("This account has been deactivated. Please contact an administrator.");
            }

            boolean passwordMatches = PasswordUtil.checkPassword(password, user.getPasswordHash());
            if (!passwordMatches) {
                logger.warn("Authentication failed: Incorrect password for user '{}'.", username.trim());
                throw new AuthenticationException("Invalid username or password.");
            }

            // Authentication succeeded - record active session
            UserSession.getInstance().login(user);
            logger.info("User '{}' ({}) successfully logged in.", user.getUsername(), user.getRole());
            return user;

        } catch (SQLException e) {
            logger.error("Database error during authentication: {}", e.getMessage(), e);
            throw new DatabaseException("Authentication service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Terminates the current active session.
     */
    public void logout() {
        User current = UserSession.getInstance().getCurrentUser();
        if (current != null) {
            logger.info("User '{}' logged out.", current.getUsername());
        }
        UserSession.getInstance().logout();
    }

    /**
     * Updates an authenticated user's password.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new ValidationException("Current password is required.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long.");
        }

        try {
            Optional<User> userOpt = userDAO.findById(userId);
            if (!userOpt.isPresent()) {
                throw new ValidationException("User not found.");
            }

            User user = userOpt.get();
            if (!PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
                throw new AuthenticationException("Current password does not match.");
            }

            String newHash = PasswordUtil.hashPassword(newPassword.trim());
            return userDAO.updatePassword(userId, newHash);
        } catch (SQLException e) {
            logger.error("Database error while changing password: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to update password: " + e.getMessage(), e);
        }
    }
}
