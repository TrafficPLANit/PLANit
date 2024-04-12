package org.goplanit.converter.idmapping;

import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Geometry;

import java.util.function.Function;

public class ZoningIdMapper extends PlanitComponentIdMapper{

  /**
   * Create id mappers per type based on a given id mapping type
   *
   * @param mappingType to apply
   */
  public ZoningIdMapper(IdMapperType mappingType){
    super(mappingType);
    add(Zone.class, IdMapperFunctionFactory.createZoneIdMappingFunction(mappingType));
    add(Connectoid.class,  IdMapperFunctionFactory.createConnectoidIdMappingFunction(mappingType));
    add(TransferZoneGroup.class, IdMapperFunctionFactory.createTransferZoneGroupIdMappingFunction(mappingType));
    add(Zoning.class, IdMapperFunctionFactory.createZoningIdMappingFunction(mappingType));
  }

  /** get id mapper for Zone
   * @return id mapper
   */
  public Function<? super Zone, String> getZoneIdMapper(){
    return get(Zone.class);
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
