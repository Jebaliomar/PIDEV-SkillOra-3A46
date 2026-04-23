package tn.esprit.tools;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationMapHelper {

    private static final int MAP_WIDTH = 860;
    private static final int MAP_HEIGHT = 280;
    private static final int TILE_SIZE = 256;
    private static final int MIN_ZOOM = 3;
    private static final int MAX_ZOOM = 17;
    private static final int DEFAULT_ZOOM = 7;
    private static final String USER_AGENT = "SkillHarbor-JavaFX/1.0";

    private final VBox hostBox;
    private final Label statusLabel;
    private final TextField targetField;
    private final PauseTransition debounce = new PauseTransition(Duration.millis(650));
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private final Map<String, Image> tileCache = new ConcurrentHashMap<>();

    private Canvas canvas;
    private double centerLat = 36.8065;
    private double centerLon = 10.1815;
    private int zoom = DEFAULT_ZOOM;
    private boolean suppressTextSync;
    private double dragStartX;
    private double dragStartY;
    private double dragStartPixelX;
    private double dragStartPixelY;

    public LocationMapHelper(VBox hostBox, Label statusLabel, TextField targetField) {
        this.hostBox = hostBox;
        this.statusLabel = statusLabel;
        this.targetField = targetField;
    }

    public void initialize() {
        canvas = new Canvas(MAP_WIDTH, MAP_HEIGHT);
        canvas.setCursor(Cursor.OPEN_HAND);

        Rectangle clip = new Rectangle(MAP_WIDTH, MAP_HEIGHT);
        clip.setArcWidth(26);
        clip.setArcHeight(26);
        canvas.setClip(clip);

        StackPane mapPane = new StackPane(canvas);
        mapPane.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        mapPane.setMaxWidth(Double.MAX_VALUE);
        mapPane.setStyle("-fx-background-color: #dfe7f3; -fx-background-radius: 20; -fx-border-color: #4f8cff; -fx-border-width: 2; -fx-border-radius: 20;");

        VBox zoomBox = buildZoomControls();
        StackPane.setAlignment(zoomBox, Pos.TOP_LEFT);
        StackPane.setMargin(zoomBox, new Insets(14, 0, 0, 14));

        Label attribution = new Label("Leaflet | © OpenStreetMap contributors");
        attribution.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-text-fill: #334155; -fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 8;");
        StackPane.setAlignment(attribution, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(attribution, new Insets(0, 10, 10, 0));

        mapPane.getChildren().addAll(zoomBox, attribution);

        hostBox.getChildren().clear();
        hostBox.getChildren().add(mapPane);
        VBox.setVgrow(mapPane, Priority.NEVER);

        statusLabel.setText("Type an address or click on the map to set location.");

        canvas.setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();
            dragStartPixelX = lonToPixelX(centerLon, zoom);
            dragStartPixelY = latToPixelY(centerLat, zoom);
            canvas.setCursor(Cursor.CLOSED_HAND);
        });

        canvas.setOnMouseDragged(event -> {
            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;
            double currentPixelX = dragStartPixelX - dx;
            double currentPixelY = dragStartPixelY - dy;
            centerLon = pixelXToLon(currentPixelX, zoom);
            centerLat = pixelYToLat(currentPixelY, zoom);
            renderMap();
        });

        canvas.setOnMouseReleased(event -> canvas.setCursor(Cursor.OPEN_HAND));

        canvas.setOnMouseClicked(event -> {
            if (Math.abs(event.getX() - dragStartX) > 4 || Math.abs(event.getY() - dragStartY) > 4) {
                return;
            }
            double[] latLon = screenToLatLon(event.getX(), event.getY());
            centerLat = latLon[0];
            centerLon = latLon[1];
            renderMap();
            reverseGeocode(centerLat, centerLon);
        });

        canvas.setOnScroll(event -> {
            if (event.getDeltaY() > 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        });

        targetField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suppressTextSync) {
                return;
            }
            debounce.stop();
            debounce.setOnFinished(event -> geocode(newValue));
            debounce.playFromStart();
        });

        renderMap();
        String initialValue = targetField.getText();
        if (initialValue != null && !initialValue.isBlank()) {
            geocode(initialValue);
        }
    }

    private VBox buildZoomControls() {
        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("−");

        for (Button button : new Button[]{zoomInButton, zoomOutButton}) {
            button.setPrefSize(38, 38);
            button.setFont(Font.font("System", FontWeight.BOLD, 24));
            button.setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-text-fill: #1e3a8a; -fx-background-radius: 0; -fx-border-color: #cbd5e1; -fx-border-width: 1;");
        }
        zoomInButton.setStyle(zoomInButton.getStyle() + "-fx-background-radius: 10 10 0 0;");
        zoomOutButton.setStyle(zoomOutButton.getStyle() + "-fx-background-radius: 0 0 10 10;");

        zoomInButton.setOnAction(event -> zoomIn());
        zoomOutButton.setOnAction(event -> zoomOut());

        VBox box = new VBox(0, zoomInButton, zoomOutButton);
        box.setMaxSize(38, 76);
        return box;
    }

    public void geocode(String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        statusLabel.setText("Searching location...");
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + encodedQuery))
                .header("User-Agent", USER_AGENT)
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::handleSearchResponse)
                .exceptionally(error -> {
                    Platform.runLater(() -> statusLabel.setText("Map search unavailable right now."));
                    return null;
                });
    }

    public void setMarker(String label, double lat, double lng) {
        centerLat = lat;
        centerLon = lng;
        renderMap();
        if (label != null && !label.isBlank()) {
            Platform.runLater(() -> {
                suppressTextSync = true;
                targetField.setText(label);
                suppressTextSync = false;
            });
        }
    }

    private void zoomIn() {
        if (zoom < MAX_ZOOM) {
            zoom++;
            renderMap();
        }
    }

    private void zoomOut() {
        if (zoom > MIN_ZOOM) {
            zoom--;
            renderMap();
        }
    }

    private void handleSearchResponse(String body) {
        Pattern pattern = Pattern.compile("\"lat\":\"([^\"]+)\".*?\"lon\":\"([^\"]+)\".*?\"display_name\":\"([^\"]+)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            Platform.runLater(() -> statusLabel.setText("Location not found."));
            return;
        }

        double lat = Double.parseDouble(matcher.group(1));
        double lon = Double.parseDouble(matcher.group(2));
        String label = decodeJsonString(matcher.group(3));

        Platform.runLater(() -> {
            centerLat = lat;
            centerLon = lon;
            suppressTextSync = true;
            targetField.setText(label);
            suppressTextSync = false;
            statusLabel.setText("Location selected.");
            renderMap();
        });
    }

    private void reverseGeocode(double lat, double lon) {
        statusLabel.setText("Resolving address...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + lat + "&lon=" + lon))
                .header("User-Agent", USER_AGENT)
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    Pattern pattern = Pattern.compile("\"display_name\":\"([^\"]+)\"", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(body);
                    String label = matcher.find() ? decodeJsonString(matcher.group(1)) : String.format("%.6f, %.6f", lat, lon);
                    Platform.runLater(() -> {
                        suppressTextSync = true;
                        targetField.setText(label);
                        suppressTextSync = false;
                        statusLabel.setText("Location selected from map.");
                    });
                })
                .exceptionally(error -> {
                    Platform.runLater(() -> statusLabel.setText("Reverse geocoding unavailable right now."));
                    return null;
                });
    }

    private void renderMap() {
        if (canvas == null) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#dfe7f3"));
        gc.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);

        double centerPixelX = lonToPixelX(centerLon, zoom);
        double centerPixelY = latToPixelY(centerLat, zoom);
        double topLeftPixelX = centerPixelX - MAP_WIDTH / 2.0;
        double topLeftPixelY = centerPixelY - MAP_HEIGHT / 2.0;

        int startTileX = (int) Math.floor(topLeftPixelX / TILE_SIZE);
        int endTileX = (int) Math.floor((topLeftPixelX + MAP_WIDTH) / TILE_SIZE);
        int startTileY = (int) Math.floor(topLeftPixelY / TILE_SIZE);
        int endTileY = (int) Math.floor((topLeftPixelY + MAP_HEIGHT) / TILE_SIZE);

        for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
                Image tile = loadTile(tileX, tileY, zoom);
                if (tile == null) {
                    continue;
                }

                double drawX = tileX * TILE_SIZE - topLeftPixelX;
                double drawY = tileY * TILE_SIZE - topLeftPixelY;
                gc.drawImage(tile, drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
        }

        drawPin(gc, MAP_WIDTH / 2.0, MAP_HEIGHT / 2.0);
    }

    private void drawPin(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#3182ce"));
        gc.setStroke(Color.web("#205493"));
        gc.setLineWidth(2);

        gc.fillOval(x - 11, y - 34, 22, 22);
        gc.strokeOval(x - 11, y - 34, 22, 22);
        gc.fillPolygon(
                new double[]{x, x - 8, x + 8},
                new double[]{y - 2, y - 20, y - 20},
                3
        );
        gc.strokePolygon(
                new double[]{x, x - 8, x + 8},
                new double[]{y - 2, y - 20, y - 20},
                3
        );
        gc.setFill(Color.WHITE);
        gc.fillOval(x - 4, y - 27, 8, 8);
    }

    private Image loadTile(int tileX, int tileY, int zoomLevel) {
        int maxTile = 1 << zoomLevel;
        int wrappedTileX = Math.floorMod(tileX, maxTile);
        if (tileY < 0 || tileY >= maxTile) {
            return null;
        }

        String key = zoomLevel + "/" + wrappedTileX + "/" + tileY;
        Image cached = tileCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://tile.openstreetmap.org/" + zoomLevel + "/" + wrappedTileX + "/" + tileY + ".png"))
                    .header("User-Agent", USER_AGENT)
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Image image = new Image(new ByteArrayInputStream(response.body()));
                tileCache.put(key, image);
                return image;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double[] screenToLatLon(double x, double y) {
        double centerPixelX = lonToPixelX(centerLon, zoom);
        double centerPixelY = latToPixelY(centerLat, zoom);
        double pixelX = centerPixelX - MAP_WIDTH / 2.0 + x;
        double pixelY = centerPixelY - MAP_HEIGHT / 2.0 + y;
        return new double[]{pixelYToLat(pixelY, zoom), pixelXToLon(pixelX, zoom)};
    }

    private double lonToPixelX(double lon, int zoomLevel) {
        double scale = TILE_SIZE * Math.pow(2, zoomLevel);
        return (lon + 180.0) / 360.0 * scale;
    }

    private double latToPixelY(double lat, int zoomLevel) {
        double sinLat = Math.sin(Math.toRadians(lat));
        double scale = TILE_SIZE * Math.pow(2, zoomLevel);
        double y = 0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI);
        return y * scale;
    }

    private double pixelXToLon(double pixelX, int zoomLevel) {
        double scale = TILE_SIZE * Math.pow(2, zoomLevel);
        return pixelX / scale * 360.0 - 180.0;
    }

    private double pixelYToLat(double pixelY, int zoomLevel) {
        double scale = TILE_SIZE * Math.pow(2, zoomLevel);
        double n = Math.PI - 2.0 * Math.PI * pixelY / scale;
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private String decodeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\t", " ")
                .replace("&quot;", "\"");
    }
}
