package jp.seraphyware.javafxexam.jfxexam1.util.prefs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import jp.seraphyware.javafxexam.jfxexam1.util.prefs.PriorityPropertiesManager.SimplePrioritySource;
import jp.seraphyware.javafxexam.jfxexam1.util.sys.DataFolderService;

@Dependent
public class PreferencesService extends AbstractPreferences {

	private static final Map<SimplePrioritySource, PriorityPropertiesManager> sharedProperties = new ConcurrentHashMap<>();

	private PriorityPropertiesManager priorityPropertiesMgr;

	private String fileNameForUser = "preferences.xml";

	private String resourceNameForAllUser = null;

	@Inject
	private DataFolderService dataFolderService;

	public String getFileNameForUser() {
		return fileNameForUser;
	}

	public void setFileNameForUser(String fileNameForUser) {
		Objects.requireNonNull(fileNameForUser);
		this.fileNameForUser = fileNameForUser;
	}

	public String getResourceNameForAllUser() {
		return resourceNameForAllUser;
	}

	public void setResourceNameForAllUser(String resourceNameForAllUser) {
		this.resourceNameForAllUser = resourceNameForAllUser;
	}

	@PreDestroy
	public void destroy() {
		if (isModified()) {
			flush();
		}
	}

	protected SimplePrioritySource createPreferenceSource() {
		Path path = dataFolderService.getApplicationDataFolder()
				.resolve(fileNameForUser);
		return new SimplePrioritySource(resourceNameForAllUser, path);
	}

	protected synchronized void init() {
		if (priorityPropertiesMgr == null) {
			SimplePrioritySource source = createPreferenceSource();
			priorityPropertiesMgr = sharedProperties.computeIfAbsent(source,
					k -> new PriorityPropertiesManager(source));
		}
	}

	@Override
	public Properties getProperties() {
		init();
		return priorityPropertiesMgr.getProperties();
	}

	@Override
	public void save() throws IOException {
		if (priorityPropertiesMgr != null) {
			priorityPropertiesMgr.save();
		}
	}
}
