package jp.seraphyware.javafxexam.jfxexam1.ui.common;

import java.lang.reflect.Method;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;

public final class RenderScaleSupport {

	/**
	 * プライベートコンストラクタ
	 */
	private RenderScaleSupport() {
		super();
	}

	/**
	 * 複数スクリーン中の、もっとも大きなスケールを取得する.
	 * @return
	 */
	public static double getMaxRenderScale() {
		return Screen.getScreens().stream().mapToDouble(screen -> getRenderScale(screen)).max().orElse(1d);
	}

	/**
	 * スクリーンにかけられている出力スケールを取得する。
	 * @param screen
	 * @return スケール、不明な場合は1
	 */
	public static double getRenderScale(Screen screen) {
		Method m;
		try {
			m = Screen.class.getDeclaredMethod("getOutputScaleY"); // until Java9 (public api)
		} catch (NoSuchMethodException e) {
			try {
				m = Screen.class.getDeclaredMethod("getScale"); // until 8u60 b15 (private api)
				m.setAccessible(true);
			} catch (NoSuchMethodException e2) {
				try {
					m = Screen.class.getDeclaredMethod("getRenderScale"); // (private api)
					m.setAccessible(true);
				} catch (NoSuchMethodException e3) {
					return 1d; // 不明なので1倍
				}
			}
		}
		try {
			if (screen == null) {
				screen = Screen.getPrimary();
			}
			return ((Number) m.invoke(screen)).doubleValue();
		} catch (Exception e) {
			return 1;
		}
	}

	/**
	 * 倍率を指定し、背景は透過としてスナップショットをとる。
	 * (ノードはSceneに属している必要がある。)
	 * @param node ノード
	 * @param scale 倍率
	 * @return 取得されたイメージ
	 */
	public static WritableImage snapshot(Node node, double scale) {
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT); // 背景は透過としてスナップショットをとる
		parameters.setTransform(Transform.scale(scale, scale));
		return node.snapshot(parameters, null);
	}
}
