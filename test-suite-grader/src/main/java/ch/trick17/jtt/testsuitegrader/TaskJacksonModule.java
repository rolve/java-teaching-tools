package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.testrunner.TestRunnerJacksonModule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MutationIdentifier;

import java.util.Collection;

public class TaskJacksonModule extends TestRunnerJacksonModule {

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.setMixInAnnotations(MutationIdentifier.class, MutationIdentifierMixin.class);
        context.setMixInAnnotations(Location.class, LocationMixin.class);
        context.setMixInAnnotations(ClassName.class, ClassNameMixin.class);
    }

    @JsonIncludeProperties({"location", "indexes", "mutator"})
    public static class MutationIdentifierMixin {
        @JsonCreator
        public MutationIdentifierMixin(@JsonProperty("location") Location location,
                                       @JsonProperty("indexes") Collection<Integer> indexes,
                                       @JsonProperty("mutator") String mutatorUniqueId) {}
    }

    public static class LocationMixin {
        @JsonCreator
        public LocationMixin(@JsonProperty("className") ClassName clazz,
                             @JsonProperty("methodName") String method,
                             @JsonProperty("methodDesc") String methodDesc) {
        }
    }

    @JsonIncludeProperties("name")
    public static class ClassNameMixin {
        @JsonCreator
        public static ClassName fromString(@JsonProperty("name") String clazz) {
            return null;
        }

        @JsonProperty("name")
        public String asInternalName() {
            return null;
        }
    }
}
