package org.goplanit.assignment.common.pas;

/**
 * Status of a PAS
 */
public enum PasStatus {
  UNKNOWN,
  CONGESTED,
  UNCONGESTED_WITHOUT_SHIFT,
  UNCONGESTED_POTENTIALLY_CONGESTED,
  UNCONGESTED_WITH_SHIFT
}
