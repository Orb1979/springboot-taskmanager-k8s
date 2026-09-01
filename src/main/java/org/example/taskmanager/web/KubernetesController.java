package org.example.taskmanager.web;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import lombok.RequiredArgsConstructor;
import org.example.taskmanager.service.KubernetesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kubernetes/jobs")
@RequiredArgsConstructor
public class KubernetesController {
	private final KubernetesService kubernetesService;

	@GetMapping
	public ResponseEntity<List<Job>> getAllJobs() {
		return ResponseEntity.ok(kubernetesService.getAllJobs());
	}

	@GetMapping("/{name}")
	public ResponseEntity<Job> getJobByName(@PathVariable String name) {
		Job job = kubernetesService.getJobsByName(name);
		if (job == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(job);
	}

	@GetMapping("/label/{label}")
	public ResponseEntity<List<Job>> getJobsByLabel(@PathVariable String label) {
		return ResponseEntity.ok(kubernetesService.getJobsByLabel(label));
	}

	@DeleteMapping
	public ResponseEntity<Void> deleteAllJobs() {
		kubernetesService.deleteAllJobs();
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{name}")
	public ResponseEntity<Void> deleteJobsByName(@PathVariable String name) {
		kubernetesService.deleteJobsByName(name);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/label/{label}")
	public ResponseEntity<Void> deleteJobsByLabel(@PathVariable String label) {
		kubernetesService.deleteJobsByLabel(label);
		return ResponseEntity.noContent().build();
	}

}