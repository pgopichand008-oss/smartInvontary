package com.smartinventory.util;

import com.smartinventory.model.User;
import com.smartinventory.model.enums.UserRole;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton managing the active user session.
 */
public final class UserSession {

    public interface SessionListener {
        void onSessionChanged(User user);
    }

    private static volatile UserSession instance;
    private static final Object LOCK = new Object();

    private volatile User currentUser;
    private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public synchronized void login(User user) {
        this.currentUser = user;
        notifyListeners();
    }

    public synchronized void logout() {
        this.currentUser = null;
        notifyListeners();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public String getFullName() {
        return currentUser != null ? currentUser.getFullName() : null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isManagerOrAdmin() {
        return currentUser != null && currentUser.isManagerOrAdmin();
    }

    public boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    public void addSessionListener(SessionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (SessionListener listener : listeners) {
            try {
                listener.onSessionChanged(currentUser);
            } catch (Exception ignored) {
            }
        }
    }
}
