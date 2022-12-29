package com.maliksalman.buildpackmanifestfixer;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@RegisterReflectionForBinding({ DependencyArtifactInfo.class, MainConfiguration.class })
@EnableConfigurationProperties(MainConfiguration.class)
@SpringBootApplication
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}
