package org.goplanit.converter.idmapping;

import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.utils.id.IdMapperType;

import java.util.function.Function;

/**
 * All discrete demand id mappers in a convenience class
 */
public class DiscreteDemandsIdMapper extends PlanitComponentIdMapper{

  /**
   * Constructor
   * @param type to use
   */
  public DiscreteDemandsIdMapper(IdMapperType type){
    super(type);
    add(Person.class, IdMapperFunctionFactory.createPersonClassIdMappingFunction(type));
    add(Household.class, IdMapperFunctionFactory.createHouseholdClassIdMappingFunction(type));
    add(Tour.class, IdMapperFunctionFactory.createTourClassIdMappingFunction(type));
    add(Trip.class, IdMapperFunctionFactory.createTripClassIdMappingFunction(type));
  }

  /** get id mapper for persons
   * @return id mapper
   */
  public Function<Person, String> getPersonClassIdMapper(){
    return get(Person.PERSON_ID_CLASS);
  }

  /** get id mapper for Household
   * @return id mapper
   */
  public Function<Household, String> getHouseholdClassIdMapper(){
    return get(Household.HOUSEHOLD_ID_CLASS);
  }

  /** get id mapper for Tour
   * @return id mapper
   */
  public Function<Tour, String> getTourClassIdMapper(){
    return get(Tour.TOUR_ID_CLASS);
  }

  /** get id mapper for Trip
   * @return id mapper
   */
  public Function<Trip, String> getTripClassIdMapper(){
    return get(Trip.TRIP_ID_CLASS);
  }

}
