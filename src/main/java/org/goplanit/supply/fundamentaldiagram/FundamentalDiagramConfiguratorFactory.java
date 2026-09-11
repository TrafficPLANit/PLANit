package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.exceptions.PlanItRunTimeException;

/**
 * Factory for the fundamental diagram types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class FundamentalDiagramConfiguratorFactory {

  /** constructor */
  public FundamentalDiagramConfiguratorFactory(){};

  /**
   * Create a configurator for given fundamental diagram type
   * 
   * @param fundamentalDiagramType type of configurator to be created
   * @return the created configurator
   */
  public static FundamentalDiagramConfigurator<? extends FundamentalDiagramComponent> createConfigurator(
          final String fundamentalDiagramType) {

    if (fundamentalDiagramType.equals(FundamentalDiagram.NEWELL)) {
      return new NewellFundamentalDiagramConfigurator();
    }else if (fundamentalDiagramType.equals(FundamentalDiagram.QUADRATIC_LINEAR)) {
      return new QuadraticLinearFundamentalDiagramConfigurator();
    } else {
      throw new PlanItRunTimeException(String.format(
              "Unable to construct configurator for given fundamentalDiagramType %s", fundamentalDiagramType));
    }
  }
}
