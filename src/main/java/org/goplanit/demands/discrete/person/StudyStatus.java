package org.goplanit.demands.discrete.person;

/**
 * Whether a person studies and where. A person whose study is not known has none.
 */
public enum StudyStatus {

  /** at school, preschool included */
  SCHOOL,

  /** at a university or another place of education after school */
  TERTIARY,

  NOT_STUDYING
}
