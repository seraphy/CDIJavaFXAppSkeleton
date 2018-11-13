#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.prefs;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;
import javafx.util.Pair;

/**
 * ウィンドウサイズ、および分割ペインの分割サイズを保存・復元する.<br>
 */
@Dependent
public class WindowSizePersistent {

	private static final Logger logger = LoggerFactory
			.getLogger(WindowSizePersistent.class);

	@Inject
	@PreferencesServiceParameter(fileName = "WindowSizePreferences.xml")
	private PreferencesService preferencesService;

	private String prefix = "default";

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * ウィンドウサイズを復元します.
	 * @param stg
	 */
	public void loadWindowSize(Stage stg) {
		Objects.requireNonNull(stg);
		try {
			boolean maximized = preferencesService
					.getPropertyBoolean(prefix + ".maximized", false);

			stg.setMaximized(maximized);
			if (!maximized) {
				double x = preferencesService.getPropertyDouble(prefix + ".x",
						0);
				double y = preferencesService.getPropertyDouble(prefix + ".y",
						0);
				double width = preferencesService
						.getPropertyDouble(prefix + ".width", 0);
				double height = preferencesService
						.getPropertyDouble(prefix + ".height", 0);

				if (x >= 0 && y >= 0 && width > 10 && height > 10) {
					stg.setX(x);
					stg.setY(y);
					stg.setWidth(width);
					stg.setHeight(height);
				}
			}

			// rootが設定済みであればSplitPaneの復元を試行する.
			Optional.ofNullable(stg.getScene())
					.map(Scene::getRoot)
					.ifPresent(root -> {
						loadSplitPaneDividerPositions(root);
						loadTableColumnWidths(root);
					});

		} catch (RuntimeException ex) {
			// ウィンドウサイズの復元中に例外が発生しても処理は継続する.
			logger.warn("failed to restorw window layout. " + ex, ex);
		}
	}

	/**
	 * ウィンドウサイズを保存します.
	 * @param stg
	 */
	public void saveWindowSize(Stage stg) {
		Objects.requireNonNull(stg);

		boolean maximized = stg.isMaximized();
		Properties props = new Properties();
		if (maximized) {
			props.put(prefix + ".maximized", "true");

		} else {
			props.put(prefix + ".maximized", "false");

			props.put(prefix + ".x", Double.toString(stg.getX()));
			props.put(prefix + ".y", Double.toString(stg.getY()));
			props.put(prefix + ".width", Double.toString(stg.getWidth()));
			props.put(prefix + ".height", Double.toString(stg.getHeight()));
		}

		preferencesService.setProperties(props);

		// rootが設定済みであればSplitPane、テーブルカラム幅の保存を試行する.
		Optional.ofNullable(stg.getScene())
				.map(Scene::getRoot)
				.ifPresent(root -> {
					saveSplitPaneDividerPositions(root);
					saveTableColumnWidths(root);
				});
	}

	/**
	 * Alertダイアログのウィンドウ位置とサイズを復元します.<br>
	 * (Alertダイアログ幅は構築時に自動調整されるため、必ずしも保存された値に復元されるとは限りません.)<br>
	 * @param alert
	 */
	public void loadWindowSize(Alert alert) {
		Objects.requireNonNull(alert);

		double x = preferencesService.getPropertyDouble(prefix + ".x",
				0);
		double y = preferencesService.getPropertyDouble(prefix + ".y",
				0);
		double width = preferencesService
				.getPropertyDouble(prefix + ".width", 0);
		double height = preferencesService
				.getPropertyDouble(prefix + ".height", 0);

		if (x >= 0 && y >= 0 && width > 10 && height > 10) {
			alert.setX(x);
			alert.setY(y);
			alert.setWidth(width);
			alert.setHeight(height);
		}
	}

