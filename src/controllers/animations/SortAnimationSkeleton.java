package controllers.animations;

import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;

// Esqueleto de animación compatible con AlgorithmChartsJavaFX: maneja swaps visuales de barras.
public class SortAnimationSkeleton {

    // Crea una transición de swap entre dos barras por índice
    public static ParallelTransition swapBars(XYChart.Series<String, Number> series, int i, int j, Duration duration) {
        XYChart.Data<String, Number> di = series.getData().get(i);
        XYChart.Data<String, Number> dj = series.getData().get(j);
        // Nota: En JavaFX, las barras son nodos que se generan después de agregar la serie al chart
        // Para un swap visual real, habría que traducir los nodos y luego intercambiar los datos.
        TranslateTransition ti = new TranslateTransition(duration, di.getNode());
        TranslateTransition tj = new TranslateTransition(duration, dj.getNode());
        // Dirección y magnitud de movimiento deberían calcularse según spacing/categorías; aquí es un placeholder.
        ti.setByX(20);
        tj.setByX(-20);
        ParallelTransition pt = new ParallelTransition(ti, tj);
        return pt;
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
}