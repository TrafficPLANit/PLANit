package org.goplanit.converter.idmapping;

import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.connectoid.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;

import java.util.function.Function;

public class ZoningIdMapper extends PlanitComponentIdMapper{

  /**
   * Create id mappers per type based on a given id mapping type
   *
   * @param mappingType to apply
   */
  public ZoningIdMapper(IdMapperType mappingType){
    super(mappingType);
    add(OdZone.getOdZoneIdClass(), IdMapperFunctionFactory.createOdZoneIdMappingFunction(mappingType));
    add(TransferZone.getTransferZoneIdClass(), IdMapperFunctionFactory.createTransferZoneIdMappingFunction(mappingType));
    add(Connectoid.class,  IdMapperFunctionFactory.createConnectoidIdMappingFunction(mappingType));
    add(TransferZoneGroup.class, IdMapperFunctionFactory.createTransferZoneGroupIdMappingFunction(mappingType));
    add(Zoning.class, IdMapperFunctionFactory.createZoningIdMappingFunction(mappingType));
  }

  /** get id mapper for OD Zone
   * @return id mapper
   */
  public Function<OdZone, String> getOdZoneIdMapper(){
    return get(OdZone.getOdZoneIdClass());
  }

  /** get id mapper for Transfer Zone
   * @return id mapper
   */
  public Function<TransferZone, String> getTransferZoneIdMapper(){
    return get(TransferZone.getTransferZoneIdClass());
  }

  /** get id mapper for Connectoid
   * @return id mapper
   */
  public Function<Connectoid, String> getConnectoidIdMapper(){
    return get(Connectoid.class);
  }

  /** get id mapper for TransferZoneGroup
   * @return id mapper
   */
  public Function<TransferZoneGroup, String> getTransferZoneGroupIdMapper(){
    return get(TransferZoneGroup.class);
  }

  /** get id mapper for Zoning
   * @return id mapper
   */
  public Function<Zoning, String> getZoningIdMapper() {
    return get(Zoning.class);
  }
}
