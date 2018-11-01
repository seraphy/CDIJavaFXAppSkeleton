#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import ${package}.util.sys.DataFolderService;

/**
 * フォントのスタイルシートを生成する.
 */
public class FontStyleSheetGenerator {

	private static final Logger logger = LoggerFactory.getLogger(FontStyleSheetGenerator.class);

	private static final String DEFAULT_CSS_TEMPLATE = "/styles.css.tmpl";

	private static final String DEFAULT_CSS_NAME = "styles.css";

	/**
	 * CSSの生成に使ったフォント、まだ生成されていなればnull
	 */
	private Font generatedCssFont;

	/**
	 * 生成されたCSSのパス、まだ生成されていなければnull
	 */
	private String generatedCssPath;

	/**
	 * シーンに対してフォントのルート設定されたCSSを追加適用します.<br>
	 * (すでに適用されている場合は呼び出し元で削除する必要があります。)<br>
	 * @param scene
	 * @param font
	 */
	public void applyStyleSheet(Scene scene, Font font) {
		if (generatedCssFont == null || !generatedCssFont.equals(font)) {
			generatedCssPath = generateCss(font);
			generatedCssFont = font;
		}

		if (generatedCssPath != null && generatedCssPath.length() > 0) {
			scene.getStylesheets().add(generatedCssPath);
		}
	}

	protected Path getOutputCssPath() {
		DataFolderService dataFolderSrv = DataFolderService.getDefault();
		Path dir = dataFolderSrv.getLocalApplicationDataFolder();
		return dir.resolve(DEFAULT_CSS_NAME);
	}

	protected InputStream getInputCssTemplate() throws IOException {
		return AbstractWindowController.class.getResourceAsStream(DEFAULT_CSS_TEMPLATE);
	}

	public static class FontInfo {

		private String family;

		private double size;

		private FontPosture posture = FontPosture.REGULAR;

		private FontWeight weight = FontWeight.NORMAL;

		public String getFamily() {
			return family;
		}

		public void setFamily(String family) {
			this.family = family;
		}

		public double getSize() {
			return size;
		}

		public void setSize(double size) {
			this.size = size;
		}

		public FontPosture getPosture() {
			return posture;
		}

		public void setPosture(FontPosture posture) {
			this.posture = posture;
		}

		public FontWeight getWeight() {
			return weight;
		}

		public void setWeight(FontWeight weight) {
			this.weight = weight;
		}

		@Override
		public String toString() {
			return "FontInfo [family=" + family + ", size=" + size + ", posture=" + posture + ", weight=" + weight
					+ "]";
		}

		public Font getFont() {
			return Font.font(family, weight, posture, size);
		}

		public static FontInfo parse(Font font) {
			FontPosture selPosture = FontPosture.REGULAR;
			FontWeight selWeight = FontWeight.NORMAL;
		    for (String styleWord : font.getStyle().split("${symbol_escape}${symbol_escape}s")) {
		        FontPosture posture = FontPosture.findByName(styleWord);
		        if (posture != null) {
		        	selPosture = posture;
		        }

		        FontWeight weight = FontWeight.findByName(styleWord);
		        if (weight != null) {
		        	selWeight = weight;
		        }
		    }

		    String family = font.getFamily();
		    double size = font.getSize();

		    FontInfo info = new FontInfo();
		    info.setFamily(family);
		    info.setSize(size);
		    info.setPosture(selPosture);
		    info.setWeight(selWeight);

		    return info;
		}
	}

	/**
	 * フォントを指定して、対応するCSSテンプレートをローカルデータフォルダ上に生成する
	 * @param font フォント
	 * @return 生成されたCSSファイルへのパス、もしくはnull
	 */
	private String generateCss(Font font) {
		if (font == null) {
			return null;
		}

		FontInfo fontInfo = FontInfo.parse(font);

		Map<String, String> varMap = new HashMap<>();
	    varMap.put("font-size", Double.toString(fontInfo.getSize()));
		varMap.put("font-name", font.getName());
		varMap.put("font-family", fontInfo.getFamily());
		varMap.put("font-weight", Integer.toString(fontInfo.getWeight().getWeight()));
		varMap.put("font-style", fontInfo.getPosture() == FontPosture.ITALIC ? "italic" : "normal");

		try {
			InputStream inp = getInputCssTemplate();
			if (inp != null) {
				Path stylesCss = getOutputCssPath();
				try (InputStreamReader rd = new InputStreamReader(inp, StandardCharsets.UTF_8);
						BufferedWriter wr = Files.newBufferedWriter(stylesCss, StandardCharsets.UTF_8)) {
					expand(rd, wr, name -> {
						return varMap.computeIfAbsent(name, k -> {
							return System.getProperty(k);
						});
					});
				}

				return stylesCss.toUri().toURL().toExternalForm();
			}
			logger.warn("the CSS template is missing. resource={}", DEFAULT_CSS_TEMPLATE);
			return null;

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * 変数${symbol_dollar}{xxx}を展開する
	 * @param rd
	 * @param wr
	 * @param resolver
	 * @throws IOException
	 */
	private void expand(Reader rd, Writer wr, Function<String, String> resolver) throws IOException {
		int ch;
		int mode = 0;
		StringBuilder nameBuf = new StringBuilder();
		while ((ch = rd.read()) != -1) {
			if (mode == 0) {
				if (ch == '${symbol_dollar}') {
					mode = 1;
				} else {
					wr.write(ch);
				}
			} else if (mode == 1) {
				if (ch == '{') {
					mode = 2;
				} else {
					wr.write('${symbol_dollar}');
					wr.write(ch);
					mode = 0;
				}
			} else if (mode == 2) {
				if (ch == '}') {
					String val = resolver.apply(nameBuf.toString());
					if (val != null) {
						wr.write(val);
					}
					mode = 0;
					nameBuf.setLength(0);
				} else {
					nameBuf.append((char) ch);
				}
			}
		}
	}
}
