package controllers;

import TDAs.BubbleSort;
import TDAs.DoublyLinkedList;
import TDAs.InsertionSort;
import TDAs.MergeSort;
import controllers.animations.SortAnimationSkeleton;
import data.ElectionDataLoader;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.util.Duration;
import model.Resultado;
import model.VotoCandidato;

import java.util.*;

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
        barChart.setAnimated(true);
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
            centerPane.widthProperty().addListener((obs, o, n) -> Platform.runLater(this::centerBarChart));
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

            // Render the current (unsorted) data first so we have nodes to animate
            renderBarChartCandidatos(votos);

            // Grab the displayed series (assumes single series for candidates)
            if (barChart.getData().isEmpty()) return;
            XYChart.Series<String, Number> series = barChart.getData().get(0);

            // Build arrays representing current values and names in display order
            int n = series.getData().size();
            int[] values = new int[n];
            String[] names = new String[n];
            for (int i = 0; i < n; i++) {
                XYChart.Data<String, Number> d = series.getData().get(i);
                values[i] = d.getYValue() == null ? 0 : d.getYValue().intValue();
                names[i] = d.getXValue();
            }

            // If the displayed values are already sorted according to the desired order, do nothing
            if (SortAnimationSkeleton.isSorted(values, descending)) {
                return; // nada ocurre si ya está ordenado
            }

            // Record swap operations according to chosen algorithm (on a copy)
            int[] copy = Arrays.copyOf(values, values.length);
            List<int[]> swaps;
            if ("Bubble Sort".equals(metodo)) swaps = SortAnimationSkeleton.recordBubbleSwaps(copy, descending);
            else if ("Insertion Sort".equals(metodo)) swaps = SortAnimationSkeleton.recordInsertionSwaps(copy, descending);
            else swaps = SortAnimationSkeleton.recordFinalOrderSwaps(copy, descending); // for Merge we compute swaps to reach final order

            // Run the real sort to update underlying data model (keeps other logic unchanged)
            if ("Bubble Sort".equals(metodo)) BubbleSort.bubbleSort(votos, descending);
            else if ("Insertion Sort".equals(metodo)) InsertionSort.insertionSort(votos, descending);
            else MergeSort.mergeSort(votos, descending);

            // Compute per-swap duration so the whole animation lasts ~30 seconds (30000 ms)
            long totalMs = 30000L;
            int swapCount = (swaps == null) ? 0 : swaps.size();
            double stepMs = 200; // default fallback
            if (swapCount > 0) {
                stepMs = (double) totalMs / (double) swapCount;
            }
            // clamp to reasonable bounds: at least 30ms, at most 8000ms per swap
            stepMs = Math.max(30.0, Math.min(stepMs, 8000.0));

            // Animate the recorded swaps on the displayed series. Use Platform.runLater to ensure nodes are ready
            final Duration stepDuration = Duration.millis(stepMs);
            Platform.runLater(() -> SortAnimationSkeleton.animateSwapsOnSeries(barChart, centerPane, series, swaps, stepDuration, () -> {
                // After animation completes, ensure final categories/order reflect underlying data
                applyCategoryOrder(votos);
                renderBarChartCandidatos(votos);
            }));

            // Mantener vertical
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
        // Ajustar eje Y para evitar extensión extra hacia arriba
        adjustYAxisToSeries(series);

        Platform.runLater(() -> {
            // Asegurar que el chart esté centrado before overlays
            try { centerBarChart(); } catch (Exception ignore) {}

            // update bar node styles and tooltip
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    Tooltip.install(d.getNode(), new Tooltip(String.valueOf(d.getYValue())));
                    try { d.getNode().setStyle("-fx-bar-fill: #2196F3;"); } catch (Exception ignore) {}
                }
            }

            // Color legend symbols to match the bars using lookup
            try {
                java.util.Set<javafx.scene.Node> symbols = barChart.lookupAll(".chart-legend-item-symbol");
                for (javafx.scene.Node sym : symbols) {
                    try { sym.setStyle("-fx-background-color: #2196F3;"); } catch (Exception ignore) {}
                }
            } catch (Exception ignore) {}

            // Ensure layout is up-to-date before computing bounds and creating overlays
            try { if (barChart != null) { barChart.applyCss(); barChart.layout(); } } catch (Exception ignore) {}

            // Delegate overlay creation and caching to SortAnimationSkeleton
            try {
                SortAnimationSkeleton.prepareOverlays(barChart, centerPane, series);
            } catch (Exception ignore) {}
        });
    }

    private void applyCategoryOrder(DoublyLinkedList<VotoCandidato> votos) {
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        ObservableList<String> cats = FXCollections.observableArrayList();
        for (VotoCandidato v : votos) cats.add(v.getNombreCandidato());
        xAxis.setCategories(cats);
        // Asegurar vertical cada vez que se actualizan categorías
        xAxis.setTickLabelRotation(90);
    }

    // ----------------- Animation helpers -----------------

    // The swap-recording and sorting-check helpers were moved to SortAnimationSkeleton

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
}