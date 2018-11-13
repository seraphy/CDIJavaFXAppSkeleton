#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ${package}.util.sys.DataFolderService;



public final class LogConfigurator {

	/**
	 * 外部のlog4j2.xmlファイルを使用するためのシステムプロパティのスイッチ名
	 */
	private static final String ENABLE_CUSTOM_LOG4J2 = "enableCustomLog4j2";

	/**
	 * log4j2.xmlのリソース名
	 */
	private static final String LOG4J2_XML = "/log4j2.xml";

	/**
	 * 標準のロギングプロパティ
	 */
	private static final String LOGGING_PROPERTIES = "/logging.properties";

	/**
	 * ログ出力先パスのシステムプロパティ.
	 */
	public static final String AppLogFilePath = "AppLogFilePath";

	/**
	 * コンソールログレベルのシステムプロパティ
	 */
	public static final String ConsoleLogLevel = "ConsoleLogLevel";

	/**
	 * ファイルログレベルのシステムプロパティ
	 */
	public static final String FileLogLevel = "FileLogLevel";

	/**
	 * ローリングサイズ
	 */
	private static final String DEFAULT_MAX_LOG_ROLLING_SIZE = "10m";

	/**
	 * 最大ローリング数
	 */
	private static final int DEFAULT_MAX_LOG_ROLLOVER = 10;


	/**
	 * ログファイルの有効期限切れとなるミリ秒,(8日)
	 */
	private static final long EXPIRE_MILLIES = 8 * 24 * 60 * 60 * 1000L;

	/**
	 * ログ設定
	 */
	private static HashMap<String, org.apache.logging.log4j.Level> currentLevels = new HashMap<>();

	private LogConfigurator() {
		super();
	}

