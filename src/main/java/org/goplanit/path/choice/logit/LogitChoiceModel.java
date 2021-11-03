package org.goplanit.path.choice.logit;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * The logit choice model base class. Different logit choice models lead to different behaviour regarding choices.
 *
 * @author markr
 *
 */
public abstract class LogitChoiceModel extends PlanitComponent<LogitChoiceModel> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -4578323513280128464L;

  /**
   * short hand for MNL class type
   */
  public static final String MNL = MultinomialLogit.class.getCanonicalName();

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected LogitChoiceModel(IdGroupingToken groupId) {
    super(groupId, LogitChoiceModel.class);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected LogitChoiceModel(LogitChoiceModel other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract LogitChoiceModel clone();

}
