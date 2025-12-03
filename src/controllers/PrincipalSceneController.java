package controllers;

import TDAs.BubbleSort;
import TDAs.DoublyLinkedList;
import TDAs.InsertionSort;
import TDAs.MergeSort;
import controllers.animations.BubbleSortAnimation;
import controllers.animations.InsertionSortAnimation;
import controllers.animations.MergeSortAnimation;
import data.ElectionDataLoader;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import model.Resultado;
import model.VotoCandidato;

import java.util.*;

// Overlay tick labels (category names) coordination with animation helpers
import controllers.animations.AnimationConstants;

public class PrincipalSceneController {

    @FXML private ComboBox<String> cmbMetodo;
    @FXML private ComboBox<String> cmbProvincia;
    @FXML private ComboBox<String> cmbVuelta;
    @FXML private Button btnMostrar;
    @FXML private Button btnOrdenar;
    @FXML private Pane centerPane;
    @FXML private BarChart<String, Number> barChart;

    // Datos en memoria para reordenar sin reler el archivo
    private DoublyLinkedList<Resultado> datosActuales = new DoublyLinkedList<>();

    private TDAs.DoublyLinkedList<String> originalOrderV1; // orden encabezado vuelta 1
    private TDAs.DoublyLinkedList<String> originalOrderV2; // orden encabezado vuelta 2

    // Remove old percentLabels field and related methods; keep only one overlay system
    private final java.util.List<Label> percentageOverlayLabels = new java.util.ArrayList<>(); // track labels added to plot area
    private final java.util.List<Label> valueOverlayLabels = new java.util.ArrayList<>(); // track labels added to plot area
    private final java.util.Map<XYChart.Data<String,Number>, Label> voteLabelsMap = new java.util.HashMap<>();

