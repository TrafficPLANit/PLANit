package org.goplanit.algorithms.sampling.demands.table;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.misc.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The sampling units of a discrete demand, each a {@link DownSamplingUnit}, while the demand is counted into a
 * {@link DiscreteDemandsDownSamplingTable}: its households and its persons without a household, joined into one
 * sampling unit where their persons share a tour, each sampling unit with its tours and trips
 * <p>
 * The down-sampling table counts each sampling unit from its households, persons, tours and trips together, while a
 * demand holds each in a container of its own and a household does not know its persons. So the demand is gathered
 * into its sampling units once, here, and they are dropped once the demand is counted. The sampling units come in the
 * order of their first household, or person without a household, in the demand's order, the households first.
 * </p>
 * <p>
 * Terms used here, all listed in {@link org.goplanit.algorithms.sampling.demands}: a <b>sampling unit</b> is what the
 * down-sampling keeps or drops whole; a <b>member</b> is one of the households or persons without a household it is
 * made of, numbered here only to join those sharing a tour.
 * </p>
 *
 * @author markr
 */
final class DownSamplingDemandUnits {

  /**
   * The member a person belongs to, their household or themselves
   *
   * @param person to look up
   * @param memberOfHousehold member of each household
   * @param memberOfPersonWithoutHousehold member of each person without a household
   * @return the member
   * @throws IllegalArgumentException when the person or their household is not in the demand
   */
  private static int findMember(Person person, Map<Household, Integer> memberOfHousehold,
      Map<Person, Integer> memberOfPersonWithoutHousehold) {
    var member = person.getHousehold() != null ?
        memberOfHousehold.get(person.getHousehold()) : memberOfPersonWithoutHousehold.get(person);
    if (member == null) {
      throw new IllegalArgumentException(String.format(
          "Person %s takes part in a tour of the demand but is not in it", person.getIdsAsString()));
    }
    return member;
  }

  /**
   * The sampling unit of each member: members sharing a tour in one sampling unit, every other member in one of its
   * own
   * <p>
   * Two households are one sampling unit when a person of each takes part in the same tour, directly or through other
   * households: h1 and h2 sharing a tour, and h2 and h3 another one, make one sampling unit of the three. Only the
   * members sharing a tour with another member are joined, through {@link ListUtils#extractDisjointSets(List,
   * java.util.Collection)}, so the many households whose tours are their own cost nothing. Sampling units are numbered
   * in the order of their first member, so the same demand always gives the same numbers.
   * </p>
   *
   * @param numberOfMembers of the demand
   * @param joinedPairs pairs of members sharing a tour
   * @return the sampling unit of each member
   */
  private static int[] collectUnitOfMember(int numberOfMembers, List<Pair<Integer, Integer>> joinedPairs) {
    var joinedMembers = joinedPairs.stream().flatMap(pair -> Stream.of(pair.first(), pair.second()))
        .distinct().sorted().collect(Collectors.toList());
    var joinedMembersOf = new HashMap<Integer, int[]>();
    for (var joinedSet : ListUtils.extractDisjointSets(joinedMembers, joinedPairs)) {
      int[] inOrder = joinedSet.stream().sorted().mapToInt(Integer::intValue).toArray();
      joinedSet.forEach(member -> joinedMembersOf.put(member, inOrder));
    }

    var unitOfMember = new int[numberOfMembers];
    Arrays.fill(unitOfMember, -1);
    int unit = 0;
    for (int member = 0; member < unitOfMember.length; ++member) {
      if (unitOfMember[member] >= 0) {
        continue;
      }
      for (int joined : joinedMembersOf.getOrDefault(member, new int[]{member})) {
        unitOfMember[joined] = unit;
      }
      ++unit;
    }
    return unitOfMember;
  }

  /** the sampling units, in the order of their first member */
  private final List<DownSamplingUnit> units = new ArrayList<>();

  /** number of members, households and persons without a household */
  private final int numberOfMembers;

