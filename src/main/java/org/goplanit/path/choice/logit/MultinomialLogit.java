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
   * Constructor (public access required for reflection purposes)
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public MultinomialLogit(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MultinomialLogit(MultinomialLogit other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultinomialLogit shallowClone() {
    return new MultinomialLogit(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultinomialLogit deepClone() {
    return new MultinomialLogit(this, true);
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
