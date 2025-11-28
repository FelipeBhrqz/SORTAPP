package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainSceneController {
	@FXML
	private Button btnElec2025;

	@FXML
	public void btnAdvance(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/view/PrincipalScene.fxml"));
			Stage stage = (Stage) btnElec2025.getScene().getWindow();
			stage.setScene(new Scene(root));
			stage.setTitle("Elecciones 2025 - Sufragantes por provincia");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}