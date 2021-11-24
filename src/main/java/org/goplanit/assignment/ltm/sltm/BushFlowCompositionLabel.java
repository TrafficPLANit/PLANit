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
public class BushFlowCompositionLabel {

  /**
   * the id label
   */
  private final long id;

  /**
   * Generate id
   * 
   * @param idToken to use
   * @return the generated id
   */
  protected static final long generateId(final IdGroupingToken idToken) {
    return IdGenerator.generateId(idToken, BushFlowCompositionLabel.class);
  }

  /**
   * Constructor
   * 
   * @param idToken to use
   */
  public BushFlowCompositionLabel(final IdGroupingToken idToken) {
    this.id = generateId(idToken);
  }

  /**
   * the unique label id within its context
   */
  public long getLabelId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object label) {
    if (this == label) {
      return true;
    }

    try {
      return getLabelId() == ((BushFlowCompositionLabel) label).getLabelId();
    } catch (Exception e) {
      return false;
    }
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
  public String toString() {
    return String.format("%d", id);
  }

}
