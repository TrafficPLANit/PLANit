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
  protected IdMapper idMapper;

  /**
   * constructor
   * 
   * @param idMapper to use as default
   */
  protected NetworkWriterImpl(IdMapper idMapper) {
    setIdMapper(idMapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdMapper getIdMapper() {
    return idMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdMapper(IdMapper idMapper) {
    this.idMapper = idMapper;
  }

}
