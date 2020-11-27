package org.planit.network.converter;

/**
 * abstract class implementation to write a PLANit network to disk
 * 
 * @author markr
 *
 */
public abstract class NetworkWriterImpl implements NetworkWriter {

  /**
   * the id mapper to use
   */
  protected IdMapperType idMapper;

  /**
   * constructor
   * 
   * @param idMapper to use as default
   */
  protected NetworkWriterImpl(IdMapperType idMapper) {
    setIdMapper(idMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdMapperType getIdMapper() {
    return idMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdMapper(IdMapperType idMapper) {
    this.idMapper = idMapper;
  }

}
