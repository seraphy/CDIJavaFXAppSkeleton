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
public class WindowSizePersistentProducer {

	@Inject
	@Default
	private Instance<WindowSizePersistent> prov;

	@Produces
	@WindowSizePersistentPrefix("")
	@Dependent
	public WindowSizePersistent create(InjectionPoint ip) {
		WindowSizePersistentPrefix prefixAnnt =
				ip.getAnnotated().getAnnotation(WindowSizePersistentPrefix.class);
		String prefix = prefixAnnt.value();
		if (prefix == null || prefix.trim().length() == 0) {
			throw new IllegalArgumentException("@WindowSizePersistentPrefix must be specified.");
		}

		WindowSizePersistent inst = prov.get();
		inst.setPrefix(prefix);
		return inst;
	}
}
