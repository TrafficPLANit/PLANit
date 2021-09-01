package org.planit.supply.fundamentaldiagram;

import org.planit.utils.id.IdGroupingToken;

/**
 * Newell fundamental diagram traffic component
 *
 * @author markr
 *
 */
public class NewellFundamentalDiagramComponent extends FundamentalDiagramComponent {

  /** generated UID */
  private static final long serialVersionUID = -3166623064510413929L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NewellFundamentalDiagramComponent(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public NewellFundamentalDiagramComponent(final NewellFundamentalDiagramComponent other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewellFundamentalDiagramComponent clone() {
    return new NewellFundamentalDiagramComponent(this);
  }

}
