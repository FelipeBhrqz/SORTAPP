package controllers.animations;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.*;

// Esqueleto de animación: maneja overlays (índices) y swaps visuales de barras.
public class SortAnimationSkeleton {

    // Caches para nodos de tick (etiquetas del eje X)
    private static final Map<String, Node> tickLabelMap = new HashMap<>();

    // Prepara cache de nodos de tick para animación. Ya no creamos overlays horizontales.
    public static void prepareOverlays(BarChart<String, Number> barChart, Pane centerPane, XYChart.Series<String, Number> series) {
        if (barChart == null || series == null) return;
        tickLabelMap.clear();
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}

        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        if (xAxis == null) return;
        try {
            // Buscar nodos de tick label y mapear por su texto
            for (Node n : xAxis.lookupAll(".tick-label")) {
                try {
                    String txt = n instanceof javafx.scene.control.Label ? ((javafx.scene.control.Label) n).getText() : n.toString();
                    if (txt != null) tickLabelMap.put(txt, n);
                    // Asegurar que estén rotadas verticalmente (la vista las usa así)
                    try { n.setRotate(90); } catch (Exception ignore) {}
                } catch (Exception ignore) {}
            }
        } catch (Exception ignore) {}
    }

    // Animate a sequence of swaps on the provided series. Delegates to swapData after each swap.
    public static void animateSwapsOnSeries(BarChart<String, Number> barChart, Pane centerPane, XYChart.Series<String, Number> series, List<int[]> swaps, Duration stepDuration, Runnable onFinished) {
        if (series == null || swaps == null || swaps.isEmpty()) {
            if (onFinished != null) Platform.runLater(onFinished);
            return;
        }
        SequentialTransition seq = new SequentialTransition();
        for (int[] s : swaps) {
            if (s == null || s.length < 2) continue;
            int i = s[0];
            int j = s[1];
            Animation anim = createDynamicSwapTransition(barChart, series, i, j, stepDuration);
            if (anim != null) seq.getChildren().add(anim);
        }
        if (onFinished != null) seq.setOnFinished(e -> Platform.runLater(onFinished));
        Platform.runLater(seq::play);
    }

    // Crea una transición dinámica que mueve barras y etiquetas del eje X y luego intercambia los datos
    private static Animation createDynamicSwapTransition(BarChart<String, Number> barChart, XYChart.Series<String, Number> series, int i, int j, Duration duration) {
        if (series == null) return null;
        int n = series.getData().size();
        if (i < 0 || j < 0 || i >= n || j >= n) return null;
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        Node ni = di.getNode();
        Node nj = dj.getNode();
        if (ni == null || nj == null) return null;

        // Obtener nodos de etiquetas del eje X (verticales) por categoría
        Node ti = tickLabelMap.get(di.getXValue());
        Node tj = tickLabelMap.get(dj.getXValue());

        // compute byX using average spacing
        double byX = getAverageBarSpacing(series);
        if (byX <= 0) byX = 20;
        // dirección: i mueve a la derecha, j a la izquierda si i<j
        double dir = (i < j) ? byX : -byX;

        TranslateTransition tiTrans = new TranslateTransition(duration, ni);
        TranslateTransition tjTrans = new TranslateTransition(duration, nj);
        tiTrans.setByX(dir);
        tjTrans.setByX(-dir);

        TranslateTransition tiLabel = null;
        TranslateTransition tjLabel = null;
        if (ti != null) {
            tiLabel = new TranslateTransition(duration, ti);
            tiLabel.setByX(dir);
        }
        if (tj != null) {
            tjLabel = new TranslateTransition(duration, tj);
            tjLabel.setByX(-dir);
        }

        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(tiTrans, tjTrans);
        if (tiLabel != null) pt.getChildren().add(tiLabel);
        if (tjLabel != null) pt.getChildren().add(tjLabel);

        pt.setOnFinished(e -> {
            // swap the underlying Y values in the series to reflect new order
            swapData(series, i, j);
            // clear translations and force layout
            try { ni.setTranslateX(0); nj.setTranslateX(0); } catch (Exception ignore) {}
            if (ti != null) try { ti.setTranslateX(0); } catch (Exception ignore) {}
            if (tj != null) try { tj.setTranslateX(0); } catch (Exception ignore) {}
            try { if (barChart != null) { barChart.applyCss(); barChart.layout(); } } catch (Exception ignore) {}
        });

        return pt;
    }

    private static double getAverageBarSpacing(XYChart.Series<String, Number> series) {
        try {
            if (series == null || series.getData().size() < 2) return 20;
            double total = 0;
            int count = 0;
            for (int k = 0; k < series.getData().size() - 1; k++) {
                Node a = series.getData().get(k).getNode();
                Node b = series.getData().get(k + 1).getNode();
                if (a == null || b == null) continue;
                Bounds ba = a.localToScene(a.getBoundsInLocal());
                Bounds bb = b.localToScene(b.getBoundsInLocal());
                double centerA = ba.getMinX() + ba.getWidth() / 2.0;
                double centerB = bb.getMinX() + bb.getWidth() / 2.0;
                total += Math.abs(centerB - centerA);
                count++;
            }
            if (count == 0) return 20;
            return total / (double) count;
        } catch (Exception ignore) {}
        return 20;
    }

    // Intercambia los valores Y en la serie (votos) después de completar la animación
    // IMPORTANT: sólo intercambiamos Y para evitar reasignar categorías X durante la animación.
    public static void swapData(XYChart.Series<String, Number> series, int i, int j) {
        if (i < 0 || j < 0 || i >= series.getData().size() || j >= series.getData().size()) return;
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        Number yi = di.getYValue();
        Number yj = dj.getYValue();
        di.setYValue(yj);
        dj.setYValue(yi);
    }

    // Record bubble swaps (adjacent swaps) on a copy of the values
    public static List<int[]> recordBubbleSwaps(int[] arr, boolean descending) {
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
    public static boolean isSorted(int[] arr, boolean descending) {
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
    public static List<int[]> recordInsertionSwaps(int[] arr, boolean descending) {
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

    // Record final-order swaps (used for merge)
    public static List<int[]> recordFinalOrderSwaps(int[] arr, boolean descending) {
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
}