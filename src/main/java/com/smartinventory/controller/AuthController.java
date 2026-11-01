package com.smartinventory.controller;

import com.smartinventory.exception.AuthenticationException;
import com.smartinventory.exception.ValidationException;
import com.smartinventory.model.User;
import com.smartinventory.service.AuthService;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Controller managing asynchronous authentication workflows for the UI.
 */
public class AuthController {

    private final AuthService authService;

    public AuthController() {
        this(new AuthService());
    }

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Executes login asynchronously to keep the Swing EDT responsive.
     *
     * @param username  username entered by user
     * @param password  password entered by user
     * @param onSuccess callback receiving authenticated User
     * @param onError   callback receiving error message
     */
    public void handleLogin(String username, String password, Consumer<User> onSuccess, Consumer<String> onError) {
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    onSuccess.accept(user);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (cause instanceof AuthenticationException || cause instanceof ValidationException) {
                        onError.accept(cause.getMessage());
                    } else {
                        onError.accept("Unable to log in: " + cause.getMessage());
                    }
                }
            }
        };
        worker.execute();
    }

    /**
     * Logs out the active user session.
     */
    public void handleLogout() {
        authService.logout();
    }
}
