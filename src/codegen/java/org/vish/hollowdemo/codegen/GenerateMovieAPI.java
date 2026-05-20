package org.vish.hollowdemo.codegen;

import com.netflix.hollow.api.codegen.HollowAPIGenerator;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import org.vish.hollowdemo.model.Movie;

import java.io.IOException;

public class GenerateMovieAPI {
    public static void main(String[] args) throws IOException {
        HollowWriteStateEngine writeEngine = new HollowWriteStateEngine();
        HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);

        mapper.initializeTypeState(Movie.class);

        HollowAPIGenerator generator = new HollowAPIGenerator.Builder()
                .withAPIClassname("MovieAPI")
                .withPackageName("org.vish.hollowdemo.api")
                .withDataModel(writeEngine)
                .build();

        String outputPath = "src/main/java";
        System.out.println("Generating Hollow API to: " + outputPath);
        generator.generateFiles(outputPath);
        System.out.println("✅ Hollow API generated successfully!");
    }
}