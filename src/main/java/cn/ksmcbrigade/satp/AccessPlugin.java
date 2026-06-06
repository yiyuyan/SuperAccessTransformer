package cn.ksmcbrigade.satp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;

public class AccessPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {

        AccessExtension extension = project.getExtensions()
                .create("superAccessTransformer", AccessExtension.class);

        project.getDependencies().registerTransform(AccessAction.class, spec -> {
            spec.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar");
            spec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "public-jar");
            spec.parameters(params -> params.getTargetPackages().set(extension.getPackages()));
        });

        project.afterEvaluate(p -> {
            p.getConfigurations().matching(c ->
                    c.getName().equals("compileClasspath") ||
                            c.getName().equals("runtimeClasspath") ||
                            c.getName().equals("testCompileClasspath") ||
                            c.getName().equals("testRuntimeClasspath")
            ).all(config -> {
                config.getAttributes().attribute(
                        ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "public-jar"
                );
            });

            //for fabric
            p.getConfigurations().matching(c -> c.getName().equals("minecraft"))
                    .all(config -> config.getAttributes().attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "public-jar"
                    ));
        });
    }
}