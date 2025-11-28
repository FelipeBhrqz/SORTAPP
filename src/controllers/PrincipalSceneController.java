package controllers;

import TDAs.BubbleSort;
import TDAs.DoublyLinkedList;
import TDAs.InsertionSort;
import TDAs.MergeSort;
import data.ElectionDataLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import model.Resultado;
import model.VotoCandidato;

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
        barChart.setLegendVisible(true); // necesario para diferenciar V1 y V2 en 'Ambas'
        barChart.getData().clear();
        // Forzar etiquetas verticales desde el inicio
        ((CategoryAxis) barChart.getXAxis()).setTickLabelRotation(90);
        try {
            originalOrderV1 = ElectionDataLoader.getCandidateNames();
            originalOrderV2 = new TDAs.DoublyLinkedList<>();
            originalOrderV2.addLast("LUISA GONZALEZ");
            originalOrderV2.addLast("DANIEL NOBOA AZIN");
        } catch (Exception ignored) {}
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
            if ("Bubble Sort".equals(metodo)) BubbleSort.bubbleSort(votos, descending);
            else if ("Insertion Sort".equals(metodo)) InsertionSort.insertionSort(votos, descending);
            else MergeSort.mergeSort(votos, descending);
            // Orden actual del listado define nuevo orden de categorías
            applyCategoryOrder(votos);
            renderBarChartCandidatos(votos);
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

    private void renderBarChartCandidatos(DoublyLinkedList<VotoCandidato> votos) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vuelta " + cmbVuelta.getSelectionModel().getSelectedItem());
        for (VotoCandidato v : votos) {
            // Agregar solo si el nombre está en las categorías actuales (evita desalineación)
            series.getData().add(new XYChart.Data<>(v.getNombreCandidato(), v.getVotos()));
        }
        barChart.getData().add(series);
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) Tooltip.install(d.getNode(), new Tooltip(String.valueOf(d.getYValue())));
            }
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
}