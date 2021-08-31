package org.planit.interactor;

/**
 * Link Volume Accessor interface
 * 
 * @author markr
 *
 */
public interface LinkInflowOutflowAccessor extends InteractorAccessor<LinkInflowOutflowAccessee> {

  /**
   * {@inheritDoc}
   */
  @Override
  default Class<LinkInflowOutflowAccessee> getCompatibleAccessee() {
    return LinkInflowOutflowAccessee.class;
  }

}
