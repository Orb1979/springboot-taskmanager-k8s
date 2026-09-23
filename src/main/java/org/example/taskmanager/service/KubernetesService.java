package org.example.taskmanager.service;


import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.taskmanager.exception.K8sJobAlreadyExistException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

@Service
@Log4j2
@RequiredArgsConstructor
public class KubernetesService {
  private final KubernetesClient kubernetesClient;

  public void createJob(String name, String image, Function<ContainerBuilder, ContainerBuilder > config) {
    Job job = getJobsByName(name);
    if (job != null) {
      throw new K8sJobAlreadyExistException("Job '" + name + "' already exists in the cluster.");
    }

    // Build the container
    Container container = config.apply(new ContainerBuilder()
            .withName(name)
            .withImage(image)
            .withImagePullPolicy("Never"))
        .build();

    // Plug that container into the Job
    Job k8sJob = new JobBuilder()
        .withNewMetadata()
          .withName(name)
          .addToLabels("name", "worker") // Add KV label for easier cleanup or lookup
        .endMetadata()
        .withNewSpec()
        .withTtlSecondsAfterFinished(60)
          .withNewTemplate()
            .withNewSpec()
              .addToContainers(container) // add the pre-built container
              .withRestartPolicy("Never")
            .endSpec()
          .endTemplate()
        .endSpec()
        .build();

    //  Spin up the Job in the cluster
   try {
      kubernetesClient.batch().v1().jobs().inNamespace("default").resource(k8sJob).create();
      log.info("Successfully launched Kubernetes job: {}", name);
    } catch (KubernetesClientException e) {
      log.error("Failed to launch job '{}' due to an unexpected error", name, e);
      throw e;
    }
  }

  /** @param label optional value of Kubernetes label key {@code name}; omit to list all jobs */
  public List<Job> listJobs(String label) {
    var jobs = kubernetesClient.batch().v1().jobs().inNamespace("default");
    if (StringUtils.hasText(label)) {
      return jobs.withLabel("name", label).list().getItems();
    }
    return jobs.list().getItems();
  }

  public Job getJobsByName(String name) {
    return kubernetesClient.batch().v1().jobs().
        inNamespace("default")
        .withName(name)
        .get();
  }

  /** @param label optional value of Kubernetes label key {@code name}; omit to delete all jobs */
  public void deleteJobs(String label) {
    try {
      var jobs = kubernetesClient.batch().v1().jobs().inNamespace("default");
      if (StringUtils.hasText(label)) {
        jobs.withLabel("name", label).delete();
        log.info("Successfully deleted Kubernetes jobs with label name={}", label);
      } else {
        jobs.delete();
        log.info("Successfully deleted all Kubernetes jobs");
      }
    } catch (KubernetesClientException e) {
      log.error("Failed to delete Kubernetes jobs due to an unexpected error", e);
      throw e;
    }
  }

  public void deleteJobsByName(String name) {
    try {
      kubernetesClient.batch().v1().jobs().inNamespace("default").withName(name).delete();
      log.info("Successfully deleted Kubernetes job for name {}", name);
    } catch (KubernetesClientException e) {
      log.error("Failed to delete Kubernetes job due to an unexpected error", e);
      throw e;
    }
  }
}
