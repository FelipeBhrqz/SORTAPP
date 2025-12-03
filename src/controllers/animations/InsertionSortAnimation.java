package controllers.animations;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;

public class InsertionSortAnimation {
    private static final java.util.List<Node> tickLabelOrder = new java.util.ArrayList<>();
    private static final java.util.List<String> categoryOrder = new java.util.ArrayList<>();
    private static final java.util.List<Node> overlayTickLabels = new java.util.ArrayList<>();
    private static CategoryAxis cachedAxis;

    public static void prepareTickLabelCache(BarChart<String, Number> barChart, Pane centerPane, XYChart.Series<String, Number> series) {
        tickLabelOrder.clear();
        categoryOrder.clear();
        overlayTickLabels.clear();
        cachedAxis = null;
        if (barChart == null || series == null) return;
        try { barChart.applyCss(); barChart.layout(); } catch (Exception ignore) {}
        CategoryAxis xAxis = (CategoryAxis) barChart.getXAxis();
        cachedAxis = xAxis;
        if (xAxis == null) return;
        Map<String, Deque<Node>> nodesByText = new HashMap<>();
        try {
            for (Node node : xAxis.lookupAll(".tick-label")) {
                String key = extractTickText(node);
                if (key == null) continue;
                nodesByText.computeIfAbsent(key, k -> new ArrayDeque<>()).add(node);
            }
        } catch (Exception ignore) {}
        for (XYChart.Data<String, Number> data : series.getData()) {
            Deque<Node> deque = nodesByText.get(data.getXValue());
            tickLabelOrder.add(deque == null ? null : deque.pollFirst());
            categoryOrder.add(data.getXValue());
        }
        Object overlayProp = barChart.getProperties().get(AnimationConstants.OVERLAY_TICK_LABELS_KEY);
        if (overlayProp instanceof java.util.List<?>) {
            for (Object obj : (java.util.List<?>) overlayProp) {
                if (obj instanceof Node) overlayTickLabels.add((Node) obj);
            }
        }
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
            Animation anim = createDynamicSwapTransition(barChart, centerPane, series, i, j, stepDuration);
            if (anim != null) seq.getChildren().add(anim);
        }
        if (onFinished != null) seq.setOnFinished(e -> Platform.runLater(onFinished));
        Platform.runLater(seq::play);
    }

    private static Animation createDynamicSwapTransition(BarChart<String, Number> barChart, Pane centerPane, XYChart.Series<String, Number> series, int i, int j, Duration duration) {
        if (series == null) return null;
        int n = series.getData().size();
        if (i < 0 || j < 0 || i >= n || j >= n) return null;
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        Node ni = di.getNode();
        Node nj = dj.getNode();
        if (ni == null || nj == null) return null;
        Node labelI = getTickLabelNode(i);
        Node labelJ = getTickLabelNode(j);
        double byX = getAverageBarSpacing(centerPane, series);
        if (byX <= 0) byX = 20;
        double dir = (i < j) ? byX : -byX;
        TranslateTransition tiTrans = new TranslateTransition(duration, ni);
        TranslateTransition tjTrans = new TranslateTransition(duration, nj);
        tiTrans.setByX(dir);
        tjTrans.setByX(-dir);
        TranslateTransition labelLeft = null;
        if (labelI != null) {
            labelLeft = new TranslateTransition(duration, labelI);
            labelLeft.setByX(dir);
        }
        TranslateTransition labelRight = null;
        if (labelJ != null) {
            labelRight = new TranslateTransition(duration, labelJ);
            labelRight.setByX(-dir);
        }
        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(tiTrans, tjTrans);
        if (labelLeft != null) pt.getChildren().add(labelLeft);
        if (labelRight != null) pt.getChildren().add(labelRight);
        pt.setOnFinished(e -> {
            swapSeriesEntries(series, i, j);
            swapTickLabelOrder(i, j);
            updateAxisCategories();
            try { ni.setTranslateX(0); nj.setTranslateX(0); } catch (Exception ignore) {}
            if (labelI != null) try { labelI.setTranslateX(0); } catch (Exception ignore) {}
            if (labelJ != null) try { labelJ.setTranslateX(0); } catch (Exception ignore) {}
        });
        return pt;
    }

    private static void swapSeriesEntries(XYChart.Series<String, Number> series, int i, int j) {
        if (series == null) return;
        int size = series.getData().size();
        if (i < 0 || j < 0 || i >= size || j >= size) return;
        Collections.swap(series.getData(), i, j);
        if (i < categoryOrder.size() && j < categoryOrder.size()) Collections.swap(categoryOrder, i, j);
    }

    private static void updateAxisCategories() {
        if (cachedAxis == null || categoryOrder.isEmpty()) return;
        cachedAxis.setCategories(FXCollections.observableArrayList(categoryOrder));
        cachedAxis.setTickLabelsVisible(overlayTickLabels.isEmpty());
    }

    private static Node getTickLabelNode(int idx) {
        if (idx >= 0 && idx < overlayTickLabels.size()) return overlayTickLabels.get(idx);
        return (idx >= 0 && idx < tickLabelOrder.size()) ? tickLabelOrder.get(idx) : null;
    }

    private static void swapTickLabelOrder(int i, int j) {
        if (i < 0 || j < 0) return;
        int max = Math.max(i, j);
        if (max >= tickLabelOrder.size()) return;
        Collections.swap(tickLabelOrder, i, j);
    }

    private static double getAverageBarSpacing(Pane centerPane, XYChart.Series<String, Number> series) {
        try {
            if (centerPane == null || series == null || series.getData().size() < 2) return 20;
            double total = 0;
            int count = 0;
            for (int k = 0; k < series.getData().size() - 1; k++) {
                Node a = series.getData().get(k).getNode();
                Node b = series.getData().get(k + 1).getNode();
                if (a == null || b == null) continue;
                Bounds ba = centerPane.sceneToLocal(a.localToScene(a.getBoundsInLocal()));
                Bounds bb = centerPane.sceneToLocal(b.localToScene(b.getBoundsInLocal()));
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

    private static String extractTickText(Node node) {
        if (node == null) return null;
        if (node instanceof Labeled) return ((Labeled) node).getText();
        if (node instanceof Text) return ((Text) node).getText();
        Object prop = node.getProperties().get("text");
        return prop != null ? prop.toString() : null;
    }

    public static void swapData(XYChart.Series<String, Number> series, int i, int j) {
        swapSeriesEntries(series, i, j);
        updateAxisCategories();
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
