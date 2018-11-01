#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.inner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import ${package}.ui.common.AbstractDocumentController;
import ${package}.ui.common.CDIFXMLLoaderMark;
import ${package}.ui.common.MainMenuCustomizer;
import ${package}.util.resources.MessageResourceParameter;

@Dependent
public class Page1Controller extends AbstractDocumentController implements Initializable, MainMenuCustomizer {

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
		MenuItem menuItemClear = new MenuItem(resources.getString("page1.menu.clear"));
		menuItemClear.setOnAction(evt -> onClear());
		menuView.getItems().addAll(menuItemClear);
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
}
