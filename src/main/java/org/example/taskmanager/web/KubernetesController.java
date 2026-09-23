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
	public ResponseEntity<List<Job>> listJobs(@RequestParam(required = false) String label) {
		return ResponseEntity.ok(kubernetesService.listJobs(label));
	}

	@GetMapping("/{name}")
	public ResponseEntity<Job> getJobByName(@PathVariable String name) {
		Job job = kubernetesService.getJobsByName(name);
		if (job == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(job);
	}

	@DeleteMapping
	public ResponseEntity<Void> deleteJobs(@RequestParam(required = false) String label) {
		kubernetesService.deleteJobs(label);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{name}")
	public ResponseEntity<Void> deleteJobsByName(@PathVariable String name) {
		kubernetesService.deleteJobsByName(name);
		return ResponseEntity.noContent().build();
	}

}