  /**
   * Gather a demand into its sampling units: its members joined over the tours they share, each sampling unit with its
   * households and their persons, its persons without a household, its tours and the trips on them
   * <p>
   * A tour belongs to the sampling unit of its first participant, and a trip on a tour to the sampling unit of the
   * tour. A trip without a tour belongs to no sampling unit here, as only the person whose schedule holds it knows it
   * is theirs; the down-sampling table counts it with that person.
   * </p>
   *
   * @param demands to gather
   * @throws IllegalArgumentException when a person lives in a household not in the demand, or takes part in a tour of
   *                                  the demand without being in it
   */
  public DownSamplingDemandUnits(DiscreteDemands demands) {
    var households = new ArrayList<Household>();
    var personsWithoutHousehold = new ArrayList<Person>();
    var memberOfHousehold = new IdentityHashMap<Household, Integer>();
    var memberOfPersonWithoutHousehold = new IdentityHashMap<Person, Integer>();
    for (var household : demands.getHouseholds()) {
      memberOfHousehold.put(household, households.size());
      households.add(household);
    }
    for (var person : demands.getPersons()) {
      if (person.getHousehold() == null) {
        memberOfPersonWithoutHousehold.put(person, households.size() + personsWithoutHousehold.size());
        personsWithoutHousehold.add(person);
      } else if (!memberOfHousehold.containsKey(person.getHousehold())) {
        throw new IllegalArgumentException(String.format(
            "Person %s lives in household %s, which is not in the demand", person.getIdsAsString(),
            person.getHousehold().getIdsAsString()));
      }
    }
    numberOfMembers = households.size() + personsWithoutHousehold.size();

    var joinedPairs = new ArrayList<Pair<Integer, Integer>>();
    for (var tour : demands.getTours()) {
      if (tour.getParticipantTours().isEmpty()) {
        continue;
      }
      int firstMember = findMember(
          tour.getParticipantTours().get(0).getPerson(), memberOfHousehold, memberOfPersonWithoutHousehold);
      for (var participation : tour.getParticipantTours()) {
        int member = findMember(participation.getPerson(), memberOfHousehold, memberOfPersonWithoutHousehold);
        if (member != firstMember) {
          joinedPairs.add(Pair.of(firstMember, member));
        }
      }
    }
    var unitOfMember = collectUnitOfMember(numberOfMembers, joinedPairs);

    var personsByHousehold = demands.getPersons().createGroupByIndex(Person::getHousehold);
    for (int member = 0; member < numberOfMembers; ++member) {
      if (unitOfMember[member] == units.size()) {
        units.add(new DownSamplingUnit());
      }
      var unit = units.get(unitOfMember[member]);
      if (member < households.size()) {
        var household = households.get(member);
        unit.addHousehold(household, personsByHousehold.getOrDefault(household, List.of()));
      } else {
        unit.addPersonWithoutHousehold(personsWithoutHousehold.get(member - households.size()));
      }
    }

    Function<Tour, DownSamplingUnit> unitOfTour = tour -> units.get(unitOfMember[findMember(
        tour.getParticipantTours().get(0).getPerson(), memberOfHousehold, memberOfPersonWithoutHousehold)]);
    for (var tour : demands.getTours()) {
      if (!tour.getParticipantTours().isEmpty()) {
        unitOfTour.apply(tour).addTour(tour);
      }
    }
    for (var trip : demands.getTrips()) {
      if (trip.getTour() != null && !trip.getTour().getParticipantTours().isEmpty()) {
        unitOfTour.apply(trip.getTour()).addTrip(trip);
      }
    }
  }

  /**
   * The sampling units
   *
   * @return the sampling units, in the order of their first household, or person without a household
   */
  public List<DownSamplingUnit> getUnits() {
    return units;
  }

  /**
   * Number of members, households and persons without a household
   *
   * @return the number
   */
  public int getNumberOfMembers() {
    return numberOfMembers;
  }
}
