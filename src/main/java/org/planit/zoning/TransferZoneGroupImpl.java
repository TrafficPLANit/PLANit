package org.planit.zoning;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;

/**
 * A transfer zone group implementation. Practically this can be used to represent public transport stations for example
 * 
 * @author markr
 *
 */
public class TransferZoneGroupImpl extends ExternalIdAbleImpl implements TransferZoneGroup {

  /**
   * Map storing all the transfer zones in the group
   */
  protected Map<Long, TransferZone> transferZoneMap = new TreeMap<Long, TransferZone>();

  /**
   * name of the transfer zone group
   */
  protected String name = null;

  /**
   * recreate the mapping such that all the keys used for each transfer zone reflect their internal id. To be called whenever the ids of transfer zones are changed
   */
  protected void recreateTransferZoneIdMapping() {
    if (!transferZoneMap.isEmpty()) {
      Map<Long, TransferZone> updatedMap = new TreeMap<Long, TransferZone>();
      transferZoneMap.forEach((oldId, transferzone) -> updatedMap.put(transferzone.getId(), transferzone));
      transferZoneMap.clear();
      transferZoneMap = updatedMap;
    }
  }

  /**
   * generate unique transfer zone group id
   *
   * @param tokenId contiguous id generation within this group for instances of this class
   * @return odZoneId
   */
  protected static long generateTransferZoneGroupId(final IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, TransferZoneGroup.class);
  }

  /**
   * Set the transfer zone group id
   * 
   * @param id to set
   */
  protected void setId(long id) {
    super.setId(id);
  }

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected TransferZoneGroupImpl(IdGroupingToken tokenId) {
    super(generateTransferZoneGroupId(tokenId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<TransferZone> iterator() {
    return transferZoneMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone addTransferZone(TransferZone transferZone) {
    TransferZone prevTransferZone = transferZoneMap.put(transferZone.getId(), transferZone);
    transferZone.addToTransferZoneGroup(this);
    return prevTransferZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZone removeTransferZone(TransferZone transferZone) {
    TransferZone removedZone = transferZoneMap.remove(transferZone.getId());
    if (transferZone.isInTransferZoneGroup(this)) {
      transferZone.removeFromTransferZoneGroup(this);
    }
    return removedZone;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasTransferZone(TransferZone transferZone) {
    return transferZoneMap.containsKey(transferZone.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return transferZoneMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<TransferZone> getTransferZones() {
    return transferZoneMap.values();
  }

}
