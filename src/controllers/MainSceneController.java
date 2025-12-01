package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

public class MainSceneController {
	@FXML
	private Button btnElec2025;
	@FXML
	private Label lblTitle; // title label

	@FXML
	public void initialize() {
		// Load all required fonts here (instead of Main)
		try { Font.loadFont(getClass().getResourceAsStream("/assets/ChangaOne-Italic.ttf"), 12); } catch (Exception ignore) {}
		try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-Bold.ttf"), 12); } catch (Exception ignore) {}
		try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-BoldItalic.ttf"), 12); } catch (Exception ignore) {}
		try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-Italic.ttf"), 12); } catch (Exception ignore) {}
		try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-Regular.ttf"), 12); } catch (Exception ignore) {}
		try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerifCaption-Italic.ttf"), 12); } catch (Exception ignore) {}
		try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerifCaption-Regular.ttf"), 12); } catch (Exception ignore) {}

		// Apply Changa One Italic to title
		try {
			Font f = Font.loadFont(getClass().getResourceAsStream("/assets/ChangaOne-Italic.ttf"), 96);
			if (f == null) f = Font.font("Changa One", 96);
			if (lblTitle != null && f != null) lblTitle.setFont(f);
		} catch (Exception ignore) {}
	}

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