	/**
	 * log4j2, slf4j, commons-logging, java.util.loggingなどの一括設定を行う.
	 * あらゆるログ出力よりも前に、このメソッドを呼び出す必要がある.<br>
	 * 以下の設定を行う.<br>
	 * <br>
	 * (1) システムプロパティ"AppLogFilePath"が未設定であれば、
	 * ローカルアプリケーションフォルダ上の"logs"をログ出力先として
	 * システムプロパティに設定したのちに、<br>
	 * (2) ログフォルダ上にある古いlogファイル(8日前)を消去し、<br>
	 * (3) システムプロパティenableCustomLog4j2がtrueであり、且つ、
	 * "log4j2.xml"が、ローカルアプリケーションフォルダにあれば、これをロードする。
	 * なければリソース上のclientLog4j2.xmlをロードする.<br>
	 * (4) Weld-Seのログをslf4jにする.<br>
	 * (5) java.util.Loggingへのログ出力をslf4jにブリッジさせる.
	 * ただし、RMIコール時の例外ログはINFOレベルで出力するようにレベルを調整する.<br>
	 * (6) 標準出力・標準エラー出力のログへの転送を行う.<br>
	 */
	public static void initialize() {
		// ローカルアプリ設定保存場所
		Path baseDir = DataFolderService.getDefault()
				.getLocalApplicationDataFolder();

		// システムプロパティでログ出力先が指定されていない場合
		// ローカルアプリ設定保存場所にログを出力する.
		if (StringUtils.isBlank(System.getProperty(AppLogFilePath))) {
			Path logDir = baseDir.resolve("logs");
			System.setProperty(AppLogFilePath, logDir.toString());
		}

		// コンソールログレベルの指定がなければ、標準設定を行う.
		if (StringUtils.isBlank(System.getProperty(ConsoleLogLevel))) {
			System.setProperty(ConsoleLogLevel, "INFO");
		}

		// ファイルログレベルの指定がなければ、コンソールログレベルと同じにする.
		if (StringUtils.isBlank(System.getProperty(FileLogLevel))) {
			System.setProperty(FileLogLevel,
					System.getProperty(ConsoleLogLevel));
		}

		// ログディレクトリを準備する
		try {
			Path logDir = Paths.get(System.getProperty(AppLogFilePath));
			if (!Files.exists(logDir)) {
				Files.createDirectories(logDir);
			}

			purgeExpiredLogs(logDir,
					System.currentTimeMillis() - EXPIRE_MILLIES);

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// log4jの設定を行う. (あれば)
		try {
			Path log4jConfPath = baseDir.resolve("log4j2.xml");
			ConfigurationSource source;
			if (Boolean.getBoolean(ENABLE_CUSTOM_LOG4J2)
					&& Files.exists(log4jConfPath)) {
				source = new ConfigurationSource(
						Files.newInputStream(log4jConfPath));

			} else {
				// リソースから明示的にロードする.
				source = new ConfigurationSource(
						LogConfigurator.class.getResourceAsStream(LOG4J2_XML));
			}
			Configurator.initialize(null, source);

		} catch (IOException e) {
			// log4j2の構成に失敗した場合は標準エラー出力のみ
			e.printStackTrace();
		}

		// Weld-Seのログをslf4jにする.
		System.setProperty("org.jboss.logging.provider", "slf4j");

		// SLF4JBridgeHandlerはJavaUtilsのハンドラとしてLoggerを転送するだけなので、
		// 各ログレベルはlog4j2.xmlではなく、logging.propertiesで設定する.
		// (もしくは、ルートロガーでFINESTを設定して全て転送させる。ただしパフォーマンス低下あり)
		//java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
		//logger.setLevel(Level.FINEST);

		try (InputStream is = LogConfigurator.class
				.getResourceAsStream(LOGGING_PROPERTIES)) {
			if (is != null) {
				java.util.logging.LogManager logManager = java.util.logging.LogManager
						.getLogManager();
				logManager.readConfiguration(is); // ← 現在の設定はクリアされる
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		// java.util.Loggingへのログ出力をslf4jにブリッジさせる.
		SLF4JBridgeHandler customHandler = new SLF4JBridgeHandler() {
			@Override
			public void publish(LogRecord record) {
				String loggerName = record.getLoggerName();
				if (loggerName.startsWith("sun.rmi.")
						&& loggerName.endsWith(".call")) {
					Level level = record.getLevel();
					if (Level.FINE.equals(level)) {
						// sun.rmi.server.callのFINEレベルの情報はINFOに昇格して出力する.
						// (例外等がFINE、それ以外がFINERで出力されるため)
						// (デフォルトのSLF4JBridgeHandlerは、FINE, FINERはDEBUG)
						record.setLevel(Level.INFO);
					}
				}
				super.publish(record);
			}
		};

		SLF4JBridgeHandler.removeHandlersForRootLogger();
		//SLF4JBridgeHandler.install();
		LogManager.getLogManager().getLogger("").addHandler(customHandler);

		// 標準出力・標準エラー出力のログへの転送
		Logger stdoutLog = LoggerFactory.getLogger("console.out");
		Logger stderrLog = LoggerFactory.getLogger("console.err");
		System.setOut(createLogStream(stdoutLog::info));
		System.setErr(createLogStream(stderrLog::info));
	}

	/**
	 * lo4j2.xmlをリロードする.<br>
	 * @param configXml 設定ファイルへのパス
	 * @return 設定された場合
	 */
	public static boolean reload(Path configXml) {
		try {
			if (configXml != null && Files.isRegularFile(configXml)) {
				final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
						.getContext(false);

				URI uri = configXml.toUri();
				if (!uri.equals(ctx.getConfigLocation())) {

					// 設定を再ロードする.
					System.out.println("reconfiguration: log4j2.xml=" + uri);
					ctx.setConfigLocation(uri); // 暗黙でreconfigureされる.

					// 実行時に変更したログレベルがあれば、それを再設定する.
					Map<String, org.apache.logging.log4j.Level> prevLevels = new HashMap<>(
							currentLevels);
					prevLevels.forEach((logger, level) -> {
						setLogLevel(logger, level);
					});

					ctx.updateLoggers();
					return true;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

		return false;
	}

	/**
	 * log4j2.xmlのリソース上の設定ファイルを指定したファイルにコピーする.
	 * @param configXmlTmpl
	 * @throws IOException
	 */
	public static void copyConfigFile(Path configXmlTmpl) throws IOException {
		try (InputStream inp = LogConfigurator.class.getResourceAsStream(LOG4J2_XML)) {
			Files.copy(inp, configXmlTmpl);
		}
	}

	/**
	 * log4j2に設定されている、すべてのロガー名を取得する.
	 * @return ロガー名
	 */
	public static List<String> getLoggers() {
		final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
				.getContext(false);
		final Configuration config = ctx.getConfiguration();
		Map<String, LoggerConfig> loggers = config.getLoggers();

		return loggers.keySet().stream().sorted().collect(Collectors.toList());
	}

	/**
	 * ログレベルを設定する
	 * @param loggerName ロガー
	 * @param level レベル
	 */
	public static void setLogLevel(String loggerName,
			org.apache.logging.log4j.Level level) {
		Objects.requireNonNull(level);
		final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
				.getContext(false);
		final Configuration config = ctx.getConfiguration();

		Map<String, LoggerConfig> loggers = config.getLoggers();
		LoggerConfig loggerConfig = loggers.get(loggerName);
		if (loggerConfig != null) {
			if (!loggerConfig.getLevel().equals(level)) {
				// 該当するロガーのレベルを変更する.
				loggerConfig.setLevel(level);
				synchronized (currentLevels) {
					currentLevels.put(loggerName, level);
				}
				ctx.updateLoggers();
			}
		}
	}

	/**
	 * ロガーレベルを取得する
	 * @param loggerName ロガー名
	 * @return レベル
	 */
	public static org.apache.logging.log4j.Level getLogLevel(
			String loggerName) {
		final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
				.getContext(false);
		final Configuration config = ctx.getConfiguration();

		Map<String, LoggerConfig> loggers = config.getLoggers();
		LoggerConfig loggerConfig = loggers.get(loggerName);
		if (loggerConfig == null) {
			loggerConfig = config.getLoggerConfig(
					org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
		}
		return loggerConfig.getLevel();
	}

	/**
	 * ログ出力先フォルダにある古いログファイルは削除する.
	 * @param logDir ログディレクトリ
	 * @param expired 期限切れとなる日時(エポックタイムからのミリ秒)
	 */
	private static void purgeExpiredLogs(Path logDir, long expired) {
		File[] files = logDir.toFile().listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".log")
						&& file.lastModified() < expired) {
					// 期限切れの拡張子logのファイルを削除する.
					file.delete();
				}
			}
		}
	}

	/**
	 * 標準出力に差し替え可能なプリントストリームを作成します.<br>
	 * プリントストリームへの出力は文字列として引数のコンシューマに渡されます.<br>
	 * スレッドごとに独立したバッファをもっています.
	 * @return プリントストリーム
	 */
	public static PrintStream createLogStream(Consumer<String> logReceiver) {
		return new PrintStream(new OutputStream() {

			private ThreadLocal<ByteArrayOutputStream> bosTls = new ThreadLocal<ByteArrayOutputStream>() {
				@Override
				protected ByteArrayOutputStream initialValue() {
					return new ByteArrayOutputStream();
				}
			};

			@Override
			public void write(int b) throws IOException {
				if (b == 0x0a || b == 0x0d) {
					flush();
				} else {
					bosTls.get().write(b);
				}
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				ByteArrayOutputStream bos = bosTls.get();
				for (int idx = 0; idx < len; idx++) {
					byte c = b[off + idx];
					if (c == 0x0a || c == 0x0d) {
						flush();
					} else {
						bos.write(c);
					}
				}
			}

			@Override
			public void close() throws IOException {
				flush();
			}

			@Override
			public void flush() throws IOException {
				ByteArrayOutputStream bos = bosTls.get();
				if (bos.size() > 0) {
					String msg = new String(bos.toByteArray());
					bos.reset();

					logReceiver.accept(msg);
				}
			}
		});
	}

	/**
	 * ローリングファイルアペンダを作成して返す.<br.
	 * 返されるアペンダは開始しておらず、ロガーにも接続されていない.<br>
	 * @param appenderName アペンダ名
	 * @param logDir ログディレクトリ
	 * @param logName ログファイルのベース名(拡張子は除く)
	 * @param maxLogRollingSizeMega ログファイルのローリングサイズ(メガ単位)
	 * @param maxLogRollover 最大ローリング数
	 * @return アペンダ
	 */
	public static Appender createRollingFileAppender(String appenderName,
			String logName, Path logDir, int maxLogRollingSizeMega,
			int maxLogRollover) {
		Objects.requireNonNull(appenderName);
		Objects.requireNonNull(logDir);
		Objects.requireNonNull(logName);

		String maxLogRollingSize;
		if (maxLogRollingSizeMega > 0) {
			maxLogRollingSize = maxLogRollingSizeMega + "m";
		} else {
			maxLogRollingSize = DEFAULT_MAX_LOG_ROLLING_SIZE;
		}
		if (maxLogRollover < 0) {
			maxLogRollover = DEFAULT_MAX_LOG_ROLLOVER;
		}

		try {
			Files.createDirectories(logDir);

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		Path logFilePath = logDir.resolve(logName + ".log");
		Path bakFilePath = logDir.resolve(logName + "-%i.log");

		// log4j2のコンテキストと設定の取得
		final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
				.getContext(false);
		final Configuration config = ctx.getConfiguration();

		// ロートロガー
		org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
				.getRootLogger();

		// SnifferAppenderで設定されているフォーマットを流用する.
		Appender snifferAppender = rootLogger.getAppenders().get("Sniffer");
		if (snifferAppender == null) {
			throw new RuntimeException("log4j2.xmlにSnifferアペンダがありません.");
		}
		Layout<? extends Serializable> layout = snifferAppender.getLayout();

		// ローリングサイズ
		TriggeringPolicy sizebase = SizeBasedTriggeringPolicy
				.createPolicy(maxLogRollingSize); // size (default 10MBytes)

		// ローリング回数(デフォルトは10世代まで)
		RolloverStrategy rollover = DefaultRolloverStrategy.createStrategy(
				Integer.toString(maxLogRollover), // max (default = 7)
				null, // min (default 1)
				null, // fileIndex (default 'max')
				null, // compressionLevelStr (zip only)
				config); // config

		// ジョブログ用のローリングファイルアベンダを作成し、動的に設定する.
		return RollingFileAppender.createAppender(
				logFilePath.toString(),
				bakFilePath.toString(),
				"true", // append
				appenderName, // name
				"true", // bufferedIo
				"8192", // bufferSizeStr
				"true", // immediateFlush
				sizebase,
				rollover,
				layout,
				null, // filter,
				"true", // ignoreException
				"false", // advertise,
				null, // advertiseUri,
				config); // config);
	}

	/**
	 * アペンダをロートロガーに接続する.<br>
	 * 同名の古いロガーがある場合は解除される.
	 * @param appender
	 */
	public static void attachAppender(Appender appender) {
		Objects.requireNonNull(appender);

		// log4j2のコンテキストと設定の取得
		final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
				.getContext(false);

		// ロートロガー
		org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
				.getRootLogger();

		// 古いアペンダがあれば、それを停止してデタッチする.
		detachAppender(appender.getName());

		// アペンダを開始しルートロガーに接続する.
		appender.start();
		rootLogger.addAppender(appender);

		// 設定を反映する.
		ctx.updateLoggers();
	}

	/**
	 * アペンダ名を指定してルートロガーからデタッチして停止させる.<br>
	 * @param appenderName アペンダ名
	 * @return 対象のアペンダ、なければnull
	 */
	public static Appender detachAppender(String appenderName) {
		Objects.requireNonNull(appenderName);

		// log4j2のコンテキストと設定の取得
		final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager
				.getContext(false);

		// ロートロガー
		org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
				.getRootLogger();

		// 古い同名のアペンダがあれば削除する.
		if (StringUtils.isNotBlank(appenderName)) {
			Appender oldAppender = rootLogger.getAppenders().get(appenderName);
			if (oldAppender != null) {
				rootLogger.removeAppender(oldAppender);
				oldAppender.stop();
				System.out.println("old appender removed: " + oldAppender);

				// 設定を反映する.
				ctx.updateLoggers();

				return oldAppender;
			}
		}
		return null;
	}
}
