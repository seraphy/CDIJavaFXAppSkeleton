#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.prefs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@ApplicationScoped
public class PreferencesServiceProducer {

	@Inject
	@Default
	private Instance<PreferencesService> prov;

	@Produces
	@PreferencesServiceParameter(fileName = "")
	@Dependent
	public PreferencesService create(InjectionPoint ip) {
		PreferencesServiceParameter parameterAnnt =
				ip.getAnnotated().getAnnotation(PreferencesServiceParameter.class);
		String fileName = parameterAnnt.fileName();
		String resourceName = parameterAnnt.resourceName();
		if (fileName == null || fileName.trim().length() == 0) {
			throw new IllegalArgumentException("filename must be specified.");
		}

		PreferencesService inst = prov.get();
		inst.setFileNameForUser(fileName);
		inst.setResourceNameForAllUser(resourceName);
		return inst;
	}

}
