package jp.seraphyware.javafxexam.jfxexam1.ui.common;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 直接ウィンドウを持たないコントローラがルートを経由してステージを取得するためのMixIn.
 */
public interface InnerDocumentControllerHelper {

	Parent getRoot();

	default Stage getStage() {
		Parent root = getRoot();
		if (root != null) {
			Scene scene = root.getScene();
			if (scene != null) {
				return (Stage) scene.getWindow();
			}
		}
		return null;
	}
}
