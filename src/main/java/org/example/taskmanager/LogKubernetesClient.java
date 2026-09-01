package org.example.taskmanager;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class LogKubernetesClient {
  private final KubernetesClient client;

  @EventListener(ApplicationReadyEvent.class)
  public void checkConnections() {
    try {
      VersionInfo version = client.getKubernetesVersion();
      log.info(
          "Kubernetes client connected. Master URL: {}, Namespace: {}, Server Version: {}",
          client.getMasterUrl(),
          client.getNamespace(),
          version != null ? version.getGitVersion() : "unknown");
    } catch (Exception e) {
      log.error("Failed to connect to Kubernetes API server", e);
    }
  }
}