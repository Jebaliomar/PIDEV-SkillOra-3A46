package tn.esprit.services;

import java.io.IOException;
import java.util.Locale;

public class QuizAudioService {

    private Thread currentSpeechThread;
    private volatile boolean audioAvailable = true;
    private volatile String lastErrorMessage = "";

    public synchronized boolean speakAsync(String text) {
        stop();

        if (text == null || text.isBlank()) {
            lastErrorMessage = "Texte audio vide.";
            return false;
        }

        currentSpeechThread = new Thread(() -> {
            try {
                boolean ok = speakInternal(text.trim());
                if (!ok) {
                    audioAvailable = false;
                    lastErrorMessage = "Aucun moteur audio disponible sur cette machine.";
                }
            } catch (Exception e) {
                audioAvailable = false;
                lastErrorMessage = e.getMessage() != null
                        ? e.getMessage()
                        : "Lecture audio impossible.";
            }
        });

        currentSpeechThread.setDaemon(true);
        currentSpeechThread.start();
        return true;
    }

    public synchronized void stop() {
        if (currentSpeechThread != null && currentSpeechThread.isAlive()) {
            currentSpeechThread.interrupt();
        }
        currentSpeechThread = null;
    }

    public boolean isAudioAvailable() {
        return audioAvailable;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage == null || lastErrorMessage.isBlank()
                ? "Lecture audio impossible."
                : lastErrorMessage;
    }

    public boolean testAvailability() {
        try {
            audioAvailable =
                    isFreeTtsAvailable()
                            || isWindowsSpeechAvailable()
                            || isMacSpeechAvailable()
                            || isLinuxSpeechAvailable();

            if (!audioAvailable) {
                lastErrorMessage = "Aucun moteur audio disponible sur cette machine.";
            }

            return audioAvailable;

        } catch (Exception e) {
            audioAvailable = false;
            lastErrorMessage = e.getMessage() != null
                    ? e.getMessage()
                    : "Lecture audio impossible.";
            return false;
        }
    }

    private boolean speakInternal(String text) throws Exception {
        if (tryFreeTts(text)) {
            return true;
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win") && tryWindowsSpeech(text)) {
            return true;
        }

        if (os.contains("mac") && tryMacSpeech(text)) {
            return true;
        }

        if ((os.contains("nix") || os.contains("nux") || os.contains("linux")) && tryLinuxSpeech(text)) {
            return true;
        }

        return false;
    }

    private boolean isFreeTtsAvailable() {
        try {
            Class<?> voiceManagerClass = Class.forName("com.sun.speech.freetts.VoiceManager");
            Object voiceManager = voiceManagerClass.getMethod("getInstance").invoke(null);

            Object voice = voiceManagerClass.getMethod("getVoice", String.class).invoke(voiceManager, "kevin16");
            if (voice == null) {
                voice = voiceManagerClass.getMethod("getVoice", String.class).invoke(voiceManager, "kevin");
            }

            return voice != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean tryFreeTts(String text) {
        try {
            Class<?> voiceManagerClass = Class.forName("com.sun.speech.freetts.VoiceManager");
            Object voiceManager = voiceManagerClass.getMethod("getInstance").invoke(null);

            Object voice = voiceManagerClass.getMethod("getVoice", String.class).invoke(voiceManager, "kevin16");
            if (voice == null) {
                voice = voiceManagerClass.getMethod("getVoice", String.class).invoke(voiceManager, "kevin");
            }

            if (voice == null) {
                return false;
            }

            Class<?> voiceClass = voice.getClass();
            voiceClass.getMethod("allocate").invoke(voice);
            voiceClass.getMethod("speak", String.class).invoke(voice, text);
            voiceClass.getMethod("deallocate").invoke(voice);
            return true;

        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isWindowsSpeechAvailable() {
        try {
            Process process = new ProcessBuilder(
                    "powershell",
                    "-Command",
                    "Add-Type -AssemblyName System.Speech"
            ).start();

            process.waitFor();
            return process.exitValue() == 0;

        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tryWindowsSpeech(String text) {
        try {
            String escaped = text
                    .replace("'", "''")
                    .replace("\n", " ")
                    .replace("\r", " ");

            String script =
                    "Add-Type -AssemblyName System.Speech; " +
                            "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$speak.Speak('" + escaped + "');";

            Process process = new ProcessBuilder(
                    "powershell",
                    "-Command",
                    script
            ).start();

            process.waitFor();
            return process.exitValue() == 0;

        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isMacSpeechAvailable() {
        try {
            Process process = new ProcessBuilder("say", "test").start();
            process.destroy();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tryMacSpeech(String text) {
        try {
            Process process = new ProcessBuilder("say", text).start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isLinuxSpeechAvailable() {
        try {
            Process process = new ProcessBuilder("espeak", "--version").start();
            process.waitFor();
            if (process.exitValue() == 0) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try {
            Process process = new ProcessBuilder("spd-say", "--version").start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tryLinuxSpeech(String text) {
        try {
            Process process = new ProcessBuilder("espeak", text).start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException e) {
            try {
                Process process = new ProcessBuilder("spd-say", text).start();
                process.waitFor();
                return process.exitValue() == 0;
            } catch (Exception ignored) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}