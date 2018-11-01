package jp.seraphyware.javafxexam.jfxexam1.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.controlsfx.dialog.FontSelectorDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.AbstractWindowController;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.CDIFXMLLoaderMark;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.FontStyleSheetGenerator;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.MainMenuCustomizer;
import jp.seraphyware.javafxexam.jfxexam1.ui.inner.Page1Controller;
import jp.seraphyware.javafxexam.jfxexam1.ui.inner.Page2Controller;
import jp.seraphyware.javafxexam.jfxexam1.ui.util.ErrorDialogUtils;
import jp.seraphyware.javafxexam.jfxexam1.util.prefs.PreferencesService;
import jp.seraphyware.javafxexam.jfxexam1.util.prefs.PreferencesServiceParameter;
import jp.seraphyware.javafxexam.jfxexam1.util.prefs.WindowSizePersistent;
import jp.seraphyware.javafxexam.jfxexam1.util.prefs.WindowSizePersistentPrefix;
import jp.seraphyware.javafxexam.jfxexam1.util.resources.MessageResourceParameter;


/**
 * メイン画面
 */
@Dependent
public class MainFrameController extends AbstractWindowController {

	/**
	 * ロガー
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	@PreferencesServiceParameter(fileName = "preferences.xml")
	private PreferencesService preferencesService;

	@Inject
	@WindowSizePersistentPrefix("mainWindow")
	private WindowSizePersistent windowSizePref;

	@Inject
	@MessageResourceParameter
	private ResourceBundle resources;

	@Inject
	private Instance<AboutController> aboutCtrlHolder;

	@Inject
	@CDIFXMLLoaderMark
	private Instance<FXMLLoader> ldrProvider;

	private Runnable destroyCallback;

	public Runnable getDestroyCallback() {
		return destroyCallback;
	}

	public void setDestroyCallback(Runnable destroyCallback) {
		this.destroyCallback = destroyCallback;
	}

	@FXML
	private BorderPane root;

	@FXML
	private MenuBar menuBar;

	@Override
	protected void makeRoot() {
		BorderPane root;
		FXMLLoader ldr = ldrProvider.get();
		try {
			ldr.setController(this);
			ldr.setLocation(getClass().getResource("/ui/MainFrame.fxml")); //$NON-NLS-1$

			try {
				root = ldr.load();
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		} finally {
			ldrProvider.destroy(ldr);
		}

		root.centerProperty().addListener((self, old, value) -> {
			if (old != null) {
				Object oldController = old.getUserData();
				if (oldController instanceof MainMenuCustomizer) {
					((MainMenuCustomizer) oldController).removeCustomizeMenu(menuBar);
				}
				windowSizePref.saveSplitPaneDividerPositions(old);
				windowSizePref.saveTableColumnWidths(old);
			}
			if (value != null) {
				Object newController = value.getUserData();
				if (newController instanceof MainMenuCustomizer) {
					((MainMenuCustomizer) newController).createCustomizeMenu(menuBar);
				}
				Platform.runLater(() -> {
					// シーングラフのgetUnmodifiableChildren()の子要素を正しく
					// 取るためには、一旦キューに入れて後回しにする必要があるようだ。
					windowSizePref.loadSplitPaneDividerPositions(value);
					windowSizePref.loadTableColumnWidths(value);
				});
			}
		});

		setRoot(root);
	}

	/**
	 * デフォルトのフォントサイズの復元
	 */
	private void loadDefaultFont() {
		// フォントサイズの復元
		String fontFamily = preferencesService.getProperty("default-font-family");
		double fontSize = preferencesService.getPropertyDouble("default-font-size", 0);
		String fontPosture = preferencesService.getProperty("default-font-posture");
		String fontWeight= preferencesService.getProperty("default-font-weight");
		if (fontFamily != null && fontSize > 0) {
			FontStyleSheetGenerator.FontInfo fontInfo = new FontStyleSheetGenerator.FontInfo();
			fontInfo.setFamily(fontFamily);
			fontInfo.setSize(fontSize);
			if (fontPosture != null) {
				fontInfo.setPosture(FontPosture.findByName(fontPosture));
			}
			if (fontWeight != null) {
				fontInfo.setWeight(FontWeight.findByName(fontWeight));
			}

			Font font = fontInfo.getFont();
			AbstractWindowController.setDefaultFont(font);
		}
	}

