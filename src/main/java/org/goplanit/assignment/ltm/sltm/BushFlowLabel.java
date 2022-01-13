package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Label that track a unique composition of flow along an edge of a bush acyclic directed graph. In addition to the label itself it also carries information regarding its
 * predecessor (if any). Predecessor composition labels are other flow compositions upstream of this composition that (partly) split off flow of this composition label at diverges,
 * resulting in a non-zero splitting rate between the two labels at this diverge.
 * 
 * @author markr
 *
 */
public class BushFlowLabel implements Comparable<BushFlowLabel> {

  /**
   * the id label
   */
  private final long id;

  /**
   * the name (if any)
   */
  private final String name;

  /**
   * Constructor
   * 
   * @param idToken to use
   */
  private BushFlowLabel(final IdGroupingToken idToken, final String name) {
    this.id = generateId(idToken);
    this.name = name;
  }

  /**
   * Constructor
   * 
   * @param idToken to use
   */
  private BushFlowLabel(final IdGroupingToken idToken) {
    this(idToken, null);
  }

  /**
   * Generate id
   * 
   * @param idToken to use
   * @return the generated id
   */
  protected static final long generateId(final IdGroupingToken idToken) {
    return IdGenerator.generateId(idToken, BushFlowLabel.class);
  }

  /**
   * The unique label id within its context
   * 
   * @return the label id
   */
  public long getLabelId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    return Long.valueOf(getLabelId()).equals(o);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return (int) id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(BushFlowLabel o) {
    return Long.compare(getLabelId(), o.getLabelId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return (name != null ? "name:" + name : "") + " id: " + id;
  }

  /**
   * Create a new flow composition label for this bush
   * 
   * @param idToken to use
   * @return created label
   */
  public static BushFlowLabel create(IdGroupingToken idToken) {
    return new BushFlowLabel(idToken);
  }

  /**
   * Create a new flow composition label for this bush
   * 
   * @param idToken to use
   * @param name    of the label
   * @return created label
   */
  public static BushFlowLabel create(IdGroupingToken idToken, String name) {
    return new BushFlowLabel(idToken, name);
  }

}