	/**
	 * Alertダイアログのウィンドウサイズを保存します.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void saveWindowSize(Alert alert) {
		Objects.requireNonNull(alert);
		saveWindowSize(alert.getX(), alert.getY(), alert.getWidth(), alert.getHeight());
	}

	/**
	 * ウィンドウサイズを保存します.
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void saveWindowSize(double x, double y, double width, double height) {
		Properties props = new Properties();
		props.put(prefix + ".maximized", "false");
		props.put(prefix + ".x", Double.toString(x));
		props.put(prefix + ".y", Double.toString(y));
		props.put(prefix + ".width", Double.toString(width));
		props.put(prefix + ".height", Double.toString(height));
		preferencesService.setProperties(props);
	}

	/**
	 * ノードツリー内にある、すべての分割ペインの現在の分割サイズを保存します.<br>
	 * 各SplitPaneには、それぞれFXML上でidが振られている必要があります.<br>
	 * また、SplitPaneが別のidをもつコンテナの子である場合にはネストしたidとして保存されます.<br>
	 * @param root
	 */
	public void saveSplitPaneDividerPositions(Node root) {
		assert Platform.isFxApplicationThread();

		if (root == null) {
			return;
		}

		Properties props = new Properties();
		traverse(root, (prefix, node) -> {
			if (node instanceof SplitPane) {
				SplitPane splitPane = (SplitPane) node;
				String styleId = splitPane.getId();
				if (styleId != null && styleId.trim().length() > 0) {
					String divs = Arrays.stream(splitPane.getDividerPositions())
							.mapToObj(Double::toString)
							.collect(Collectors.joining(","));
					String key = ".splitpane" + prefix + "." + styleId;
					props.put(key, divs);
					logger.info("save split dividers:{}={}", styleId, divs);
				}
			}
			return true;
		});

		preferencesService.setProperties(props);
	}

	/**
	 * ノードツリー内にある、すべての分割ペインの現在の分割サイズを復元します.<br>
	 * 各SplitPaneには、それぞれFXML上でidが振られている必要があります.<br>
	 * また、SplitPaneが別のidをもつコンテナの子である場合にはネストしたidとして判別されます.<br>
	 * 現在のノードツリーをトラバースするためには、ノードがシーングラフにアタッチ済みでなければなりません.<br>
	 * @param root
	 */
	public void loadSplitPaneDividerPositions(Node root) {
		assert Platform.isFxApplicationThread();

		if (root == null) {
			return;
		}

		try {
			traverse(root, (prefix, node) -> {
				if (node instanceof SplitPane) {
					SplitPane splitPane = (SplitPane) node;
					String styleId = splitPane.getId();
					if (styleId != null && styleId.trim().length() > 0) {
						String key = ".splitpane" + prefix + "." + styleId;
						String args = preferencesService.getProperty(key, null);
						if (args != null && args.trim().length() > 0) {
							double[] divs = Arrays.stream(args.split(","))
									.mapToDouble(Double::parseDouble)
									.toArray();
							splitPane.setDividerPositions(divs);
							logger.info("restore split dividers:{}={}", key,
									Arrays.toString(divs));
						}
					}
				}
				return true;
			});

		} catch (RuntimeException ex) {
			// DivierPositionの復元に失敗しても処理は継続する.
			logger.warn("failed to resotre divier positions. " + ex, ex);
		}
	}

	/**
	 * ノードツリー内にある、すべてのテーブルビューの現在のカラム幅を保存します.<br>
	 * 各テーブルビューには、それぞれFXML上でidが振られている必要があります.<br>
	 * また、テーブルビューが別のidをもつコンテナの子である場合にはネストしたidとして保存されます.<br>
	 * @param root
	 */
	public void saveTableColumnWidths(Node root) {
		assert Platform.isFxApplicationThread();

		if (root == null) {
			return;
		}

		Properties props = new Properties();
		traverse(root, (prefix, node) -> {
			if (node instanceof TreeTableView) {
				TreeTableView<?> treeTableView = (TreeTableView<?>) node;
				String styleId = treeTableView.getId();
				if (styleId != null && styleId.trim().length() > 0) {
					String widths = String.join(",", walkTableColumn(treeTableView.getColumns()));
					String key = ".treeTableView" + prefix + "." + styleId;
					props.put(this.prefix + key, widths);
					logger.info("save treeTableColumn widths:{}={}", key,
							widths);
				}

			} else if (node instanceof TableView) {
				TableView<?> tableView = (TableView<?>) node;
				String styleId = tableView.getId();
				if (styleId != null && styleId.trim().length() > 0) {
					String widths = String.join(",", walkTableColumn(tableView.getColumns()));
					String key = ".tableView" + prefix + "." + styleId;
					props.put(this.prefix + key, widths);
					logger.info("save tableColumn widths:{}={}", styleId, widths);
				}
			}
			return true;
		});

		preferencesService.setProperties(props);
	}

