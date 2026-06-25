package org.goplanit.demands.discrete.tour;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactory;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;

/**
 * Factory class for tour instances to be registered on its parent container passed in to constructor
 */
public class ToursFactory extends ManagedIdEntityFactoryImpl<Tour>
    implements ManagedIdEntityFactory<Tour> {

  /** container to use */
  protected final Tours tours;

  /**
   * Create a newly created instance without registering on the container
   *
   * @return created time period
   */
  protected Tour createNew() {
    return new Tour(getIdGroupingToken());
  }

  /**
   * Constructor
   *
   * @param tokenId    to use
   * @param tours to use
   */
  protected ToursFactory(final IdGroupingToken tokenId, final Tours tours) {
    super(tokenId);
    this.tours = tours;
  }

  /**
   * register a new entry on the container and return it
   *
   * @return created instance
   */
  public Tour registerNew() {
    var newInstance = new Tour(getIdGroupingToken());
    tours.register(newInstance);
    return newInstance;
  }

}
