package org.example.taskmanager.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfiguration {

	@Value("${kubernetes.context:}")
	private String kubernetesContext;

	@Bean
	KubernetesClient kubernetesClient() {

		if (kubernetesContext.isBlank() ) {
			return new KubernetesClientBuilder().build();
		}

		Config config = Config.autoConfigure("rancher-desktop");
    return new KubernetesClientBuilder()
        .withConfig(config)
        .build();
	}
}

