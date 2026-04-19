package tn.esprit.controllers.availabilityslot;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.controllers.AdminPanelController;
import tn.esprit.controllers.StudentLayoutController;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.entities.Notification;
import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;
import tn.esprit.services.AvailabilitySlotService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.RendezVousService;
import tn.esprit.services.UserService;
import tn.esprit.tools.MyConnection;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javafx.geometry.Insets;

public class AvailabilitySlotController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CALENDAR_API_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CARD_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private static final double DEFAULT_MAP_LAT = 36.82364;
    private static final double DEFAULT_MAP_LON = 10.15813;
    private static final String DEFAULT_GOOGLE_CALENDAR_ID = "yesserboubakri8@gmail.com";
    private static final String GOOGLE_CALENDAR_ID = resolveConfigValue("GOOGLE_CALENDAR_ID", "skillora.googleCalendarId", DEFAULT_GOOGLE_CALENDAR_ID);
    private static final String GOOGLE_CALENDAR_API_KEY = resolveConfigValue("GOOGLE_CALENDAR_API_KEY", "skillora.googleCalendarApiKey", null);
    private static final int CARDS_PER_ROW = 2;
    private static final int ROWS_PER_PAGE = 3;
    private static final int CARDS_PER_PAGE = CARDS_PER_ROW * ROWS_PER_PAGE;
    private static final double CARD_FALLBACK_WIDTH = 310;
    private static final double CARD_MIN_WIDTH = 255;
    private static final String STATUS_CANCELLED = "annule";

    private static final String BTN_ACTIVE = "-fx-background-color: #264fb2; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 14;";
    private static final String BTN_INACTIVE = "-fx-background-color: #e9effb; -fx-text-fill: #264fb2; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 14;";

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Pane cardsContainer;

    @FXML
    private ScrollPane cardsScrollPane;

    @FXML
    private WebView calendarWebView;

    @FXML
    private Pagination slotsPagination;

    @FXML
    private Button filterAllBtn;

    @FXML
    private Button filterAvailableBtn;

    @FXML
    private Button filterBookedBtn;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label availableCountLabel;

    @FXML
    private Label bookedCountLabel;

    @FXML
    private PieChart slotsStatusPieChart;

    @FXML
    private Label slotsPieSummaryLabel;

    @FXML
    private LineChart<String, Number> slotsTrendLineChart;

    @FXML
    private Button heroCreateBtn;

    @FXML
    private Button addSlotBtn;

    @FXML
    private Button editSlotBtn;

    @FXML
    private Button deleteSlotBtn;

    private final AvailabilitySlotService availabilitySlotService = new AvailabilitySlotService();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final NotificationService notificationService = new NotificationService();
    private final Map<Integer, String> professorNameCache = new HashMap<>();
    private List<AvailabilitySlot> allSlots = List.of();
    private List<AvailabilitySlot> filteredSlots = List.of();
    private Set<Integer> reservedSlotIds = Set.of();
    private Set<Integer> lockedSlotIds = Set.of();
    private AvailabilitySlot selectedSlot;
    private SlotFilter currentFilter = SlotFilter.ALL;

    private enum SlotFilter {
        ALL,
        AVAILABLE,
        BOOKED
    }

    @FXML
    public void initialize() {
        if (calendarWebView != null) {
            refreshCalendar();
            return;
        }

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
        if (slotsPagination != null) {
            slotsPagination.currentPageIndexProperty().addListener((obs, oldValue, newValue) -> renderCurrentPage());
        }
        if (cardsScrollPane != null) {
            cardsScrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> renderCurrentPage());
        }
        applyRolePermissions();
        setActiveFilter(SlotFilter.ALL);
        refreshSlots();
    }

    private void applyRolePermissions() {
        boolean canManage = canManageSlots();

        if (heroCreateBtn != null) {
            heroCreateBtn.setManaged(canManage);
            heroCreateBtn.setVisible(canManage);
        }
        if (addSlotBtn != null) {
            addSlotBtn.setDisable(!canManage);
            addSlotBtn.setManaged(canManage);
            addSlotBtn.setVisible(canManage);
        }
        if (editSlotBtn != null) {
            editSlotBtn.setDisable(!canManage);
            editSlotBtn.setManaged(canManage);
            editSlotBtn.setVisible(canManage);
        }
        if (deleteSlotBtn != null) {
            deleteSlotBtn.setDisable(!canManage);
            deleteSlotBtn.setManaged(canManage);
            deleteSlotBtn.setVisible(canManage);
        }
    }

    @FXML
    private void refreshSlots() {
        try {
            List<AvailabilitySlot> loaded = availabilitySlotService.getAll();
            reservedSlotIds = fetchReservedSlotIds();
            lockedSlotIds = fetchLockedSlotIds();
            synchronizeSlotBookedFlags(loaded);
            List<AvailabilitySlot> baseSlots;
            if (isAdminMode()) {
                // Admin: full visibility over all slots.
                baseSlots = loaded;
            } else {
                // Non-admin users keep future-only visibility.
                baseSlots = loaded.stream()
                        .filter(slot -> !isPastSlot(slot))
                        .collect(Collectors.toList());
            }
            if (isProfessorMode()) {
                allSlots = baseSlots.stream()
                        .filter(slot -> slot != null && slot.getProfessorId() != null && slot.getProfessorId().equals(getCurrentUserId()))
                        .collect(Collectors.toList());
            } else {
                allSlots = baseSlots;
            }
            professorNameCache.clear();
            filteredSlots = List.of();
            selectedSlot = null;
            if (slotsPagination != null) {
                slotsPagination.setCurrentPageIndex(0);
            }
            updateSummaryCards();
            applyFiltersAndRender();
        } catch (SQLException exception) {
            showError("Unable to load slots", exception);
        }
    }

    @FXML
    private void refreshCalendar() {
        if (calendarWebView == null) {
            return;
        }
        try {
            reservedSlotIds = fetchReservedSlotIds();
        } catch (SQLException ignored) {
            reservedSlotIds = Set.of();
        }
        try {
            JSONArray events = fetchGoogleCalendarEvents();
            calendarWebView.getEngine().loadContent(buildSlotsCalendarHtml(events.toString()));
            if (statusLabel != null) {
                statusLabel.setText(events.length() + " événement(s) Google Calendar chargé(s)");
            }
        } catch (Exception exception) {
            // Keep calendar usable even if Google API is temporarily unavailable.
            try {
                List<AvailabilitySlot> slots = availabilitySlotService.getAll();
                JSONArray fallbackEvents = mapSlotsToCalendarEvents(slots);
                calendarWebView.getEngine().loadContent(buildSlotsCalendarHtml(fallbackEvents.toString()));
                if (statusLabel != null) {
                    statusLabel.setText("Google indisponible, fallback local: " + slots.size() + " créneau(x)");
                }
            } catch (SQLException fallbackException) {
                showError("Unable to load slots calendar", fallbackException);
            }
        }
    }

    private JSONArray fetchGoogleCalendarEvents() throws IOException, InterruptedException {
        String apiKey = sanitizeConfigValue(GOOGLE_CALENDAR_API_KEY);
        if (apiKey == null) {
            throw new IOException("GOOGLE_CALENDAR_API_KEY is missing.");
        }

        String calendarId = sanitizeConfigValue(GOOGLE_CALENDAR_ID);
        if (calendarId == null) {
            throw new IOException("GOOGLE_CALENDAR_ID is missing.");
        }

        String encodedCalendarId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String url = "https://www.googleapis.com/calendar/v3/calendars/" + encodedCalendarId
                + "/events?singleEvents=true&orderBy=startTime&maxResults=2500&key=" + encodedApiKey;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SkillOraDesktop/1.0 (JavaFX slots calendar)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google Calendar HTTP " + response.statusCode());
        }

        JSONObject root = new JSONObject(response.body());
        JSONArray items = root.optJSONArray("items");
        return mapGoogleEventsToCalendarEvents(items == null ? new JSONArray() : items);
    }

    private JSONArray mapGoogleEventsToCalendarEvents(JSONArray items) {
        JSONArray events = new JSONArray();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String status = sanitizeConfigValue(item.optString("status", ""));
            if ("cancelled".equalsIgnoreCase(status)) {
                continue;
            }

            JSONObject start = item.optJSONObject("start");
            JSONObject end = item.optJSONObject("end");
            String startRaw = extractGoogleDateValue(start);
            String endRaw = extractGoogleDateValue(end);
            if (startRaw == null || endRaw == null) {
                continue;
            }

            String summary = sanitizeConfigValue(item.optString("summary", ""));
            String description = sanitizeConfigValue(item.optString("description", ""));
            boolean booked = inferBookedFromGoogleEvent(summary, description, status);

            JSONObject event = new JSONObject();
            event.put("id", sanitizeConfigValue(item.optString("id", String.valueOf(i))));
            event.put("title", summary == null ? (booked ? "Réservé" : "Disponible") : summary);
            event.put("start", startRaw);
            event.put("end", endRaw);
            event.put("booked", booked);
            event.put("color", booked ? "#ef4444" : "#22c55e");
            event.put("professor", "-");
            event.put("locationLabel", defaultValue(item.optString("location", null)));
            events.put(event);
        }
        return events;
    }

    private String extractGoogleDateValue(JSONObject dateObject) {
        if (dateObject == null) {
            return null;
        }
        String dateTime = sanitizeConfigValue(dateObject.optString("dateTime", null));
        if (dateTime != null) {
            return dateTime;
        }
        String date = sanitizeConfigValue(dateObject.optString("date", null));
        if (date == null) {
            return null;
        }
        return date + "T00:00:00";
    }

    private boolean inferBookedFromGoogleEvent(String summary, String description, String status) {
        String source = defaultValue(summary) + " " + defaultValue(description) + " " + defaultValue(status);
        String normalized = source.toLowerCase(Locale.ROOT);

        if (normalized.contains("dispon") || normalized.contains("available")
                || normalized.contains("free") || normalized.contains("libre")) {
            return false;
        }
        return normalized.contains("reserv")
                || normalized.contains("booked")
                || normalized.contains("busy")
                || normalized.contains("occup")
                || normalized.contains("indispon")
                || normalized.contains("unavailable")
                || normalized.contains("taken");
    }

    private JSONArray mapSlotsToCalendarEvents(List<AvailabilitySlot> slots) {
        JSONArray events = new JSONArray();
        for (AvailabilitySlot slot : slots) {
            if (slot == null || slot.getStartAt() == null || slot.getEndAt() == null || isPastSlot(slot)) {
                continue;
            }

            boolean booked = isSlotBooked(slot);
            JSONObject event = new JSONObject();
            event.put("id", slot.getId());
            event.put("title", booked ? "Réservé" : "Disponible");
            event.put("start", slot.getStartAt().format(CALENDAR_API_DATE_TIME_FORMATTER));
            event.put("end", slot.getEndAt().format(CALENDAR_API_DATE_TIME_FORMATTER));
            event.put("booked", booked);
            event.put("color", booked ? "#ef4444" : "#22c55e");
            event.put("professor", slot.getProfessorId() == null ? "-" : String.valueOf(slot.getProfessorId()));
            event.put("locationLabel", defaultValue(slot.getLocationLabel()));
            events.put(event);
        }
        return events;
    }

    private boolean isPastSlot(AvailabilitySlot slot) {
        if (slot == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (slot.getEndAt() != null) {
            return slot.getEndAt().isBefore(now);
        }
        if (slot.getStartAt() != null) {
            return slot.getStartAt().isBefore(now);
        }
        return false;
    }

    private String buildSlotsCalendarHtml(String eventsJson) {
        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <style>
                    :root {
                      --bg: #f3f4f8;
                      --panel: #ffffff;
                      --panel-2: #eef3fb;
                      --text: #1f2a44;
                      --muted: #6f84aa;
                      --border: #d2dff3;
                    }
                    * { box-sizing: border-box; }
                    html, body {
                      margin: 0;
                      padding: 0;
                      width: 100%;
                      height: 100%;
                      font-family: "Segoe UI", Arial, sans-serif;
                      background: var(--bg);
                      color: var(--text);
                    }
                    .wrap {
                      width: 100%;
                      height: 100%;
                      display: flex;
                      flex-direction: column;
                      gap: 10px;
                      padding: 12px;
                    }
                    .toolbar {
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      background: var(--panel);
                      border: 1px solid var(--border);
                      border-radius: 12px;
                      padding: 10px 12px;
                    }
                    .toolbar-left {
                      display: flex;
                      align-items: center;
                      gap: 8px;
                    }
                    .title {
                      font-size: 22px;
                      font-weight: 800;
                    }
                    .btn {
                      border: 1px solid #9fb3dd;
                      background: #edf2fc;
                      color: #264fb2;
                      border-radius: 10px;
                      padding: 6px 10px;
                      font-weight: 700;
                      cursor: pointer;
                    }
                    .legend {
                      display: flex;
                      align-items: center;
                      gap: 12px;
                      font-size: 13px;
                      color: var(--muted);
                    }
                    .dot {
                      width: 12px;
                      height: 12px;
                      border-radius: 999px;
                      display: inline-block;
                      margin-right: 6px;
                    }
                    .calendar {
                      display: grid;
                      grid-template-columns: repeat(7, minmax(0, 1fr));
                      gap: 8px;
                      height: calc(100% - 64px);
                    }
                    .weekday {
                      text-align: center;
                      padding: 8px;
                      font-size: 12px;
                      font-weight: 700;
                      color: #264fb2;
                      background: var(--panel-2);
                      border: 1px solid var(--border);
                      border-radius: 10px;
                    }
                    .day {
                      background: var(--panel);
                      border: 1px solid var(--border);
                      border-radius: 10px;
                      min-height: 110px;
                      display: flex;
                      flex-direction: column;
                      overflow: hidden;
                    }
                    .day.muted {
                      opacity: 0.45;
                    }
                    .day-number {
                      font-size: 12px;
                      font-weight: 800;
                      color: #223a6d;
                      padding: 6px 8px 4px 8px;
                    }
                    .slots {
                      display: flex;
                      flex-direction: column;
                      gap: 4px;
                      padding: 0 6px 6px 6px;
                      overflow: auto;
                    }
                    .slot {
                      border-radius: 7px;
                      padding: 4px 6px;
                      font-size: 11px;
                      font-weight: 700;
                      color: #fff;
                      border: 1px solid rgba(0,0,0,0.06);
                      white-space: nowrap;
                      overflow: hidden;
                      text-overflow: ellipsis;
                    }
                    .slot.available { background: #16a34a; }
                    .slot.booked { background: #dc2626; }
                    .empty-hint {
                      color: var(--muted);
                      font-size: 12px;
                      padding: 6px 8px;
                    }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="toolbar">
                      <div class="toolbar-left">
                        <button class="btn" id="prevBtn">◀</button>
                        <div class="title" id="monthLabel">Calendrier</div>
                        <button class="btn" id="nextBtn">▶</button>
                      </div>
                      <div class="legend">
                        <span><span class="dot" style="background:#22c55e;"></span>Disponible</span>
                        <span><span class="dot" style="background:#ef4444;"></span>Réservé</span>
                      </div>
                    </div>
                    <div class="calendar" id="calendarGrid"></div>
                  </div>

                  <script>
                    const events = __EVENTS_JSON__;
                    const monthLabel = document.getElementById('monthLabel');
                    const grid = document.getElementById('calendarGrid');
                    const prevBtn = document.getElementById('prevBtn');
                    const nextBtn = document.getElementById('nextBtn');
                    const weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
                    const locale = 'fr-FR';
                    const current = new Date();
                    current.setDate(1);

                    function toDate(value) {
                      return new Date(value);
                    }

                    function sameDay(left, right) {
                      return left.getFullYear() === right.getFullYear()
                        && left.getMonth() === right.getMonth()
                        && left.getDate() === right.getDate();
                    }

                    function formatRange(startRaw, endRaw) {
                      const start = toDate(startRaw);
                      const end = toDate(endRaw);
                      const pad = (n) => String(n).padStart(2, '0');
                      return pad(start.getHours()) + ':' + pad(start.getMinutes()) + ' - ' + pad(end.getHours()) + ':' + pad(end.getMinutes());
                    }

                    function render() {
                      grid.innerHTML = '';
                      const monthName = current.toLocaleDateString(locale, { month: 'long', year: 'numeric' });
                      monthLabel.textContent = monthName.charAt(0).toUpperCase() + monthName.slice(1);

                      weekdays.forEach((day) => {
                        const el = document.createElement('div');
                        el.className = 'weekday';
                        el.textContent = day;
                        grid.appendChild(el);
                      });

                      const firstDay = new Date(current.getFullYear(), current.getMonth(), 1);
                      const daysInMonth = new Date(current.getFullYear(), current.getMonth() + 1, 0).getDate();
                      const startOffset = (firstDay.getDay() + 6) % 7;
                      const prevMonthDays = new Date(current.getFullYear(), current.getMonth(), 0).getDate();

                      const cells = 42;
                      for (let i = 0; i < cells; i++) {
                        const dayEl = document.createElement('div');
                        dayEl.className = 'day';
                        const numEl = document.createElement('div');
                        numEl.className = 'day-number';
                        const slotsEl = document.createElement('div');
                        slotsEl.className = 'slots';

                        let date;
                        if (i < startOffset) {
                          const d = prevMonthDays - startOffset + i + 1;
                          date = new Date(current.getFullYear(), current.getMonth() - 1, d);
                          dayEl.classList.add('muted');
                          numEl.textContent = d;
                        } else if (i >= startOffset + daysInMonth) {
                          const d = i - (startOffset + daysInMonth) + 1;
                          date = new Date(current.getFullYear(), current.getMonth() + 1, d);
                          dayEl.classList.add('muted');
                          numEl.textContent = d;
                        } else {
                          const d = i - startOffset + 1;
                          date = new Date(current.getFullYear(), current.getMonth(), d);
                          numEl.textContent = d;
                        }

                        const dayEvents = events
                          .filter((event) => sameDay(toDate(event.start), date))
                          .sort((a, b) => new Date(a.start) - new Date(b.start));

                        if (dayEvents.length === 0) {
                          const hint = document.createElement('div');
                          hint.className = 'empty-hint';
                          hint.textContent = '';
                          slotsEl.appendChild(hint);
                        } else {
                          dayEvents.forEach((event) => {
                            const chip = document.createElement('div');
                            chip.className = 'slot ' + (event.booked ? 'booked' : 'available');
                            const range = formatRange(event.start, event.end);
                            chip.title = range + ' | ' + event.title + ' | Lieu: ' + (event.locationLabel || '-');
                            chip.textContent = range + ' ' + event.title;
                            slotsEl.appendChild(chip);
                          });
                        }

                        dayEl.appendChild(numEl);
                        dayEl.appendChild(slotsEl);
                        grid.appendChild(dayEl);
                      }
                    }

                    prevBtn.addEventListener('click', () => {
                      current.setMonth(current.getMonth() - 1);
                      render();
                    });
                    nextBtn.addEventListener('click', () => {
                      current.setMonth(current.getMonth() + 1);
                      render();
                    });

                    render();
                  </script>
                </body>
                </html>
                """;
        return html.replace("__EVENTS_JSON__", eventsJson);
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applyFiltersAndRender();
    }

    @FXML
    private void setFilterAll() {
        setActiveFilter(SlotFilter.ALL);
    }

    @FXML
    private void setFilterAvailable() {
        setActiveFilter(SlotFilter.AVAILABLE);
    }

    @FXML
    private void setFilterBooked() {
        setActiveFilter(SlotFilter.BOOKED);
    }

    private void setActiveFilter(SlotFilter filter) {
        currentFilter = filter;
        filterAllBtn.setStyle(filter == SlotFilter.ALL ? BTN_ACTIVE : BTN_INACTIVE);
        filterAvailableBtn.setStyle(filter == SlotFilter.AVAILABLE ? BTN_ACTIVE : BTN_INACTIVE);
        filterBookedBtn.setStyle(filter == SlotFilter.BOOKED ? BTN_ACTIVE : BTN_INACTIVE);
        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filteredSlots = allSlots.stream()
                .filter(slot -> matchesFilter(slot, currentFilter))
                .filter(slot -> matchesSearch(slot, q))
                .collect(Collectors.toList());
        if (slotsPagination != null) {
            slotsPagination.setCurrentPageIndex(0);
        }
        updatePagination();
        renderCurrentPage();
        updateSummaryCards();
        statusLabel.setText(filteredSlots.size() + " créneau(x) affiché(s)");
    }

    private void updateSummaryCards() {
        long total = allSlots.size();
        long available = allSlots.stream().filter(slot -> !isSlotBooked(slot)).count();
        long booked = allSlots.stream().filter(this::isSlotBooked).count();

        if (totalCountLabel != null) {
            totalCountLabel.setText(String.valueOf(total));
        }
        if (availableCountLabel != null) {
            availableCountLabel.setText(String.valueOf(available));
        }
        if (bookedCountLabel != null) {
            bookedCountLabel.setText(String.valueOf(booked));
        }

        filterAllBtn.setText("Tous");
        filterAvailableBtn.setText("Disponibles");
        filterBookedBtn.setText("Réservés");

        updateSlotsPieChart(total, available, booked);
        updateSlotsTrendChart();
    }

    private void updateSlotsPieChart(long total, long available, long booked) {
        if (slotsStatusPieChart == null) {
            return;
        }

        ObservableList<PieChart.Data> chartData;
        if (total <= 0) {
            chartData = FXCollections.observableArrayList(new PieChart.Data("Aucun créneau", 100));
            slotsStatusPieChart.setData(chartData);
            slotsStatusPieChart.setLegendVisible(false);
            slotsStatusPieChart.setLabelsVisible(true);
            slotsStatusPieChart.setTitle("Répartition");
            if (slotsPieSummaryLabel != null) {
                slotsPieSummaryLabel.setText("Aucun créneau futur disponible pour le moment.");
            }
            Platform.runLater(() -> applySlotsPieColors(chartData, true));
            return;
        }

        chartData = FXCollections.observableArrayList();
        if (available > 0) {
            chartData.add(new PieChart.Data("Disponibles " + formatPercent(available, total), available));
        }
        if (booked > 0) {
            chartData.add(new PieChart.Data("Réservés " + formatPercent(booked, total), booked));
        }
        if (chartData.isEmpty()) {
            chartData.add(new PieChart.Data("Aucun créneau", 100));
        }

        slotsStatusPieChart.setData(chartData);
        slotsStatusPieChart.setLegendVisible(false);
        slotsStatusPieChart.setLabelsVisible(true);
        slotsStatusPieChart.setTitle("Répartition");
        slotsStatusPieChart.setStartAngle(90);

        if (slotsPieSummaryLabel != null) {
            slotsPieSummaryLabel.setText(
                    "Total: " + total
                            + "  •  Disponibles: " + available + " (" + formatPercent(available, total) + ")"
                            + "  •  Réservés: " + booked + " (" + formatPercent(booked, total) + ")"
            );
        }

        Platform.runLater(() -> applySlotsPieColors(chartData, false));
    }

    private void applySlotsPieColors(ObservableList<PieChart.Data> chartData, boolean emptyState) {
        if (chartData == null || chartData.isEmpty()) {
            return;
        }
        if (emptyState) {
            setPieSliceColor(chartData.get(0), "#cbd5e1");
            return;
        }

        for (PieChart.Data data : chartData) {
            String name = data.getName() == null ? "" : data.getName().toLowerCase(Locale.ROOT);
            if (name.contains("dispon")) {
                setPieSliceColor(data, "#22c55e");
            } else if (name.contains("réserv") || name.contains("reserv")) {
                setPieSliceColor(data, "#ef4444");
            } else {
                setPieSliceColor(data, "#cbd5e1");
            }
        }
    }

    private void setPieSliceColor(PieChart.Data data, String color) {
        if (data == null || color == null) {
            return;
        }
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-pie-color: " + color + ";");
            }
        });
    }

    private String formatPercent(long value, long total) {
        if (total <= 0) {
            return "0.0%";
        }
        double percent = (value * 100.0) / total;
        return String.format(Locale.US, "%.1f%%", percent);
    }

    private void updateSlotsTrendChart() {
        if (slotsTrendLineChart == null) {
            return;
        }

        LocalDate startDate = LocalDate.now();
        int dayWindow = 7;
        Map<LocalDate, Long> countsByDay = new HashMap<>();
        for (AvailabilitySlot slot : allSlots) {
            if (slot == null || slot.getStartAt() == null) {
                continue;
            }
            LocalDate day = slot.getStartAt().toLocalDate();
            if (day.isBefore(startDate) || !day.isBefore(startDate.plusDays(dayWindow))) {
                continue;
            }
            countsByDay.merge(day, 1L, Long::sum);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Créneaux");
        for (int i = 0; i < dayWindow; i++) {
            LocalDate day = startDate.plusDays(i);
            String dayLabel = formatTrendDayLabel(day);
            long count = countsByDay.getOrDefault(day, 0L);
            series.getData().add(new XYChart.Data<>(dayLabel, count));
        }

        slotsTrendLineChart.setAnimated(false);
        slotsTrendLineChart.setLegendVisible(false);
        slotsTrendLineChart.setCreateSymbols(true);
        slotsTrendLineChart.setHorizontalGridLinesVisible(false);
        slotsTrendLineChart.setVerticalGridLinesVisible(false);
        slotsTrendLineChart.setStyle("-fx-background-color: transparent; -fx-default-color0: #3b82f6;");
        if (!slotsTrendLineChart.getStyleClass().contains("slots-trend-chart")) {
            slotsTrendLineChart.getStyleClass().add("slots-trend-chart");
        }

        if (slotsTrendLineChart.getXAxis() instanceof CategoryAxis xAxis) {
            xAxis.setLabel("Jours");
            xAxis.setTickLabelRotation(0);
            xAxis.setTickLabelsVisible(true);
            xAxis.setTickLabelFill(Color.web("#4e6491"));
            xAxis.setTickMarkVisible(false);
            xAxis.setTickLabelGap(7);
            xAxis.setOpacity(1.0);
        }
        if (slotsTrendLineChart.getYAxis() instanceof NumberAxis yAxis) {
            yAxis.setLabel("Créneaux");
            yAxis.setTickLabelsVisible(true);
            yAxis.setTickLabelFill(Color.web("#4e6491"));
            yAxis.setForceZeroInRange(true);
            yAxis.setMinorTickVisible(false);
            yAxis.setTickMarkVisible(false);
            yAxis.setOpacity(1.0);
        }

        slotsTrendLineChart.getData().setAll(series);
    }

    private String formatTrendDayLabel(LocalDate day) {
        if (day == null) {
            return "-";
        }
        String shortName = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
        shortName = shortName == null ? "-" : shortName.replace(".", "").trim();
        if (shortName.isEmpty()) {
            return "-";
        }
        return shortName.substring(0, 1).toUpperCase(Locale.FRENCH) + shortName.substring(1).toLowerCase(Locale.FRENCH);
    }

    private boolean matchesFilter(AvailabilitySlot slot, SlotFilter filter) {
        boolean booked = isSlotBooked(slot);
        return switch (filter) {
            case ALL -> true;
            case AVAILABLE -> !booked;
            case BOOKED -> booked;
        };
    }

    private boolean matchesSearch(AvailabilitySlot slot, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return String.valueOf(slot.getId()).toLowerCase().contains(q)
                || String.valueOf(slot.getProfessorId()).toLowerCase().contains(q)
                || resolveProfessorDisplayName(slot.getProfessorId()).toLowerCase(Locale.ROOT).contains(q)
                || defaultValue(slot.getLocationLabel()).toLowerCase().contains(q)
                || defaultValue(formatDateTime(slot.getStartAt())).toLowerCase().contains(q)
                || defaultValue(formatDateTime(slot.getEndAt())).toLowerCase().contains(q)
                || String.valueOf(isSlotBooked(slot)).toLowerCase().contains(q);
    }

    private void renderCards(List<AvailabilitySlot> slots) {
        if (cardsContainer == null) {
            return;
        }
        if (cardsContainer instanceof GridPane gridContainer) {
            renderSlotCards(gridContainer, slots);
            return;
        }
        if (cardsContainer instanceof VBox tableContainer) {
            renderSlotRows(tableContainer, slots);
        }
    }

    private void renderSlotCards(GridPane gridContainer, List<AvailabilitySlot> slots) {
        gridContainer.getChildren().clear();
        gridContainer.setMinHeight(Region.USE_PREF_SIZE);
        gridContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        if (slots.isEmpty()) {
            Label empty = new Label("Aucun créneau trouvé avec les filtres actuels.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px;");
            gridContainer.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, CARDS_PER_ROW);
            return;
        }

        double cardWidth = computeCardWidth(gridContainer);
        int index = 0;
        for (AvailabilitySlot slot : slots) {
            VBox card = buildSlotCard(slot, cardWidth);
            int row = index / CARDS_PER_ROW;
            int col = index % CARDS_PER_ROW;
            gridContainer.add(card, col, row);
            index++;
        }
    }

    private void renderSlotRows(VBox tableContainer, List<AvailabilitySlot> slots) {
        tableContainer.getChildren().clear();
        if (slots.isEmpty()) {
            Label empty = new Label("Aucun créneau trouvé avec les filtres actuels.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px; -fx-padding: 16;");
            tableContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < slots.size(); i++) {
            AvailabilitySlot slot = slots.get(i);
            tableContainer.getChildren().add(buildSlotRow(slot, i == slots.size() - 1));
        }
    }

    private double computeCardWidth(GridPane gridContainer) {
        double fallback = CARD_FALLBACK_WIDTH;
        double viewportWidth = 0;
        if (cardsScrollPane != null && cardsScrollPane.getViewportBounds() != null) {
            viewportWidth = cardsScrollPane.getViewportBounds().getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = gridContainer.getWidth();
        }
        if (viewportWidth <= 0) {
            return fallback;
        }

        Insets padding = gridContainer.getPadding();
        double horizontalPadding = padding == null ? 0 : padding.getLeft() + padding.getRight();
        double available = viewportWidth - horizontalPadding - gridContainer.getHgap() * (CARDS_PER_ROW - 1);
        if (available <= 0) {
            return fallback;
        }

        return Math.max(CARD_MIN_WIDTH, Math.floor(available / CARDS_PER_ROW));
    }

    private void updatePagination() {
        if (slotsPagination == null) {
            return;
        }
        int pageCount = Math.max(1, (int) Math.ceil((double) filteredSlots.size() / CARDS_PER_PAGE));
        slotsPagination.setPageCount(pageCount);

        if (slotsPagination.getCurrentPageIndex() >= pageCount) {
            slotsPagination.setCurrentPageIndex(pageCount - 1);
        }

        boolean showPagination = filteredSlots.size() > CARDS_PER_PAGE;
        slotsPagination.setVisible(showPagination);
        slotsPagination.setManaged(showPagination);
    }

    private void renderCurrentPage() {
        if (slotsPagination == null) {
            renderCards(filteredSlots);
            return;
        }

        int pageIndex = Math.max(0, slotsPagination.getCurrentPageIndex());
        int from = pageIndex * CARDS_PER_PAGE;
        int to = Math.min(from + CARDS_PER_PAGE, filteredSlots.size());
        if (from >= filteredSlots.size()) {
            renderCards(List.of());
            return;
        }
        renderCards(filteredSlots.subList(from, to));
    }

    @FXML
    private void goToEvents(ActionEvent event) {
        showWarning("Events", "Events page is not implemented yet.");
    }

    @FXML
    private void goToSlots(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/availability-slot/availability-slot-list.fxml", "SkillOra - Availability Slots");
    }

    @FXML
    private void goToSlotsBackOffice(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/backoffice/availability-slot-backoffice.fxml", "SkillOra - BackOffice Slots");
    }

    @FXML
    private void goToRendezVous(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/rendezvous/rendezvous-list.fxml", "SkillOra - RendezVous");
    }

    @FXML
    private void goToRendezVousBackOffice(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/backoffice/rendezvous-backoffice.fxml", "SkillOra - BackOffice RendezVous");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.clearProperty("skillora.userId");
        System.clearProperty("skillora.role");
        switchScene(event, "/tn/esprit/views/auth/login-view.fxml", "SkillOra - Login");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1160, 740));
            stage.setTitle(title);
            stage.show();
        } catch (IOException exception) {
            showError("Unable to open page", exception);
        }
    }

    private VBox buildSlotCard(AvailabilitySlot slot, double cardWidth) {
        boolean booked = isSlotBooked(slot);
        boolean selected = selectedSlot != null && selectedSlot.getId() != null && selectedSlot.getId().equals(slot.getId());

        String bgTop = booked ? "#ef4444" : "#22c55e";
        String bgBottom = booked ? "#dc2626" : "#16a34a";
        String toneDark = booked ? "#7f1d1d" : "#14532d";
        String border = selected ? "#ffffff" : "rgba(255,255,255,0.40)";
        String shadow = booked ? "rgba(185, 28, 28, 0.34)" : "rgba(21, 128, 61, 0.30)";

        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " + bgTop + ", " + bgBottom + ");"
                        + "-fx-border-color: " + border + ";"
                        + "-fx-border-width: 1.1;"
                        + "-fx-border-radius: 16;"
                        + "-fx-background-radius: 16;"
                        + "-fx-padding: 12 14 12 14;"
                        + "-fx-effect: dropshadow(gaussian, " + shadow + ", 18, 0.16, 0, 6);"
        );
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.setMinHeight(210);

        HBox header = new HBox(8);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label menuDots = new Label("⋮");
        menuDots.setStyle("-fx-text-fill: rgba(255,255,255,0.86); -fx-font-size: 16px; -fx-font-weight: 700;");
        header.getChildren().addAll(headerSpacer, menuDots);

        Label slotMeta = new Label(
                "Créneau " + defaultValue(slot.getId()) + " • "
                        + (slot.getStartAt() == null ? "-" : capitalize(slot.getStartAt().format(CARD_DATE_FORMATTER)))
        );
        slotMeta.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 11px; -fx-font-weight: 600;");

        HBox metrics = new HBox(12);

        VBox statusMetric = new VBox(2);
        statusMetric.setStyle("-fx-alignment: center-left;");
        Label statusValue = new Label(booked ? "Réservé" : "Disponible");
        statusValue.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 22px; -fx-font-weight: 800;");
        Label statusInfo = new Label(booked ? "Statut du créneau" : "Prêt pour réservation");
        statusInfo.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 11px; -fx-font-weight: 600;");
        statusMetric.getChildren().addAll(statusValue, statusInfo);

        VBox professorMetric = new VBox(2);
        professorMetric.setStyle("-fx-alignment: center-left;");
        Label professorValue = new Label(resolveProfessorDisplayName(slot.getProfessorId()));
        professorValue.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: 800;");
        professorValue.setWrapText(true);
        Label professorInfo = new Label("Professeur");
        professorInfo.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 11px; -fx-font-weight: 600;");
        professorMetric.getChildren().addAll(professorValue, professorInfo);

        VBox durationMetric = new VBox(2);
        durationMetric.setStyle("-fx-alignment: center-left;");
        Label durationValue = new Label(formatDurationMinutes(slot.getStartAt(), slot.getEndAt()));
        durationValue.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: 800;");
        Label durationInfo = new Label("Durée");
        durationInfo.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 11px; -fx-font-weight: 600;");
        durationMetric.getChildren().addAll(durationValue, durationInfo);

        HBox.setHgrow(statusMetric, Priority.ALWAYS);
        HBox.setHgrow(professorMetric, Priority.ALWAYS);
        HBox.setHgrow(durationMetric, Priority.ALWAYS);
        metrics.getChildren().addAll(statusMetric, professorMetric, durationMetric);

        Label details = new Label(formatTimeRange(slot.getStartAt(), slot.getEndAt()) + "   •   " + defaultValue(slot.getLocationLabel()));
        details.setStyle("-fx-text-fill: rgba(255,255,255,0.94); -fx-font-size: 11px; -fx-font-weight: 600;");
        details.setWrapText(true);

        HBox actions = new HBox(8);
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        actions.getChildren().add(actionSpacer);
        if (canManageSlots()) {
            if (canEditSlot(slot)) {
                Button editBtn = new Button("Modifier");
                editBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: " + toneDark + "; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 9;");
                editBtn.setOnAction(event -> {
                    event.consume();
                    editSlot(slot);
                });
                actions.getChildren().add(editBtn);
            }
            Button deleteBtn = new Button("Supprimer");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 9; -fx-border-color: #ffffff; -fx-border-radius: 9;");
            deleteBtn.setOnAction(event -> {
                event.consume();
                deleteSlot(slot);
            });
            actions.getChildren().add(deleteBtn);
        }

        card.getChildren().addAll(header, slotMeta, metrics, details, actions);

        card.setOnMouseClicked(event -> {
            selectedSlot = slot;
            applyFiltersAndRender();
        });
        return card;
    }

    private HBox buildSlotRow(AvailabilitySlot slot, boolean lastRow) {
        boolean booked = isSlotBooked(slot);
        boolean selected = selectedSlot != null && selectedSlot.getId() != null && selectedSlot.getId().equals(slot.getId());

        HBox row = new HBox(0);
        String background = selected ? "#eef4ff" : "#ffffff";
        String sideBorder = selected ? "1.4 0 1.4 3.4" : "1 0 1 0";
        String borderColor = selected ? "#2f5fc8" : "#d2dff3";
        String borderWidth = lastRow ? (selected ? "1.4 0 0 3.4" : "1 0 0 0") : sideBorder;
        row.setStyle("-fx-alignment: center-left; -fx-padding: 12 14; -fx-background-color: " + background + "; -fx-border-color: " + borderColor + "; -fx-border-width: " + borderWidth + ";");

        VBox startCell = new VBox(2);
        startCell.setPrefWidth(245);
        Label startDate = new Label(slot.getStartAt() == null ? "-" : slot.getStartAt().format(DATE_TIME_FORMATTER));
        startDate.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label startMeta = new Label(slot.getStartAt() == null ? "-" : slot.getStartAt().format(TIME_FORMATTER));
        startMeta.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 12px; -fx-font-weight: 700;");
        startCell.getChildren().addAll(startDate, startMeta);

        VBox endCell = new VBox(2);
        endCell.setPrefWidth(245);
        Label endDate = new Label(slot.getEndAt() == null ? "-" : slot.getEndAt().format(DATE_TIME_FORMATTER));
        endDate.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label endMeta = new Label(slot.getEndAt() == null ? "-" : slot.getEndAt().format(TIME_FORMATTER));
        endMeta.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 12px; -fx-font-weight: 700;");
        endCell.getChildren().addAll(endDate, endMeta);

        HBox durationCell = new HBox();
        durationCell.setPrefWidth(170);
        Label durationBadge = new Label("◷ " + formatDurationMinutes(slot.getStartAt(), slot.getEndAt()));
        durationBadge.setStyle("-fx-background-color: #edf2fc; -fx-text-fill: #264fb2; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;");
        durationCell.getChildren().add(durationBadge);

        HBox statusCell = new HBox();
        statusCell.setPrefWidth(180);
        Label statusBadge = new Label(booked ? "● Réservé" : "● Disponible");
        statusBadge.setStyle(booked
                ? "-fx-background-color: #fdecee; -fx-text-fill: #c2414b; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;"
                : "-fx-background-color: #eaf9f1; -fx-text-fill: #16855a; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;");
        statusCell.getChildren().add(statusBadge);

        VBox locationCell = new VBox(2);
        locationCell.setPrefWidth(280);
        Label locationText = new Label(defaultValue(slot.getLocationLabel()));
        locationText.setWrapText(true);
        locationText.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 12px;");
        Label professorText = new Label(resolveProfessorDisplayName(slot.getProfessorId()));
        professorText.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 12px;");
        locationCell.getChildren().addAll(locationText, professorText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setPrefWidth(130);
        actions.setStyle("-fx-alignment: center-left;");
        if (canManageSlots()) {
            if (canEditSlot(slot)) {
                Button editBtn = new Button("✎");
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #264fb2; -fx-font-size: 20px; -fx-font-weight: 900;");
                editBtn.setOnAction(event -> {
                    event.consume();
                    editSlot(slot);
                });
                actions.getChildren().add(editBtn);
            }
            Button deleteBtn = new Button("🗑");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c2414b; -fx-font-size: 18px; -fx-font-weight: 900;");
            deleteBtn.setOnAction(event -> {
                event.consume();
                deleteSlot(slot);
            });
            actions.getChildren().add(deleteBtn);
        } else {
            Label noAction = new Label("—");
            noAction.setStyle("-fx-text-fill: #7d91b6; -fx-font-size: 14px;");
            actions.getChildren().add(noAction);
        }

        row.getChildren().addAll(startCell, endCell, durationCell, statusCell, locationCell, spacer, actions);
        row.setOnMouseClicked(event -> {
            selectedSlot = slot;
            applyFiltersAndRender();
        });
        return row;
    }

    @FXML
    private void handleCreate() {
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professor/admin can create slots.");
            return;
        }
        Optional<AvailabilitySlot> input = showSlotDialog(null);
        if (input.isEmpty()) {
            return;
        }

        try {
            availabilitySlotService.add(input.get());
            refreshSlots();
            statusLabel.setText("Créneau ajouté avec succès");
        } catch (SQLException exception) {
            showError("Unable to add slot", exception);
        }
    }

    @FXML
    private void handleEdit() {
        editSlot(selectedSlot);
    }

    private void editSlot(AvailabilitySlot slot) {
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professor/admin can edit slots.");
            return;
        }
        if (slot == null || slot.getId() == null) {
            showWarning("Selection required", "Select a slot " + selectionTargetLabel() + " before trying to edit.");
            return;
        }

        AvailabilitySlot persistedSlot;
        try {
            persistedSlot = availabilitySlotService.getById(slot.getId());
            if (persistedSlot == null) {
                refreshSlots();
                showWarning("Selection required", "This slot no longer exists.");
                return;
            }
            if (isSlotLockedByConfirmedRendezVous(persistedSlot.getId())) {
                refreshSlots();
                showWarning("Action not allowed", "Confirmed slots cannot be modified. You can only delete this slot.");
                return;
            }
        } catch (SQLException exception) {
            showError("Unable to validate slot status", exception);
            return;
        }
        if (!canEditSlot(persistedSlot)) {
            showWarning("Action not allowed", "Confirmed slots cannot be modified. You can only delete this slot.");
            return;
        }

        Optional<AvailabilitySlot> input = showSlotDialog(persistedSlot);
        if (input.isEmpty()) {
            return;
        }

        AvailabilitySlot updated = input.get();
        updated.setId(persistedSlot.getId());
        updated.setProfessorId(persistedSlot.getProfessorId());
        try {
            boolean ok = availabilitySlotService.update(updated);
            if (ok) {
                refreshSlots();
                statusLabel.setText("Créneau #" + persistedSlot.getId() + " mis à jour");
            } else {
                showWarning("Update failed", "The selected slot could not be updated.");
            }
        } catch (SQLException exception) {
            showError("Unable to update slot", exception);
        }
    }

    @FXML
    private void handleDelete() {
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professor/admin can delete slots.");
            return;
        }
        if (selectedSlot == null) {
            showWarning("Selection required", "Select a slot " + selectionTargetLabel() + " before trying to delete.");
            return;
        }
        deleteSlot(selectedSlot);
    }

    private void deleteSlot(AvailabilitySlot slot) {
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professor/admin can delete slots.");
            return;
        }
        if (slot == null || slot.getId() == null) {
            showWarning("Selection required", "Select a slot first.");
            return;
        }

        AvailabilitySlot persistedSlot;
        try {
            persistedSlot = availabilitySlotService.getById(slot.getId());
        } catch (SQLException exception) {
            showError("Unable to load selected slot", exception);
            return;
        }
        if (persistedSlot == null) {
            refreshSlots();
            showWarning("Selection required", "This slot no longer exists.");
            return;
        }
        if (!canManageSpecificSlot(persistedSlot)) {
            showWarning("Action not allowed", "You can only manage your own reserved slots.");
            return;
        }

        List<RendezVous> linkedRendezVous;
        try {
            linkedRendezVous = rendezVousService.findBySlotId(persistedSlot.getId());
        } catch (SQLException exception) {
            showError("Unable to load linked rendez-vous", exception);
            return;
        }

        List<RendezVous> activeLinkedRendezVous = linkedRendezVous.stream()
                .filter(rendezVous -> rendezVous != null && rendezVous.getId() != null)
                .filter(rendezVous -> isReservedRendezVousStatus(rendezVous.getStatut()))
                .collect(Collectors.toList());

        if (!activeLinkedRendezVous.isEmpty()) {
            cancelReservedSlot(persistedSlot, activeLinkedRendezVous);
            return;
        }

        String message = "Delete slot #" + persistedSlot.getId() + "?";
        if (!linkedRendezVous.isEmpty()) {
            message += "\nThis will also delete " + linkedRendezVous.size() + " linked rendez-vous.";
        }
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.YES,
                ButtonType.CANCEL
        );
        confirmation.setHeaderText("Confirm deletion");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            int deletedRdvCount = 0;
            for (RendezVous rendezVous : linkedRendezVous) {
                if (rendezVous == null || rendezVous.getId() == null) {
                    continue;
                }
                boolean deletedRendezVous = rendezVousService.delete(rendezVous.getId());
                if (deletedRendezVous) {
                    deletedRdvCount++;
                    if (isReservedRendezVousStatus(rendezVous.getStatut())) {
                        notifyStudentSlotDeletion(rendezVous, persistedSlot.getId());
                    }
                }
            }
            boolean deleted = availabilitySlotService.delete(persistedSlot.getId());
            if (deleted) {
                Integer deletedId = persistedSlot.getId();
                selectedSlot = null;
                refreshSlots();
                statusLabel.setText("Créneau #" + deletedId + " supprimé" + (deletedRdvCount > 0 ? " + " + deletedRdvCount + " rendez-vous annulé(s)" : ""));
            } else {
                showWarning("Delete failed", "The selected slot could not be deleted.");
            }
        } catch (SQLException exception) {
            showError("Unable to delete the selected slot", exception);
        }
    }

    private void cancelReservedSlot(AvailabilitySlot slot, List<RendezVous> reservedRendezVous) {
        if (slot == null || slot.getId() == null || reservedRendezVous == null || reservedRendezVous.isEmpty()) {
            showWarning("Action not allowed", "No reserved rendez-vous found for this slot.");
            return;
        }

        boolean professorCancellation = isProfessorMode() && !isAdminMode();
        if (professorCancellation && (slot.getProfessorId() == null || !sameInteger(slot.getProfessorId(), getCurrentUserId()))) {
            showWarning("Action not allowed", "You can only cancel reservations for your own slot.");
            return;
        }

        CancellationReasonInput reasonInput = showCancellationReasonDialog(professorCancellation);
        if (!reasonInput.confirmed()) {
            return;
        }

        String cancellationReason = toNull(reasonInput.reason());
        if (professorCancellation && cancellationReason == null) {
            showWarning("Validation", "Cancellation reason is required for professor cancellation.");
            return;
        }

        String confirmationMessage = "Cancel " + reservedRendezVous.size() + " reserved rendez-vous for slot #"
                + slot.getId() + "?\nThis slot will be deleted permanently.";
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                confirmationMessage,
                ButtonType.YES,
                ButtonType.CANCEL
        );
        confirmation.setHeaderText("Confirm cancellation");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        String professorName = resolveProfessorDisplayName(slot.getProfessorId());
        int cancelledCount = 0;

        try {
            for (RendezVous rendezVous : reservedRendezVous) {
                if (rendezVous == null || rendezVous.getId() == null || !isReservedRendezVousStatus(rendezVous.getStatut())) {
                    continue;
                }

                RendezVous updated = copyRendezVous(rendezVous);
                updated.setStatut(STATUS_CANCELLED);
                updated.setRefusalReason(buildCancellationReasonForStorage(professorCancellation, cancellationReason));
                updated.setMeetingLink(null);

                boolean ok = rendezVousService.update(updated);
                if (!ok) {
                    continue;
                }

                cancelledCount++;
                notifyStudentSlotCancellation(updated, professorCancellation, professorName, cancellationReason);
            }

            if (cancelledCount == 0) {
                showWarning("Already cancelled", "All linked reservations are already cancelled/refused.");
                refreshSlots();
                return;
            }

            boolean slotDeleted = deleteSlotAfterCancellation(slot);
            if (!slotDeleted) {
                showWarning("Delete failed", "Reservations were cancelled, but the slot could not be deleted.");
                refreshSlots();
                return;
            }
            selectedSlot = null;
            refreshSlots();
            statusLabel.setText("Créneau #" + slot.getId() + " annulé (" + cancelledCount + " rendez-vous) puis supprimé définitivement.");
        } catch (SQLException exception) {
            showError("Unable to cancel reserved slot", exception);
        }
    }

    private CancellationReasonInput showCancellationReasonDialog(boolean reasonRequired) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Annuler un rendez-vous réservé");
        dialog.setHeaderText(null);
        dialog.setResizable(false);

        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmType = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, confirmType);

        Label title = new Label("Annuler le rendez-vous réservé");
        title.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 20px; -fx-font-weight: 900;");

        Label helper = new Label(reasonRequired
                ? "Le professeur doit fournir un motif d'annulation."
                : "L'admin peut fournir un motif (optionnel).");
        helper.setWrapText(true);
        helper.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 14px; -fx-font-weight: 700;");

        TextField reasonField = buildDialogTextField(
                reasonRequired ? "Motif obligatoire" : "Motif optionnel",
                ""
        );
        reasonField.setPrefWidth(560);

        Label inlineError = new Label("Le motif d'annulation est obligatoire.");
        inlineError.setWrapText(true);
        inlineError.setManaged(false);
        inlineError.setVisible(false);
        inlineError.setStyle("-fx-background-color: #fdecee; -fx-border-color: #e9a8b2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 10; -fx-text-fill: #c2414b; -fx-font-size: 13px; -fx-font-weight: 700;");

        VBox content = new VBox(12, title, helper, inlineError, reasonField);
        content.setPadding(new Insets(18));
        content.setStyle("-fx-background-color: #f7f9fd; -fx-border-color: #c6d3eb; -fx-border-radius: 14; -fx-background-radius: 14;");

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setContent(content);

        Button confirmButton = (Button) pane.lookupButton(confirmType);
        Button cancelButton = (Button) pane.lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: #e9effb; -fx-border-color: #9fb3dd; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #264fb2; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 8 14;");
        }
        if (confirmButton != null) {
            confirmButton.setStyle("-fx-background-color: linear-gradient(to bottom, #2f5fc8, #264fb2); -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 8 14;");
            confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
                String reason = toNull(reasonField.getText());
                if (reasonRequired && reason == null) {
                    inlineError.setManaged(true);
                    inlineError.setVisible(true);
                    event.consume();
                }
            });
        }

        reasonField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (inlineError.isVisible()) {
                inlineError.setManaged(false);
                inlineError.setVisible(false);
            }
        });
        Platform.runLater(reasonField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != confirmType) {
            return new CancellationReasonInput(false, null);
        }
        return new CancellationReasonInput(true, toNull(reasonField.getText()));
    }

    private boolean deleteSlotAfterCancellation(AvailabilitySlot slot) throws SQLException {
        if (slot == null || slot.getId() == null) {
            return false;
        }

        try {
            if (availabilitySlotService.delete(slot.getId())) {
                return true;
            }
        } catch (SQLException ignored) {
            // Continue with cleanup flow below in case linked rendez-vous prevent deletion.
        }

        List<RendezVous> allLinked = rendezVousService.findBySlotId(slot.getId());
        for (RendezVous rendezVous : allLinked) {
            if (rendezVous == null || rendezVous.getId() == null) {
                continue;
            }
            rendezVousService.delete(rendezVous.getId());
        }
        return availabilitySlotService.delete(slot.getId());
    }

    private RendezVous copyRendezVous(RendezVous source) {
        RendezVous copy = new RendezVous();
        copy.setId(source.getId());
        copy.setSlotId(source.getSlotId());
        copy.setStatut(source.getStatut());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setOwnerToken(source.getOwnerToken());
        copy.setStudentId(source.getStudentId());
        copy.setProfessorId(source.getProfessorId());
        copy.setCourseId(source.getCourseId());
        copy.setMeetingType(source.getMeetingType());
        copy.setMeetingLink(source.getMeetingLink());
        copy.setLocation(source.getLocation());
        copy.setLocationLabel(source.getLocationLabel());
        copy.setLocationLat(source.getLocationLat());
        copy.setLocationLng(source.getLocationLng());
        copy.setMessage(source.getMessage());
        copy.setRefusalReason(source.getRefusalReason());
        copy.setCoursePdfName(source.getCoursePdfName());
        return copy;
    }

    private String buildCancellationReasonForStorage(boolean professorCancellation, String reason) {
        if (professorCancellation) {
            return toNull(reason);
        }
        String optionalReason = toNull(reason);
        if (optionalReason == null) {
            return "Annulation par admin.";
        }
        return "Annulation par admin. Raison: " + optionalReason;
    }

    private void notifyStudentSlotCancellation(RendezVous rendezVous, boolean professorCancellation, String professorName, String reason) {
        if (rendezVous == null || rendezVous.getStudentId() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(rendezVous.getStudentId());
        notification.setTitle("Rendez-vous annulé");
        if (professorCancellation) {
            String safeProfessorName = toNull(professorName) == null ? "Unknown" : professorName;
            notification.setMessage("Sorry, Professor " + safeProfessorName + " cancelled your rendez-vous. Reason: " + reason + ".");
        } else {
            String optionalReason = toNull(reason);
            String message = "Your rendez-vous was cancelled by admin.";
            if (optionalReason != null) {
                message += " Reason: " + optionalReason + ".";
            }
            notification.setMessage(message);
        }
        notification.setLink("/rendezvous/" + defaultValue(rendezVous.getId()));
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notificationService.add(notification);
        } catch (SQLException exception) {
            showWarning("Notification", "Cancellation saved, but student notification could not be saved: " + exception.getMessage());
        }
    }

    private Optional<AvailabilitySlot> showSlotDialog(AvailabilitySlot source) {
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professor/admin can manage slot form.");
            return Optional.empty();
        }
        if (source != null) {
            try {
                if (isSlotLockedByConfirmedRendezVous(source.getId())) {
                    showWarning("Action not allowed", "Confirmed slots cannot be modified. You can only delete this slot.");
                    return Optional.empty();
                }
            } catch (SQLException exception) {
                showError("Unable to validate slot status", exception);
                return Optional.empty();
            }
            if (!canEditSlot(source)) {
                showWarning("Action not allowed", "Confirmed slots cannot be modified. You can only delete this slot.");
                return Optional.empty();
            }
        }
        boolean editMode = source != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editMode ? "Modifier un créneau" : "Ajouter un créneau");

        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        Button hiddenClose = (Button) dialog.getDialogPane().lookupButton(closeType);
        if (hiddenClose != null) {
            hiddenClose.setVisible(false);
            hiddenClose.setManaged(false);
        }

        AvailabilitySlot[] builtSlot = new AvailabilitySlot[1];
        AtomicBoolean submitted = new AtomicBoolean(false);
        LocationSuggestion[] selectedLocation = new LocationSuggestion[1];

        DatePicker startDatePicker = buildDatePicker(editMode && source.getStartAt() != null ? source.getStartAt().toLocalDate() : LocalDate.now());
        ComboBox<String> startTimeCombo = buildTimeCombo(editMode && source.getStartAt() != null ? source.getStartAt().format(TIME_FORMATTER) : null);
        DatePicker endDatePicker = buildDatePicker(editMode && source.getEndAt() != null ? source.getEndAt().toLocalDate() : LocalDate.now());
        ComboBox<String> endTimeCombo = buildTimeCombo(editMode && source.getEndAt() != null ? source.getEndAt().format(TIME_FORMATTER) : null);

        TextField locationSearchField = buildDialogTextField("Rechercher un lieu...", editMode ? defaultEmpty(source.getLocationLabel()) : "");
        Button searchLocationBtn = new Button("Rechercher");
        searchLocationBtn.setStyle("-fx-background-color: #264fb2; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10;");

        ListView<LocationSuggestion> suggestionList = new ListView<>();
        suggestionList.setPrefHeight(120);
        suggestionList.setStyle("-fx-control-inner-background: #ffffff; -fx-background-color: #ffffff; -fx-border-color: #c6d3eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #1f2a44;");
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        Label selectedAddressValue = new Label("Aucune adresse sélectionnée");
        selectedAddressValue.setWrapText(true);
        selectedAddressValue.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10; -fx-background-color: #ffffff; -fx-border-color: #c6d3eb; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label coordinatesValue = new Label("-");
        coordinatesValue.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 12px;");

        WebView mapView = new WebView();
        mapView.setContextMenuEnabled(false);
        mapView.setPrefSize(760, 210);
        mapView.setMinSize(760, 210);
        mapView.setMaxSize(Double.MAX_VALUE, 210);

        StackPane mapContainer = new StackPane(mapView);
        mapContainer.setMinHeight(210);
        mapContainer.setPrefHeight(210);
        mapContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #c6d3eb; -fx-border-radius: 12; -fx-background-radius: 12;");

        double initialLat = DEFAULT_MAP_LAT;
        double initialLon = DEFAULT_MAP_LON;

        if (editMode && source.getLocationLabel() != null && source.getLocationLat() != null && source.getLocationLng() != null) {
            selectedLocation[0] = new LocationSuggestion(source.getLocationLabel(), source.getLocationLat(), source.getLocationLng());
            selectedAddressValue.setText(source.getLocationLabel());
            coordinatesValue.setText(formatCoordinates(source.getLocationLat(), source.getLocationLng()));
            locationSearchField.setText(source.getLocationLabel());
            initialLat = source.getLocationLat();
            initialLon = source.getLocationLng();
        }

        InteractiveMapBridge mapBridge = new InteractiveMapBridge((lat, lon) -> {
            coordinatesValue.setText(formatCoordinates(lat, lon));
            setMapMarker(mapView, lat, lon);
            suggestionList.setVisible(false);
            suggestionList.setManaged(false);

            CompletableFuture.supplyAsync(() -> {
                try {
                    return reverseGeocode(lat, lon);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }).whenComplete((resolved, throwable) -> Platform.runLater(() -> {
                if (throwable != null || resolved == null) {
                    String fallback = String.format(Locale.US, "Position %.6f, %.6f", lat, lon);
                    selectedLocation[0] = new LocationSuggestion(fallback, lat, lon);
                    locationSearchField.setText(fallback);
                    selectedAddressValue.setText(fallback);
                    return;
                }

                selectedLocation[0] = resolved;
                locationSearchField.setText(resolved.displayName());
                selectedAddressValue.setText(resolved.displayName());
            }));
        });
        initializeInteractiveMap(mapView, initialLat, initialLon, mapBridge);

        HBox locationSearchRow = new HBox(8, locationSearchField, searchLocationBtn);
        HBox.setHgrow(locationSearchField, Priority.ALWAYS);

        searchLocationBtn.setOnAction(event -> launchLocationSearch(
                locationSearchField.getText(),
                suggestionList,
                searchLocationBtn
        ));
        locationSearchField.setOnAction(event -> launchLocationSearch(
                locationSearchField.getText(),
                suggestionList,
                searchLocationBtn
        ));

        suggestionList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            selectedLocation[0] = newValue;
            locationSearchField.setText(newValue.displayName());
            selectedAddressValue.setText(newValue.displayName());
            coordinatesValue.setText(formatCoordinates(newValue.lat(), newValue.lon()));
            setMapMarker(mapView, newValue.lat(), newValue.lon());
            suggestionList.setVisible(false);
            suggestionList.setManaged(false);
        });

        VBox form = new VBox(9);
        form.setStyle("-fx-background-color: #f7f9fd; -fx-padding: 18;");
        form.getChildren().addAll(
                formSectionLabel("Date de début"), startDatePicker,
                formSectionLabel("Heure de début"), startTimeCombo,
                formSectionLabel("Date de fin"), endDatePicker,
                formSectionLabel("Heure de fin"), endTimeCombo,
                formSectionLabel("Lieu (optionnel)"), locationSearchRow,
                suggestionList,
                formSectionLabel("Adresse sélectionnée"), selectedAddressValue,
                formSectionLabel("Coordonnées"), coordinatesValue,
                mapContainer
        );

        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setPrefViewportHeight(520);
        formScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5f739a; -fx-font-size: 15px; -fx-font-weight: 700;");
        cancelBtn.setOnAction(event -> dialog.setResult(closeType));

        Button submitBtn = new Button(editMode ? "Enregistrer" : "Créer le créneau");
        submitBtn.setStyle("-fx-background-color: #264fb2; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 8 18;");
        submitBtn.setOnAction(event -> {
            try {
                AvailabilitySlot slot = new AvailabilitySlot();
                Integer professorId = editMode && source.getProfessorId() != null
                        ? source.getProfessorId()
                        : getCurrentUserId();
                slot.setProfessorId(professorId);
                slot.setStartAt(parseDateTimeRequired(startDatePicker, startTimeCombo, "Start"));
                slot.setEndAt(parseDateTimeRequired(endDatePicker, endTimeCombo, "End"));
                if (!slot.getEndAt().isAfter(slot.getStartAt())) {
                    throw new IllegalArgumentException("End date/time must be after start date/time.");
                }
                if (slot.getStartAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
                    throw new IllegalArgumentException("Start date/time must be now or in the future.");
                }
                long durationMinutes = Duration.between(slot.getStartAt(), slot.getEndAt()).toMinutes();
                if (durationMinutes < 30) {
                    throw new IllegalArgumentException("A slot must be at least 30 minutes.");
                }
                if (durationMinutes > 240) {
                    throw new IllegalArgumentException("A slot cannot exceed 4 hours.");
                }

                slot.setIsBooked(editMode && source.getIsBooked() != null ? source.getIsBooked() : false);

                if (selectedLocation[0] != null) {
                    slot.setLocationLabel(selectedLocation[0].displayName());
                    slot.setLocationLat((float) selectedLocation[0].lat());
                    slot.setLocationLng((float) selectedLocation[0].lon());
                } else {
                    slot.setLocationLabel(toNull(locationSearchField.getText()));
                    slot.setLocationLat(null);
                    slot.setLocationLng(null);
                }
                if (slot.getLocationLabel() != null && slot.getLocationLabel().length() > 255) {
                    throw new IllegalArgumentException("Location is too long (max 255 characters).");
                }

                slot.setCreatedAt(editMode && source.getCreatedAt() != null ? source.getCreatedAt() : LocalDateTime.now());
                builtSlot[0] = slot;
                submitted.set(true);
                dialog.setResult(closeType);
            } catch (IllegalArgumentException ex) {
                showWarning("Validation error", ex.getMessage());
            }
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(10, footerSpacer, cancelBtn, submitBtn);
        footer.setStyle("-fx-padding: 12 18 16 18; -fx-background-color: #ffffff; -fx-border-color: #d2dff3; -fx-border-width: 1 0 0 0;");

        Label icon = new Label("⊕");
        icon.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 12;");
        Label title = new Label(editMode ? "Modifier un créneau" : "Ajouter un créneau");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 34px; -fx-font-weight: 800;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeIconBtn = new Button("✕");
        closeIconBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dbe8ff; -fx-font-size: 18px;");
        closeIconBtn.setOnAction(event -> dialog.setResult(closeType));
        HBox header = new HBox(10, icon, title, headerSpacer, closeIconBtn);
        header.setStyle("-fx-alignment: center-left; -fx-padding: 14 18; -fx-background-color: linear-gradient(to right, #2f5fc8, #264fb2); -fx-background-radius: 16 16 0 0;");

        VBox dialogCard = new VBox(header, formScroll, footer);
        dialogCard.setStyle("-fx-background-color: #f7f9fd; -fx-border-color: #c6d3eb; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;");
        dialogCard.setPrefWidth(840);
        dialogCard.setMaxWidth(840);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setContent(dialogCard);

        Node buttonBar = pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
        }

        dialog.showAndWait();
        if (!submitted.get() || builtSlot[0] == null) {
            return Optional.empty();
        }
        return Optional.of(builtSlot[0]);
    }

    private Set<Integer> fetchLockedSlotIds() throws SQLException {
        return extractSlotIdsByStatus(rendezVousService.getAll(), this::isConfirmedRendezVousStatus);
    }

    private Set<Integer> fetchReservedSlotIds() throws SQLException {
        return extractSlotIdsByStatus(rendezVousService.getAll(), this::isReservedRendezVousStatus);
    }

    private Set<Integer> extractSlotIdsByStatus(List<RendezVous> rendezVousList, java.util.function.Predicate<String> statusMatcher) {
        Set<Integer> slotIds = new HashSet<>();
        if (rendezVousList == null || statusMatcher == null) {
            return slotIds;
        }
        for (RendezVous rendezVous : rendezVousList) {
            if (rendezVous == null || rendezVous.getSlotId() == null) {
                continue;
            }
            if (statusMatcher.test(rendezVous.getStatut())) {
                slotIds.add(rendezVous.getSlotId());
            }
        }
        return slotIds;
    }

    private void synchronizeSlotBookedFlags(List<AvailabilitySlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        for (AvailabilitySlot slot : slots) {
            if (slot == null || slot.getId() == null) {
                continue;
            }
            boolean shouldBeBooked = reservedSlotIds.contains(slot.getId());
            boolean currentlyBooked = Boolean.TRUE.equals(slot.getIsBooked());
            if (currentlyBooked != shouldBeBooked) {
                slot.setIsBooked(shouldBeBooked);
                try {
                    availabilitySlotService.updateBookedStatus(slot.getId(), shouldBeBooked);
                } catch (SQLException ignored) {
                    // Keep UI consistent even if DB sync fails temporarily.
                }
            }
        }
    }

    private boolean canEditSlot(AvailabilitySlot slot) {
        if (!canManageSlots() || slot == null || slot.getId() == null) {
            return false;
        }
        return !lockedSlotIds.contains(slot.getId());
    }

    private boolean isConfirmedRendezVousStatus(String rawStatus) {
        String value = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.ROOT);
        return value.contains("confirm");
    }

    private boolean isRefusedRendezVousStatus(String rawStatus) {
        String value = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.ROOT);
        return value.contains("refus") || value.contains("rejet");
    }

    private boolean isCancelledRendezVousStatus(String rawStatus) {
        String value = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.ROOT);
        return value.contains("annul") || value.contains("cancel");
    }

    private boolean isReservedRendezVousStatus(String rawStatus) {
        return !isRefusedRendezVousStatus(rawStatus) && !isCancelledRendezVousStatus(rawStatus);
    }

    private boolean isSlotBooked(AvailabilitySlot slot) {
        if (slot == null || slot.getId() == null) {
            return false;
        }
        return reservedSlotIds.contains(slot.getId());
    }

    private boolean isSlotLockedByConfirmedRendezVous(Integer slotId) throws SQLException {
        if (slotId == null) {
            return false;
        }
        List<RendezVous> linked = rendezVousService.findBySlotId(slotId);
        for (RendezVous rendezVous : linked) {
            if (rendezVous != null && isConfirmedRendezVousStatus(rendezVous.getStatut())) {
                return true;
            }
        }
        return false;
    }

    private void notifyStudentSlotDeletion(RendezVous rendezVous, Integer slotId) {
        if (rendezVous == null || rendezVous.getStudentId() == null || rendezVous.getId() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(rendezVous.getStudentId());
        notification.setTitle("Rendez-vous annulé");
        notification.setMessage("Le professeur a supprimé le créneau #" + defaultValue(slotId)
                + ". Votre rendez-vous #" + rendezVous.getId() + " a été supprimé.");
        notification.setLink("/rendezvous");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notificationService.add(notification);
        } catch (SQLException exception) {
            showWarning("Notification", "Slot deleted, but student notification could not be saved: " + exception.getMessage());
        }
    }

    private void launchLocationSearch(String query, ListView<LocationSuggestion> suggestionList, Button searchButton) {
        String value = query == null ? "" : query.trim();
        if (value.length() < 3) {
            showWarning("Recherche", "Tapez au moins 3 caractères pour rechercher un lieu.");
            return;
        }

        searchButton.setDisable(true);
        suggestionList.getItems().clear();
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        CompletableFuture.supplyAsync(() -> {
            try {
                return fetchLocationSuggestions(value, 8);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((results, throwable) -> Platform.runLater(() -> {
            searchButton.setDisable(false);
            if (throwable != null) {
                String details = throwable.getCause() == null ? throwable.getMessage() : throwable.getCause().getMessage();
                showWarning("API Nominatim", "Unable to fetch location suggestions: " + details);
                return;
            }

            if (results.isEmpty()) {
                showWarning("Recherche", "Aucun résultat trouvé pour ce lieu.");
                return;
            }

            suggestionList.setItems(FXCollections.observableArrayList(results));
            suggestionList.setVisible(true);
            suggestionList.setManaged(true);
        }));
    }

    private List<LocationSuggestion> fetchLocationSuggestions(String query, int limit) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit="
                + limit + "&q=" + encoded;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SkillOraDesktop/1.0 (JavaFX availability module)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Nominatim HTTP " + response.statusCode());
        }

        JSONArray jsonArray = new JSONArray(response.body());
        List<LocationSuggestion> suggestions = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            String displayName = item.optString("display_name", "");
            String latRaw = item.optString("lat", "");
            String lonRaw = item.optString("lon", "");
            if (displayName.isBlank() || latRaw.isBlank() || lonRaw.isBlank()) {
                continue;
            }
            try {
                double lat = Double.parseDouble(latRaw);
                double lon = Double.parseDouble(lonRaw);
                suggestions.add(new LocationSuggestion(displayName, lat, lon));
            } catch (NumberFormatException ignored) {
                // Skip malformed coordinates.
            }
        }
        return suggestions;
    }

    private void initializeInteractiveMap(WebView mapView, double lat, double lon, InteractiveMapBridge bridge) {
        mapView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            JSObject window = (JSObject) mapView.getEngine().executeScript("window");
            window.setMember("javaBridge", bridge);
        });
        mapView.getEngine().loadContent(buildInteractiveMapHtml(lat, lon));
    }

    private void setMapMarker(WebView mapView, double lat, double lon) {
        mapView.getEngine().executeScript(String.format(
                Locale.US,
                "if(window.setMapMarker){window.setMapMarker(%.6f, %.6f);}",
                lat,
                lon
        ));
    }

    private String buildInteractiveMapHtml(double lat, double lon) {
        return String.format(Locale.US, """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <style>
                    html, body, #map { height: 100%%; width: 100%%; margin: 0; padding: 0; background: #f3f4f8; }
                    .leaflet-container { font-family: Arial, sans-serif; }
                  </style>
                </head>
                <body>
                  <div id="map"></div>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <script>
                    const map = L.map('map').setView([%.6f, %.6f], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      maxZoom: 19,
                      attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);

                    const marker = L.marker([%.6f, %.6f]).addTo(map);

                    window.setMapMarker = function(lat, lon) {
                      marker.setLatLng([lat, lon]);
                      map.setView([lat, lon], map.getZoom());
                    };

                    map.on('click', function(e) {
                      const lat = e.latlng.lat;
                      const lon = e.latlng.lng;
                      marker.setLatLng([lat, lon]);
                      if (window.javaBridge && typeof window.javaBridge.onMapClicked === 'function') {
                        window.javaBridge.onMapClicked(lat, lon);
                      }
                    });
                  </script>
                </body>
                </html>
                """, lat, lon, lat, lon);
    }

    private LocationSuggestion reverseGeocode(double lat, double lon) throws IOException, InterruptedException {
        String url = String.format(
                Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f",
                lat,
                lon
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SkillOraDesktop/1.0 (JavaFX availability module)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Nominatim reverse HTTP " + response.statusCode());
        }

        JSONObject json = new JSONObject(response.body());
        String displayName = json.optString("display_name", "").trim();
        if (displayName.isBlank()) {
            return null;
        }
        return new LocationSuggestion(displayName, lat, lon);
    }

    private TextField buildDialogTextField(String prompt, String value) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b8c8e7; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #1f2a44; -fx-prompt-text-fill: #8aa0c6; -fx-font-size: 14px;");
        return field;
    }

    private DatePicker buildDatePicker(LocalDate value) {
        DatePicker picker = new DatePicker(value);
        picker.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b8c8e7; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #1f2a44; -fx-font-size: 14px;");
        return picker;
    }

    private ComboBox<String> buildTimeCombo(String selected) {
        ComboBox<String> combo = new ComboBox<>();
        List<String> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            times.add(String.format(Locale.US, "%02d:00", hour));
            times.add(String.format(Locale.US, "%02d:30", hour));
        }
        combo.getItems().setAll(times);
        combo.setPromptText("--:--");
        if (selected != null && !selected.isBlank()) {
            combo.setValue(selected);
        }
        combo.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b8c8e7; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #1f2a44; -fx-font-size: 14px;");
        return combo;
    }

    private Label formSectionLabel(String value) {
        Label label = new Label(value);
        label.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 700;");
        return label;
    }

    private Integer parseIntRequired(String raw, String fieldName) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    private LocalDateTime parseDateTimeRequired(DatePicker datePicker, ComboBox<String> timeCombo, String fieldName) {
        if (datePicker.getValue() == null) {
            throw new IllegalArgumentException(fieldName + " date is required.");
        }
        String timeValue = timeCombo.getValue() == null ? "" : timeCombo.getValue().trim();
        if (timeValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " time is required.");
        }
        try {
            LocalTime localTime = LocalTime.parse(timeValue, TIME_FORMATTER);
            return LocalDateTime.of(datePicker.getValue(), localTime);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " time format must be HH:mm.");
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            return "-";
        }
        return startAt.format(TIME_FORMATTER) + " - " + endAt.format(TIME_FORMATTER);
    }

    private String formatDurationMinutes(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            return "-";
        }
        long minutes = Math.max(0, Duration.between(startAt, endAt).toMinutes());
        return minutes + " min";
    }

    private String formatCoordinates(double lat, double lon) {
        return String.format(Locale.US, "Lat: %.6f  |  Lng: %.6f", lat, lon);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1);
    }

    private String toNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultEmpty(String value) {
        return value == null ? "" : value;
    }

    private String defaultValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String selectionTargetLabel() {
        return cardsContainer instanceof VBox ? "row" : "card";
    }

    private String resolveProfessorDisplayName(Integer professorId) {
        if (professorId == null) {
            return "-";
        }
        String cached = professorNameCache.get(professorId);
        if (cached != null) {
            return cached;
        }
        String resolved = loadProfessorDisplayName(professorId);
        professorNameCache.put(professorId, resolved);
        return resolved;
    }

    private String loadProfessorDisplayName(Integer professorId) {
        if (professorId == null) {
            return "-";
        }
        String fallback = "Prof #" + professorId;
        try {
            Connection connection = MyConnection.getInstance().getConnection();
            String userTable = findExistingTable(connection, "user", "users");
            if (userTable == null) {
                return fallback;
            }

            String firstNameColumn = findExistingColumn(connection, userTable, "first_name", "firstname");
            String lastNameColumn = findExistingColumn(connection, userTable, "last_name", "lastname");
            String usernameColumn = findExistingColumn(connection, userTable, "username", "user_name", "login");
            String emailColumn = findExistingColumn(connection, userTable, "email", "mail");
            if (firstNameColumn == null && lastNameColumn == null && usernameColumn == null && emailColumn == null) {
                return fallback;
            }

            List<String> projections = new ArrayList<>();
            if (firstNameColumn != null) {
                projections.add("`" + firstNameColumn + "` AS first_name");
            }
            if (lastNameColumn != null) {
                projections.add("`" + lastNameColumn + "` AS last_name");
            }
            if (usernameColumn != null) {
                projections.add("`" + usernameColumn + "` AS username");
            }
            if (emailColumn != null) {
                projections.add("`" + emailColumn + "` AS email");
            }
            if (projections.isEmpty()) {
                return fallback;
            }

            String sql = "SELECT " + String.join(", ", projections) + " FROM `" + userTable + "` WHERE `id` = ? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, professorId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return fallback;
                    }

                    String firstName = toNull(firstNameColumn == null ? null : resultSet.getString("first_name"));
                    String lastName = toNull(lastNameColumn == null ? null : resultSet.getString("last_name"));
                    String username = toNull(usernameColumn == null ? null : resultSet.getString("username"));
                    String email = toNull(emailColumn == null ? null : resultSet.getString("email"));

                    String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
                    if (!fullName.isEmpty()) {
                        return fullName;
                    }
                    if (username != null) {
                        return username;
                    }
                    if (email != null) {
                        return email;
                    }
                }
            }
        } catch (SQLException ignored) {
            return fallback;
        }
        return fallback;
    }

    private String findExistingTable(Connection connection, String... candidates) throws SQLException {
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (tableExists(connection, catalog, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tableExists(Connection connection, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private String findExistingColumn(Connection connection, String tableName, String... candidates) throws SQLException {
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (columnExists(connection, catalog, tableName, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean columnExists(Connection connection, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName, columnName)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            return resultSet.next();
        }
    }

    private boolean canManageSlots() {
        return isAdminMode() || isProfessorMode();
    }

    private boolean canManageSpecificSlot(AvailabilitySlot slot) {
        if (!canManageSlots() || slot == null) {
            return false;
        }
        if (isAdminMode()) {
            return true;
        }
        return isProfessorMode() && slot.getProfessorId() != null && sameInteger(slot.getProfessorId(), getCurrentUserId());
    }

    private boolean sameInteger(Integer left, Integer right) {
        return left != null && right != null && left.intValue() == right.intValue();
    }

    private boolean isAdminMode() {
        return getCurrentUserRole().contains("admin");
    }

    private boolean isProfessorMode() {
        String role = getCurrentUserRole();
        return !isAdminMode() && (role.contains("prof") || role.contains("teacher"));
    }

    private static String resolveConfigValue(String envName, String propertyName, String fallback) {
        String fromProperty = sanitizeConfigValue(System.getProperty(propertyName));
        if (fromProperty != null) {
            return fromProperty;
        }

        String fromEnv = sanitizeConfigValue(System.getenv(envName));
        if (fromEnv != null) {
            return fromEnv;
        }

        String fromDotEnv = readDotEnvValue(envName);
        if (fromDotEnv != null) {
            return fromDotEnv;
        }

        return sanitizeConfigValue(fallback);
    }

    private static String readDotEnvValue(String key) {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String value = sanitizeConfigValue(line);
                if (value == null || value.startsWith("#")) {
                    continue;
                }
                int eqIndex = value.indexOf('=');
                if (eqIndex <= 0) {
                    continue;
                }
                String name = sanitizeConfigValue(value.substring(0, eqIndex));
                if (!key.equals(name)) {
                    continue;
                }
                String raw = sanitizeConfigValue(value.substring(eqIndex + 1));
                if (raw == null) {
                    return null;
                }
                if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                    raw = raw.substring(1, raw.length() - 1);
                }
                return sanitizeConfigValue(raw);
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String sanitizeConfigValue(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int parseCurrentUserId() {
        String raw = System.getProperty("skillora.userId", "20");
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 20;
        }
    }

    private static String normalizeRole(String raw) {
        if (raw == null) {
            return "student";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace("role_", "");
        if (normalized.contains("admin")) {
            return "admin";
        }
        if (normalized.contains("prof") || normalized.contains("teacher") || normalized.contains("instructor")) {
            return "professor";
        }
        return "student";
    }

    private int getCurrentUserId() {
        String raw = System.getProperty("skillora.userId");
        if (raw != null) {
            try {
                int parsed = Integer.parseInt(raw.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Fallback to layout-backed session.
            }
        }

        Integer fromLayout = resolveUserIdFromLayoutSession();
        if (fromLayout != null && fromLayout > 0) {
            System.setProperty("skillora.userId", String.valueOf(fromLayout));
            return fromLayout;
        }

        return parseCurrentUserId();
    }

    private String getCurrentUserRole() {
        String raw = System.getProperty("skillora.role");
        if (raw != null && !raw.trim().isEmpty()) {
            return normalizeRole(raw);
        }

        Integer userId = resolveUserIdFromLayoutSession();
        if (userId != null && userId > 0) {
            try {
                String resolved = normalizeRole(new UserService().getUserRole(userId));
                System.setProperty("skillora.role", resolved);
                return resolved;
            } catch (SQLException ignored) {
                // Keep fallback below.
            }
        }

        return "student";
    }

    private Integer resolveUserIdFromLayoutSession() {
        User adminUser = AdminPanelController.getCurrentUser();
        if (adminUser != null && adminUser.getId() != null) {
            return adminUser.getId();
        }
        User studentUser = StudentLayoutController.getCurrentUser();
        if (studentUser != null && studentUser.getId() != null) {
            return studentUser.getId();
        }
        return null;
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface MapSelectionHandler {
        void onLocationSelected(double lat, double lon);
    }

    private record CancellationReasonInput(boolean confirmed, String reason) {
    }

    public static class InteractiveMapBridge {
        private final MapSelectionHandler handler;

        InteractiveMapBridge(MapSelectionHandler handler) {
            this.handler = handler;
        }

        public void onMapClicked(double lat, double lon) {
            if (handler != null) {
                Platform.runLater(() -> handler.onLocationSelected(lat, lon));
            }
        }
    }

    private record LocationSuggestion(String displayName, double lat, double lon) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
