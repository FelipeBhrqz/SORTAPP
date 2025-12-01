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

public class InsertionSortAnimation {
    private static final Map<String, Node> tickLabelMap = new HashMap<>();

    public static void prepareOverlays(BarChart<String, Number> barChart, Pane centerPane, XYChart.Series<String, Number> series) {
        if (barChart == null || series == null) return;
        tickLabelMap.clear();
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        if (xAxis == null) return;
        try {
            for (Node n : xAxis.lookupAll(".tick-label")) {
                try {
                    String txt = n instanceof javafx.scene.control.Label ? ((javafx.scene.control.Label) n).getText() : n.toString();
                    if (txt != null) tickLabelMap.put(txt, n);
                    try { n.setRotate(90); } catch (Exception ignore) {}
                } catch (Exception ignore) {}
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
        Node ti = tickLabelMap.get(di.getXValue());
        Node tj = tickLabelMap.get(dj.getXValue());
        double byX = getAverageBarSpacing(series);
        if (byX <= 0) byX = 20;
        double dir = (i < j) ? byX : -byX;
        TranslateTransition tiTrans = new TranslateTransition(duration, ni);
        TranslateTransition tjTrans = new TranslateTransition(duration, nj);
        tiTrans.setByX(dir);
        tjTrans.setByX(-dir);
        TranslateTransition tiLabel = null;
        TranslateTransition tjLabel = null;
        if (ti != null) { tiLabel = new TranslateTransition(duration, ti); tiLabel.setByX(dir); }
        if (tj != null) { tjLabel = new TranslateTransition(duration, tj); tjLabel.setByX(-dir); }
        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(tiTrans, tjTrans);
        if (tiLabel != null) pt.getChildren().add(tiLabel);
        if (tjLabel != null) pt.getChildren().add(tjLabel);
        pt.setOnFinished(e -> {
            swapData(series, i, j);
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

    public static void swapData(XYChart.Series<String, Number> series, int i, int j) {
        if (i < 0 || j < 0 || i >= series.getData().size() || j >= series.getData().size()) return;
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        Number yi = di.getYValue();
        Number yj = dj.getYValue();
        di.setYValue(yj);
        dj.setYValue(yi);
    }

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
                    arr[j + 1] = arr[j];
                    swaps.add(new int[]{j, j + 1});
                    j--;
                } else break;
            }
            arr[j + 1] = cur;
        }
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