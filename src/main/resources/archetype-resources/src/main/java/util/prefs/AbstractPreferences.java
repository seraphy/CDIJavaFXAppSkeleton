#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.prefs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPreferences implements Preferences {

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractPreferences.class);

	private final AtomicBoolean modified = new AtomicBoolean();

	public abstract Properties getProperties();

	public boolean isModified() {
		return modified.get();
	}

	public void flush() {
		if (modified.getAndSet(false)) {
			try {
				save();

			} catch (IOException ex) {
				logger.error("failed to save properties", ex);
				throw new UncheckedIOException(ex);
			}
		}
	}

	public String getProperty(String key) {
		flush();
		return getProperties().getProperty(key);
	}

	public Set<String> getPropertyNames() {
		return getPropertyNamesStartsWith(null);
	}

	public Set<String> getPropertyNamesStartsWith(String prefix) {
		flush();
		Set<String> names = new TreeSet<>();
		names.addAll(getProperties().stringPropertyNames());

		if (prefix != null && prefix.length() > 0) {
			names.removeIf(item -> !item.startsWith(prefix));
		}

		return names;
	}

	public String getProperty(String key, String defaultValue) {
		flush();
		String val = this.getProperty(key);
		if (val == null) {
			val = defaultValue;
		}
		return val;
	}

	public int getPropertyInt(String key, int defaultValue) {
		flush();
		try {
			return Integer.parseInt(getProperty(key,
					Integer.toString(defaultValue)));

		} catch (RuntimeException ex) {
			return defaultValue;
		}
	}

	@Override
	public long getPropertyLong(String key, int defaultValue) {
		flush();
		try {
			return Long.parseLong(getProperty(key,
					Long.toString(defaultValue)));

		} catch (RuntimeException ex) {
			return defaultValue;
		}
	}

	@Override
	public float getPropertyFloat(String key, float defaultValue) {
		flush();
		try {
			return Float.parseFloat(getProperty(key,
					Float.toString(defaultValue)));

		} catch (RuntimeException ex) {
			return defaultValue;
		}
	}

	public double getPropertyDouble(String key, double defaultValue) {
		flush();
		try {
			return Double.parseDouble(getProperty(key,
					Double.toString(defaultValue)));

		} catch (RuntimeException ex) {
			return defaultValue;
		}
	}

	public boolean getPropertyBoolean(String key, boolean defaultValue) {
		flush();
		try {
			return Boolean.parseBoolean(getProperty(key,
					Boolean.toString(defaultValue)));

		} catch (RuntimeException ex) {
			return defaultValue;
		}
	}

	@Override
	public Path getPropertyPath(String key, Path defaultValue) {
		flush();
		try {
			String strPath = getProperty(key, null);
			if (strPath != null && strPath.trim().length() > 0) {
				return Paths.get(strPath.trim());
			}

		} catch (RuntimeException ex) {
			// 何もしない
		}
		return defaultValue;
	}

	@Override
	public <E extends Enum<E>> Enum<E> getPropertyEnum(String key,
			Enum<E> defaultValue) {
		flush();
		Objects.requireNonNull(defaultValue);
		try {
			String name = getProperty(key, null);
			if (name != null && name.trim().length() > 0) {
				Enum.valueOf(defaultValue.getDeclaringClass(), name);
			}

		} catch (RuntimeException ex) {
			// 何もしない
		}
		return defaultValue;
	}

	public void setProperties(Properties inputProps) {
		if (inputProps == null) {
			return;
		}
		for (String key : inputProps.stringPropertyNames()) {
			String value = inputProps.getProperty(key);
			if (value == null) {
				value = "";
			}
			setProperty(key, value);
		}
		flush();
	}

	public void setProperty(String key, String value) {
		if (value == null) {
			value = "";
		}

		Properties props = getProperties();
		String oldValue = props.getProperty(key);

		if (!Objects.equals(oldValue, value)) {
			logger.info("set preferences key=" + key + "/value=" + value);
			props.setProperty(key, value);
			modified.set(true); // 連続して更新することを想定し、フラグを立てるのみ
		}
	}

	@Override
	public void setPropertyInt(String key, int value) {
		setProperty(key, Integer.toString(value));
	}

	@Override
	public void setPropertyLong(String key, long value) {
		setProperty(key, Long.toString(value));
	}

	@Override
	public void setPropertyFloat(String key, float value) {
		setProperty(key, Float.toString(value));
	}

	@Override
	public void setPropertyDouble(String key, double value) {
		setProperty(key, Double.toString(value));
	}

	@Override
	public void setPropertyBoolean(String key, boolean value) {
		setProperty(key, Boolean.toString(value));
	}

	@Override
	public void setPropertyPath(String key, Path value) {
		setProperty(key, (value == null) ? "" : value.toString());
	}

	@Override
	public <E extends Enum<E>> void setPropertyEnum(String key, Enum<E> value) {
		Objects.requireNonNull(value);
		setProperty(key, value.name());
	}

}