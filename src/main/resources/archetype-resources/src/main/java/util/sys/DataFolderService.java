#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.sys;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Vetoed;

import org.apache.commons.lang3.StringUtils;

public interface DataFolderService {

	/**
	 * 環境変数%APPDATA%の位置、AppData${symbol_escape}Roaming${symbol_escape}appname下を指す。
	 * (当該環境変数がない場合はカレントディレクトリ"."を返す.)
	 * @return
	 */
	Path getApplicationDataFolder();

	/**
	 * 環境変数"%LOCALAPPDATA%の位置、AppData${symbol_escape}Local${symbol_escape}appname下を指す。
	 * (当該環境変数がない場合は%APPDATA%と同じ場所を返す.)
	 * @return
	 */
	Path getLocalApplicationDataFolder();

	/**
	 * WebViewのLocalStorageの保存先.<br>
	 * (WebViewを使うとLocalStorageの有無にかかわらず、自動的に保存先に空フォルダが作成されるため、
	 * 保存先は明示しておくのが良い)
	 * @return
	 */
	File getWebViewUserDataDirectory();

	static DataFolderService getDefault() {
		return DataFolderServiceFactory.defaultDataFolderService;
	}
}

@Vetoed
final class DataFolderServiceImpl implements DataFolderService {

	private static final String APPLICATION_NAME;

	private static final ResourceBundle resourceBundle;

	static {
		resourceBundle = ResourceBundle.getBundle("app");
		APPLICATION_NAME = resourceBundle.getString("APP_DIR_NAME");
	}

	public Path getApplicationDataFolder() {
		String appData = System.getenv("APPDATA");
		if (!StringUtils.isBlank(appData)) {
			return Paths.get(appData, APPLICATION_NAME);
		}
		// 代替フォルダ
		return Paths.get(".");
	}

	public Path getLocalApplicationDataFolder() {
		String appData = System.getenv("LOCALAPPDATA");
		if (StringUtils.isBlank(appData)) {
			// 代替フォルダ
			appData = System.getenv("APPDATA");
			if (StringUtils.isBlank(appData)) {
				// 代替フォルダ2
				appData = ".";
			}
		}
		return Paths.get(appData, APPLICATION_NAME);
	}

	@Override
	public File getWebViewUserDataDirectory() {
		return getLocalApplicationDataFolder().resolve("webView").toFile();
	}
}

@Dependent
class DataFolderServiceFactory {

	public static DataFolderServiceImpl defaultDataFolderService = new DataFolderServiceImpl();

	@Produces
	@ApplicationScoped
	public DataFolderService createDataFolderService() {
		return defaultDataFolderService;
	}
}
