package org.goplanit.path.choice;

import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Stochastic path choice component. Stochasticity is reflected by the fact that the path choice is applied by means of
 * a logit model, to be configured here. Also, due to being  stochastic the paths mey be provided beforehand.
 * The latter is also configured via this class
 *
 * @author markr
 *
 */
public class StochasticPathChoice extends PathChoice {

  /**
   * generated UID
   */
  private static final long serialVersionUID = 6617920674217225019L;

  /**
   * the logger
   */
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
  public StochasticPathChoice(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * indicate whether paths are to be created on the fly for each iteration or not. This implementation eventually
   * should support both depending on how it is configured
   *
   * @return todo
   */
  @Override
  public boolean isPathsFixed() {
    return odPathSet == null; //todo change this when we are addressing this approach
  }

  /**
   * Copy constructor
   *
   * @param other    to copy
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
  public StochasticPathChoice shallowClone() {
    return new StochasticPathChoice(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StochasticPathChoice deepClone() {
    return new StochasticPathChoice(this,true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    logitChoiceModel.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return logitChoiceModel.collectSettingsAsKeyValueMap();
  }
}
