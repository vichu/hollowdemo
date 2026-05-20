package org.vish.hollowdemo.codegen;

import com.netflix.hollow.api.codegen.HollowAPIGenerator;
import com.netflix.hollow.core.write.HollowWriteStateEngine;
import com.netflix.hollow.core.write.objectmapper.HollowObjectMapper;
import org.vish.hollowdemo.model.User;

import java.io.IOException;

public class GenerateUserAPI {
    public static void main(String[] args) throws IOException {
        HollowWriteStateEngine writeEngine = new HollowWriteStateEngine();
        HollowObjectMapper mapper = new HollowObjectMapper(writeEngine);

        mapper.initializeTypeState(User.class);

        HollowAPIGenerator generator = new HollowAPIGenerator.Builder()
                .withAPIClassname("UserAPI")
                .withPackageName("org.vish.hollowdemo.api.users")
                .withDataModel(writeEngine)
                .build();

        String outputPath = "src/main/java";
        System.out.println("Generating User Hollow API to: " + outputPath);
        generator.generateFiles(outputPath);
        System.out.println("✅ User Hollow API generated successfully!");
    }
}
