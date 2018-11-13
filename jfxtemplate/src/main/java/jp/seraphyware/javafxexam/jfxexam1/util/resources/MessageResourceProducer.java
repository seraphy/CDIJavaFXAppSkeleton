package jp.seraphyware.javafxexam.jfxexam1.util.resources;

import java.util.ResourceBundle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

@ApplicationScoped
public class MessageResourceProducer {

	@Produces
	@MessageResourceParameter
	@Dependent
	public ResourceBundle getMessages(InjectionPoint ip) {
		MessageResourceParameter parameterAnnt = ip.getAnnotated().getAnnotation(MessageResourceParameter.class);
		String resourceName = parameterAnnt.resourceName();
		if (resourceName == null || resourceName.trim().length() == 0) {
			throw new IllegalArgumentException("resourceName must be specified."); //$NON-NLS-1$
		}

		return ResourceBundle.getBundle(resourceName, new XMLResourceBundleControl());
	}
}
