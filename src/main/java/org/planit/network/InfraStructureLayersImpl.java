package org.planit.network;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;

/**
 * Base implementation of the InfrastructureLayers interface
 * 
 * @author markr
 *
 */
public class InfraStructureLayersImpl implements InfrastructureLayers {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(InfraStructureLayersImpl.class.getCanonicalName());

  /** track the registered infrastructure layers */
  protected final Map<Long, InfrastructureLayer> infrastructureLayers = new TreeMap<Long, InfrastructureLayer>();

  /**
   * create id's for infrastructure layers based on this token
   */
  private final IdGroupingToken groupingId;

  /**
   * Constructor
   * 
   * @param groupingId to generated id's for infrastructure layers
   */
  public InfraStructureLayersImpl(IdGroupingToken groupingId) {
    this.groupingId = groupingId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureLayer remove(InfrastructureLayer entity) {

    if (entity == null) {
      LOGGER.warning("cannot remove infrastructure layer, null provided");
      return null;
    }

    return remove(entity.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureLayer remove(long id) {
    return infrastructureLayers.remove(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicPhysicalNetwork createNew() {
    final MacroscopicPhysicalNetwork newInfrastructureLayer = new MacroscopicPhysicalNetwork(groupingId);
    register(newInfrastructureLayer);
    return newInfrastructureLayer;
  }

  /**
   * return iterator over the available infrastructure layers
   */
  @Override
  public Iterator<InfrastructureLayer> iterator() {
    return infrastructureLayers.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureLayer register(InfrastructureLayer entity) {

    if (entity == null) {
      LOGGER.warning("cannot register infrastructure layer, null provided");
      return null;
    }

    return infrastructureLayers.put(entity.getId(), entity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicPhysicalNetwork registerNew() {
    final MacroscopicPhysicalNetwork newInfrastructureLayer = createNew();
    register(newInfrastructureLayer);
    return newInfrastructureLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return infrastructureLayers.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureLayer get(long id) {
    return infrastructureLayers.get(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureLayer get(final Mode mode) {
    for (InfrastructureLayer layer : this) {
      if (layer.supports(mode)) {
        return layer;
      }
    }

    return null;
  }

}
