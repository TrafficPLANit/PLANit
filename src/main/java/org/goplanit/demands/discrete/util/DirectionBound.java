package org.goplanit.demands.discrete.util;

/**
 * Indicates directionality relative to a parent (tour) with an origin and destination, e.g.,
 * if inbound it goes D to O, if outbound it foes O to D
 */
public enum DirectionBound {
  /** direction is inbound relative to its parent */
  INBOUND,
  /** direction is outbound relative to its parent */
  OUTBOUND
}
