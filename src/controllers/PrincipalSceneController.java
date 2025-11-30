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

    // cached mapping from category text -> tick label node to use during animations
    private final Map<String, javafx.scene.Node> tickLabelMap = new HashMap<>();
    // overlay labels placed over the scene so we can animate them reliably (independent of axis relayout)
    private final Map<String, javafx.scene.Node> overlayLabelMap = new HashMap<>();
    // ordered list of overlay labels corresponding to series display order
    private final List<javafx.scene.Node> overlayLabelList = new ArrayList<>();

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
            if (isSorted(values, descending)) {
                return; // nada ocurre si ya está ordenado
            }

            // Record swap operations according to chosen algorithm (on a copy)
            int[] copy = Arrays.copyOf(values, values.length);
            List<int[]> swaps;
            if ("Bubble Sort".equals(metodo)) swaps = recordBubbleSwaps(copy, descending);
            else if ("Insertion Sort".equals(metodo)) swaps = recordInsertionSwaps(copy, descending);
            else swaps = recordFinalOrderSwaps(copy, descending); // for Merge we compute swaps to reach final order

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
            Platform.runLater(() -> animateSwapsOnSeries(series, swaps, stepDuration, () -> {
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
            // Asegurar que el chart esté centrado antes de crear overlays
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

            // build cache of tick label nodes (category -> node)
            try {
                tickLabelMap.clear();
                java.util.Set<javafx.scene.Node> labels = barChart.getXAxis().lookupAll(".tick-label");
                for (javafx.scene.Node n : labels) {
                    if (n instanceof Text) {
                        Text t = (Text) n;
                        tickLabelMap.put(t.getText(), t);
                    } else {
                        tickLabelMap.put(n.toString(), n);
                    }
                }
            } catch (Exception ignore) {}

            // hide the native axis tick labels
            try { ((CategoryAxis) barChart.getXAxis()).setTickLabelsVisible(false); } catch (Exception ignore) {}

            // remove previous overlay labels
            try {
                Iterator<javafx.scene.Node> it = centerPane.getChildren().iterator();
                while (it.hasNext()) {
                    javafx.scene.Node n = it.next();
                    if ("overlay-tick".equals(n.getUserData())) it.remove();
                }
                overlayLabelMap.clear();
                overlayLabelList.clear();
            } catch (Exception ignore) {}

            // create overlay tick labels positioned under each bar
            try {
                javafx.geometry.Bounds chartBounds = barChart.getBoundsInParent();
                // aumentar el offset vertical para bajar los índices y evitar colapso con las barras
                double baseY = chartBounds.getMaxY() + 40; // mayor offset base para bajar etiquetas
                for (XYChart.Data<String, Number> d : series.getData()) {
                    javafx.scene.Node barNode = d.getNode();
                    if (barNode == null) continue;
                    javafx.geometry.Bounds b = barNode.getBoundsInParent();
                    // compute center in scene coordinates then convert to centerPane local coords to position label accurately
                    javafx.geometry.Bounds barLocalBounds = barNode.getBoundsInLocal();
                    javafx.geometry.Bounds barSceneBounds = barNode.localToScene(barLocalBounds);
                    double sceneCenterX = barSceneBounds.getMinX() + barSceneBounds.getWidth() / 2.0;
                    double localCenterX = centerPane.sceneToLocal(sceneCenterX, barSceneBounds.getMinY()).getX();
                    Text lbl = new Text(d.getXValue());
                    lbl.getStyleClass().add("overlay-tick-label");
                    // reducir tamaño de letra del overlay para que los índices sean más pequeños
                    try { lbl.setFont(Font.font(10)); } catch (Exception ignore) {}
                    lbl.setRotate(90);
                    lbl.setUserData("overlay-tick");
                    // initial layout using computed local center (will be refined below once layoutBounds are available)
                    double initialTextW = lbl.getLayoutBounds() != null ? lbl.getLayoutBounds().getWidth() : 12;
                    // bajar la Y de las etiquetas (mayor valor) para que queden claramente por debajo del gráfico
                    lbl.setLayoutX(localCenterX - initialTextW / 2.0);
                    lbl.setLayoutY(baseY + 40); // mayor offset vertical para mayor separación
                    centerPane.getChildren().add(lbl);
                    overlayLabelMap.put(d.getXValue(), lbl);
                    overlayLabelList.add(lbl);
                }

                // refine positions after adding to scene
                for (Map.Entry<String, javafx.scene.Node> e : overlayLabelMap.entrySet()) {
                    javafx.scene.Node n = e.getValue();
                    if (n instanceof Text) {
                        Text t = (Text) n;
                        // find corresponding bar to center under
                        javafx.scene.Node bar = tickLabelMap.containsKey(t.getText()) ? tickLabelMap.get(t.getText()) : null;
                        if (bar == null) {
                            // fallback: search data nodes
                            for (XYChart.Data<String, Number> d : series.getData()) {
                                if (d.getXValue().equals(t.getText()) && d.getNode() != null) { bar = d.getNode(); break; }
                            }
                        }
                        if (bar != null) {
                            try {
                                javafx.geometry.Bounds barLocalBounds = bar.getBoundsInLocal();
                                javafx.geometry.Bounds barSceneBounds = bar.localToScene(barLocalBounds);
                                double sceneCenterX = barSceneBounds.getMinX() + barSceneBounds.getWidth() / 2.0;
                                double localCenterX = centerPane.sceneToLocal(sceneCenterX, barSceneBounds.getMinY()).getX();
                                double textW = t.getLayoutBounds().getWidth();
                                t.setLayoutX(localCenterX - textW / 2.0);
                                // también asegurar que la Y se mantenga por debajo del chart en caso de recalculo
                                try { t.setLayoutY(chartBounds.getMaxY() + 52); } catch (Exception ignoreY) {}
                            } catch (Exception ignore) {
                                // fallback: try previous method using boundsInParent
                                try {
                                    javafx.geometry.Bounds bb = bar.getBoundsInParent();
                                    double cx = bb.getMinX() + bb.getWidth() / 2.0;
                                    double textW = t.getLayoutBounds().getWidth();
                                    t.setLayoutX(cx - textW / 2.0);
                                } catch (Exception ignore2) {}
                            }
                        }
                    }
                }
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

    // Record bubble swaps (adjacent swaps) on a copy of the values
    private List<int[]> recordBubbleSwaps(int[] arr, boolean descending) {
        List<int[]> swaps = new ArrayList<>();
        int n = arr.length;
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < n - 1; i++) {
                int a = arr[i];
                int b = arr[i + 1];
                int cmp = Integer.compare(a, b);
                if (descending) cmp = -cmp;
                if (cmp > 0) {
                    // swap
                    int t = arr[i]; arr[i] = arr[i + 1]; arr[i + 1] = t;
                    swaps.add(new int[]{i, i + 1});
                    swapped = true;
                }
            }
        } while (swapped);
        return swaps;
    }

    // Helper: check if an integer array is already sorted (ascending if descending==false, descending otherwise)
    private boolean isSorted(int[] arr, boolean descending) {
        if (arr == null || arr.length < 2) return true;
        for (int k = 0; k < arr.length - 1; k++) {
            if (!descending) {
                if (arr[k] > arr[k + 1]) return false;
            } else {
                if (arr[k] < arr[k + 1]) return false;
            }
        }
        return true;
    }

    // Record insertion swaps as adjacent swaps moving current element left until position
    private List<int[]> recordInsertionSwaps(int[] arr, boolean descending) {
        List<int[]> swaps = new ArrayList<>();
        int n = arr.length;
        for (int k = 1; k < n; k++) {
            int cur = arr[k];
            int j = k - 1;
            while (j >= 0) {
                int cmp = Integer.compare(arr[j], cur);
                if (descending) cmp = -cmp;
                if (cmp > 0) {
                    // swap arr[j] and arr[j+1]
                    arr[j + 1] = arr[j];
                    swaps.add(new int[]{j, j + 1});
                    j--;
                } else break;
            }
            arr[j + 1] = cur;
        }
        return swaps;
    }

    // Record final-order swaps (used for merge) and other animateSwapsOnSeries and dynamic transition methods
    private List<int[]> recordFinalOrderSwaps(int[] arr, boolean descending) {
        int n = arr.length;
        int[] original = Arrays.copyOf(arr, n);
        Integer[] sorted = new Integer[n];
        for (int i = 0; i < n; i++) sorted[i] = arr[i];
        Arrays.sort(sorted, (x, y) -> descending ? Integer.compare(y, x) : Integer.compare(x, y));

        List<int[]> swaps = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        for (int v : original) current.add(v);
        for (int i = 0; i < n; i++) {
            int target = sorted[i];
            int pos = current.indexOf(target);
            while (pos > i) {
                swaps.add(new int[]{pos - 1, pos});
                Collections.swap(current, pos - 1, pos);
                pos--;
            }
        }
        return swaps;
    }

    private void animateSwapsOnSeries(XYChart.Series<String, Number> series, List<int[]> swaps, Duration stepDuration, Runnable onFinished) {
        if (swaps == null || swaps.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        SequentialTransition seq = new SequentialTransition();

        for (int[] s : swaps) {
            int i = s[0];
            int j = s[1];
            if (i < 0 || j < 0 || i >= series.getData().size() || j >= series.getData().size()) continue;

            XYChart.Data<String, Number> di = series.getData().get(i);
            XYChart.Data<String, Number> dj = series.getData().get(j);
            if (di.getNode() == null || dj.getNode() == null) {
                PauseTransition p = new PauseTransition(Duration.millis(50));
                seq.getChildren().add(p);
                int ii = i, jj = j;
                seq.getChildren().add(new PauseTransition(Duration.ZERO));
                seq.getChildren().add(createDynamicSwapTransition(series, ii, jj, stepDuration));
            } else {
                double xi = di.getNode().getBoundsInParent().getMinX();
                double xj = dj.getNode().getBoundsInParent().getMinX();
                double delta = xj - xi;
                TranslateTransition ti = new TranslateTransition(stepDuration, di.getNode());
                TranslateTransition tj = new TranslateTransition(stepDuration, dj.getNode());
                ti.setByX(delta);
                tj.setByX(-delta);
                // find corresponding tick label nodes for X-axis and animate them too
                javafx.scene.Node tickI = (overlayLabelList.size() > i) ? overlayLabelList.get(i) : findTickLabelNode(di.getXValue());
                javafx.scene.Node tickJ = (overlayLabelList.size() > j) ? overlayLabelList.get(j) : findTickLabelNode(dj.getXValue());
                // compute actual tick delta from their current positions (more reliable)
                double tickDelta = 0;
                try {
                    if (tickI != null && tickJ != null) {
                        double tiX = tickI.getBoundsInParent().getMinX();
                        double tjX = tickJ.getBoundsInParent().getMinX();
                        tickDelta = tjX - tiX;
                    }
                } catch (Exception ignore) {}
                TranslateTransition tTickI = (tickI != null) ? new TranslateTransition(stepDuration, tickI) : null;
                TranslateTransition tTickJ = (tickJ != null) ? new TranslateTransition(stepDuration, tickJ) : null;
                if (tTickI != null) tTickI.setByX(tickDelta);
                if (tTickJ != null) tTickJ.setByX(-tickDelta);

                ParallelTransition pt = new ParallelTransition(ti, tj,
                        (tTickI != null ? tTickI : new PauseTransition(Duration.ZERO)),
                        (tTickJ != null ? tTickJ : new PauseTransition(Duration.ZERO))
                );
                pt.setOnFinished(ev -> {
                    Platform.runLater(() -> {
                        SortAnimationSkeleton.swapData(series, i, j);
                        // Force chart relayout and wait a bit before resetting translateX to avoid snap-back
                        try { if (barChart != null) { barChart.applyCss(); barChart.layout(); } } catch (Exception ignore) {}
                        PauseTransition small = new PauseTransition(Duration.millis(120));
                        small.setOnFinished(e2 -> {
                            try {
                                if (di.getNode() != null) di.getNode().setTranslateX(0);
                                if (dj.getNode() != null) dj.getNode().setTranslateX(0);
                                if (tickI != null) tickI.setTranslateX(0);
                                if (tickJ != null) tickJ.setTranslateX(0);
                            } catch (Exception ignore) {}
                        });
                        small.play();
                     });
                 });
                 seq.getChildren().add(pt);
             }
         }

        seq.setOnFinished(ev -> {
            if (onFinished != null) onFinished.run();
        });
        seq.play();
    }

    private Animation createDynamicSwapTransition(XYChart.Series<String, Number> series, int i, int j, Duration stepDuration) {
        return new Transition() {
            { setCycleDuration(stepDuration); }
            double startXi, startXj, delta;
            XYChart.Data<String, Number> di, dj;
            javafx.scene.Node tickI, tickJ;
            @Override protected void interpolate(double frac) {
                // On first call, initialize nodes and compute delta
                if (di == null || dj == null) {
                    if (i < 0 || j < 0 || i >= series.getData().size() || j >= series.getData().size()) return;
                    di = series.getData().get(i);
                    dj = series.getData().get(j);
                    if (di.getNode() == null || dj.getNode() == null) return;
                    startXi = di.getNode().getBoundsInParent().getMinX();
                    startXj = dj.getNode().getBoundsInParent().getMinX();
                    delta = startXj - startXi;
                    // locate tick label nodes once
                    tickI = (overlayLabelList.size() > i) ? overlayLabelList.get(i) : findTickLabelNode(di.getXValue());
                    tickJ = (overlayLabelList.size() > j) ? overlayLabelList.get(j) : findTickLabelNode(dj.getXValue());
                    // compute actual tick delta once and store in userData
                    double tickDelta = 0;
                    try { if (tickI != null && tickJ != null) tickDelta = tickJ.getBoundsInParent().getMinX() - tickI.getBoundsInParent().getMinX(); } catch (Exception ignore) {}
                    if (tickI != null) tickI.setUserData(tickDelta);
                    if (tickJ != null) tickJ.setUserData(-tickDelta);
                 }
                 if (di.getNode() == null || dj.getNode() == null) return;
                 di.getNode().setTranslateX(delta * frac);
                 dj.getNode().setTranslateX(-delta * frac);
                if (tickI != null) {
                    Object ud = tickI.getUserData();
                    double shift = ud instanceof Number ? ((Number) ud).doubleValue() : 0;
                    tickI.setTranslateX(shift * frac);
                }
                if (tickJ != null) {
                    Object ud = tickJ.getUserData();
                    double shift = ud instanceof Number ? ((Number) ud).doubleValue() : 0;
                    tickJ.setTranslateX(shift * frac);
                }
                if (frac >= 1.0) {
                    // finalize: perform swap after a runLater and delay reset to let chart relayout
                    Platform.runLater(() -> {
                        SortAnimationSkeleton.swapData(series, i, j);
                        try { if (barChart != null) { barChart.applyCss(); barChart.layout(); } } catch (Exception ignore) {}
                        PauseTransition small = new PauseTransition(Duration.millis(120));
                        small.setOnFinished(e -> {
                            try {
                                if (di != null && di.getNode() != null) di.getNode().setTranslateX(0);
                                if (dj != null && dj.getNode() != null) dj.getNode().setTranslateX(0);
                                if (tickI != null) tickI.setTranslateX(0);
                                if (tickJ != null) tickJ.setTranslateX(0);
                            } catch (Exception ignore) {}
                        });
                        small.play();
                    });
                }
            }
        };
    }

    // Find the tick label node (Text) on the CategoryAxis that displays the given category string
    private javafx.scene.Node findTickLabelNode(String category) {
        try {
            // first check overlay map (we prefer overlay labels for animation)
            if (category != null && overlayLabelMap.containsKey(category)) return overlayLabelMap.get(category);
            // first check cache populated at render time
            if (category != null && tickLabelMap.containsKey(category)) return tickLabelMap.get(category);
            // if cache miss, fall back to lookup
            java.util.Set<javafx.scene.Node> labels = barChart.getXAxis().lookupAll(".tick-label");
            for (javafx.scene.Node n : labels) {
                if (n instanceof Text) {
                    Text t = (Text) n;
                    if (category.equals(t.getText())) return t;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    // Compute average spacing between adjacent bar nodes (X centers). Returns 0 if not available.
    private double getAverageBarSpacing(XYChart.Series<String, Number> series) {
        try {
            List<Double> xs = new ArrayList<>();
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    double x = d.getNode().getBoundsInParent().getMinX() + d.getNode().getBoundsInParent().getWidth() / 2.0;
                    xs.add(x);
                }
            }
            if (xs.size() < 2) return 0;
            double sum = 0; int cnt = 0;
            for (int k = 1; k < xs.size(); k++) {
                sum += Math.abs(xs.get(k) - xs.get(k-1)); cnt++;
            }
            return cnt > 0 ? (sum / cnt) : 0;
        } catch (Exception ignore) {}
        return 0;
    }

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
