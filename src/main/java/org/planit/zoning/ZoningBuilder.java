package org.planit.zoning;

import java.util.Collection;

import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.zoning.Connectoids;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.DirectedConnectoids;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroups;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoids;
import org.planit.utils.zoning.Zones;

/**
 * Class that takes care of the creation of all zoning components as well as the generation of their internal ids
 * 
 * @author markr
 *
 */
public interface ZoningBuilder {

  /**
   * Create a new undirected connectoid instance
   * 
   * @param accessVertex for connectoid zone combination
   * @return created undirected connectoid
   */
  public abstract UndirectedConnectoid createUndirectedConnectoid(final DirectedVertex accessVertex);

  /**
   * Create a new directed connectoid instance
   * 
   * @param accessLinkSegment to use
   * @return created directed connectoid
   */
  public abstract DirectedConnectoid createDirectedConnectoid(final LinkSegment accessLinkSegment);

  /**
   * Create a new OdZone instance
   * 
   * @return created odZone
   */
  public abstract OdZone createOdZone();

  /**
   * Create a new OdZone instance
   * 
   * @return created odZone
   */
  public abstract TransferZone createTransferZone();

  /**
   * Create a new transfer zone group
   * 
   * @return created transfer zone group
   */
  public abstract TransferZoneGroup createTransferZoneGroup();

  /**
   * Recreate the ids for all passed in connectoids
   * 
   * @param undirectedConnectoids to recreate ids for
   * @param directedConnectoids   to recreate ids for
   */
  public abstract void recreateConnectoidIds(UndirectedConnectoids undirectedConnectoids, DirectedConnectoids directedConnectoids);

  /**
   * Recreate the ids for all passed in od zones
   * 
   * @param odZones      to recreate ids for
   * @param connectoids  to consider
   * @param resetZoneIds when true the zone ids of all zone implementations (OdZone, TransferZone) are rest upon commencing), false then not
   */
  public abstract void recreateOdZoneIds(Zones<OdZone> odZones, Collection<Connectoids<?>> connectoids, boolean resetZoneIds);

  /**
   * Recreate the ids for all passed in transferZones and the references by id for transfer zone groups, and connectoids (that are invalidated by changing the ids).
   * 
   * @param transferZones      to recreate ids for
   * @param transferZoneGroups to update id indexed transfer zones for within the groups
   * @param connectoids        to consider
   * @param resetZoneIds       when true the zone ids of all zone implementations (OdZone, TransferZone) are reset upon commencing), false then not
   */
  public abstract void recreateTransferZoneIds(Zones<TransferZone> transferZones, TransferZoneGroups transferZoneGroups, Collection<Connectoids<?>> connectoids, boolean resetZoneIds);

  /**
   * Recreate the ids for all passed in transfer zone groups
   * 
   * @param transferZoneGroups to recreate ids for
   */
  public abstract void recreateTransferZoneGroupIds(TransferZoneGroups transferZoneGroups);

}
