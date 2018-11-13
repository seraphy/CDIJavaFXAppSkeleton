package jp.seraphyware.javafxexam.jfxexam1.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.AbstractWindowController;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.CDIFXMLLoaderMark;
import jp.seraphyware.javafxexam.jfxexam1.util.prefs.WindowSizePersistent;
import jp.seraphyware.javafxexam.jfxexam1.util.prefs.WindowSizePersistentPrefix;
import jp.seraphyware.javafxexam.jfxexam1.util.resources.MessageResourceParameter;
import jp.seraphyware.javafxexam.jfxexam1.util.sys.DataFolderService;

@Dependent
public class AboutController extends AbstractWindowController implements Initializable {

	@Inject
	@CDIFXMLLoaderMark
	private Instance<FXMLLoader> ldrProvider;

	@Inject
	@MessageResourceParameter
	private ResourceBundle resources;

	@Inject
	@WindowSizePersistentPrefix("aboutWindow")
	private WindowSizePersistent windowSizePref;

	@Inject
	private DataFolderService dataFolderService;

	@FXML
	private WebView webview;

	{
		setSizeToScene(false); // ウィンドウサイズの自動フィットをしない
	}

	public void showAndWait() {
		Stage stg = getStage();
		stg.initModality(Modality.WINDOW_MODAL);
		windowSizePref.loadWindowSize(stg); // 前回ウィンドウサイズの復元
		stg.showAndWait();
	}

	@Override
	protected Stage createStage() {
		Stage stg = super.createStage();
		stg.setTitle(resources.getString("about.title"));
		return stg;
	}

	@Override
	public void onCloseRequest(WindowEvent event) {
		onClose();
	}

	@Override
	protected void makeRoot() {
		FXMLLoader ldr = ldrProvider.get();
		try {
			URL url = getClass().getResource("/ui/About.fxml"); //$NON-NLS-1$
			assert url != null;

			ldr.setLocation(url);
			ldr.setController(this);

			try {
				setRoot(ldr.load());

			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}

		} finally {
			ldrProvider.destroy(ldr);;
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		WebEngine engine = webview.getEngine();

		// HTML5 LocalStorageの保存場所
		File userDataDir = dataFolderService.getWebViewUserDataDirectory();
		engine.setUserDataDirectory(userDataDir);

		// HTMLリソースのロード
		String docPath = resources.getString("about.resourceName"); //$NON-NLS-1$
		ClassLoader ldr = AboutController.class.getClassLoader();
		try (InputStream is = ldr.getResourceAsStream(docPath)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			if (is != null) {
				byte[] buf = new byte[4096];
				for (;;) {
					int rd = is.read(buf);
					if (rd < 0) {
						break;
					}
					bos.write(buf, 0, rd);
				}
			}
			engine.loadContent(new String(bos.toByteArray(), StandardCharsets.UTF_8));
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@FXML
	protected void onClose() {
		// 現在のウィンドウサイズの保存
		windowSizePref.saveWindowSize(getStage());
		closeWindow();
	}

	@FXML
	protected void onShowSysProps() {
		StringBuilder buf = new StringBuilder();

		// System Properties
		buf.append("[System Properties]\r\n");
		Properties props = System.getProperties();
		buf.append(Collections.list(props.propertyNames()).stream().sorted()
				.map(key -> key + "=" + System.getProperty(key.toString()))
				.collect(Collectors.joining("\r\n", "", "\r\n")));

		// Environments
		buf.append("\r\n[Environments]\r\n");
		Map<String, String> env = System.getenv();
		buf.append(env.entrySet().stream()
				.sorted((a, b) -> String.CASE_INSENSITIVE_ORDER
						.compare(a.getKey(), b.getKey()))
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining("\r\n", "", "\r\n")));

		TextArea node = new TextArea();
		node.setText(buf.toString());
		node.setEditable(false);

		Alert alert = new Alert(AlertType.NONE);
		alert.initOwner(getStage());
		alert.getButtonTypes().setAll(ButtonType.OK);
		alert.setTitle(resources.getString("sysprops.title")); //$NON-NLS-1$
		alert.getDialogPane().setContent(node);
		alert.setResizable(true);
		alert.setWidth(450);
		alert.setHeight(450);
		alert.showAndWait();
	}
}
