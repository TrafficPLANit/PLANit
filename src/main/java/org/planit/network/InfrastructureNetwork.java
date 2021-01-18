package org.planit.network;

import java.util.logging.Logger;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitJtsUtils;
import org.planit.mode.ModesImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Modes;

/**
 * A network with physical infrastructure layers
 * 
 * @author markr
 *
 */
public class InfrastructureNetwork extends Network {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(InfrastructureNetwork.class.getCanonicalName());

  // Protected

  // Public

  /**
   * class instance containing all modes specific functionality across the layers
   */
  public final Modes modes;

  /** stores the various layers grouped by their supported modes of transport */
  public final InfrastructureLayers infrastructureLayers;

  /** the coordinate reference system used for all layers in this network */
  private CoordinateReferenceSystem coordinateReferenceSystem;

  /**
   * Default constructor
   * 
   * @param tokenId
   */
  public InfrastructureNetwork(IdGroupingToken tokenId) {
    this(tokenId, PlanitJtsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public InfrastructureNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId);
    /* for mode management */
    this.modes = new ModesImpl(tokenId);
    /* for layer management */
    this.infrastructureLayers = new InfraSructureLayersImpl(tokenId);
    /* default crs */
    this.coordinateReferenceSystem = coordinateReferenceSystem;
  }

  /**
   * collect the used crs
   * 
   * @return coordinateReferencesystem used by this infrastructure network
   */
  public CoordinateReferenceSystem getCoordinateReferenceSystem() {
    return this.coordinateReferenceSystem;
  }

  /**
   * set the coordinate reference system used for all layers
   * 
   * @return
   */
  public void setCoordinateReferenceSystem(final CoordinateReferenceSystem coordinateReferenceSystem) {
    if (infrastructureLayers.isEachLayerEmpty()) {
      this.coordinateReferenceSystem = coordinateReferenceSystem;
    } else {
      LOGGER.warning("Coordinate Reference System is already set. To change the CRS after instantiation, use transform() method");
    }
  }

  /**
   * change the coordinate system, which will result in an update of all geometries in the network layers from the original CRS to the new CRS. If the network is empty and no CRS
   * is set then this is identical to calling setCoordinateReferenceSystem, otherwise it will change the CRS while the set method will throw an exception
   * 
   * @param newCoordinateReferenceSystem to transform the network to
   * @throws PlanItException
   * @throws FactoryException
   */
  public void transform(final CoordinateReferenceSystem newCoordinateReferenceSystem) throws PlanItException {
    for (InfrastructureLayer layer : infrastructureLayers) {
      layer.transform(coordinateReferenceSystem, newCoordinateReferenceSystem);
    }
  }

}
