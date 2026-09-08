package de.ottoextra.horses.ui;

import de.ottoextra.api.model.AnimalBreedingHorse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class HorseOverviewView {
    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");

    private HorseOverviewView() {
    }

    static List<FarmGroup> groups(List<AnimalBreedingHorse> records,
                                  String rawQuery, SortOrder order) {
        SortSelection selection = new SortSelection();
        selection.select(order);
        return groups(records, rawQuery, selection);
    }

    static List<FarmGroup> groups(List<AnimalBreedingHorse> records,
                                  String rawQuery, SortSelection selection) {
        String query = rawQuery == null ? ""
                : rawQuery.trim().toLowerCase(Locale.ROOT);
        Map<String, List<AnimalBreedingHorse>> grouped = new LinkedHashMap<>();
        records.stream()
                .filter(record -> matches(record, query))
                .forEach(record -> grouped.computeIfAbsent(
                        record.currentFarm().trim(), ignored -> new ArrayList<>())
                        .add(record));

        Comparator<String> farms = String.CASE_INSENSITIVE_ORDER;
        if (selection.farmOrder() == SortOrder.FARMS_DESCENDING) {
            farms = farms.reversed();
        }
        Comparator<String> namedFarms = farms;
        Comparator<AnimalBreedingHorse> byName = Comparator
                .comparing(AnimalBreedingHorse::registryName,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AnimalBreedingHorse::entityUuid);
        Comparator<AnimalBreedingHorse> metrics = (left, right) -> 0;
        for (SortOrder order : selection.criteria()) {
            Comparator<Double> values = order.ascending()
                    ? Comparator.naturalOrder() : Comparator.reverseOrder();
            metrics = metrics.thenComparing(horse -> numericValue(
                    order.isSpeed() ? horse.speed() : horse.jumpHeight()), Comparator.nullsLast(values));
        }
        Comparator<AnimalBreedingHorse> horses = metrics.thenComparing(byName);

        return grouped.entrySet().stream()
                .sorted((left, right) -> {
                    if (left.getKey().isBlank()) {
                        return right.getKey().isBlank() ? 0 : 1;
                    }
                    if (right.getKey().isBlank()) {
                        return -1;
                    }
                    return namedFarms.compare(left.getKey(), right.getKey());
                })
                .map(entry -> new FarmGroup(entry.getKey(),
                        entry.getValue().stream().sorted(horses).toList()))
                .toList();
    }

    private static Double numericValue(String raw) {
        if (raw == null) return null;
        var number = NUMBER.matcher(raw);
        if (!number.find()) return null;
        try {
            double value = Double.parseDouble(number.group().replace(',', '.'));
            return Double.isFinite(value) && value >= 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean matches(AnimalBreedingHorse record, String query) {
        if (query.isBlank()) {
            return true;
        }
        return contains(record.registryName(), query)
                || contains(record.entityUuid(), query)
                || contains(record.origin(), query)
                || contains(record.currentFarm(), query)
                || contains(record.animalType(), query);
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    record FarmGroup(String farm, List<AnimalBreedingHorse> horses) {
    }

    /** Farm grouping is independent; metric selection order defines tie-break priority. */
    static final class SortSelection {
        private SortOrder farmOrder = SortOrder.FARMS_ASCENDING;
        private final List<SortOrder> criteria = new ArrayList<>();

        SortOrder farmOrder() { return farmOrder; }
        List<SortOrder> criteria() { return List.copyOf(criteria); }

        SortOrder metric(boolean speed) {
            for (SortOrder order : criteria) {
                if (order.isSpeed() == speed) return order;
            }
            return null;
        }

        int priority(SortOrder order) {
            return criteria.indexOf(order) + 1;
        }

        boolean active(SortOrder order) {
            return order == farmOrder || criteria.contains(order);
        }

        void select(SortOrder order) {
            if (!order.isSpeed() && !order.isJump()) {
                farmOrder = order;
                return;
            }
            SortOrder previous = metric(order.isSpeed());
            if (previous == null) criteria.add(order);
            else if (previous == order) criteria.remove(previous);
            else criteria.set(criteria.indexOf(previous), order);
        }

        void cycle(boolean speed) {
            SortOrder previous = metric(speed);
            if (previous == null) select(speed ? SortOrder.SPEED_DESCENDING : SortOrder.JUMP_DESCENDING);
            else if (previous.ascending()) select(previous); // ascending -> off
            else select(speed ? SortOrder.SPEED_ASCENDING : SortOrder.JUMP_ASCENDING);
        }

        void promote(SortOrder order) {
            int index = criteria.indexOf(order);
            if (index > 0) {
                criteria.remove(index);
                criteria.add(0, order);
            }
        }

        void reset() {
            farmOrder = SortOrder.FARMS_ASCENDING;
            criteria.clear();
        }
    }

    enum SortOrder {
        FARMS_ASCENDING("sort_az", true),
        FARMS_DESCENDING("sort_za", false),
        SPEED_ASCENDING("sort_speed_asc", true),
        SPEED_DESCENDING("sort_speed_desc", false),
        JUMP_ASCENDING("sort_jump_asc", true),
        JUMP_DESCENDING("sort_jump_desc", false);

        private final String key;
        private final boolean ascending;

        SortOrder(String key, boolean ascending) {
            this.key = key;
            this.ascending = ascending;
        }

        String translationKey() { return "ottoextra.horses.overview." + key; }
        boolean ascending() { return ascending; }
        boolean isSpeed() { return this == SPEED_ASCENDING || this == SPEED_DESCENDING; }
        boolean isJump() { return this == JUMP_ASCENDING || this == JUMP_DESCENDING; }
    }
}
