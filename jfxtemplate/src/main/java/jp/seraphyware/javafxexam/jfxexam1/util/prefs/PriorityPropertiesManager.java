package jp.seraphyware.javafxexam.jfxexam1.util.prefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityPropertiesManager {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public interface PrioritySource {

		int getNumOfLevels();

		InputStream getInputStream(int level) throws IOException;

		OutputStream getOutputStream() throws IOException;
	}

	public static class SimplePrioritySource implements PrioritySource {

		private final String resourceName;

		private final Path fileName;

		@FunctionalInterface
		public interface InputStreamFactory {

			InputStream getInputStream() throws IOException;
		}

		private List<InputStreamFactory> sups = new ArrayList<>();

		public SimplePrioritySource(String resourceName, Path fileName) {
			this.resourceName = resourceName;
			this.fileName = fileName;

			if (StringUtils.isNotBlank(resourceName)) {
				sups.add(createResourceInputStream(resourceName));
			}
			if (fileName != null) {
				sups.add(createFileInputStream(fileName));
			}
		}

		protected InputStreamFactory createResourceInputStream(
				String resourceName) {
			return () -> getClass().getResourceAsStream(resourceName);
		}

		protected InputStreamFactory createFileInputStream(Path fileName) {
			return () -> {
				if (Files.isReadable(fileName)) {
					return Files.newInputStream(fileName);
				}
				return null;
			};
		}

		@Override
		public int hashCode() {
			return Objects.hash(resourceName, fileName);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SimplePrioritySource) {
				SimplePrioritySource o = (SimplePrioritySource) obj;
				return Objects.equals(resourceName, o.resourceName)
						&& Objects.equals(fileName, o.fileName);
			}
			return false;
		}

		public Path getFileName() {
			return fileName;
		}

		public String getResourceName() {
			return resourceName;
		}

		@Override
		public int getNumOfLevels() {
			return sups.size();
		}

		@Override
		public InputStream getInputStream(int level) throws IOException {
			return sups.get(level).getInputStream();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			if (fileName != null) {
				Path parent = fileName.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				return Files.newOutputStream(fileName);
			}
			return null;
		}

		@Override
		public String toString() {
			return "resourceName=" + resourceName + ", fileName=" + fileName;
		}
	}

	private PrioritySource source;

	private Properties properties;

	public PriorityPropertiesManager(PrioritySource source) {
		Objects.requireNonNull(source);
		this.source = source;
	}

	public PrioritySource getSource() {
		return source;
	}

	protected Properties loadProperties() {
		Properties last = null;
		for (int level = 0; level < source.getNumOfLevels(); level++) {
			Properties prop = new Properties(last);
			last = prop;

			try (InputStream is = source.getInputStream(level)) {
				if (is != null) {
					prop.loadFromXML(is);
				} else {
					logger.warn("properties not found.: level={}, source={}",
							level, source);
				}

			} catch (IOException ex) {
				logger.error("failed to load properties: level={}, source={}",
						level, source, ex);
				throw new UncheckedIOException(ex);
			}
		}
		if (last == null) {
			last = new Properties(); // 読み込むものがない場合は空を返す.
		}
		return last;
	}

	public void save() throws IOException {
		try (OutputStream os = source.getOutputStream()) {
			if (os != null) {
				getProperties().storeToXML(os, getClass().toString());
				logger.info("saved");
			} else {
				logger.warn("can't save properties. source={}", source);
			}
		}
	}

	public synchronized Properties getProperties() {
		if (properties == null) {
			properties = loadProperties();
			assert properties != null;
		}
		return properties;
	}
}
