package jp.seraphyware.javafxexam.jfxexam1.util.prefs;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface PreferencesServiceParameter {
	@Nonbinding String fileName();
	@Nonbinding String resourceName() default "";
}