	public <E> void saveTableColumnWidths(TableView<E> tableView, String styleId) {
		Objects.requireNonNull(tableView);
		String widths = String.join(",", walkTableColumn(tableView.getColumns()));
		String key = prefix + ".treeTableView." + styleId;
		preferencesService.setProperty(key, widths);
	}

	private static <E> List<String> walkTableColumn(ObservableList<? extends TableColumnBase<E, ?>> cols) {
		List<String> tokens = new ArrayList<>();
		for (TableColumnBase<E, ?> col : cols) {
			if (!col.getColumns().isEmpty()) {
				tokens.addAll(walkTableColumn(col.getColumns()));

				String id = col.getId();
				if (StringUtils.isBlank(id)) {
					// 親カラムは無名カラムとしては幅を記録しない.
					continue;
				}
			}

			String colName = col.getId();
			if (colName == null) {
				colName = "";
			}
			String token = colName + ":" + Double.toString(col.getWidth());
			tokens.add(token);
		}
		return tokens;
	}

	private static List<Map.Entry<String, Double>> parseColWidths(String args) {
		List<Map.Entry<String, Double>> colWidths = new ArrayList<>();
		if (StringUtils.isNotBlank(args)) {
			String[] cols = args.split(",");
			for (String col : cols) {
				String[] tokens = col.split(":");
				String name = tokens[0];
				double width = Double.parseDouble(tokens[1]);
				colWidths.add(new AbstractMap.SimpleEntry<>(
						name, width));
			}
		}
		return colWidths;
	}

	/**
	 * TableViewの現在のカラムサイズを復元するためのコールバック.<br>
	 */
	@FunctionalInterface
	public interface LoadTableColumnWidthCallback<E> {

		void setWidth(TableView<E> tableView,
				List<Map.Entry<String, Double>> columnWidths);
	}

	/**
	 * TreeTableViewの現在のカラムサイズを復元するためのコールバック.<br>
	 */
	@FunctionalInterface
	public interface LoadTreeTableColumnWidthCallback<E> {

		void setWidth(TreeTableView<E> tableView,
				List<Map.Entry<String, Double>> columnWidths);
	}

	/**
	 * ノードツリー内にある、すべてのTableView/TreeTableViewの現在のカラムサイズを復元します.<br>
	 * 各TableViewには、それぞれFXML上でidが振られている必要があります.<br>
	 * また、TableViewが別のidをもつコンテナの子である場合にはネストしたidとして判別されます.<br>
	 * 現在のノードツリーをトラバースするためには、ノードがシーングラフにアタッチ済みでなければなりません.<br>
	 * @param root
	 */
	public void loadTableColumnWidths(Node root) {
		loadTableColumnWidths(root, this::applyTableColumnWidth);
		loadTreeTableColumnWidths(root, this::applyTreeTableColumnWidth);
	}

	public <E> void loadTableColumnWidths(TableView<E> tableView, String styleId) {
		String key = prefix + ".treeTableView." + styleId;
		String args = preferencesService.getProperty(key);
		if (StringUtils.isNotBlank(args)) {
			List<Map.Entry<String, Double>> colWidths = parseColWidths(args);
			applyTableColumnWidth(tableView, colWidths);
		}
	}

	/**
	 * TableViewのカラム幅を適用する
	 *
	 * @param tableView
	 * @param columnWidths
	 */
	public <E> void applyTableColumnWidth(
			TableView<E> tableView,
			List<Map.Entry<String, Double>> columnWidths) {
		applyTableColumnWidth(
				tableView.getColumns(),
				columnWidths);
	}

