#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.inner;

import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import ${package}.ui.common.AbstractDocumentController;
import ${package}.ui.common.CDIFXMLLoaderMark;
import ${package}.ui.common.InnerDocumentControllerHelper;
import ${package}.ui.common.MainMenuCustomizer;
import ${package}.ui.util.ErrorDialogUtils;
import ${package}.ui.util.ProgressController;
import ${package}.util.concurrent.BackgroundTaskService;
import ${package}.util.concurrent.ChainedJavaFXTask;
import ${package}.util.resources.MessageResourceParameter;

@Dependent
public class Page1Controller extends AbstractDocumentController
		implements Initializable, InnerDocumentControllerHelper, MainMenuCustomizer {

	@Inject
	private BackgroundTaskService bgTaskService;

	@Inject
	@CDIFXMLLoaderMark
	private Instance<FXMLLoader> ldrProvider;

	@Inject
	@MessageResourceParameter
	private ResourceBundle resources;

	@FXML
	private TextArea textarea;

	private Menu menuView;

	@Override
	protected void makeRoot() {
		FXMLLoader ldr = ldrProvider.get();
		try {
			URL url = getClass().getResource("/ui/inner/Page1.fxml"); //${symbol_dollar}NON-NLS-1${symbol_dollar}
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
		menuView = new Menu(resources.getString("page1.menu")); //${symbol_dollar}NON-NLS-1${symbol_dollar}

		MenuItem menuItemWork = new MenuItem(resources.getString("page1.menu.work"));
		menuItemWork.setOnAction(evt -> onWork());

		MenuItem menuItemClear = new MenuItem(resources.getString("page1.menu.clear"));
		menuItemClear.setOnAction(evt -> onClear());

		menuView.getItems().addAll(menuItemWork, menuItemClear);
	}

	@Override
	public void createCustomizeMenu(MenuBar menuBar) {
		if (!menuBar.getMenus().contains(menuView)) {
			menuBar.getMenus().add(2, menuView); // 左から3番目にメニューを挿入
		}
	}

	@Override
	public void removeCustomizeMenu(MenuBar menuBar) {
		menuBar.getMenus().remove(menuView);
	}

	protected void onClear() {
		textarea.setText("");
	}

	protected void onWork() {
		Runnable checkTestException = () -> {
			PointerInfo pInfo = MouseInfo.getPointerInfo();
			double x = pInfo.getLocation().getX();
			if (x == 0) {
				throw new RuntimeException("TEST EXCEPTION!!");
			}
		};

		Task<Void> task1 = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateTitle("Phase 1/2");
				int max = 100;
				for (int idx = 0; idx < max; idx++) {
					updateProgress(idx, max);
					updateMessage("building... " + idx + " 座標0でテスト例外発生");
					checkTestException.run();
					Thread.sleep(50);
				}
				return null;
			}
		};

		Task<Void> task2 = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateTitle("Phase 2/2");
				int max = 50;
				for (int idx = 0; idx < max; idx++) {
					updateProgress(idx, max);
					updateMessage("finishing... " + idx);
					System.out.println("finishing... " + idx + "/" + max); // コンソールへの出力はログにブリッジされる
					checkTestException.run();
					Thread.sleep(40);
				}
				return null;
			}
		};

		// 複数のTASKを連結して1つのプログレスダイアログで扱う
		ChainedJavaFXTask tasks = new ChainedJavaFXTask();
		tasks.addTask(task1);
		tasks.addTask(task2);

		// プログレスダイアログの表示と完了待ち
		CompletableFuture<?> cf = ProgressController.doProgressAndWait(getStage(), bgTaskService, tasks);

		cf.whenCompleteAsync((ret, ex) -> {
			if (ex != null) {
				// エラー表示
				ErrorDialogUtils.showException(getStage(), ex);
			} else {
				// 完了表示
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.initOwner(getStage());
				alert.initModality(Modality.WINDOW_MODAL);
				alert.setHeaderText("FINISHED");
				alert.showAndWait();
			}
		}, Platform::runLater);
	}
}
