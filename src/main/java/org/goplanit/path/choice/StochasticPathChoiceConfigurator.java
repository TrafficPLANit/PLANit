package org.goplanit.path.choice;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.ChoiceModelConfigurator;
import org.goplanit.choice.ChoiceModelConfiguratorFactory;
import org.goplanit.path.filter.PathFilter;
import org.goplanit.path.filter.PathFilterConfigurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Configurator for StochasticPathChoice implementation
 * 
 * @author markr
 */
public class StochasticPathChoiceConfigurator extends PathChoiceConfigurator<StochasticPathChoice> {
  
  private static final String SET_OD_PATH_MATRIX = "setOdPathMatrix";

  private static final String SET_REMOVE_PATH_PROBABILITY_THRESHOLD = "setRemovePathPobabilityThreshold";
    
  /**
   * Choice model configurator
   */
  protected ChoiceModelConfigurator<? extends ChoiceModel> choiceModelConfigurator;

  /**
   * PathFilter configurator
   */
  protected PathFilterConfigurator pathFilterConfigurator;

  /**
   * Threshold to apply when deciding whether to keep or remove paths considered in path choice, value
   * between 0 and 1 expected
   */
  protected double removePathPobabilityThreshold;
  
  /**
   * Constructor
   * 
   */
  protected StochasticPathChoiceConfigurator() {
    super(StochasticPathChoice.class);
    pathFilterConfigurator = new PathFilterConfigurator();
  }
  
  /**
   * create and register the choice model to apply
   * 
   * @param choiceModelType name of the class to be instantiated
   * @return the logit choice model that is registered
   * @throws PlanItException thrown if error
   */
  public ChoiceModelConfigurator<? extends ChoiceModel> createAndRegisterChoiceModel(final String choiceModelType) throws PlanItException {
    this.choiceModelConfigurator = ChoiceModelConfiguratorFactory.createConfigurator(choiceModelType);
    return choiceModelConfigurator;
  }
  
  /**
   * Collect the choice model configurator
   * 
   * @return choice model configurator
   */
  public ChoiceModelConfigurator<? extends ChoiceModel> getChoiceModel() {
    return choiceModelConfigurator;
  }

  /**
   * Collect the path filter configurator (only relevant when on-the-fly paths are being generated, otherwise ignored)
   *
   * @return path filter configurator
   */
  public PathFilterConfigurator getPathFilter() {
    return pathFilterConfigurator;
  }

  /**
   * Get the threshold below which paths are expected to be removed (and therefore no longer considered in path choice,
   * where 0 means only unused paths will be removed whereas 1 means all paths will be removed
   *
   * @return removePathPobabilityThreshold set
   */
  public double getRemovePathPobabilityThreshold() {
    return getTypedFirstParameterOfDelayedMethodCall(SET_REMOVE_PATH_PROBABILITY_THRESHOLD);
  }

  /**
   * Set the threshold below which paths are expected to be removed (and therefore no longer considered in path choice,
   * where 0 means only unused paths will be removed whereas 1 means all paths will be removed
   *
   * @param removePathPobabilityThreshold the threshold to apply (between 0 and 1)
   */
  public void setRemovePathPobabilityThreshold(double removePathPobabilityThreshold) {
    registerDelayedMethodCall(SET_REMOVE_PATH_PROBABILITY_THRESHOLD,removePathPobabilityThreshold);
  }
}