	/**
	 * TreeTableViewのカラム幅を適用する
	 *
	 * @param tableView
	 * @param columnWidths
	 */
	public <E> void applyTreeTableColumnWidth(
			TreeTableView<E> tableView,
			List<Map.Entry<String, Double>> columnWidths) {
		applyTableColumnWidth(
				tableView.getColumns(),
				columnWidths);
	}

	/**
	 * TableColumn/TreeTableColumnのカラム幅を適用する共通ルーチン
	 * @param tableView
	 * @param columnWidths
	 */
	public <E> void applyTableColumnWidth(
			List<? extends TableColumnBase<E, ?>> columns,
			List<Map.Entry<String, Double>> columnWidths) {
		Objects.requireNonNull(columns);
		Objects.requireNonNull(columnWidths);

		ApplyTableColumnHelper<E> helper = new ApplyTableColumnHelper<>();
		helper.apply(columns, columnWidths);
	}

	private static class ApplyTableColumnHelper<E> {

		private LinkedList<Double> noNamed = new LinkedList<>();

		private Map<String, Double> named = new HashMap<>();

		public void apply(List<? extends TableColumnBase<E, ?>> columns,
			List<Map.Entry<String, Double>> columnWidths) {
			Objects.requireNonNull(columns);
			Objects.requireNonNull(columnWidths);

			for (Map.Entry<String, Double> entry : columnWidths) {
				String colName = entry.getKey();
				Double width = entry.getValue();
				if (StringUtils.isNotBlank(colName)) {
					named.put(colName, width);
				} else {
					noNamed.add(width);
				}
			}

			applyTableColumnWidth(columns);
		}

		private void applyTableColumnWidth(List<? extends TableColumnBase<E, ?>> columns) {
			for (TableColumnBase<E, ?> column : columns) {
				String colName = column.getId();
				Double width = null;
				if (StringUtils.isNotBlank(colName)) {
					width = named.get(colName);
				} else if (!noNamed.isEmpty()) {
					if (column.getColumns().isEmpty()) {
						// 親カラム以外のみ適用する
						// (無名の親カラムは幅を保存しないため)
						width = noNamed.removeFirst();
					}
				}
				if (width != null) {
					column.setPrefWidth(width);
				}

				// 子をトラバースする.(なければ即時リターンされる)
				applyTableColumnWidth(column.getColumns());
			}
		}
	}

	/**
	 * ノードツリー内にある、すべてのTableViewの現在のカラムサイズを復元します.<br>
	 * 各TableViewには、それぞれFXML上でidが振られている必要があります.<br>
	 * また、TableViewが別のidをもつコンテナの子である場合にはネストしたidとして判別されます.<br>
	 * 現在のノードツリーをトラバースするためには、ノードがシーングラフにアタッチ済みでなければなりません.<br>
	 * @param root
	 */
	public <E> void loadTableColumnWidths(Node root,
			LoadTableColumnWidthCallback<E> callback) {
		Objects.requireNonNull(callback);
		assert Platform.isFxApplicationThread();

		if (root == null) {
			return;
		}

		try {
			traverse(root, (prefix, node) -> {
				if (node instanceof TableView) {
					@SuppressWarnings("unchecked")
					TableView<E> tableView = (TableView<E>) node;
					String styleId = tableView.getId();
					if (styleId != null && styleId.trim().length() > 0) {
						String key = ".tableView" + prefix + "." + styleId;
						String args = preferencesService.getProperty(this.prefix + key, null);
						if (args != null && args.trim().length() > 0) {
							String[] cols = args.split(",");
							logger.info("load tableView widths:{}={}", styleId,
									Arrays.toString(cols));

							List<Map.Entry<String, Double>> colWidths = new ArrayList<>();
							for (String col : cols) {
								String[] tokens = col.split(":");
								String name = tokens[0];
								double width = Double.parseDouble(tokens[1]);
								colWidths.add(new AbstractMap.SimpleEntry<>(
										name, width));
							}
							callback.setWidth(tableView, colWidths);
						}
					}
				}
				return true;
			});

		} catch (RuntimeException ex) {
			// DivierPositionの復元に失敗しても処理は継続する.
			logger.warn("failed to resotre tableView columns. " + ex, ex);
		}
	}

