package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font; // restored for font preload

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			// Preload custom fonts (reverted to previous approach)
			try { Font.loadFont(getClass().getResourceAsStream("/assets/ChangaOne-Italic.ttf"), 12); } catch (Exception ignore) {}
			try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-Bold.ttf"), 12); } catch (Exception ignore) {}
			try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-BoldItalic.ttf"), 12); } catch (Exception ignore) {}
			try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-Italic.ttf"), 12); } catch (Exception ignore) {}
			try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerif-Regular.ttf"), 12); } catch (Exception ignore) {}
			try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerifCaption-Italic.ttf"), 12); } catch (Exception ignore) {}
			try { Font.loadFont(getClass().getResourceAsStream("/assets/PTSerifCaption-Regular.ttf"), 12); } catch (Exception ignore) {}

			Parent root = FXMLLoader.load(getClass().getResource("/view/MainScene.fxml"));
			Scene scene = new Scene(root);
			primaryStage.setTitle("SORTAPP");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}