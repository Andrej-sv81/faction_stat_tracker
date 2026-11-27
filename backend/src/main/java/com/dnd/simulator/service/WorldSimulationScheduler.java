package com.dnd.simulator.service;

import com.dnd.simulator.dto.ApiDtos;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorldSimulationScheduler {
  private static final Logger logger = LoggerFactory.getLogger(WorldSimulationScheduler.class);
  private static final int DEFAULT_INTERVAL_MINUTES = 5;
  private static final int DEFAULT_SEVERITY = 3;

  private final WorldService worlds;
  private final WorldSimulationService simulations;
  private final ScheduledExecutorService executor =
      Executors.newScheduledThreadPool(
          2,
          task -> {
            Thread thread = new Thread(task, "world-simulation");
            thread.setDaemon(true);
            return thread;
          });
  private final Map<String, SimulationJob> jobs = new HashMap<>();

  public WorldSimulationScheduler(WorldService worlds, WorldSimulationService simulations) {
    this.worlds = worlds;
    this.simulations = simulations;
  }

  public synchronized ApiDtos.SimulationStatusResponse start(
      String worldId,
      String ownerId,
      ApiDtos.SimulationControlRequest request) {
    worlds.get(worldId, ownerId);
    validate(request);

    SimulationJob current = jobs.get(worldId);
    if (current != null && current.isRunning()) {
      return current.status();
    }

    SimulationJob job =
        new SimulationJob(ownerId, request.intervalMinutes(), request.severity());
    job.future =
        executor.scheduleWithFixedDelay(
            () -> runTurn(worldId, job),
            request.intervalMinutes(),
            request.intervalMinutes(),
            TimeUnit.MINUTES);
    jobs.put(worldId, job);
    return job.status();
  }

  public synchronized ApiDtos.SimulationStatusResponse stop(String worldId, String ownerId) {
    worlds.get(worldId, ownerId);
    SimulationJob job = jobs.get(worldId);
    if (job == null) {
      return stoppedStatus(DEFAULT_INTERVAL_MINUTES, DEFAULT_SEVERITY, null);
    }
    if (job.future != null) {
      job.future.cancel(false);
      job.future = null;
    }
    return job.status();
  }

  public synchronized ApiDtos.SimulationStatusResponse status(String worldId, String ownerId) {
    worlds.get(worldId, ownerId);
    SimulationJob job = jobs.get(worldId);
    return job == null
        ? stoppedStatus(DEFAULT_INTERVAL_MINUTES, DEFAULT_SEVERITY, null)
        : job.status();
  }

  private void runTurn(String worldId, SimulationJob job) {
    try {
      simulations.simulate(worldId, job.ownerId, job.severity);
      job.lastError = null;
    } catch (RuntimeException exception) {
      job.lastError = "The most recent turn failed. Check the server logs.";
      logger.error("Periodic simulation failed for world {}", worldId, exception);
    }
  }

  private void validate(ApiDtos.SimulationControlRequest request) {
    if (request.intervalMinutes() < 1 || request.intervalMinutes() > 1440) {
      throw new IllegalArgumentException("Simulation interval must be between 1 and 1440 minutes");
    }
    if (request.severity() < 1 || request.severity() > 5) {
      throw new IllegalArgumentException("Simulation severity must be between 1 and 5");
    }
  }

  private ApiDtos.SimulationStatusResponse stoppedStatus(
      int intervalMinutes, int severity, String lastError) {
    return new ApiDtos.SimulationStatusResponse(false, intervalMinutes, severity, lastError);
  }

  @PreDestroy
  public synchronized void shutdown() {
    jobs.values().forEach(
        job -> {
          if (job.future != null) {
            job.future.cancel(false);
          }
        });
    jobs.clear();
    executor.shutdownNow();
  }

  private final class SimulationJob {
    private final String ownerId;
    private final int intervalMinutes;
    private final int severity;
    private volatile String lastError;
    private ScheduledFuture<?> future;

    private SimulationJob(String ownerId, int intervalMinutes, int severity) {
      this.ownerId = ownerId;
      this.intervalMinutes = intervalMinutes;
      this.severity = severity;
    }

    private boolean isRunning() {
      return future != null && !future.isDone();
    }

    private ApiDtos.SimulationStatusResponse status() {
      return new ApiDtos.SimulationStatusResponse(
          isRunning(), intervalMinutes, severity, lastError);
    }
  }
}
