package org.goplanit.demands.discrete.person;

/**
 * Whether a person works and how much. A person whose employment is not known has none.
 */
public enum EmploymentStatus {

  FULL_TIME,

  PART_TIME,

  /** not working, which includes children */
  NOT_EMPLOYED
}
