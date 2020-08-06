package org.planit.path.choice.logit;

import org.planit.utils.id.IdGroupingToken;

/**
 * MNL choice model implementation
 *
 * @author markr
 *
 */
public class MultinomialLogit extends LogitChoiceModel {

  /** generated UID */
  private static final long serialVersionUID = -7602543264466240409L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected MultinomialLogit(final IdGroupingToken groupId) {
    super(groupId);
  }

}
