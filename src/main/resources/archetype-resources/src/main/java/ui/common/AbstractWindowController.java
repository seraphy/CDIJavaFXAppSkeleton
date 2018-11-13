#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.common;

import java.util.Arrays;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;


/**
 * ステージをもつJavaFXのコンテナを作成するための抽象クラス.
 */
public abstract class AbstractWindowController extends AbstractDocumentController {

	/**
	 * デフォルトフォントのCSSのジェネレータ
	 */
	private static final FontStyleSheetGenerator fontStyleGen = new FontStyleSheetGenerator();

	/**
	 * デフォルトフォント
	 */
	private static Font defaultFont;

	/**
	 * 親ウィンドウ、null可
	 */
	private Window owner;

	/**
	 * ステージ構築時にシーンに合わせてサイズ調整するか？
	 */
	private boolean sizeToScene = true;

	/**
	 * 作成されたステージ、未作成ならばnull
	 */
	private Stage stage;

	/**
	 * 作成されたシーン、未作成ならnull
	 */
	private Scene scene;

	/**
	 * ステージの閉じるボタン押下時のイベントハンドラ
	 */
	private final EventHandler<WindowEvent> closeRequestHandler = event -> {
		onCloseRequest(event);
		event.consume();
	};

	/**
	 * デフォルトコンストラクタ
	 */
	protected AbstractWindowController() {
		this(null, true);
	}

	/**
	 * コンストラクタ
	 * @param owner 親(null可)
	 * @param sizeToScene ステージ構築時にシーンに合わせてサイズ調整するか？
	 */
	protected AbstractWindowController(Window owner, boolean sizeToScene) {
		this.owner = owner;
		this.sizeToScene = sizeToScene;
	}

	/**
	 * ステージの閉じるボタン押下時に処理する内容.
	 * @param event
	 */
	public abstract void onCloseRequest(WindowEvent event);

	/**
	 * 親を設定する.<br>
	 * (ステージを構築する前に指定する必要がある.)<br>
	 * @param owner
	 */
	public void setOwner(Window owner) {
		this.owner = owner;
	}

	/**
	 * 親を取得する.
	 * @return
	 */
	public Window getOwner() {
		return owner;
	}

	/**
	 * ステージ構築時にシーンに合わせてサイズ調整するか否か設定する
	 * @param sizeToScene
	 */
	public void setSizeToScene(boolean sizeToScene) {
		this.sizeToScene = sizeToScene;
	}

	/**
	 * ステージ構築時にシーンに合わせてサイズ調整するか？
	 * @return
	 */
	public boolean isSizeToScene() {
		return sizeToScene;
	}

	/**
	 * ステージを設定する.<br>
	 * すでにステージが設定済みであってはならない.<br>
	 * @param stage
	 */
	public void setStage(Stage stage) {
		if (this.stage != null) {
			throw new IllegalStateException();
		}
		this.stage = stage;
	}

	/**
	 * ステージを取得する.<br>
	 * ステージがまだ作成されていない場合はステージを作成する.<br>
	 * ステージはシーンが設定済みとなる.<br>
	 * @return
	 */
	public Stage getStage() {
		if (stage == null) {
			stage = createStage();
			stage.setScene(getScene());

			if (sizeToScene) {
				// 初回のみ
				stage.sizeToScene();
			}
		}
		return stage;
	}

	/**
	 * ステージを作成する.<br>
	 * アイコンが設定される.<br>
	 * @return
	 */
	protected Stage createStage() {
		Stage stage = new Stage();
		stage.initOwner(owner);
		stage.setOnCloseRequest(closeRequestHandler);
		stage.setUserData(this);

		// アイコンの設定 (最適なサイズが選択される)
		Class<?> cls = AbstractWindowController.class;
		stage.getIcons().addAll(Arrays.asList(
				new Image(cls.getResourceAsStream("/ui/common/icon.png")),
				new Image(cls.getResourceAsStream("/ui/common/icon48.png")),
				new Image(cls.getResourceAsStream("/ui/common/icon32.png")),
				new Image(cls.getResourceAsStream("/ui/common/icon16.png"))));

		return stage;
	}

	/**
	 * シーンを取得する.<br>
	 * シーンが作成されていなければ作成される.<br>
	 * シーンにはJavaFXルート要素が設定済みとなる.<br>
	 * @return
	 */
	public Scene getScene() {
		assert Platform.isFxApplicationThread();

		if (scene == null) {
			scene = new Scene(getRoot());
			initStyles(scene);
		}

		return scene;
	}

	protected void initStyles(Scene scene) {
		applyStyleSheet(scene);
	}

	/**
	 * ステージを表示する.<br>
	 * ステージが作成されていなけば作成される.<br>
	 */
	public void openWindow() {
		assert Platform.isFxApplicationThread();

		getStage().show();
		getStage().toFront();
	}

	/**
	 * ステージを閉じる.<br>
	 */
	public void closeWindow() {
		assert Platform.isFxApplicationThread();
		if (stage != null) {
			stage.close();
		}
	}

	/**
	 * デフォルトフォントを設定する
	 * @param font
	 */
	public static void setDefaultFont(Font font) {
		defaultFont = font;
	}

	/**
	 * デフォルトフォント
	 * @return
	 */
	public static Font getDefaultFont() {
		if (defaultFont == null) {
			defaultFont = Font.getDefault();
		}
		return defaultFont;
	}

	/**
	 * スタイルシートを適用する
	 * @param scene
	 */
	public static void applyStyleSheet(Scene scene) {
		fontStyleGen.applyStyleSheet(scene, getDefaultFont());
	}
}
