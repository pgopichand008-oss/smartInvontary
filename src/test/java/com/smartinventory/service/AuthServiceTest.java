package com.smartinventory.service;

import com.smartinventory.config.DatabaseConnection;
import com.smartinventory.dao.UserDAO;
import com.smartinventory.exception.AuthenticationException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.User;
import com.smartinventory.model.enums.UserRole;
import com.smartinventory.util.PasswordUtil;
import com.smartinventory.util.UserSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for Authentication, UserDAO, and Session Management.
 */
public class AuthServiceTest {

    private static AuthService authService;
    private static UserDAO userDAO;

    @BeforeAll
    public static void setUp() throws SQLException {
        authService = new AuthService();
        userDAO = new UserDAO();

        // Ensure default admin password hash is fresh and valid for 'admin123'
        Optional<User> adminOpt = userDAO.findByUsername("admin");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            if (!PasswordUtil.checkPassword("admin123", admin.getPasswordHash())) {
                String freshHash = PasswordUtil.hashPassword("admin123");
                userDAO.updatePassword(admin.getUserId(), freshHash);
            }
        }
    }

    @AfterAll
    public static void tearDown() {
        authService.logout();
        DatabaseConnection.closePool();
    }

    @Test
    @DisplayName("Admin user successfully logs in and initializes UserSession")
    public void testSuccessfulAdminLogin() {
        User user = authService.login("admin", "admin123");

        assertNotNull(user, "Authenticated user should not be null");
        assertEquals("admin", user.getUsername());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isAdmin());

        // Verify session management
        UserSession session = UserSession.getInstance();
        assertTrue(session.isLoggedIn(), "Session should be marked as active");
        assertEquals("admin", session.getCurrentUser().getUsername());
        assertTrue(session.isAdmin(), "Session should recognize Admin role");
    }

    @Test
    @DisplayName("Invalid password throws AuthenticationException")
    public void testInvalidPasswordThrowsException() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("admin", "wrong_password_123");
        });
    }

    @Test
    @DisplayName("Non-existent username throws AuthenticationException")
    public void testNonExistentUserThrowsException() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("non_existent_user_999", "some_password");
        });
    }

    @Test
    @DisplayName("Blank credentials throw ValidationException")
    public void testBlankCredentialsValidation() {
        assertThrows(ValidationException.class, () -> {
            authService.login("", "password123");
        });

        assertThrows(ValidationException.class, () -> {
            authService.login("admin", "");
        });

        assertThrows(ValidationException.class, () -> {
            authService.login(null, "password123");
        });
    }

    @Test
    @DisplayName("Deactivated user is blocked from logging in")
    public void testDeactivatedUserCannotLogin() throws SQLException {
        String testUsername = "inactive_user_" + System.currentTimeMillis();
        String password = "userpass123";
        String hash = PasswordUtil.hashPassword(password);

        User inactiveUser = new User(testUsername, hash, "Inactive Test User", testUsername + "@test.com", UserRole.CASHIER);
        inactiveUser.setActive(false);

        User created = userDAO.create(inactiveUser);
        assertNotNull(created.getUserId());

        try {
            AuthenticationException ex = assertThrows(AuthenticationException.class, () -> {
                authService.login(testUsername, password);
            });
            assertTrue(ex.getMessage().contains("deactivated"), "Exception message should indicate account is deactivated");
        } finally {
            userDAO.delete(created.getUserId());
        }
    }

    @Test
    @DisplayName("Logout clears UserSession")
    public void testLogoutClearsSession() {
        authService.login("admin", "admin123");
        assertTrue(UserSession.getInstance().isLoggedIn());

        authService.logout();
        assertFalse(UserSession.getInstance().isLoggedIn(), "Session should be null after logout");
        assertNull(UserSession.getInstance().getCurrentUser());
    }
}
