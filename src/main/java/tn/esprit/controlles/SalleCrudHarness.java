package tn.esprit.controlles;

import tn.esprit.entities.Salle;
import tn.esprit.services.SalleService;

import java.sql.SQLException;
import java.util.List;

public class SalleCrudHarness {
    public static void main(String[] args) {
        SalleService service = new SalleService();
        try {
            List<Salle> all = service.getAll();
            System.out.println("Salles loaded: " + all.size());
        } catch (SQLException e) {
            System.out.println("Salle service check failed: " + e.getMessage());
        }
    }
}

