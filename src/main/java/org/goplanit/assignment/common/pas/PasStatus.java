package org.goplanit.assignment.common.pas;

/**
 * Status of a PAS
 * Note: used originally to speed up calcs by computing uncongested flow shifts separately and without node model
 * updates. This is now abandonded until convergence is more stable. Once stable we can revisit this optimization.
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
