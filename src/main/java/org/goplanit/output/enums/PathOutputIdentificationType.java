package org.goplanit.output.enums;

/**
 * Enumeration of possible Id types used to identify a Path when writing it out
 * 
 * @author gman6028
 *
 */
public enum PathOutputIdentificationType {

  /** Identify the path using link segment XML ids. */
  LINK_SEGMENT_XML_ID,
  /** Identify the path using link segment external ids. */
  LINK_SEGMENT_EXTERNAL_ID,
  /** Identify the path using internal link segment ids. */
  LINK_SEGMENT_ID,
  /** Identify the path using node external ids. */
  NODE_EXTERNAL_ID,
  /** Identify the path using node XML ids. */
  NODE_XML_ID,
  /** Identify the path using internal node ids. */
  NODE_ID;

}
