package jp.seraphyware.javafxexam.jfxexam1;

import java.awt.SplashScreen;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import jp.seraphyware.javafxexam.jfxexam1.ui.MainFrameController;
import jp.seraphyware.javafxexam.jfxexam1.util.concurrent.BackgroundTaskService;
import jp.seraphyware.javafxexam.jfxexam1.util.log.LogConfigurator;

/**
 * アプリケーションエントリ
 * @param args
 */
public class MainApp extends Application {

	/**
	 * シングルトン.
	 */
	private static MainApp singleton;

	/**
	 * CDIコンテナ.
	 */
	private CdiContainer cdiContainer;

	/**
	 * ロガー.
	 */
	private Logger logger;

	/**
	 * バックグラウンドジョブを管理する.
	 */
	@Inject
	private BackgroundTaskService bgTaskService;

	/**
	 * シングルトンを取得する.
	 *
	 * @return
	 */
	@Produces
	@ApplicationScoped
	// いつも同じインスタンスを返すので実質的にApplicationScopedと同じ.
	public static MainApp getSingleton() {
		if (singleton == null) {
			throw new IllegalStateException();
		}
		return singleton;
	}

	/**
	 * シングルトンの設定
	 * @param inst
	 */
	private static void setSingleton(MainApp inst) {
		if (singleton != null) {
			throw new IllegalStateException("シングルトンは初期化済みです.");
		}
		singleton = inst;
	}

	/**
	 * 初期化
	 */
	@Override
	public void init() throws Exception {
		try {
			// シングルトンの記憶
			setSingleton(this);

			// ログの準備
			LogConfigurator.initialize();
			logger = LoggerFactory.getLogger(getClass());

			// CDIコンテナの作成と起動
			cdiContainer = CdiContainerLoader.getCdiContainer();
			cdiContainer.boot();

			// コンテキストの有効化
			ContextControl contextControl = cdiContainer.getContextControl();
			contextControl.startContexts();

			// このインスタンスにInjectする.
			BeanProvider.injectFields(this);

		} catch (Exception ex) {
			ex.printStackTrace(); // ログの設定に失敗している可能性があるためコンソールへ
			throw ex;
		}
	}

	/**
	 * アプリケーションの開始
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			logger.info("★★started.");

			// 明示的に終了させる.
			Platform.setImplicitExit(false);

			// メインフレームを構築する
			Instance<MainFrameController> mainFrameCtrlHolder = CDI.current().select(MainFrameController.class);
			MainFrameController mainFrameCtrl = mainFrameCtrlHolder.get();
			mainFrameCtrl.setDestroyCallback(() -> {
				// 閉じるボタンまたはCloseコマンドにより、メインフレームを閉じてアプリケーションを終了する
				mainFrameCtrl.closeWindow();
				mainFrameCtrlHolder.destroy(mainFrameCtrl);
				Platform.exit();
			});
			mainFrameCtrl.openWindow();

			// スプラッシュスクリーンを閉じる
			closeSplashScreen();

		} catch (Exception ex) {
			ex.printStackTrace(); // ログの設定に失敗している可能性があるためコンソールへ
			logger.error(ex.toString(), ex);
			throw ex;
		}
	}

	/**
	 * スプラッシュスクリーンがあれば閉じる.
	 */
	private void closeSplashScreen() {
		// スプラッシュスクリーンの取得(表示されていれば)
		SwingUtilities.invokeLater(() -> {
			try {
				SplashScreen splashScreen = SplashScreen.getSplashScreen();
				if (splashScreen != null) {
					// スプラッシュを閉じる
					splashScreen.close();
				}
			} catch (Exception ex) {
				// スプラッシュ取得エラーは無視して良い.
				logger.warn("splash制御に失敗しました。:" + ex, ex);
			}
		});
	}

	/**
	 * アプリケーションの停止
	 */
	@Override
	public void stop() throws Exception {

		// 先にCDIがシャットダウンしないようにTaskなどの完了を待ち合わせる
		logger.info("★bgJob shutdown");
		bgTaskService.shutdown();

		// Taskの完了ハンドラでJavaFXの画面更新などを行う場合に
		// 先にCDIがシャットダウンしないように後回しにする.
		CompletableFuture<Void> waitForCDIShutdownTask = new CompletableFuture<>();
		Platform.runLater(() -> {
			try {
				logger.info("stop cdi context");
				ContextControl contextControl = cdiContainer.getContextControl();
				contextControl.stopContexts();

				logger.info("cdi shutdown");
				cdiContainer.shutdown();

				logger.info("cdi stopped");
				waitForCDIShutdownTask.complete(null);

			} catch (Throwable ex) {
				logger.error("shutdown failed." + ex, ex);
				waitForCDIShutdownTask.completeExceptionally(ex);
			}
		});
		waitForCDIShutdownTask.whenComplete((v, ex) -> {
			// 分散GCを早期実施させるため明示的にGCを行う.
			gc();

			// 明示的に終了する.
			logger.info("exit.");
			System.exit(0);
		});
	}

	/**
	 * GCを強制する.(RMIの分散GCを早期実施させるためなどの目的)
	 */
	private static void gc() {
		for (int idx = 0; idx < 3; idx++) {
			System.gc();
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				break;
			}
		}
		System.gc();
	}

	/**
	 * 古典的なランチャによるエントリポイント.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