	private void saveDefaultFont() {
		Font font = AbstractWindowController.getDefaultFont();
		FontStyleSheetGenerator.FontInfo fontInfo = FontStyleSheetGenerator.FontInfo.parse(font);

		preferencesService.setProperty("default-font-family", fontInfo.getFamily());
		preferencesService.setPropertyDouble("default-font-size", fontInfo.getSize());
		preferencesService.setProperty("default-font-posture", fontInfo.getPosture().name());
		preferencesService.setProperty("default-font-weight", fontInfo.getWeight().name());
		preferencesService.flush();
	}

	@Override
	public void openWindow() {
		// デフォルトのフォントサイズ等の復元
		loadDefaultFont();

		// タイトルの設定
		String title = resources.getString("application.title"); //$NON-NLS-1$
		Stage stg = getStage();
		stg.setTitle(title);
		super.openWindow();

		windowSizePref.loadWindowSize(getStage());
	}

	/**
	 * 閉じて良いか確認してからウィンドウを閉じる.
	 * @return 閉じた場合はtrue、キャンセルした場合はfalse
	 */
	public boolean performClose() {
		// 現在のウィンドウサイズの保存
		windowSizePref.saveWindowSize(getStage());

		destroy();

		getStage().toFront(); // 対象のドキュメントを前面にだしてから問い合わせる

		// ドキュメントを閉じて良いか確認する.
		Alert closeConfirmAlert = new Alert(AlertType.CONFIRMATION);
		closeConfirmAlert.initOwner(getStage());
		closeConfirmAlert.setHeaderText(resources.getString("mainFrame.closeConfirm")); //$NON-NLS-1$
		Optional<ButtonType> result = closeConfirmAlert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			if (destroyCallback != null) {
				destroyCallback.run();
				return true;
			}
		}
		return false;
	}

	@Override
	public void onCloseRequest(WindowEvent event) {
		event.consume();
		performClose();
	}

	/**
	 * CDI経由で生成したInstanceをdestroyするためのdisposerのリスト.<br>
	 */
	private final LinkedList<Runnable> disposers = new LinkedList<>();

	protected void destroy() {
		disposeCenterPane();
		logger.info("☆destroy"); //$NON-NLS-1$
	}

	protected void disposeCenterPane() {
		root.setCenter(null);

		disposers.forEach(Runnable::run);
		disposers.clear();
	}

	@FXML
	protected void onFileClose() {
		try {
			performClose();

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			Stage stage = getStage();
			ErrorDialogUtils.showException(stage, ex);
		}
	}

	@FXML
	protected void onFontSetting() {
		Font defaultFont = AbstractWindowController.getDefaultFont();
		FontSelectorDialog fontDlg = new FontSelectorDialog(defaultFont);
		Optional<Font> result = fontDlg.showAndWait();
		if (result.isPresent()) {
			defaultFont = result.get();
			AbstractWindowController.setDefaultFont(defaultFont);
			saveDefaultFont();

			// 既存のスタイルを一旦消す
			Scene scene = getScene();
			scene.getStylesheets().clear();
			AbstractWindowController.applyStyleSheet(scene);
		}
	}

	@FXML
	protected void onAbout() {
		AboutController ctrl = aboutCtrlHolder.get();
		try {
			ctrl.setOwner(getStage());
			ctrl.showAndWait();

		} finally {
			aboutCtrlHolder.destroy(ctrl);
		}
	}

	@Inject
	private Instance<Page1Controller> page1ControllerHolder;

	@Inject
	private Instance<Page2Controller> page2ControllerHolder;

	@FXML
	protected void onPage1() {
		try {
			disposeCenterPane();
			Page1Controller ctrl = page1ControllerHolder.get();
			disposers.add(() -> {
				page1ControllerHolder.destroy(ctrl);
			});

			Parent contentRoot = ctrl.getRoot();
			root.setCenter(contentRoot);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void onPage2() {
		try {
			disposeCenterPane();
			Page2Controller ctrl = page2ControllerHolder.get();
			disposers.add(() -> {
				page2ControllerHolder.destroy(ctrl);
			});

			Parent contentRoot = ctrl.getRoot();
			root.setCenter(contentRoot);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}
}
