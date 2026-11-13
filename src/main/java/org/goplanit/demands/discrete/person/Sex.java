package org.goplanit.demands.discrete.person;

/**
 * The sex of a person. Next to female and male there is a third value, as newer surveys and censuses allow a third
 * answer. A person whose sex is not known has none.
 */
public enum Sex {

  FEMALE,

  MALE,

  /** neither female nor male */
  OTHER
}
