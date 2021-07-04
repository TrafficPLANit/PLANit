package org.planit.zoning;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Connectoids;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.DirectedConnectoids;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroups;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoids;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;

/**
 * Class that implemets ZoningBuilder interface
 * 
 * @author markr
 *
 */
public class ZoningBuilderImpl implements ZoningBuilder {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ZoningBuilderImpl.class.getCanonicalName());

  /**
   * recreate ids for the connectoids container provided where we use the id generator based on the theClass token, while we set the generated id using the method provided via the
   * biConsumer
   * 
   * @param <T>         type of connectoids in the container
   * @param connectoids to recreate ids for
   * @param theClass    to base the newly created ids on via the id generator
   * @param biConsumer
   */
  @SuppressWarnings("unchecked")
  private <T extends Connectoid, U extends T> void recreateTypedConnectoidIds(Connectoids<? extends T> connectoids, Class<T> theClass, BiConsumer<U, Long> biConsumer) {
    if (connectoids.size() > 0) {
      for (Connectoid connectoid : connectoids) {
        if (theClass.isInstance(connectoid)) {
          biConsumer.accept(((U) connectoid), IdGenerator.generateId(idToken, theClass));
        } else {
          LOGGER.severe(String.format("attempting to reset id on connectoid (%s) that is not compatible with the connectoid implementation generated by this builder, ignored",
              connectoid.getClass().getCanonicalName()));
        }
      }
    }
  }

  /**
   * recreate ids for the zones container provided where we use the id generator based on the theClass token, while we set the generated id using the method provided via the
   * biConsumer
   * 
   * @param <T>        type of zones in the container
   * @param zones      to recreate ids for
   * @param theClass   to base the newly created ids on via the id generator
   * @param biConsumer
   */
  @SuppressWarnings("unchecked")
  private <T extends Zone, U extends T> void recreateTypedZoneIds(Zones<? extends T> zones, Class<T> theClass, BiConsumer<U, Long> biConsumer) {
    if (zones.size() > 0) {
      for (Zone zone : zones) {
        if (theClass.isInstance(zone)) {
          biConsumer.accept(((U) zone), IdGenerator.generateId(idToken, theClass));
        } else {
          LOGGER.severe(String.format("attempting to reset id on connectoid (%s) that is not compatible with the connectoid implementation generated by this builder, ignored",
              zone.getClass().getCanonicalName()));
        }
      }
    }
  }

  /**
   * recreate the id index mapping on the provided container class given it is of the right type
   * 
   * @param <T>         connectoid type
   * @param connectoids container to recreate index mappings for
   */
  private <T extends Connectoid> void recreateConnectoidIdIndexMapping(Connectoids<T> connectoids) {
    if (connectoids instanceof ConnectoidsImpl<?>) {
      ((ConnectoidsImpl<?>) connectoids).updateIdMapping();
    } else {
      LOGGER
          .severe(String.format("attempting to update id mapping on connectoids (%s) that is not compatible with the connectoids implementation generated by this builder, ignored",
              connectoids.getClass().getCanonicalName()));
    }
  }

  /**
   * recreate the id index mapping on the provided container class given it is of the right type
   * 
   * @param <T>   zone type
   * @param zones container to recreate index mappings for
   */
  private <T extends Zone> void recreateZoneIdIndexMapping(Zones<T> zones) {
    if (zones instanceof ZonesImpl<?>) {
      ((ZonesImpl<?>) zones).updateIdMapping();
    } else {
      LOGGER.severe(String.format("attempting to update id mapping on zones (%s) that is not compatible with the zones implementation generated by this builder, ignored",
          zones.getClass().getCanonicalName()));
    }
  }

  /** the token to use for creating internal ids for the various zoning entities */
  protected final IdGroupingToken idToken;

  /**
   * create a centroid implementation
   * 
   * @param parentZone of the centroid
   * @return centroid created
   */
  protected Centroid createCentroid(Zone parentZone) {
    return new CentroidImpl(idToken, parentZone);
  }
  
  /** Recreate the zone id based index on connectoids provided whenever zone ids are recreated
   * @param connectoids to update
   */
  protected void recreateZoneIdReferencesInConnectoids(Collection<Connectoids<?>> connectoids) {
    /* update all connectoids access zones id mappings since the transfer zone index is used for this */
    for (Connectoids<?> connectoidsEntry : connectoids) {        
      for(Connectoid connectoid : connectoidsEntry) {
        if(!(connectoid instanceof ConnectoidImpl)) {
          LOGGER.severe("recreation of transfer zone ids utilises unsupported implementation of connectoids interface when attempting to update access zone references");  
        }
        ((ConnectoidImpl)connectoid).recreateAccessZoneIdMapping();
      }
    } 
  }  

  /**
   * recreate the ids on the provided connectoids regarding their internal ids across all connectoids
   * 
   * @param <T>         type of connectoid
   * @param connectoids to recreate ids for
   */
  protected <T extends Connectoid> void recreateConnectoidIds(Connectoids<T> connectoids) {
    BiConsumer<ConnectoidImpl, Long> biConsumer = (connectoid, newId) -> {
      connectoid.setId(newId);
    };
    recreateTypedConnectoidIds(connectoids, Connectoid.class, biConsumer);
    /* id used as index, so recreate indices as well */
    recreateConnectoidIdIndexMapping(connectoids);
  }

  /**
   * recreate the ids on the provided zones regarding their internal ids across all zones
   * 
   * @param <T>   type of zones
   * @param zones to recreate ids for
   */
  protected <T extends Zone> void recreateZoneIds(Zones<T> zones) {
    BiConsumer<ZoneImpl, Long> biConsumer = (zone, newId) -> {
      zone.setId(newId);
    };
    recreateTypedZoneIds(zones, Zone.class, biConsumer);
    /* id used as index, so recreate indices as well */
    recreateZoneIdIndexMapping(zones);
  }

  /**
   * recreate the undirected connectoid ids on the provided undirected connectoids
   *
   * @param undirectedConnectoids to recreate ids for
   */
  protected void recreateUndirectedConnectoidIds(UndirectedConnectoids undirectedConnectoids) {
    BiConsumer<UndirectedConnectoidImpl, Long> biComsumer = (connectoid, newId) -> {
      connectoid.setUndirectedConnectoidId(newId);
    };
    recreateTypedConnectoidIds(undirectedConnectoids, UndirectedConnectoid.class, biComsumer);
  }

  /**
   * recreate the directed connectoid ids on the provided undirected connectoids
   *
   * @param directedConnectoids to recreate ids for
   */
  protected void recreateDirectedConnectoidIds(DirectedConnectoids directedConnectoids) {
    BiConsumer<DirectedConnectoidImpl, Long> biComsumer = (connectoid, newId) -> {
      connectoid.setDirectedConnectoidId(newId);
    };
    recreateTypedConnectoidIds(directedConnectoids, DirectedConnectoid.class, biComsumer);
  }

  /**
   * Recreate the od zones ids on the provided od zones
   *
   * @param odZones to recreate ids for
   */
  protected void recreateOdZoneIds(Zones<OdZone> odZones) {
    BiConsumer<OdZoneImpl, Long> biComsumer = (odZone, newId) -> {
      odZone.setOdZoneId(newId);
    };
    recreateTypedZoneIds(odZones, OdZone.class, biComsumer);
  }

  /**
   * Recreate the transfer zones ids on the provided transfer zones
   *
   * @param transferZones zones to recreate ids for
   */
  protected void recreateTransferZoneIds(Zones<TransferZone> transferZones) {
    BiConsumer<TransferZoneImpl, Long> biComsumer = (odZone, newId) -> {
      odZone.setTransferZoneId(newId);
    };
    recreateTypedZoneIds(transferZones, TransferZone.class, biComsumer);
  }

  /**
   * The id token to use
   * 
   * @param idToken to use
   */
  public ZoningBuilderImpl(IdGroupingToken idToken) {
    this.idToken = idToken;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UndirectedConnectoid createUndirectedConnectoid(final DirectedVertex accessVertex) {
    return new UndirectedConnectoidImpl(idToken, accessVertex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DirectedConnectoid createDirectedConnectoid(final LinkSegment accessLinkSegment) {
    return new DirectedConnectoidImpl(idToken, accessLinkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone createOdZone() {
    OdZoneImpl OdZone = new OdZoneImpl(idToken);
    OdZone.setCentroid(createCentroid(OdZone));
    return OdZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone createTransferZone() {
    TransferZoneImpl transferZone = new TransferZoneImpl(idToken);
    transferZone.setCentroid(createCentroid(transferZone));
    return transferZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup createTransferZoneGroup() {
    return new TransferZoneGroupImpl(idToken);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateConnectoidIds(UndirectedConnectoids undirectedConnectoids, DirectedConnectoids directedConnectoids) {

    if (!undirectedConnectoids.isEmpty() || !directedConnectoids.isEmpty()) {
      /* first reset the connectoid class ids using the base class signature and token */
      IdGenerator.reset(idToken, Connectoid.class);
      recreateConnectoidIds(undirectedConnectoids);
      recreateConnectoidIds(directedConnectoids);

      /* now do the same for the unique ids within the type */
      if (!undirectedConnectoids.isEmpty()) {
        IdGenerator.reset(idToken, UndirectedConnectoid.class);
        recreateUndirectedConnectoidIds(undirectedConnectoids);
      }
      if (!directedConnectoids.isEmpty()) {
        IdGenerator.reset(idToken, DirectedConnectoid.class);
        recreateDirectedConnectoidIds(directedConnectoids);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateOdZoneIds(Zones<OdZone> odZones, Collection<Connectoids<?>> connectoids, boolean resetZoneIds) {
    if (resetZoneIds) {
      IdGenerator.reset(idToken, Zone.class);
    }

    if (!odZones.isEmpty()) {
      recreateZoneIds(odZones);

      /* now do the same for the unique ids within the type */
      IdGenerator.reset(idToken, OdZone.class);
      recreateOdZoneIds(odZones);
    }
    
    /* recreate id based mapping for access zones of connecoids */
    recreateZoneIdReferencesInConnectoids(connectoids);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateTransferZoneIds(Zones<TransferZone> transferZones, TransferZoneGroups transferZoneGroups, Collection<Connectoids<?>> connectoids, boolean resetZoneIds) {
    if (resetZoneIds) {
      IdGenerator.reset(idToken, Zone.class);
    }

    if (!transferZones.isEmpty()) {
      recreateZoneIds(transferZones);

      /* now do the same for the unique ids within the type */
      IdGenerator.reset(idToken, TransferZone.class);
      recreateTransferZoneIds(transferZones);

      /* update all transfer zone groups id mappings for transfer zones since the transfer zone index is used for this */
      for (TransferZoneGroup group : transferZoneGroups) {
        if (!(group instanceof TransferZoneGroupImpl)) {
          LOGGER.severe("recreation of transfer zone ids utilises unsupported implementation of TransferZoneGroup interface when attempting to update references");
        }
        ((TransferZoneGroupImpl) group).recreateTransferZoneIdMapping();
      }
      
      /* recreate id based mapping for access zones of connecoids */
      recreateZoneIdReferencesInConnectoids(connectoids);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateTransferZoneGroupIds(TransferZoneGroups transferZoneGroups) {
    if (!transferZoneGroups.isEmpty()) {
      IdGenerator.reset(idToken, TransferZoneGroup.class);
      for (TransferZoneGroup group : transferZoneGroups) {
        if (TransferZoneGroupImpl.class.isInstance(group)) {
          ((TransferZoneGroupImpl) group).setId(TransferZoneGroupImpl.generateTransferZoneGroupId(idToken));
        } else {
          LOGGER.severe(String.format(
              "attempting to reset id on transfer zone group (%s) that is not compatible with the transfer zone group implementation generated by this builder, ignored",
              group.getClass().getCanonicalName()));
        }
      }
    }

    /* id used as index, so recreate indices as well */
    if (transferZoneGroups instanceof TransferZoneGroupsImpl) {
      ((TransferZoneGroupsImpl) transferZoneGroups).updateIdMapping();
    } else {
      LOGGER.severe(String.format(
          "attempting to update id mapping on transfer zone groups (%s) that is not compatible with the transfer zone groups implementation generated by this builder, ignored",
          transferZoneGroups.getClass().getCanonicalName()));
    }
  }

}
