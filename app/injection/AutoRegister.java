package injection;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denote an auto registered guice Module.
 *
 * @author marco
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {
}