    // Cache for overlay tick labels so animation helpers can move them
    private final java.util.List<Text> overlayTickLabels = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        // llenar métodos
        cmbMetodo.getItems().addAll("Bubble Sort", "Insertion Sort", "Merge Sort");
        cmbMetodo.getSelectionModel().selectFirst();
        // vuelta: 1, 2, Ambas
        cmbVuelta.getItems().clear();
        cmbVuelta.getItems().addAll("1", "2"); // sin 'Ambas'
        cmbVuelta.getSelectionModel().selectFirst();
        // llenar provincias
        try {
            DoublyLinkedList<String> provincias = ElectionDataLoader.getProvinceNames();
            cmbProvincia.getItems().add("Todas");
            for (String p : provincias) cmbProvincia.getItems().add(p);
            cmbProvincia.getSelectionModel().selectFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // estilos básicos del BarChart
        barChart.setAnimated(false);
        ((CategoryAxis) barChart.getXAxis()).setAnimated(false);
        ((NumberAxis) barChart.getYAxis()).setAnimated(false);
        // Hide legend (user requested) - previously true
        barChart.setLegendVisible(false);
        barChart.getData().clear();
        // Forzar etiquetas verticales desde el inicio
        ((CategoryAxis) barChart.getXAxis()).setTickLabelRotation(90);
        // Reducir tamaño de letra de las etiquetas del eje X (índices)
        try { ((CategoryAxis) barChart.getXAxis()).setTickLabelFont(Font.font(10)); } catch (Exception ignore) {}
        try {
            originalOrderV1 = ElectionDataLoader.getCandidateNames();
            originalOrderV2 = new TDAs.DoublyLinkedList<>();
            originalOrderV2.addLast("LUISA GONZALEZ");
            originalOrderV2.addLast("DANIEL NOBOA AZIN");
        } catch (Exception ignored) {}

        // Recentrar automáticamente al cambiar el tamaño del pane
        try {
            centerPane.widthProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
                centerBarChart();
                repositionPercentageOverlayLabels();
                repositionOverlayTickLabels();
            }));
            centerPane.heightProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
                repositionPercentageOverlayLabels();
                repositionOverlayTickLabels();
            }));
            centerPane.widthProperty().addListener((obs, o, n) -> Platform.runLater(this::repositionValueOverlayLabels));
            centerPane.heightProperty().addListener((obs, o, n) -> Platform.runLater(this::repositionValueOverlayLabels));
        } catch (Exception ignore) {}
    }

    @FXML
    public void onShow() {
        String provincia = cmbProvincia.getSelectionModel().getSelectedItem();
        int vuelta = parseVuelta();
        try {
            TDAs.DoublyLinkedList<VotoCandidato> votos = (provincia != null && provincia.equalsIgnoreCase("Todas"))
                ? ElectionDataLoader.loadCandidateVotesAllProvinces(vuelta)
                : ElectionDataLoader.loadCandidateVotesForProvince(provincia, vuelta);
            // Restaurar orden original de categorías antes de renderizar
            resetAxisCategories(vuelta);
            renderBarChartCandidatos(votos);
            // Quitar restauración horizontal: mantener vertical
            ((CategoryAxis) barChart.getXAxis()).setTickLabelRotation(90);
            // After rendering, install vote labels
            Platform.runLater(() -> attachAndShowVoteLabels());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onSort() {
        String provincia = cmbProvincia.getSelectionModel().getSelectedItem();
        int vuelta = parseVuelta();
        try {
            DoublyLinkedList<VotoCandidato> votos = (provincia != null && provincia.equalsIgnoreCase("Todas"))
                ? ElectionDataLoader.loadCandidateVotesAllProvinces(vuelta)
                : ElectionDataLoader.loadCandidateVotesForProvince(provincia, vuelta);

            String metodo = cmbMetodo.getSelectionModel().getSelectedItem();
            boolean descending = false;

            // Render current data first (ensures nodes exist)
            renderBarChartCandidatos(votos);

            if (barChart.getData().isEmpty()) return;
            XYChart.Series<String, Number> series = barChart.getData().get(0);

            int n = series.getData().size();
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                XYChart.Data<String, Number> d = series.getData().get(i);
                values[i] = d.getYValue() == null ? 0 : d.getYValue().intValue();
            }

            // isSorted per algorithm
            boolean alreadySorted;
            if ("Bubble Sort".equals(metodo)) alreadySorted = BubbleSortAnimation.isSorted(values, descending);
            else if ("Insertion Sort".equals(metodo)) alreadySorted = InsertionSortAnimation.isSorted(values, descending);
            else alreadySorted = MergeSortAnimation.isSorted(values, descending);
            if (alreadySorted) return;

            // record swaps per algorithm
            int[] copy = Arrays.copyOf(values, values.length);
            List<int[]> swaps;
            if ("Bubble Sort".equals(metodo)) swaps = BubbleSortAnimation.recordBubbleSwaps(copy, descending);
            else if ("Insertion Sort".equals(metodo)) swaps = InsertionSortAnimation.recordInsertionSwaps(copy, descending);
            else swaps = MergeSortAnimation.recordFinalOrderSwaps(copy, descending);

            // Update underlying data structure using selected algorithm (no change to logic)
            if ("Bubble Sort".equals(metodo)) BubbleSort.bubbleSort(votos, descending);
            else if ("Insertion Sort".equals(metodo)) InsertionSort.insertionSort(votos, descending);
            else MergeSort.mergeSort(votos, descending);

            long totalMs = 30000L;
            int swapCount = (swaps == null) ? 0 : swaps.size();
            double stepMs = (swapCount > 0) ? (double) totalMs / (double) swapCount : 200.0;
            stepMs = Math.max(30.0, Math.min(stepMs, 8000.0));
            final Duration stepDuration = Duration.millis(stepMs);

            Platform.runLater(() -> {
                Runnable after = () -> {
                    applyCategoryOrder(votos);
                    refreshOverlaysAfterAnimation();
                };

                if ("Bubble Sort".equals(metodo)) {
                    BubbleSortAnimation.prepareTickLabelCache(barChart, centerPane, series);
                    BubbleSortAnimation.animateSwapsOnSeries(barChart, centerPane, series, swaps, stepDuration, after);
                } else if ("Insertion Sort".equals(metodo)) {
                    InsertionSortAnimation.prepareTickLabelCache(barChart, centerPane, series);
                    InsertionSortAnimation.animateSwapsOnSeries(barChart, centerPane, series, swaps, stepDuration, after);
                } else {
                    MergeSortAnimation.prepareTickLabelCache(barChart, centerPane, series);
                    MergeSortAnimation.animateSwapsOnSeries(barChart, centerPane, series, swaps, stepDuration, after);
                }
            });

            ((CategoryAxis) barChart.getXAxis()).setTickLabelRotation(90);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarDatosYMostrar() {
        String provincia = cmbProvincia.getSelectionModel().getSelectedItem();
        String vueltaStr = cmbVuelta.getSelectionModel().getSelectedItem();
        int vuelta = 1;
        try { vuelta = Integer.parseInt(vueltaStr); } catch (Exception ignore) {}
        try {
            if (provincia == null || provincia.equals("Todas")) {
                datosActuales = ElectionDataLoader.loadAllByVuelta(vuelta);
            } else {
                datosActuales = ElectionDataLoader.loadByProvince(provincia, vuelta);
            }
            renderBarChart(datosActuales);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarCandidatosProvincia() {
        String provincia = cmbProvincia.getSelectionModel().getSelectedItem();
        String vueltaStr = cmbVuelta.getSelectionModel().getSelectedItem();
        int vuelta = 1; try { vuelta = Integer.parseInt(vueltaStr); } catch(Exception ignored) {}
        if (provincia == null || provincia.equalsIgnoreCase("Todas")) {
            barChart.getData().clear();
            return; // requiere provincia específica según requerimiento
        }
        try {
            DoublyLinkedList<VotoCandidato> votos = ElectionDataLoader.loadCandidateVotesForProvince(provincia, vuelta);
            renderBarChartCandidatos(votos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderBarChart(DoublyLinkedList<Resultado> data) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Resultado r : data) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(r.getNombreProvincia(), r.getSufragantes());
            series.getData().add(d);
        }
        series.setName("V" + (data != null && !data.iterator().hasNext() ? "" : ""));
        barChart.getData().add(series);
        // Ajustar eje Y para evitar extensión extra hacia arriba
        adjustYAxisToSeries(series);
        // Centrar el gráfico horizontalmente dentro del espacio disponible
        Platform.runLater(this::centerBarChart);
    }

    private void renderBarChartCandidatos(DoublyLinkedList<VotoCandidato> votos) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vuelta " + cmbVuelta.getSelectionModel().getSelectedItem());
        for (VotoCandidato v : votos) {
            series.getData().add(new XYChart.Data<>(v.getNombreCandidato(), v.getVotos()));
        }
        barChart.getData().add(series);
        adjustYAxisToSeries(series);
        Platform.runLater(() -> {
            try { centerBarChart(); } catch (Exception ignore) {}
            try { if (barChart != null) { barChart.applyCss(); barChart.layout(); } } catch (Exception ignore) {}
            // Add absolute value overlays (black, left-aligned, collision aware)
            addValueLabels(barChart);
            buildOverlayTickLabels(series);
        });
    }

    private void buildOverlayTickLabels(XYChart.Series<String, Number> series) {
        try {
            overlayTickLabels.clear();
            // remove previous overlays from centerPane
            centerPane.getChildren().removeIf(node -> "overlay-tick".equals(node.getUserData()));
            // hide native tick labels to avoid duplicates
            ((CategoryAxis) barChart.getXAxis()).setTickLabelsVisible(false);
            Bounds chartBounds = barChart.getBoundsInParent();
            double baseY = chartBounds.getMaxY() + 52;
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node barNode = data.getNode();
                if (barNode == null) continue;
                Bounds barSceneBounds = barNode.localToScene(barNode.getBoundsInLocal());
                double sceneCenterX = barSceneBounds.getMinX() + barSceneBounds.getWidth() / 2.0;
                double localCenterX = centerPane.sceneToLocal(sceneCenterX, barSceneBounds.getMinY()).getX();
                Text label = new Text(data.getXValue());
                label.getStyleClass().add("overlay-tick-label");
                label.setFont(Font.font(10));
                label.setRotate(90);
                label.setUserData("overlay-tick");
                label.setLayoutX(localCenterX - label.getLayoutBounds().getWidth() / 2.0);
                label.setLayoutY(baseY);
                centerPane.getChildren().add(label);
                overlayTickLabels.add(label);
            }
            barChart.getProperties().put(AnimationConstants.OVERLAY_TICK_LABELS_KEY, overlayTickLabels);
            Platform.runLater(this::repositionOverlayTickLabels);
        } catch (Exception ignore) {}
    }

    private void repositionOverlayTickLabels() {
        if (barChart == null || centerPane == null || overlayTickLabels.isEmpty()) return;
        XYChart.Series<String, Number> series = barChart.getData().isEmpty() ? null : barChart.getData().get(0);
        if (series == null) return;
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        Bounds chartBounds = barChart.getBoundsInParent();
        double baseY = chartBounds.getMaxY() + 52;
        int limit = Math.min(overlayTickLabels.size(), series.getData().size());
        for (int idx = 0; idx < limit; idx++) {
            Text label = overlayTickLabels.get(idx);
            XYChart.Data<String, Number> data = series.getData().get(idx);
            Node barNode = data.getNode();
            if (barNode == null) continue;
            try {
                Bounds barSceneBounds = barNode.localToScene(barNode.getBoundsInLocal());
                double sceneCenterX = barSceneBounds.getMinX() + barSceneBounds.getWidth() / 2.0;
                double localCenterX = centerPane.sceneToLocal(sceneCenterX, barSceneBounds.getMinY()).getX();
                double textW = label.getLayoutBounds().getWidth();
                label.setLayoutX(localCenterX - textW / 2.0);
                label.setLayoutY(baseY);
            } catch (Exception ignore) {}
        }
    }

    // -------- Value overlays (absolute votes) --------
    private void addValueLabels(BarChart<String, Number> chart) {
        if (chart == null || chart.getData().isEmpty()) return;
        XYChart.Series<String, Number> series = chart.getData().get(0);
        if (series.getData().isEmpty()) return;
        // clear previous vote overlays from plot area parent
        Node plotArea = chart.lookup(".chart-plot-background");
        Pane parent = (plotArea != null && plotArea.getParent() instanceof Pane) ? (Pane) plotArea.getParent() : null;
        if (parent == null) return;
        if(!valueOverlayLabels.isEmpty()){
            try { parent.getChildren().removeAll(valueOverlayLabels); } catch(Exception ignore){}
            valueOverlayLabels.clear();
        }
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US);
        for(XYChart.Data<String, Number> d: series.getData()){
            Node bar = d.getNode(); if(bar==null || d.getYValue()==null) continue;
            String text = nf.format(d.getYValue().longValue());
            Label lbl = new Label(text);
            lbl.getStyleClass().add("bar-votes-label");
            lbl.setStyle("-fx-font-family: 'PT Serif'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: black;");
            lbl.setTextAlignment(TextAlignment.LEFT);
            try{
                Bounds barScene = bar.localToScene(bar.getBoundsInLocal());
                Bounds barLocal = chart.sceneToLocal(barScene);
                double x = barLocal.getMinX()+2;
                double y = barLocal.getMinY()-8;
                lbl.setLayoutX(x);
                lbl.setLayoutY(y);
            } catch(Exception ignore){}
            valueOverlayLabels.add(lbl);
            try { parent.getChildren().add(lbl); lbl.toFront(); } catch(Exception ignore){}
        }
        resolveValueLabelCollisions();
    }

    private void repositionValueOverlayLabels() {
        if (barChart == null || valueOverlayLabels.isEmpty()) return;
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        XYChart.Series<String, Number> series = barChart.getData().isEmpty() ? null : barChart.getData().get(0);
        if (series == null) return;
        int idx = 0;
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (idx >= valueOverlayLabels.size()) break;
            Label lbl = valueOverlayLabels.get(idx);
            Node bar = d.getNode();
            if (bar == null) { idx++; continue; }
            try {
                Bounds barScene = bar.localToScene(bar.getBoundsInLocal());
                Bounds barLocal = barChart.sceneToLocal(barScene);
                double x = barLocal.getMinX() + 2;
                double yPos = barLocal.getMinY() - 8;
                lbl.setLayoutX(x);
                lbl.setLayoutY(yPos);
            } catch (Exception ignore) {}
            idx++;
        }
        resolveValueLabelCollisions();
    }

    private void resolveValueLabelCollisions() {
        if (valueOverlayLabels.size() < 2) return;
        valueOverlayLabels.sort(java.util.Comparator.comparingDouble(Label::getLayoutX));
        for (int i = 1; i < valueOverlayLabels.size(); i++) {
            Label current = valueOverlayLabels.get(i);
            for (int j = 0; j < i; j++) {
                Label prev = valueOverlayLabels.get(j);
                if (labelsOverlap(prev, current)) {
                    current.setLayoutY(Math.max(2, current.getLayoutY() - (prev.getHeight() + 4))); // raise a bit
                    j = -1; // restart checks
                }
            }
        }
    }

    private String formatNumber(long n) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.US);
        nf.setGroupingUsed(true);
        return nf.format(n);
    }
    // -----------------------------------------------

    // ----------------- Percentage overlays -----------------
    // Single method to add percentage labels (left-aligned at bar start, black color)
    private void addPercentageLabels(BarChart<String, Number> chart) {
        if (chart == null || chart.getData().isEmpty()) return;
        XYChart.Series<String, Number> series = chart.getData().get(0);
        if (series.getData().isEmpty()) return;
        double total = 0;
        for (XYChart.Data<String, Number> d : series.getData()) {
            Number y = d.getYValue();
            if (y != null) total += y.doubleValue();
        }
        if (total <= 0) return;
        Node plotArea = chart.lookup(".chart-plot-background");
        Pane parent = (plotArea != null && plotArea.getParent() instanceof Pane) ? (Pane) plotArea.getParent() : null;
        if (parent == null) return;
        // Clear previous overlays
        if (!percentageOverlayLabels.isEmpty()) {
            try { parent.getChildren().removeAll(percentageOverlayLabels); } catch (Exception ignore) {}
            percentageOverlayLabels.clear();
        }
        for (XYChart.Data<String, Number> d : series.getData()) {
            Node bar = d.getNode();
            if (bar == null || d.getYValue() == null) continue;
            double pct = (d.getYValue().doubleValue() / total) * 100.0;
            String text = (pct < 10.0) ? String.format(java.util.Locale.US, "%.3f%%", pct) : (pct < 100.0 ? String.format(java.util.Locale.US, "%.2f%%", pct) : "100%");
            Label lbl = new Label(text);
            lbl.getStyleClass().add("bar-percentage-label");
            lbl.setStyle("-fx-font-family: 'PT Serif'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: black;");
            lbl.setTextAlignment(TextAlignment.LEFT);
            try {
                Bounds barScene = bar.localToScene(bar.getBoundsInLocal());
                Bounds barLocal = chart.sceneToLocal(barScene);
                double x = barLocal.getMinX() + 2;
                double y = barLocal.getMinY() - 8;
                lbl.setLayoutX(x);
                lbl.setLayoutY(y);
            } catch (Exception ignore) {}
            percentageOverlayLabels.add(lbl);
            try { parent.getChildren().add(lbl); lbl.toFront(); } catch (Exception ignore) {}
        }
        resolvePercentageLabelCollisions();
    }

    // Collision resolution by raising overlapping labels upward
    private void resolvePercentageLabelCollisions() {
        if (percentageOverlayLabels.size() < 2) return;
        percentageOverlayLabels.sort(java.util.Comparator.comparingDouble(Label::getLayoutX));
        for (int i = 1; i < percentageOverlayLabels.size(); i++) {
            Label current = percentageOverlayLabels.get(i);
            for (int j = 0; j < i; j++) {
                Label prev = percentageOverlayLabels.get(j);
                if (labelsOverlap(prev, current)) {
                    current.setLayoutY(Math.max(2, current.getLayoutY() - (prev.getHeight() + 4))); // raise above previous
                    j = -1; // restart checks
                }
            }
        }
    }

    private boolean labelsOverlap(Label a, Label b) {
        double ax1 = a.getLayoutX(), ax2 = ax1 + a.getWidth();
        double ay1 = a.getLayoutY(), ay2 = ay1 + a.getHeight();
        double bx1 = b.getLayoutX(), bx2 = bx1 + b.getWidth();
        double by1 = b.getLayoutY(), by2 = by1 + b.getHeight();
        return ax1 <= bx2 && bx1 <= ax2 && ay1 <= by2 && by1 <= ay2;
    }

    // Reposition overlays after resize/layout updates
    private void repositionPercentageOverlayLabels() {
        if (barChart == null || percentageOverlayLabels.isEmpty()) return;
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        XYChart.Series<String, Number> series = barChart.getData().isEmpty() ? null : barChart.getData().get(0);
        if (series == null) return;
        int idx = 0;
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (idx >= percentageOverlayLabels.size()) break;
            Label lbl = percentageOverlayLabels.get(idx);
            Node bar = d.getNode();
            if (bar == null) { idx++; continue; }
            try {
                Bounds barScene = bar.localToScene(bar.getBoundsInLocal());
                Bounds barLocal = barChart.sceneToLocal(barScene);
                double x = barLocal.getMinX() + 2;
                double y = barLocal.getMinY() - 8;
                lbl.setLayoutX(x);
                lbl.setLayoutY(y);
            } catch (Exception ignore) {}
            idx++;
        }
        resolvePercentageLabelCollisions();
    }

    // ----------------- Animation helpers -----------------
    // (implemented in per-algorithm animation classes)
    // ----------------- Non-animation helpers -----------------

    // Ajusta el eje Y (NumberAxis) para limitar la extensión superior en base a los datos de la serie
    private void adjustYAxisToSeries(XYChart.Series<String, Number> series) {
        try {
            if (series == null || series.getData() == null || series.getData().isEmpty()) return;
            double max = 0;
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d != null && d.getYValue() != null) max = Math.max(max, d.getYValue().doubleValue());
            }
            NumberAxis yAxis = (NumberAxis) barChart.getYAxis();
            if (yAxis == null) return;
            if (max <= 0) {
                yAxis.setAutoRanging(true);
                return;
            }
            // Disable auto-ranging and set tight bounds just above max to remove extra empty space
            yAxis.setAutoRanging(false);
            double upper = Math.ceil(max * 1.05); // 5% headroom
            if (upper <= max) upper = Math.ceil(max) + 1;
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(upper);
            // choose a reasonable tick unit
            double tick = Math.max(1, Math.ceil(upper / 5.0));
            yAxis.setTickUnit(tick);
        } catch (Exception ignore) {}
    }

    private int parseVuelta() {
        String vueltaStr = cmbVuelta.getSelectionModel().getSelectedItem();
        int v = 1; try { v = Integer.parseInt(vueltaStr); } catch (Exception ignored) {}
        return v;
    }

    private void resetAxisCategories(int vuelta) {
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        ObservableList<String> cats = FXCollections.observableArrayList();
        TDAs.DoublyLinkedList<String> ref = (vuelta == 2) ? originalOrderV2 : originalOrderV1;
        if (ref != null) {
            for (String c : ref) cats.add(c);
        }
        xAxis.setCategories(cats);
    }

    private void centerBarChart() {
        try {
            if (centerPane == null || barChart == null) return;
            // asegurar que el layout está actualizado
            try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
            double paneWidth = centerPane.getWidth();
            javafx.geometry.Bounds barBounds = barChart.getBoundsInParent();
            double barWidth = barBounds.getWidth();
            if (paneWidth > 0 && barWidth > 0) {
                double desiredX = (paneWidth - barWidth) / 2.0;
                // establecer layoutX para centrar el chart dentro del centerPane
                barChart.setLayoutX(desiredX);
            }
        } catch (Exception ignore) {}
    }

    // Add formatter helper
    private String formatPct(double pct){
        if(pct<10.0) return String.format(java.util.Locale.US,"%.3f%%",pct);
        if(pct<100.0) return String.format(java.util.Locale.US,"%.2f%%",pct);
        return "100%";
    }

    private void applyCategoryOrder(DoublyLinkedList<VotoCandidato> votos) {
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        ObservableList<String> cats = FXCollections.observableArrayList();
        for (VotoCandidato v : votos) cats.add(v.getNombreCandidato());
        xAxis.setCategories(cats);
        xAxis.setTickLabelRotation(90);
    }

    private void attachAndShowVoteLabels() {
        if (barChart == null || barChart.getData().isEmpty()) return;
        XYChart.Series<String, Number> series = barChart.getData().get(0);
        if (series.getData().isEmpty()) return;
        // Remove existing label nodes from centerPane
        for (Label lbl : voteLabelsMap.values()) {
            try { centerPane.getChildren().remove(lbl); } catch (Exception ignore) {}
        }
        voteLabelsMap.clear();
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US);
        for (XYChart.Data<String, Number> d : series.getData()) {
            Number y = d.getYValue();
            Node bar = d.getNode();
            if (bar == null || y == null) continue;
            Label lbl = new Label(nf.format(y.longValue()));
            lbl.setStyle("-fx-font-family:'PT Serif'; -fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:black;");
            lbl.setTextAlignment(TextAlignment.LEFT);
            lbl.setManaged(false); // overlay positioning
            centerPane.getChildren().add(lbl);
            voteLabelsMap.put(d, lbl);
        }
        // Initial positioning pass
        positionVoteLabels();
        // Schedule a second pass after a pulse to refine positioning if bars shift
        Platform.runLater(this::positionVoteLabels);
        // Add a short delayed third pass to ensure final layout
        new javafx.animation.PauseTransition(Duration.millis(120)).setOnFinished(e -> positionVoteLabels());
    }

    private void positionVoteLabels() {
        if (voteLabelsMap.isEmpty()) return;
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        for (Map.Entry<XYChart.Data<String,Number>, Label> entry : voteLabelsMap.entrySet()) {
            XYChart.Data<String,Number> data = entry.getKey();
            Label lbl = entry.getValue();
            Node bar = data.getNode();
            if (bar == null) continue;
            try {
                Bounds barScene = bar.localToScene(bar.getBoundsInLocal());
                Bounds barInPane = centerPane.sceneToLocal(barScene);
                double x = barInPane.getMinX() + 2; // start of bar + small padding
                double y = barInPane.getMinY() - lbl.getHeight() - 4; // just above bar
                lbl.relocate(x, Math.max(2, y));
            } catch (Exception ignore) {}
        }
        resolveVoteLabelCollisionsOverlay();
    }

    private void resolveVoteLabelCollisionsOverlay() {
        if (voteLabelsMap.size() < 2) return;
        java.util.List<Label> labels = new java.util.ArrayList<>(voteLabelsMap.values());
        labels.sort(java.util.Comparator.comparingDouble(Label::getLayoutX));
        for (int i = 1; i < labels.size(); i++) {
            Label current = labels.get(i);
            for (int j = 0; j < i; j++) {
                Label prev = labels.get(j);
                if (overlaps(prev, current)) {
                    current.setLayoutY(Math.max(2, current.getLayoutY() - (prev.getHeight() + 4)));
                    j = -1; // restart checks after shift
                }
            }
        }
    }

    private boolean overlaps(Label a, Label b) {
        double ax1 = a.getLayoutX(), ax2 = ax1 + a.getWidth();
        double ay1 = a.getLayoutY(), ay2 = ay1 + a.getHeight();
        double bx1 = b.getLayoutX(), bx2 = bx1 + b.getWidth();
        double by1 = b.getLayoutY(), by2 = by1 + b.getHeight();
        return ax1 <= bx2 && bx1 <= ax2 && ay1 <= by2 && by1 <= ay2;
    }

    private void refreshOverlaysAfterAnimation() {
        Platform.runLater(() -> {
            if (barChart.getData().isEmpty()) return;
            XYChart.Series<String, Number> series = barChart.getData().get(0);
            try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
            addValueLabels(barChart);
            repositionOverlayTickLabels();
            attachAndShowVoteLabels();
        });
    }
}