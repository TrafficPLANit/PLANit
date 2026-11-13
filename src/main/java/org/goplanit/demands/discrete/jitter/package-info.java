/**
 * Temporal jitter of discrete demand schedules: spreading the times of trips and tours within the time bins they were
 * read in, while keeping every person's day in order, also when several persons share a tour. The entry point is
 * {@link org.goplanit.demands.discrete.jitter.ScheduleJitterExecutor}, the other classes are its parts
 */
package org.goplanit.demands.discrete.jitter;
