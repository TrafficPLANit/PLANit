package org.goplanit.path.choice.logit;

import java.util.Map;

import org.goplanit.utils.id.IdGroupingToken;

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

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected MultinomialLogit(MultinomialLogit other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultinomialLogit clone() {
    return new MultinomialLogit(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // No internal state (yet), do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

}
