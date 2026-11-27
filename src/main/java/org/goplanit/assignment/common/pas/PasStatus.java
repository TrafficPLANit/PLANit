package org.goplanit.assignment.common.pas;

/**
 * Status of a PAS
 * Note: originally used to speed up computation by computing uncongested flow shifts separately, without node model
 * updates. This is set aside until convergence is more stable, after which this optimisation can be revisited.
 */
public enum PasStatus {
  /** Congestion state has not yet been determined. */
  UNKNOWN,
  /** PAS currently exhibits congestion that requires the congested handling path. */
  CONGESTED,
  /** PAS is uncongested and no flow shift is applied. */
  UNCONGESTED_WITHOUT_SHIFT,
//  UNCONGESTED_POTENTIALLY_CONGESTED,
  /** PAS is uncongested but still receives a flow shift. */
  UNCONGESTED_WITH_SHIFT
}