	/**
	 * ノードツリー内にある、すべてのTableViewの現在のカラムサイズを復元します.<br>
	 * 各TableViewには、それぞれFXML上でidが振られている必要があります.<br>
	 * また、TableViewが別のidをもつコンテナの子である場合にはネストしたidとして判別されます.<br>
	 * 現在のノードツリーをトラバースするためには、ノードがシーングラフにアタッチ済みでなければなりません.<br>
	 * @param root
	 */
	public <E> void loadTreeTableColumnWidths(Node root,
			LoadTreeTableColumnWidthCallback<E> callback) {
		Objects.requireNonNull(callback);
		assert Platform.isFxApplicationThread();

		if (root == null) {
			return;
		}

		try {
			traverse(root, (prefix, node) -> {
				if (node instanceof TreeTableView) {
					@SuppressWarnings("unchecked")
					TreeTableView<E> tableView = (TreeTableView<E>) node;
					String styleId = tableView.getId();
					if (styleId != null && styleId.trim().length() > 0) {
						String key = ".treeTableView" + prefix + "." + styleId;
						String args = preferencesService.getProperty(this.prefix + key, null);
						if (args != null && args.trim().length() > 0) {
							List<Map.Entry<String, Double>> colWidths = parseColWidths(args);
							logger.info("load tableView widths:{}={}", styleId, colWidths);
							callback.setWidth(tableView, colWidths);
						}
					}
				}
				return true;
			});

		} catch (RuntimeException ex) {
			// DivierPositionの復元に失敗しても処理は継続する.
			logger.warn("failed to resotre tableView columns. " + ex, ex);
		}
	}

	/**
	 * シーングラフをトラバースし、各ノードと、そのノードが出現した位置を表すID文字列をコールバックします.<br>
	 * Parentがある場合は、その子を掘り下げて解析します.<br>
	 * Parentの子ノードはシーングラフに追加されていない場合は探索できません.<br>
	 * ParentまたはNodeにidがある場合は、先行するidに.を付与して連結されたものがIDとなります.<br>
	 * @param root 解析する親ノード. nullの場合は何もしない.
	 * @param func 親のキー名と現在ノードを渡されるPredicator。処理を中止する場合はfalseを返す。
	 */
	private static void traverse(Node root, BiPredicate<String, Node> func) {
		if (root == null) {
			return;
		}

		// 自分自身を検査する.
		String rootId = root.getId();
		if (StringUtils.isBlank(rootId)) {
			rootId = "";
		}
		if (!func.test(rootId, root)) {
			return;
		}

		// 検出された親リスト
		LinkedList<Pair<String, Parent>> parents = new LinkedList<>();
		if (root instanceof Parent) {
			parents.add(new Pair<>(rootId, (Parent) root));
		}

		while (!parents.isEmpty()) {
			Pair<String, Parent> entry = parents.pop();
			String prefix = entry.getKey();
			Parent parent = entry.getValue();
			for (Node child : parent.getChildrenUnmodifiable()) {
				// 親のキーと自分のNodeを検査する.
				if (!func.test(prefix, child)) {
					return;
				}

				if (child instanceof Parent) {
					String id = child.getId();
					String suffix = "";
					// idをもつ場合は親のidの末尾に追加する.
					if (id != null && id.trim().length() > 0) {
						suffix = "." + id;
					}
					parents.add(new Pair<>(prefix + suffix, (Parent) child));
				}
			}
		}
	}

	public void saveColumnVisibleMap(Map<String, Boolean> visibleMap) {
		visibleMap.forEach((name, visible) -> {
			String key = prefix + ".columnVisible." + name;
			preferencesService.setProperty(key, Boolean.toString(visible));
		});
	}

	public Map<String, Boolean> loadColumnVisibleMap() {
		HashMap< String, Boolean> visibleMap = new HashMap<>();
		String keyPrefix = prefix + ".columnVisible.";
		for (String name : preferencesService
				.getPropertyNamesStartsWith(keyPrefix)) {
			String val = preferencesService.getProperty(name);
			if (val != null) {
				boolean visible = Boolean.parseBoolean(val);
				String key = name.substring(keyPrefix.length());
				visibleMap.put(key, visible);
			}
		}
		return visibleMap;
	}
}
