package org.goplanit.converter;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;

import java.util.HashMap;
import java.util.Map;
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
  }

  /** get id mapper for zones
   * @return id mapper
   */
  public Function<Zone, String> getZoneIdMapper(){
    return get(Zone.class);
  }

  /** get id mapper for connectoids
   * @return id mapper
   */
  public Function<Connectoid, String> getConnectoidIdMapper(){
    return get(Connectoid.class);
  }

  /** get id mapper for transfer zone groups
   * @return id mapper
   */
  public Function<TransferZoneGroup, String> getTransferZoneGroupIdMapper(){
    return get(TransferZoneGroup.class);
  }
}
