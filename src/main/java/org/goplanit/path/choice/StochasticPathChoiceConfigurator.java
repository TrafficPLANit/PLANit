package org.goplanit.path.choice;

import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.ChoiceModelConfigurator;
import org.goplanit.choice.ChoiceModelConfiguratorFactory;
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
   * Constructor
   * 
   */
  protected StochasticPathChoiceConfigurator() {
    super(StochasticPathChoice.class);        
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

}
