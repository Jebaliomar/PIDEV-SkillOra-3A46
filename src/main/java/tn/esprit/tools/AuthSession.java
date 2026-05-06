package tn.esprit.tools;

import tn.esprit.entities.User;

public final class AuthSession {

    private static User currentUser;
    private static String currentRole = "student";

    private AuthSession() {
    }

    public static void setCurrentUser(User user, String role) {
        currentUser = user;
        currentRole = normalizeRole(role);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static boolean isStudent() {
        return "student".equals(currentRole);
    }

    public static boolean isAdminAreaAllowed() {
        return "admin".equals(currentRole) || "professor".equals(currentRole) || "teacher".equals(currentRole);
    }

    public static void clear() {
        currentUser = null;
        currentRole = "student";
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "student";
        }
        return role.trim().toLowerCase().replace("role_", "");
    }
}
