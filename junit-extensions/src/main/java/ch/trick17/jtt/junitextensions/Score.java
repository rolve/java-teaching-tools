package ch.trick17.jtt.junitextensions;

import ch.trick17.jtt.junitextensions.internal.ScoreExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Can be used to collect "scores" from tests. Put this annotation on a
 * <code>public</code>, non-<code>static</code> field of type {@link Double}
 * (not <code>double</code>) in a test class and assign a non-<code>null</code>
 * value in a test method to register a score.
 */
@Target(FIELD)
@Retention(RUNTIME)
@ExtendWith(ScoreExtension.class)
public @interface Score {
}
