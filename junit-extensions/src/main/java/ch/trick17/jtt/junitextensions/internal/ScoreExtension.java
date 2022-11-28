package ch.trick17.jtt.junitextensions.internal;

import ch.trick17.jtt.junitextensions.Score;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static java.util.logging.Logger.getLogger;
import static org.junit.platform.commons.support.HierarchyTraversalMode.BOTTOM_UP;
import static org.junit.platform.commons.support.ModifierSupport.isNotStatic;
import static org.junit.platform.commons.support.ModifierSupport.isPublic;
import static org.junit.platform.commons.support.ReflectionSupport.findFields;

public class ScoreExtension implements AfterEachCallback {

    private static final Logger logger = getLogger(ScoreExtension.class.getName());

    @Override
    public void afterEach(ExtensionContext context) throws IllegalAccessException {
        var instance = context.getRequiredTestInstance();
        var testClass = instance.getClass();
        var scoreFields = findFields(testClass, this::isScoreField, BOTTOM_UP);
        if (scoreFields.size() == 0) {
            throw new AssertionError("no @Score field found on class " + testClass
                    + " (must be public, non-static, with type Double)");
        } else if (scoreFields.size() > 1) {
            logger.warning("multiple @Score fields found");
        }

        var score = (Double) scoreFields.get(0).get(instance);
        if (score == null) {
            System.out.println(context.getDisplayName() + ": no score");
        } else {
            System.out.println(context.getDisplayName() + " score: " + score);
        }
    }

    private boolean isScoreField(Field f) {
        return f.isAnnotationPresent(Score.class)
                && isPublic(f)
                && isNotStatic(f)
                && f.getType() == Double.class;
    }
}
