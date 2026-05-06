package tn.esprit.services;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VoiceRecognitionService implements AutoCloseable {

    private static final int RECOGNITION_TIMEOUT_SECONDS = 18;
    private static final int MIN_CAPTURE_SECONDS = 5;
    private static final double MIN_VOICE_RMS = 0.0012;
    private static final double MIN_VOICE_PEAK = 0.012;
    private static final String RESULT_PREFIX = "VOICE_RESULT::";
    private static final String ERROR_PREFIX = "VOICE_ERROR::";
    private static final AudioFormat[] CAPTURE_FORMATS = {
            new AudioFormat(16000.0f, 16, 1, true, false),
            new AudioFormat(44100.0f, 16, 1, true, false),
            new AudioFormat(48000.0f, 16, 1, true, false)
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "voice-recognition");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Process currentProcess;
    private volatile TargetDataLine currentLine;

    public CompletableFuture<String> recognizeEventSearchText() {
        return recognizeFromMicrophone();
    }

    public CompletableFuture<String> recognizeReservationVoiceFill() {
        return recognizeFromMicrophone();
    }

    private CompletableFuture<String> recognizeFromMicrophone() {
        stopListening();
        return CompletableFuture.supplyAsync(this::captureAndRecognize, executor);
    }

    public void stopListening() {
        TargetDataLine line = currentLine;
        if (line != null) {
            line.stop();
            line.close();
        }

        Process process = currentProcess;
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    private String captureAndRecognize() {
        Path audioPath = null;
        try {
            CapturedAudio capturedAudio = captureMicrophoneAudio();
            audioPath = writeWaveFile(capturedAudio);
            String recognizedText = recognizeWithWindowsSpeech(audioPath).trim();
            if (recognizedText.isBlank()) {
                log("recognition failed: no voice detected");
                throw new NoVoiceDetectedException("No voice detected, please try again");
            }
            log("speech recognized: " + recognizedText);
            return recognizedText;
        } catch (VoiceRecognitionException exception) {
            log("recognition failed: " + exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log("recognition failed: " + readableMessage(exception));
            throw new RecognitionFailedException("Speech recognition failed. Please try again.", exception);
        } finally {
            if (audioPath != null) {
                try {
                    Files.deleteIfExists(audioPath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private CapturedAudio captureMicrophoneAudio() {
        TargetDataLine line = null;
        try {
            OpenedMicrophone microphone = openMicrophone();
            line = microphone.line();
            AudioFormat format = microphone.format();
            currentLine = line;

            line.start();
            log("microphone started");

            int bytesToCapture = Math.round(format.getFrameRate() * format.getFrameSize() * MIN_CAPTURE_SECONDS);
            int bufferSize = Math.max(format.getFrameSize(), Math.round(format.getFrameRate() * format.getFrameSize() / 4));
            bufferSize -= bufferSize % format.getFrameSize();
            byte[] buffer = new byte[Math.max(bufferSize, format.getFrameSize())];
            ByteArrayOutputStream output = new ByteArrayOutputStream(bytesToCapture);

            while (output.size() < bytesToCapture) {
                if (!line.isOpen()) {
                    throw new MicrophoneUnavailableException("Microphone not detected or permission denied");
                }

                int remaining = bytesToCapture - output.size();
                int read = line.read(buffer, 0, Math.min(buffer.length, remaining));
                if (read > 0) {
                    output.write(buffer, 0, read);
                } else if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new RecognitionFailedException("Speech recognition was interrupted.");
                }
            }

            byte[] audioBytes = output.toByteArray();
            AudioStats stats = analyzeAudio(audioBytes);
            log(String.format(Locale.ROOT, "audio captured (%d bytes, rms=%.4f, peak=%.4f)",
                    audioBytes.length, stats.rms(), stats.peak()));
            if (!stats.hasVoice()) {
                throw new NoVoiceDetectedException("No voice detected, please try again");
            }
            return new CapturedAudio(format, audioBytes);
        } catch (LineUnavailableException | SecurityException | IllegalArgumentException | IllegalStateException exception) {
            throw new MicrophoneUnavailableException("Microphone not detected or permission denied", exception);
        } finally {
            if (currentLine == line) {
                currentLine = null;
            }
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

    private OpenedMicrophone openMicrophone() throws LineUnavailableException {
        Exception lastFailure = null;
        for (AudioFormat format : CAPTURE_FORMATS) {
            try {
                TargetDataLine line = AudioSystem.getTargetDataLine(format);
                line.open(format);
                return new OpenedMicrophone(line, format);
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException exception) {
                lastFailure = exception;
            }
        }

        if (lastFailure instanceof LineUnavailableException lineUnavailableException) {
            throw lineUnavailableException;
        }
        if (lastFailure instanceof SecurityException securityException) {
            throw securityException;
        }
        if (lastFailure instanceof IllegalArgumentException illegalArgumentException) {
            throw illegalArgumentException;
        }
        throw new LineUnavailableException("No supported microphone line was found.");
    }

    private Path writeWaveFile(CapturedAudio capturedAudio) {
        try {
            Path path = Files.createTempFile("skillora-voice-", ".wav");
            long frameLength = capturedAudio.audioBytes().length / capturedAudio.format().getFrameSize();
            try (AudioInputStream audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(capturedAudio.audioBytes()),
                    capturedAudio.format(),
                    frameLength)) {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, path.toFile());
            }
            return path;
        } catch (IOException exception) {
            throw new RecognitionFailedException("Speech recognition failed. Please try again.", exception);
        }
    }

    private AudioStats analyzeAudio(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length < 2) {
            return new AudioStats(0, 0);
        }

        long sumSquares = 0;
        int peak = 0;
        int samples = 0;
        for (int index = 0; index + 1 < audioBytes.length; index += 2) {
            int low = audioBytes[index] & 0xff;
            int high = audioBytes[index + 1];
            int sample = (high << 8) | low;
            int absolute = Math.abs(sample == Short.MIN_VALUE ? Short.MAX_VALUE : sample);
            long sampleValue = sample;
            sumSquares += sampleValue * sampleValue;
            peak = Math.max(peak, absolute);
            samples++;
        }

        if (samples == 0) {
            return new AudioStats(0, 0);
        }

        double rms = Math.sqrt(sumSquares / (double) samples) / Short.MAX_VALUE;
        double normalizedPeak = peak / (double) Short.MAX_VALUE;
        return new AudioStats(rms, normalizedPeak);
    }

    private String recognizeWithWindowsSpeech(Path audioPath) {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    buildRecognitionScript(audioPath)
            );
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
            currentProcess = process;
            log("speech recognition service started");

            boolean finished = process.waitFor(RECOGNITION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Voice recognition timed out.");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return parseRecognitionOutput(output, process.exitValue());
        } catch (IOException e) {
            throw new RecognitionFailedException("Speech recognition service could not be started.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecognitionFailedException("Speech recognition was interrupted.", e);
        } finally {
            if (currentProcess == process) {
                currentProcess = null;
            }
        }
    }

    private String parseRecognitionOutput(String output, int exitCode) {
        for (String line : output.split("\\R")) {
            if (line.startsWith(RESULT_PREFIX)) {
                return line.substring(RESULT_PREFIX.length()).trim();
            }
            if (line.startsWith(ERROR_PREFIX)) {
                throw new RecognitionFailedException(line.substring(ERROR_PREFIX.length()).trim());
            }
        }

        if (exitCode != 0) {
            throw new RecognitionFailedException(output.isBlank() ? "Speech recognition failed. Please try again." : output.trim());
        }
        return "";
    }

    private String buildRecognitionScript(Path audioPath) {
        String safeAudioPath = escapePowerShellSingleQuotedString(audioPath.toAbsolutePath().toString());
        return """
                [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
                $recognizer = $null
                try {
                    Add-Type -AssemblyName System.Speech
                    $recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine
                    $recognizer.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))
                    $recognizer.SetInputToWaveFile('%s')
                    $result = $recognizer.Recognize([TimeSpan]::FromSeconds(12))
                    if ($null -ne $result -and -not [string]::IsNullOrWhiteSpace($result.Text)) {
                        Write-Output ('VOICE_RESULT::' + $result.Text.Trim())
                    } else {
                        Write-Output 'VOICE_RESULT::'
                    }
                    exit 0
                } catch {
                    Write-Output ('VOICE_ERROR::' + $_.Exception.Message)
                    exit 1
                } finally {
                    if ($recognizer -ne $null) { $recognizer.Dispose() }
                }
                """.formatted(safeAudioPath);
    }

    private String escapePowerShellSingleQuotedString(String value) {
        return (value == null ? "" : value).replace("'", "''");
    }

    private void log(String message) {
        System.out.println("[VoiceRecognition] " + message);
    }

    private String readableMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    @Override
    public void close() {
        stopListening();
        executor.shutdownNow();
    }

    private record OpenedMicrophone(TargetDataLine line, AudioFormat format) {
    }

    private record CapturedAudio(AudioFormat format, byte[] audioBytes) {
    }

    private record AudioStats(double rms, double peak) {
        private boolean hasVoice() {
            return rms >= MIN_VOICE_RMS && peak >= MIN_VOICE_PEAK;
        }
    }

    public static class VoiceRecognitionException extends RuntimeException {
        public VoiceRecognitionException(String message) {
            super(message);
        }

        public VoiceRecognitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MicrophoneUnavailableException extends VoiceRecognitionException {
        public MicrophoneUnavailableException(String message) {
            super(message);
        }

        public MicrophoneUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NoVoiceDetectedException extends VoiceRecognitionException {
        public NoVoiceDetectedException(String message) {
            super(message);
        }
    }

    public static class RecognitionFailedException extends VoiceRecognitionException {
        public RecognitionFailedException(String message) {
            super(message);
        }

        public RecognitionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
