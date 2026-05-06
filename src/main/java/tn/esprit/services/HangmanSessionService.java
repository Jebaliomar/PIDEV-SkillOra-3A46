package tn.esprit.services;

import tn.esprit.entities.HangmanGame;

import java.util.LinkedHashSet;
import java.util.Set;

public class HangmanSessionService {

    private HangmanGame currentGame;
    private final Set<Character> guessedLetters = new LinkedHashSet<>();
    private int mistakes;
    private boolean finished;

    public void startGame(HangmanGame game) {
        this.currentGame = game;
        this.guessedLetters.clear();
        this.mistakes = 0;
        this.finished = false;
    }

    public HangmanGame getCurrentGame() {
        return currentGame;
    }

    public Set<Character> getGuessedLetters() {
        return guessedLetters;
    }

    public int getMistakes() {
        return mistakes;
    }

    public int getRemainingAttempts() {
        if (currentGame == null || currentGame.getMaxMistakes() == null) {
            return 0;
        }
        return Math.max(0, currentGame.getMaxMistakes() - mistakes);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isWon() {
        if (currentGame == null || currentGame.getAnswer() == null) {
            return false;
        }

        String normalized = normalize(currentGame.getAnswer());

        for (char c : normalized.toCharArray()) {
            if (Character.isLetterOrDigit(c) && !guessedLetters.contains(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean isLost() {
        if (currentGame == null || currentGame.getMaxMistakes() == null) {
            return false;
        }
        return mistakes >= currentGame.getMaxMistakes();
    }

    public boolean guessLetter(char letter) {
        if (currentGame == null || finished) {
            return false;
        }

        char upper = Character.toUpperCase(letter);

        if (!Character.isLetterOrDigit(upper)) {
            return false;
        }

        if (guessedLetters.contains(upper)) {
            return false;
        }

        guessedLetters.add(upper);

        String answer = normalize(currentGame.getAnswer());
        boolean correct = answer.indexOf(upper) >= 0;

        if (!correct) {
            mistakes++;
        }

        if (isWon() || isLost()) {
            finished = true;
        }

        return correct;
    }

    public String getMaskedWord() {
        if (currentGame == null || currentGame.getAnswer() == null) {
            return "";
        }

        String answer = normalize(currentGame.getAnswer());
        StringBuilder sb = new StringBuilder();

        for (char c : answer.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (guessedLetters.contains(c)) {
                    sb.append(c).append(' ');
                } else {
                    sb.append('_').append(' ');
                }
            } else {
                sb.append(c).append(' ');
            }
        }

        return sb.toString().trim();
    }

    public String getGuessedLettersAsString() {
        StringBuilder sb = new StringBuilder();
        for (Character c : guessedLetters) {
            sb.append(c);
        }
        return sb.toString();
    }

    public int calculateScore() {
        if (currentGame == null) {
            return 0;
        }

        int baseScore;
        String level = currentGame.getLevel() == null ? "" : currentGame.getLevel().trim().toUpperCase();

        switch (level) {
            case "HARD":
                baseScore = 150;
                break;
            case "MEDIUM":
                baseScore = 120;
                break;
            default:
                baseScore = 100;
                break;
        }

        if (!isWon()) {
            return 0;
        }

        int penalty = mistakes * 10;
        return Math.max(baseScore - penalty, 0);
    }

    public String revealAnswer() {
        if (currentGame == null || currentGame.getAnswer() == null) {
            return "";
        }
        return normalize(currentGame.getAnswer());
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}