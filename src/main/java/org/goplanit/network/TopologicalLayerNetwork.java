package org.goplanit.network;

import java.util.logging.Logger;

import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.TopologicalLayer;
import org.goplanit.utils.network.layers.TopologicalLayers;

/**
 * A network with topological transport layers, meaning that apart from representing a physical reality the result is topologically meaningful, has nodes, links, and some
 * geographic notion via a coordinate reference system
 * 
 * @author markr
 *
 */
public abstract class TopologicalLayerNetwork<T extends TopologicalLayer, U extends TopologicalLayers<T>> extends LayeredNetwork<T, U> {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(TopologicalLayerNetwork.class.getCanonicalName());

  // Protected

  // Public

  /** the coordinate reference system used for all layers in this network */
  private CoordinateReferenceSystem coordinateReferenceSystem;

  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public TopologicalLayerNetwork(IdGroupingToken tokenId) {
    this(tokenId, PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public TopologicalLayerNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId);

    /* default crs */
    this.coordinateReferenceSystem = coordinateReferenceSystem;
  }

  /**
   * Copy constructor. Beware shallow copy only for managed id containers.
   *
   * @param other                   to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param modeMapper to use for tracking mapping between original and copied modes
   * @param layerMapper to use for tracking mapping between original and copied layers
   */
  protected TopologicalLayerNetwork(final TopologicalLayerNetwork<T, U> other, boolean deepCopy, ManagedIdDeepCopyMapper<Mode> modeMapper, ManagedIdDeepCopyMapper<T> layerMapper) {
    super(other, deepCopy, modeMapper, layerMapper);
    this.coordinateReferenceSystem = other.getCoordinateReferenceSystem();
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
   * @param coordinateReferenceSystem to set
   */
  public void setCoordinateReferenceSystem(final CoordinateReferenceSystem coordinateReferenceSystem) {
    if (getTransportLayers().isEachLayerEmpty()) {
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
   * @throws PlanItException thrown if error
   */
  public void transform(final CoordinateReferenceSystem newCoordinateReferenceSystem) throws PlanItException {
    for (TopologicalLayer layer : getTransportLayers()) {
      layer.transform(coordinateReferenceSystem, newCoordinateReferenceSystem);
    }
  }

  /**
   * Based on the underlying layer geographies construct a rectangular bounding box reflecting the extremities of the network.
   * Note this is created from scratch with every call, so for large networks this is a costly operation
   *
   * @return bounding box envelope
   */
  public Envelope createBoundingBox(){
    Envelope envelope = null;
    for(TopologicalLayer layer : getTransportLayers()){
      Envelope layerBoundingBox = layer.createBoundingBox();
      if(envelope==null){
        envelope = layerBoundingBox;
      }else{
        envelope.expandToInclude(layerBoundingBox);
      }
    }
    return envelope;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayerNetwork shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TopologicalLayerNetwork deepClone();
}
