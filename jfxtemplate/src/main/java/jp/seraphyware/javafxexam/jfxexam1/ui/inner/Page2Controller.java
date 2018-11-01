package jp.seraphyware.javafxexam.jfxexam1.ui.inner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.AbstractDocumentController;
import jp.seraphyware.javafxexam.jfxexam1.ui.common.CDIFXMLLoaderMark;
import jp.seraphyware.javafxexam.jfxexam1.util.resources.MessageResourceParameter;

@Dependent
public class Page2Controller extends AbstractDocumentController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(Page2Controller.class);

	@Inject
	@CDIFXMLLoaderMark
	private Instance<FXMLLoader> ldrProvider;

	@Inject
	@MessageResourceParameter
	private ResourceBundle resources;

	@FXML
	private VBox root;

	@FXML
	private Label labelNow;

	private static final long INTERVAL = 100 * 1000 * 1000L;

	private AnimationTimer timer;

	@Override
	protected void makeRoot() {
		FXMLLoader ldr = ldrProvider.get();
		try {
			URL url = getClass().getResource("Page2.fxml"); //$NON-NLS-1$
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
		timer = new AnimationTimer() {
			private long last;

			@Override
			public void handle(long now) {
				if ((now - last) > INTERVAL) {
					last = now;
					onTimer();
				}
			}
		};
		onTimer();

		root.sceneProperty().addListener((self, old, scene) -> {
			if (scene != null) {
				timer.start();
				logger.info("start timer");
			} else {
				timer.stop();
				logger.info("stop timer");
			}
		});
	}

	protected void onTimer() {
		labelNow.setText(new Date().toString());
	}
}
