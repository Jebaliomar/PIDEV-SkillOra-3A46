package tn.esprit.controllers.availabilityslot;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.entities.Notification;
import tn.esprit.entities.RendezVous;
import tn.esprit.services.AvailabilitySlotService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.RendezVousService;
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
    private static final int CURRENT_USER_ID = parseCurrentUserId();
    private static final String CURRENT_USER_ROLE = normalizeRole(System.getProperty("skillora.role", "student"));
    private static final String DEFAULT_GOOGLE_CALENDAR_ID = "yesserboubakri8@gmail.com";
    private static final String GOOGLE_CALENDAR_ID = resolveConfigValue("GOOGLE_CALENDAR_ID", "skillora.googleCalendarId", DEFAULT_GOOGLE_CALENDAR_ID);
    private static final String GOOGLE_CALENDAR_API_KEY = resolveConfigValue("GOOGLE_CALENDAR_API_KEY", "skillora.googleCalendarApiKey", null);
    private static final int CARDS_PER_ROW = 3;
    private static final int ROWS_PER_PAGE = 3;
    private static final int CARDS_PER_PAGE = CARDS_PER_ROW * ROWS_PER_PAGE;

    private static final String BTN_ACTIVE = "-fx-background-color: #1d4ed8; -fx-text-fill: #dbeafe; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 14;";
    private static final String BTN_INACTIVE = "-fx-background-color: #172746; -fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 14;";

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
            lockedSlotIds = fetchLockedSlotIds();
            if (isProfessorMode()) {
                allSlots = loaded.stream()
                        .filter(slot -> slot != null && slot.getProfessorId() != null && slot.getProfessorId().equals(CURRENT_USER_ID))
                        .collect(Collectors.toList());
            } else {
                allSlots = loaded;
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
            if (slot == null || slot.getStartAt() == null || slot.getEndAt() == null) {
                continue;
            }

            boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
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

    private String buildSlotsCalendarHtml(String eventsJson) {
        String html = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <style>
                    :root {
                      --bg: #041237;
                      --panel: #071a49;
                      --panel-2: #0b255f;
                      --text: #e2e8f0;
                      --muted: #94a3b8;
                      --border: #1f3f7c;
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
                      border: 1px solid #335ea4;
                      background: #143271;
                      color: var(--text);
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
                      color: #93c5fd;
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
                      color: #bfdbfe;
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
                      border: 1px solid rgba(255,255,255,0.16);
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
        long available = allSlots.stream().filter(slot -> !Boolean.TRUE.equals(slot.getIsBooked())).count();
        long booked = allSlots.stream().filter(slot -> Boolean.TRUE.equals(slot.getIsBooked())).count();

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
    }

    private boolean matchesFilter(AvailabilitySlot slot, SlotFilter filter) {
        boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
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
                || String.valueOf(Boolean.TRUE.equals(slot.getIsBooked())).toLowerCase().contains(q);
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
        double fallback = 330;
        if (cardsScrollPane == null) {
            return fallback;
        }

        double viewportWidth = cardsScrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0) {
            return fallback;
        }

        Insets padding = gridContainer.getPadding();
        double horizontalPadding = padding == null ? 0 : padding.getLeft() + padding.getRight();
        double available = viewportWidth - horizontalPadding - gridContainer.getHgap() * (CARDS_PER_ROW - 1);
        if (available <= 0) {
            return fallback;
        }

        return Math.max(280, Math.floor(available / CARDS_PER_ROW));
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
        boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
        boolean selected = selectedSlot != null && selectedSlot.getId() != null && selectedSlot.getId().equals(slot.getId());

        VBox card = new VBox(0);
        String border = selected ? "#3b82f6" : "#163062";
        card.setStyle("-fx-background-color: #081a42; -fx-border-color: " + border + "; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16;");
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);

        StackPane banner = new StackPane();
        banner.setMinHeight(128);
        banner.setPrefHeight(128);
        banner.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 16 16 0 0;");
        Label stamp = new Label(booked ? "UNAVAILABLE" : "AVAILABLE");
        stamp.setRotate(-12);
        stamp.setStyle(booked
                ? "-fx-text-fill: #dc2626; -fx-border-color: #dc2626; -fx-border-width: 3; -fx-font-size: 28px; -fx-font-weight: 900; -fx-padding: 5 14;"
                : "-fx-text-fill: #16a34a; -fx-border-color: #16a34a; -fx-border-width: 3; -fx-font-size: 28px; -fx-font-weight: 900; -fx-padding: 5 14;");
        banner.getChildren().add(stamp);

        VBox body = new VBox(7);
        body.setStyle("-fx-padding: 10 12 12 12;");

        HBox topRow = new HBox(8);
        Label statusBadge = new Label(booked ? "Réservé" : "Disponible");
        statusBadge.setStyle(booked
                ? "-fx-background-color: #1f2f57; -fx-text-fill: #bfdbfe; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 12;"
                : "-fx-background-color: #0f3d35; -fx-text-fill: #86efac; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label month = new Label(slot.getStartAt() == null ? "-" : slot.getStartAt().format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)));
        month.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px; -fx-font-weight: 700;");
        topRow.getChildren().addAll(statusBadge, spacer, month);

        Label dateLabel = new Label(slot.getStartAt() == null ? "-" : capitalize(slot.getStartAt().format(CARD_DATE_FORMATTER)));
        dateLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 16px; -fx-font-weight: 800;");
        dateLabel.setWrapText(true);

        Label professorLabel = new Label("Prof: " + resolveProfessorDisplayName(slot.getProfessorId()));
        professorLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-font-weight: 700;");

        Label locationLabel = new Label("Lieu: " + defaultValue(slot.getLocationLabel()));
        locationLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px;");
        locationLabel.setWrapText(true);

        Label timeLabel = new Label(formatTimeRange(slot.getStartAt(), slot.getEndAt()));
        timeLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 12px; -fx-font-weight: 700;");

        HBox actions = new HBox(8);
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        actions.getChildren().add(actionSpacer);
        if (canManageSlots()) {
            if (canEditSlot(slot)) {
                Button editBtn = new Button("Modifier");
                editBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;");
                editBtn.setOnAction(event -> {
                    selectedSlot = slot;
                    handleEdit();
                });
                actions.getChildren().add(editBtn);
            }
            Button deleteBtn = new Button("Supprimer");
            deleteBtn.setStyle("-fx-background-color: #1f2f57; -fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;");
            deleteBtn.setOnAction(event -> deleteSlot(slot));
            actions.getChildren().add(deleteBtn);
        }

        body.getChildren().addAll(topRow, dateLabel, professorLabel, locationLabel, timeLabel, actions);
        card.getChildren().addAll(banner, body);

        card.setOnMouseClicked(event -> {
            selectedSlot = slot;
            applyFiltersAndRender();
        });
        return card;
    }

    private HBox buildSlotRow(AvailabilitySlot slot, boolean lastRow) {
        boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
        boolean selected = selectedSlot != null && selectedSlot.getId() != null && selectedSlot.getId().equals(slot.getId());

        HBox row = new HBox(0);
        String background = selected ? "#0f2a55" : "#081737";
        String sideBorder = selected ? "1.4 0 1.4 3.4" : "1 0 1 0";
        String borderColor = selected ? "#2f6fed" : "#1a3665";
        String borderWidth = lastRow ? (selected ? "1.4 0 0 3.4" : "1 0 0 0") : sideBorder;
        row.setStyle("-fx-alignment: center-left; -fx-padding: 12 14; -fx-background-color: " + background + "; -fx-border-color: " + borderColor + "; -fx-border-width: " + borderWidth + ";");

        VBox startCell = new VBox(2);
        startCell.setPrefWidth(245);
        Label startDate = new Label(slot.getStartAt() == null ? "-" : slot.getStartAt().format(DATE_TIME_FORMATTER));
        startDate.setStyle("-fx-text-fill: #f3f7ff; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label startMeta = new Label(slot.getStartAt() == null ? "-" : slot.getStartAt().format(TIME_FORMATTER));
        startMeta.setStyle("-fx-text-fill: #9dc0f1; -fx-font-size: 12px; -fx-font-weight: 700;");
        startCell.getChildren().addAll(startDate, startMeta);

        VBox endCell = new VBox(2);
        endCell.setPrefWidth(245);
        Label endDate = new Label(slot.getEndAt() == null ? "-" : slot.getEndAt().format(DATE_TIME_FORMATTER));
        endDate.setStyle("-fx-text-fill: #f3f7ff; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label endMeta = new Label(slot.getEndAt() == null ? "-" : slot.getEndAt().format(TIME_FORMATTER));
        endMeta.setStyle("-fx-text-fill: #9dc0f1; -fx-font-size: 12px; -fx-font-weight: 700;");
        endCell.getChildren().addAll(endDate, endMeta);

        HBox durationCell = new HBox();
        durationCell.setPrefWidth(170);
        Label durationBadge = new Label("◷ " + formatDurationMinutes(slot.getStartAt(), slot.getEndAt()));
        durationBadge.setStyle("-fx-background-color: #1a2f54; -fx-text-fill: #dbeafe; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;");
        durationCell.getChildren().add(durationBadge);

        HBox statusCell = new HBox();
        statusCell.setPrefWidth(180);
        Label statusBadge = new Label(booked ? "● Réservé" : "● Disponible");
        statusBadge.setStyle(booked
                ? "-fx-background-color: #2b1c2a; -fx-text-fill: #fca5a5; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;"
                : "-fx-background-color: #11362e; -fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;");
        statusCell.getChildren().add(statusBadge);

        VBox locationCell = new VBox(2);
        locationCell.setPrefWidth(280);
        Label locationText = new Label(defaultValue(slot.getLocationLabel()));
        locationText.setWrapText(true);
        locationText.setStyle("-fx-text-fill: #9dc0f1; -fx-font-size: 12px;");
        Label professorText = new Label(resolveProfessorDisplayName(slot.getProfessorId()));
        professorText.setStyle("-fx-text-fill: #7ea2d6; -fx-font-size: 12px;");
        locationCell.getChildren().addAll(locationText, professorText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setPrefWidth(130);
        actions.setStyle("-fx-alignment: center-left;");
        if (canManageSlots()) {
            if (canEditSlot(slot)) {
                Button editBtn = new Button("✎");
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #60a5fa; -fx-font-size: 20px; -fx-font-weight: 900;");
                editBtn.setOnAction(event -> {
                    selectedSlot = slot;
                    handleEdit();
                });
                actions.getChildren().add(editBtn);
            }
            Button deleteBtn = new Button("🗑");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dbeafe; -fx-font-size: 18px; -fx-font-weight: 900;");
            deleteBtn.setOnAction(event -> deleteSlot(slot));
            actions.getChildren().add(deleteBtn);
        } else {
            Label noAction = new Label("—");
            noAction.setStyle("-fx-text-fill: #617ca8; -fx-font-size: 14px;");
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
            showWarning("Action not allowed", "Only professors can create slots.");
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
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professors can edit slots.");
            return;
        }
        if (selectedSlot == null) {
            showWarning("Selection required", "Select a slot " + selectionTargetLabel() + " before trying to edit.");
            return;
        }
        try {
            if (isSlotLockedByConfirmedRendezVous(selectedSlot.getId())) {
                refreshSlots();
                showWarning("Action not allowed", "Confirmed slots cannot be modified. You can only delete this slot.");
                return;
            }
        } catch (SQLException exception) {
            showError("Unable to validate slot status", exception);
            return;
        }
        if (!canEditSlot(selectedSlot)) {
            showWarning("Action not allowed", "Confirmed slots cannot be modified. You can only delete this slot.");
            return;
        }

        Optional<AvailabilitySlot> input = showSlotDialog(selectedSlot);
        if (input.isEmpty()) {
            return;
        }

        AvailabilitySlot updated = input.get();
        updated.setId(selectedSlot.getId());
        try {
            boolean ok = availabilitySlotService.update(updated);
            if (ok) {
                refreshSlots();
                statusLabel.setText("Créneau #" + selectedSlot.getId() + " mis à jour");
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
            showWarning("Action not allowed", "Only professors can delete slots.");
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
            showWarning("Action not allowed", "Only professors can delete slots.");
            return;
        }
        if (slot == null || slot.getId() == null) {
            showWarning("Selection required", "Select a slot first.");
            return;
        }

        List<RendezVous> linkedRendezVous;
        try {
            linkedRendezVous = rendezVousService.findBySlotId(slot.getId());
        } catch (SQLException exception) {
            showError("Unable to load linked rendez-vous", exception);
            return;
        }

        String message = "Delete slot #" + slot.getId() + "?";
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
                    notifyStudentSlotDeletion(rendezVous, slot.getId());
                }
            }
            boolean deleted = availabilitySlotService.delete(slot.getId());
            if (deleted) {
                Integer deletedId = slot.getId();
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

    private Optional<AvailabilitySlot> showSlotDialog(AvailabilitySlot source) {
        if (!canManageSlots()) {
            showWarning("Action not allowed", "Only professors can manage slot form.");
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
        searchLocationBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10;");

        ListView<LocationSuggestion> suggestionList = new ListView<>();
        suggestionList.setPrefHeight(120);
        suggestionList.setStyle("-fx-control-inner-background: #101f40; -fx-background-color: #101f40; -fx-border-color: #27406d; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0;");
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        Label selectedAddressValue = new Label("Aucune adresse sélectionnée");
        selectedAddressValue.setWrapText(true);
        selectedAddressValue.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10; -fx-background-color: #071533; -fx-border-color: #1b3768; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label coordinatesValue = new Label("-");
        coordinatesValue.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px;");

        WebView mapView = new WebView();
        mapView.setContextMenuEnabled(false);
        mapView.setPrefSize(760, 210);
        mapView.setMinSize(760, 210);
        mapView.setMaxSize(Double.MAX_VALUE, 210);

        StackPane mapContainer = new StackPane(mapView);
        mapContainer.setMinHeight(210);
        mapContainer.setPrefHeight(210);
        mapContainer.setStyle("-fx-background-color: #071533; -fx-border-color: #1b3768; -fx-border-radius: 12; -fx-background-radius: 12;");

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
        form.setStyle("-fx-background-color: #08142d; -fx-padding: 18;");
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
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 15px; -fx-font-weight: 700;");
        cancelBtn.setOnAction(event -> dialog.setResult(closeType));

        Button submitBtn = new Button(editMode ? "Enregistrer" : "Créer le créneau");
        submitBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 8 18;");
        submitBtn.setOnAction(event -> {
            try {
                AvailabilitySlot slot = new AvailabilitySlot();
                Integer professorId = editMode && source.getProfessorId() != null
                        ? source.getProfessorId()
                        : CURRENT_USER_ID;
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
        footer.setStyle("-fx-padding: 12 18 16 18; -fx-background-color: #050f24; -fx-border-color: #193565; -fx-border-width: 1 0 0 0;");

        Label icon = new Label("⊕");
        icon.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: #e2e8f0; -fx-font-size: 18px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 12;");
        Label title = new Label(editMode ? "Modifier un créneau" : "Ajouter un créneau");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 34px; -fx-font-weight: 800;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeIconBtn = new Button("✕");
        closeIconBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dbeafe; -fx-font-size: 18px;");
        closeIconBtn.setOnAction(event -> dialog.setResult(closeType));
        HBox header = new HBox(10, icon, title, headerSpacer, closeIconBtn);
        header.setStyle("-fx-alignment: center-left; -fx-padding: 14 18; -fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); -fx-background-radius: 16 16 0 0;");

        VBox dialogCard = new VBox(header, formScroll, footer);
        dialogCard.setStyle("-fx-background-color: #08142d; -fx-border-color: #1b3768; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;");
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
        List<RendezVous> allRendezVous = rendezVousService.getAll();
        Set<Integer> locked = new HashSet<>();
        for (RendezVous rendezVous : allRendezVous) {
            if (rendezVous == null || rendezVous.getSlotId() == null) {
                continue;
            }
            if (isConfirmedRendezVousStatus(rendezVous.getStatut())) {
                locked.add(rendezVous.getSlotId());
            }
        }
        return locked;
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
                    html, body, #map { height: 100%%; width: 100%%; margin: 0; padding: 0; background: #0b1f3f; }
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
        field.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #94a3b8; -fx-font-size: 14px;");
        return field;
    }

    private DatePicker buildDatePicker(LocalDate value) {
        DatePicker picker = new DatePicker(value);
        picker.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
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
        combo.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
        return combo;
    }

    private Label formSectionLabel(String value) {
        Label label = new Label(value);
        label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
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

    private boolean isAdminMode() {
        return CURRENT_USER_ROLE.contains("admin");
    }

    private boolean isProfessorMode() {
        return !isAdminMode() && (CURRENT_USER_ROLE.contains("prof") || CURRENT_USER_ROLE.contains("teacher"));
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
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "student" : normalized;
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
