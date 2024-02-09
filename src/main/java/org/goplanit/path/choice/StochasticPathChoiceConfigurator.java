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
    
  /**
   * Choice model configurator
   */
  protected ChoiceModelConfigurator<? extends ChoiceModel> choiceModelConfigurator;

  /**
   * PathFilter configurator
   */
  protected PathFilterConfigurator pathFilterConfigurator;
  
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
}
