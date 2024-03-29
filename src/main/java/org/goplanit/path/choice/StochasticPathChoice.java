package org.goplanit.path.choice;

import java.util.logging.Logger;

import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Stochastic path choice component. Stochasticity is reflected by the fact that the path choice is applied by means of a logit model, to be configured here. Also, due to being
 * stochastic the path can/must be provided beforehand. This is also configured via this class
 *
 * @author markr
 *
 */
public abstract class StochasticPathChoice extends PathChoice {

  /** generated UID */
  private static final long serialVersionUID = 6617920674217225019L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StochasticPathChoice.class.getCanonicalName());

  /**
   * The registered logit choice model
   */
  protected LogitChoiceModel logitChoiceModel = null;

  /**
   * The registered od path set instance
   */
  protected OdPathMatrix odPathSet = null;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public StochasticPathChoice(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected StochasticPathChoice(final StochasticPathChoice other, boolean deepCopy) {
    super(other, deepCopy);
    this.logitChoiceModel = other.logitChoiceModel; // not owned
    this.odPathSet = other.odPathSet;               // not owned
  }

  /**
   * set the chosen logit model
   * 
   * @param logitChoiceModel chosen model
   */
  public void setLogitModel(LogitChoiceModel logitChoiceModel) {
    this.logitChoiceModel = logitChoiceModel;
  }

  /**
   * Register a fixed od path set to use in the form of an ODPathMatrix
   *
   * @param odPathSet the fixed od path set in the shape of an od path matrix
   */
  public void setOdPathMatrix(final OdPathMatrix odPathSet) {
    this.odPathSet = odPathSet;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract StochasticPathChoice shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract StochasticPathChoice deepClone();

}
