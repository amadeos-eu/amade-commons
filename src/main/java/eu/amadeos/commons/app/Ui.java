package eu.amadeos.commons.app;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the {@link java.util.concurrent.Executor} that runs on the event dispatch thread, where results of
 * background work continue. {@link CommonsModule} binds it to the {@link eu.amadeos.commons.swing.EdtExecutor};
 * presenters take it as a plain {@code Executor}, so tests can pass a direct or queueing one instead.
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface Ui {
}
