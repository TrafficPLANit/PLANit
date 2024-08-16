package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * Factory for the fundamental diagram types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class FundamentalDiagramConfiguratorFactory {

  /**
   * Create a configurator for given fundamental diagram type
   * 
   * @param fundamentalDiagramType type of configurator to be created
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static FundamentalDiagramConfigurator<? extends FundamentalDiagramComponent> createConfigurator(final String fundamentalDiagramType) throws PlanItException {

    if (fundamentalDiagramType.equals(FundamentalDiagram.NEWELL)) {
      return new NewellFundamentalDiagramConfigurator();
    }else if (fundamentalDiagramType.equals(FundamentalDiagram.QUADRATIC_LINEAR)) {
      return new QuadraticLinearFundamentalDiagramConfigurator();
    } else {
      throw new PlanItException(String.format(
              "Unable to construct configurator for given fundamentalDiagramType %s", fundamentalDiagramType));
    }
  }
}
