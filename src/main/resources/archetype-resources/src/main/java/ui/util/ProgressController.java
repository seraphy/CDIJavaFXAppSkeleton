#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ${package}.ui.common.AbstractWindowController;
import ${package}.ui.common.CDIFXMLLoaderMark;
import ${package}.util.concurrent.ChainedJavaFXTask;

@Dependent
public class ProgressController extends AbstractWindowController {

	private static final Logger log = LoggerFactory
			.getLogger(ProgressController.class);

	@FXML
	private Button btnCancel;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label txtLabel;

	@Inject
	@CDIFXMLLoaderMark
	private Instance<FXMLLoader> ldrProvider;

	@Override
	protected void makeRoot() {
		FXMLLoader ldr = ldrProvider.get();
		try {
			ldr.setController(this);
			ldr.setLocation(getClass().getResource("/ui/util/Progress.fxml")); //${symbol_dollar}NON-NLS-1${symbol_dollar}

			try {
				setRoot(ldr.load());

			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}

			assert txtLabel != null;
			assert btnCancel != null;
			assert progressIndicator != null;

		} finally {
			ldrProvider.destroy(ldr);
		}
	}

	@Override
	public void onCloseRequest(WindowEvent event) {
		doCancel();
	}

	@FXML
	protected void onCancel(ActionEvent evt) {
		doCancel();
	}

	protected void doCancel() {
		ActionEvent evt = new ActionEvent(this, ActionEvent.NULL_SOURCE_TARGET);
		if (handlerCancel != null) {
			handlerCancel.handle(evt);
		}
		if (!evt.isConsumed()) {
			closeWindow();
		}
	}

	private EventHandler<ActionEvent> handlerCancel;

	public void setOnCancel(EventHandler<ActionEvent> handler) {
		this.handlerCancel = handler;
	}

	public StringProperty labelTextProperty() {
		return txtLabel.textProperty();
	}

	public DoubleProperty progressProperty() {
		return progressIndicator.progressProperty();
	}

	@Override
	protected Stage createStage() {
		Stage stg = super.createStage();
		stg.initModality(Modality.APPLICATION_MODAL);
		return stg;
	}

	private void bind(ProgressInfo bgTask) {
		Stage stg = getStage();

		stg.titleProperty().bind(bgTask.titleProperty());
		labelTextProperty().bind(bgTask.messageProperty());

		progressProperty().set(-1); // 既定はintermediate (サークル表示)
		bgTask.init(() -> {
			progressProperty().bind(bgTask.progressProperty());
		});

		setOnCancel(evt -> {
			log.info("☆☆☆request cancel☆☆☆"); //${symbol_dollar}NON-NLS-1${symbol_dollar}
			bgTask.cancel();
			evt.consume();
		});
	}

	private void unbind() {
		getStage().titleProperty().unbind();
		progressProperty().unbind();
		labelTextProperty().unbind();

		// ※ intermediateを解除しないとメモリリークする. (java8u77現在)
		progressProperty().set(1);
	}

	/**
	 * 連続したワーカーの実行と、実行中のプログレスダイアログの表示制御を行う.<br>
	 * 複数のタスクを指定した場合は、最初のタスクから順番に実行される.<br>
	 * ワーカーに{@link javafx.concurrent.Task}を指定した場合はUI制御も行うことができる.<br>
	 * 実行中のタスクがキャンセルまたは失敗した場合は、以降のタスクは処理されない.<br>
	 * ワーカー群はChainedJavaFXTaskによって1つのタスクにまとめられて、
	 * {@link ${symbol_pound}showProgressAndWait(Window, Task, Executor)}が呼び出されている.<br>
	 * @param owner 親ウィンドウ、null可
	 * @param jobExecutor ジョブを実行するエグゼキュータ
	 * @param bgTasks ワーカーのリスト、Taskクラスの場合はUI制御も可能
	 * @return ジョブ全体の待ち合わせに使われたCompletableFuture
	 * @see ChainedJavaFXTask
	 */
	public static CompletableFuture<Object> doProgressAndWait(Window owner, Executor jobExecutor,
			FutureTask<?>... bgTasks) {
		Objects.requireNonNull(bgTasks);

		ChainedJavaFXTask bgTask = new ChainedJavaFXTask();
		for (FutureTask<?> task : bgTasks) {
			bgTask.addTask(task);
		}

		return doProgressAndWait(owner, jobExecutor, bgTask);
	}

	/**
	 * ワーカーを指定したエグゼキュータで実行し、実行中のプログレスダイアログの表示制御を行う.<br>
	 * @param owner 親ウィンドウ、null可
	 * @param bgTask UIタスク
	 * @param jobExecutor ジョブを実行するエグゼキュータ
	 * @return ジョブの待ち合わせに使用されたCompletableFuture.(戻り値を受けた時点で、すでに完了済みである)
	 */
	public static <T> CompletableFuture<T> doProgressAndWait(Window owner, Executor jobExecutor, Task<T> bgTask) {
		Objects.requireNonNull(bgTask);
		Objects.requireNonNull(jobExecutor);

		CompletableFuture<T> cf = new CompletableFuture<>();
		Runnable uiTaskWrap = () -> {
			try {
				bgTask.run();
				cf.complete(bgTask.get());

			} catch (Throwable ex) {
				cf.completeExceptionally(ex);
			}
		};
		jobExecutor.execute(uiTaskWrap);

		showProgressAndWait(owner, ProgressInfo.adapt(bgTask), cf);
		return cf;
	}

	/**
	 * プログレスダイアログを表示する.<br>
	 * このメソッド自身ではジョブの実行制御は行わないため、すでに起動しているbgTaskを与えるか、もしくは
	 * cfFactory関数が呼び出された時点で開始することを想定している.<br>
	 * cfFactoryが返したCompluetableFutureで完了状態になったらプログレスダイアログは閉じられる.<br>
	 * @param owner 親ウィンドウ、null可
	 * @param progressInfo UI制御プロパティもつ持つジョブ。ジョブのUIを接続するだけで、開始等の制御については関知しない。
	 * @param cf ジョブの完了を待ち合わせることのできるCompletableFuture。これによりジョブ終了を検知する。<br>
	 */
	public static <R> void showProgressAndWait(Window owner, ProgressInfo progressInfo, CompletableFuture<R> cf) {
		Objects.requireNonNull(progressInfo);
		Objects.requireNonNull(cf);

		Instance<ProgressController> progProv = CDI.current()
				.select(ProgressController.class);
		ProgressController controller = progProv.get();
		try {
			controller.setOwner(owner);

			Stage stg = controller.getStage();
			controller.bind(progressInfo);

			// タスク完了した場合にダイアログを閉じる.
			cf.whenCompleteAsync((ret, ex) -> {
				controller.unbind();

				// ダイアログを閉じる
				controller.closeWindow();

			} , Platform::runLater); // JavaFXスレッドで実行する.

			// モーダルダイアログで表示する.
			if (!cf.isDone()) {
				stg.showAndWait();
			}

		} finally {
			progProv.destroy(controller);
		}
	}
}


