package tn.esprit.tools;

public enum CameraReservationField {
    PRENOM("First Name"),
    NOM("Last Name"),
    TELEPHONE("Phone");

    private final String displayName;

    CameraReservationField(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CameraReservationField next() {
        return switch (this) {
            case PRENOM -> NOM;
            case NOM -> TELEPHONE;
            case TELEPHONE -> TELEPHONE;
        };
    }
}
