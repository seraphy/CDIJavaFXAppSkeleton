package jp.seraphyware.javafxexam.jfxexam1.ui.common;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.apache.deltaspike.core.api.provider.BeanProvider;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import jp.seraphyware.javafxexam.jfxexam1.util.resources.MessageResourceParameter;

/**
 * FXMLLoaderのカスタマイズ済みインスタンスを生成するためのファクトリクラス.
 */
@ApplicationScoped
public class CDIFXMLLoaderFactory {

	/**
	 * メッセージリソース定義
	 */
	@Inject
	@MessageResourceParameter
	private ResourceBundle resources;

	/**
	 * リソースとCDIインスタンスと関連づけられたFXMLLoaderを作成して返す.
	 * @return
	 */
	@Produces
	@Dependent
	@CDIFXMLLoaderMark
	public FXMLLoader createLoader(InjectionPoint ip) {
		FXMLLoader ldr = new FXMLLoader();
		ldr.setBuilderFactory(builderFactory);
		ldr.setResources(resources);
		ldr.setControllerFactory(cls -> {

			// FXML内でコントローラクラス名が指定されている場合、
			// もしくは子FXMLで公司とローラクラスが指定されている場合、
			// コントローラクラスのインスタンスを作成する場合に
			// CDI経由でインスタンスを取得する.
			return BeanProvider.getContextualReference(cls, false);
		});
		return ldr;
	}

	private final CustomBuilderFactory builderFactory = new CustomBuilderFactory();

	/**
	 * FXMLのカスタムビルダファクトリ
	 */
	private static class CustomBuilderFactory implements BuilderFactory {

		private BuilderFactory baseFactory = new JavaFXBuilderFactory();

		private JavaFXFontBuilder2 fontBuilder = new JavaFXFontBuilder2();

		public Builder<?> getBuilder(Class<?> clazz) {
			if (clazz.equals(Font.class)) { // フォント定義の読み込みをカスタマイズする
				return fontBuilder;
			}
			return baseFactory.getBuilder(clazz);
		}
	}
}

/**
 * FXMLのFontを構築するビルダのカスタマイズ。
 * フォントサイズをルートからの相対サイズに変換して適用する。
 */
class JavaFXFontBuilder2 extends AbstractMap<String, Object> implements Builder<Font> {

	private String name = null;
	private double size = Font.getDefault().getSize();
	private FontWeight weight = null;
	private FontPosture posture = null;
	private URL url = null;

	@Override
	public Font build() {
		Font f;
		if (url != null) {
			//TODO Implement some font name caching so that the font
			// is not constructed from the stream every time
			InputStream in = null;
			try {
				in = url.openStream();
				f = Font.loadFont(in, size);
			} catch (Exception e) {
				//TODO
				throw new RuntimeException("Load of font file failed from " + url, e);
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (Exception e) {
					//TODO
					e.printStackTrace();
				}
			}
		} else {
			if (weight == null && posture == null) {
				f = new Font(name, size);
			} else {
				if (weight == null)
					weight = FontWeight.NORMAL;
				if (posture == null)
					posture = FontPosture.REGULAR;
				f = Font.font(name, weight, posture, size);
			}
		}
		return f;
	}

	@Override
	public Object put(String key, Object value) {
		if ("name".equals(key)) {
			if (value instanceof URL) {
				url = (URL) value;
			} else {
				name = (String) value;
			}
		} else if ("size".equals(key)) {
			String str = (String) value;
			str = str.trim();

			double defSize = Font.getDefault().getSize(); // JavaFXの既定のフォントサイズ

			double relsize;
			if (str.endsWith("%")) {
				// %表記の場合
				str = str.substring(0, str.length() - 1);
				relsize = defSize * (Double.parseDouble(str) / 100d);

			} else {
				// 数値表記の場合
				relsize = Double.parseDouble(str);
			}

			double mul = relsize / defSize; // デフォルトのフォントサイズからの比率

			Font font = AbstractWindowController.getDefaultFont(); // ルートのフォントサイズ
			double baseSize = font.getSize();
			size = mul * baseSize; // ルートのフォントサイズに対する比率として適用する
			//System.out.println("font size: " + relsize + ", base=" + baseSize + ", apply=" + size);

		} else if ("style".equals(key)) {
			String style = (String) value;
			if (style != null && style.length() > 0) {
				boolean isWeightSet = false;
				for (StringTokenizer st = new StringTokenizer(style, " "); st.hasMoreTokens();) {
					String stylePart = st.nextToken();
					FontWeight fw;
					if (!isWeightSet && (fw = FontWeight.findByName(stylePart)) != null) {
						weight = fw;
						isWeightSet = true;
						continue;
					}
					FontPosture fp;
					if ((fp = FontPosture.findByName(stylePart)) != null) {
						posture = fp;
						continue;
					}
				}
			}
		} else if ("url".equals(key)) {
			if (value instanceof URL) {
				url = (URL) value;
			} else {
				try {
					url = new URL(value.toString());
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException("Invalid url " + value.toString(), e);
				}
			}
		} else {
			throw new IllegalArgumentException("Unknown Font property: " + key);
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		return false; // False in this context means that the property is NOT read only
	}

	@Override
	public Object get(Object key) {
		return null; // In certain cases, get is also required to return null for read-write "properties"
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
