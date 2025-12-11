package org.goplanit.assignment.common.pas;

/**
 * Status of a PAS
 * Note: used originally to speed up calcs by computing uncongested flow shifts separately and without node model
 * updates. This is now abandonded until convergence is more stable. Once stable we can revisit this optimization.
 */
public enum PasStatus {
  UNKNOWN,
  CONGESTED,
  UNCONGESTED_WITHOUT_SHIFT,
//  UNCONGESTED_POTENTIALLY_CONGESTED,
  UNCONGESTED_WITH_SHIFT
}
