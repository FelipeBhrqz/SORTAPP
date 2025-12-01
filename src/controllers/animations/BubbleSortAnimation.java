package controllers.animations;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.*;

public class BubbleSortAnimation {
    private static final Map<String, Node> tickLabelMap = new HashMap<>();
    private static final List<Node> tickLabelOrder = new ArrayList<>();

    public static void prepareOverlays(BarChart<String, Number> barChart, Pane centerPane, XYChart.Series<String, Number> series) {
        if (barChart == null || series == null) return;
        tickLabelOrder.clear();
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        if (xAxis == null) return;
        ObservableList<String> categories = xAxis.getCategories();
        if (categories == null || categories.isEmpty()) return;
        try {
            for (String cat : categories) {
                Node tick = xAxis.lookupAll(".axis .tick-label").stream()
                    .filter(n -> n instanceof javafx.scene.control.Label && cat.equals(((javafx.scene.control.Label) n).getText()))
                    .findFirst().orElse(null);
                tickLabelOrder.add(tick);
            }
        } catch (Exception ignore) {}
    }

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

    private static Animation createDynamicSwapTransition(BarChart<String, Number> barChart, XYChart.Series<String, Number> series, int i, int j, Duration duration) {
        if (series == null) return null;
        int n = series.getData().size();
        if (i < 0 || j < 0 || i >= n || j >= n) return null;
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        Node ni = di.getNode();
        Node nj = dj.getNode();
        if (ni == null || nj == null) return null;

        Node labelI = (i >= 0 && i < tickLabelOrder.size()) ? tickLabelOrder.get(i) : null;
        Node labelJ = (j >= 0 && j < tickLabelOrder.size()) ? tickLabelOrder.get(j) : null;

        double byX = getAverageBarSpacing(series);
        if (byX <= 0) byX = 20;
        double dir = (i < j) ? byX : -byX;

        TranslateTransition dataLeft = new TranslateTransition(duration, ni);
        TranslateTransition dataRight = new TranslateTransition(duration, nj);
        dataLeft.setByX(dir);
        dataRight.setByX(-dir);

        TranslateTransition labelLeftTrans = null;
        if (labelI != null) {
            labelLeftTrans = new TranslateTransition(duration, labelI);
            labelLeftTrans.setByX(dir);
        }
        TranslateTransition labelRightTrans = null;
        if (labelJ != null) {
            labelRightTrans = new TranslateTransition(duration, labelJ);
            labelRightTrans.setByX(-dir);
        }

        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(dataLeft, dataRight);
        if (labelLeftTrans != null) pt.getChildren().add(labelLeftTrans);
        if (labelRightTrans != null) pt.getChildren().add(labelRightTrans);

        pt.setOnFinished(e -> {
            swapData(series, i, j);
            Collections.swap(tickLabelOrder, i, j);
            try { ni.setTranslateX(0); nj.setTranslateX(0); } catch (Exception ignore) {}
            if (labelI != null) try { labelI.setTranslateX(0); } catch (Exception ignore) {}
            if (labelJ != null) try { labelJ.setTranslateX(0); } catch (Exception ignore) {}
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

    public static void swapData(XYChart.Series<String, Number> series, int i, int j) {
        if (i < 0 || j < 0 || i >= series.getData().size() || j >= series.getData().size()) return;
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        Number yi = di.getYValue();
        Number yj = dj.getYValue();
        di.setYValue(yj);
        dj.setYValue(yi);
    }

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
                    int t = arr[i]; arr[i] = arr[i + 1]; arr[i + 1] = t;
                    swaps.add(new int[]{i, i + 1});
                    swapped = true;
                }
            }
        } while (swapped);
        return swaps;
    }

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
}