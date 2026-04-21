package tn.esprit.mains;

import tn.esprit.tools.MyConnection;

public class MainApp {

    public static void main(String[] args) {
        try {
            MyConnection.getInstance().getConnection();
            System.out.println("MVC project structure is ready.");
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
}
