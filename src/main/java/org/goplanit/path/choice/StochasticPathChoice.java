package org.goplanit.path.choice;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.path.SimpleDirectedPath;

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
  protected ChoiceModel choiceModel = null;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public StochasticPathChoice(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   *
   * @param other    to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected StochasticPathChoice(final StochasticPathChoice other, boolean deepCopy) {
    super(other, deepCopy);
    this.choiceModel = other.choiceModel; // not owned
  }

  /**
   * Perform the path choice by determining the path probabilities based on path cost and scaling factor
   *
   * @param paths path alternatives to consider
   * @param pathCosts costs of each path in order of collection
   * @return computed probabilities
   */
  public double[] computePathProbabilities(Collection<? extends SimpleDirectedPath> paths, double[] pathCosts) {
    var numPaths = pathCosts.length;
    if(numPaths==1){
      return new double[]{1.0};
    }

    // delegate to choice model
    return choiceModel.computeProbabilities(pathCosts);
  }

  /**
   * set the chosen choice model
   *
   * @param choiceModel chosen model
   */
  public void setChoiceModel(ChoiceModel choiceModel) {
    this.choiceModel = choiceModel;
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
    choiceModel.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return choiceModel.collectSettingsAsKeyValueMap();
  }

}
