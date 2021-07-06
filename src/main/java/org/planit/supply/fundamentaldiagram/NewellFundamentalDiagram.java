package org.planit.supply.fundamentaldiagram;

import org.planit.utils.id.IdGroupingToken;

/**
 * Newell fundamental diagram traffic component
 *
 * @author markr
 *
 */
public class NewellFundamentalDiagram extends FundamentalDiagram {

  /** generated UID */
  private static final long serialVersionUID = -3166623064510413929L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NewellFundamentalDiagram(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public NewellFundamentalDiagram(final NewellFundamentalDiagram other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewellFundamentalDiagram clone() {
    return new NewellFundamentalDiagram(this);
  }

}
