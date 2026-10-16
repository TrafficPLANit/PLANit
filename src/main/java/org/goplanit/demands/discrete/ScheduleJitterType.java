package org.goplanit.demands.discrete;

/**
 * Strategy for spreading discrete demand schedules within their originating time bin, used to remove the peaking
 * that results from all elements in a bin sharing that bin's exact start instant.
 *
 * @author markr
 */
public enum ScheduleJitterType {

  /**
   * Draw a single offset per person and shift that person's entire schedule rigidly by it. Spreads the aggregate
   * profile uniformly across the bin, but leaves elements sharing a bin exactly simultaneous and same-bin tours
   * with a zero duration.
   */
  RIGID_PERSON,

  /**
   * Split each time bin into as many sub-bins as it holds points and draw each point within its own sub-bin,
   * where a point is a trip start or a tour end. Tour starts are aliased to their outbound trip, being the same
   * event. Preserves the aggregate spread of {@code RIGID_PERSON} while removing same-bin collisions and zero
   * duration tours. Spreads forward only, so any symmetric setting is ignored.
   */
  SUB_BIN;
}
