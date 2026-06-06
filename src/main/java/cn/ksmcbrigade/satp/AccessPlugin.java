package cn.ksmcbrigade.satp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;

public class AccessPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AccessExtension extension = project.getExtensions()
                .create("superAccessTransformer", AccessExtension.class);

        project.getDependencies().registerTransform(AccessAction.class, spec -> {
            spec.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar");
            spec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "public-jar");
            spec.parameters(params -> {
                params.getTargetPackages().set(extension.getPackages());
                params.getSkipMethods().set(extension.getSkipMethods());
            });
        });

        //for neo forge
        project.getDependencies().attributesSchema(schema ->
                schema.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE)
                        .getCompatibilityRules()
                        .add(ZipCompatibilityRule.class)
        );

        project.afterEvaluate(p -> {
            p.getConfigurations().matching(c ->
                    c.getName().equals("compileClasspath") ||
                            c.getName().equals("runtimeClasspath") ||
                            c.getName().equals("testCompileClasspath") ||
                            c.getName().equals("testRuntimeClasspath")
            ).all(config -> config.getAttributes().attribute(
                    ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "public-jar"
            ));

            //for fabric loom
            p.getConfigurations().matching(c -> c.getName().equals("minecraft"))
                    .all(config -> config.getAttributes().attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "public-jar"
                    ));
        });
    }


    public static class ZipCompatibilityRule implements AttributeCompatibilityRule<String> {
        @Override
        public void execute(CompatibilityCheckDetails<String> details) {
            String consumerValue = details.getConsumerValue();
            String producerValue = details.getProducerValue();
            if ("public-jar".equals(consumerValue) && "zip".equals(producerValue)) {
                details.compatible();
            }
        }
    }
}