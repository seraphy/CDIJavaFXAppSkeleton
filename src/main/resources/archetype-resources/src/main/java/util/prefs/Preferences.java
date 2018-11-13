#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.prefs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public interface Preferences {

	String getProperty(String key);

	String getProperty(String key, String defaultValue);

	int getPropertyInt(String key, int defaultValue);

	long getPropertyLong(String key, int defaultValue);

	float getPropertyFloat(String key, float defaultValue);

	double getPropertyDouble(String key, double defaultValue);

	boolean getPropertyBoolean(String key, boolean defaultValue);

	Path getPropertyPath(String key, Path defaultValue);

	<E extends Enum<E>> Enum<E> getPropertyEnum(String key, Enum<E> defaultValue);

	void setProperties(Properties inputProps);

	void setProperty(String key, String value);

	void setPropertyInt(String key, int value);

	void setPropertyLong(String key, long value);

	void setPropertyFloat(String key, float value);

	void setPropertyDouble(String key, double value);

	void setPropertyBoolean(String key, boolean value);

	void setPropertyPath(String key, Path value);

	<E extends Enum<E>> void setPropertyEnum(String key, Enum<E> value);

	void save() throws IOException;
}
