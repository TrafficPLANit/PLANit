package org.planit.zoning;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroups;

/**
 * Container for transfer zone groups where each transfer zone group logically groups multiple transfer zones together. 
 * Practically this can be used to represent public transport stations for example
 * 
 * @author markr
 *
 */
public class TransferZoneGroupsImpl implements TransferZoneGroups {
  
  /**
   * Map storing all the transfer zone groups
   */
  protected final Map<Long, TransferZoneGroup> transferZoneGroupsMap = new TreeMap<Long, TransferZoneGroup>();

  /** token to use for id generation */
  private IdGroupingToken tokenId;
  
  /** the grouping token id for id generation
   * 
   * @return token
   */
  protected IdGroupingToken getGroupingTokenId() {
    return tokenId;
  }

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  public TransferZoneGroupsImpl(IdGroupingToken tokenId) {
    this.tokenId = tokenId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<TransferZoneGroup> iterator() {
    return transferZoneGroupsMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public TransferZoneGroup register(TransferZoneGroup transferZoneGroup) {
    return transferZoneGroupsMap.put(transferZoneGroup.getId(), transferZoneGroup);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public TransferZoneGroup registerNew() {
    TransferZoneGroup transferZoneGroup = createNew();
    register(transferZoneGroup);
    return transferZoneGroup;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public TransferZoneGroup createNew() {
    return new TransferZoneGroupImpl(getGroupingTokenId());
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public TransferZoneGroup remove(TransferZoneGroup transferZone) {
    return transferZoneGroupsMap.remove(transferZone.getId());
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public boolean hasTransferZoneGroup(TransferZoneGroup transferZoneGroup) {
    return transferZoneGroupsMap.containsKey(transferZoneGroup.getId());
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public TransferZoneGroup get(long transferZoneGroupId) {
    return transferZoneGroupsMap.get(transferZoneGroupId);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public int size() {
    return transferZoneGroupsMap.size();
  }


}
