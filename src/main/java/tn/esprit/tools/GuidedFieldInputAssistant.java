package tn.esprit.tools;

import java.util.Arrays;
import java.util.List;

public class GuidedFieldInputAssistant {

    public String buildTitle(CameraReservationField field) {
        return switch (field) {
            case PRENOM -> "SAISIE GUIDEE : choisissez des lettres ou des prenoms frequents";
            case NOM -> "SAISIE GUIDEE : choisissez des lettres ou des noms frequents";
            case TELEPHONE -> "SAISIE GUIDEE : utilisez le pave numerique assiste";
        };
    }

    public List<String> getQuickTokens(CameraReservationField field) {
        return switch (field) {
            case PRENOM -> Arrays.asList(
                    "Mohamed", "Ahmed", "Sarra", "Amal", "Ali", "A", "E", "I", "O", "U", "M", "N", "R", "S", "L", "T", "H"
            );
            case NOM -> Arrays.asList(
                    "Ben Ali", "Trabelsi", "Hamdi", "Gharbi", "Ayari", "B", "A", "E", "I", "N", "R", "S", "L", "T", "H", " "
            );
            case TELEPHONE -> Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "+216");
        };
    }

    public String appendToken(CameraReservationField field, String currentValue, String token) {
        String current = currentValue == null ? "" : currentValue;

        if (field == CameraReservationField.TELEPHONE) {
            return current + token;
        }
        if (" ".equals(token)) {
            return current.endsWith(" ") || current.isBlank() ? current : current + " ";
        }
        if (token.length() > 1) {
            return current.isBlank() ? token : token;
        }
        return current + token;
    }

    public String normalize(CameraReservationField field, String value) {
        String current = value == null ? "" : value.trim();
        if (current.isBlank()) {
            return "";
        }

        if (field == CameraReservationField.TELEPHONE) {
            return current.replaceAll("[^+0-9]", "");
        }

        String compact = current.replaceAll("\\s+", " ").trim().toLowerCase();
        String[] words = compact.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
            if (i < words.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }
}
