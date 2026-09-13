package de.ottoextra.horses.ui;

import de.ottoextra.api.model.AnimalBreedingHorse;
import de.ottoextra.config.OttoExtraConfig;

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

    static String displayName(AnimalBreedingHorse record) {
        return record.registryName().isBlank() ? "???" : record.registryName();
    }

    static List<AnimalKind> filters(OttoExtraConfig.AnimalInfo config) {
        return java.util.Arrays.stream(AnimalKind.values())
                .filter(kind -> kind != AnimalKind.OTHER && kind.enabled(config)).toList();
    }

    static List<AnimalBreedingHorse> filter(List<AnimalBreedingHorse> records, AnimalKind selected,
                                           OttoExtraConfig.AnimalInfo config) {
        return records.stream().filter(record -> {
            AnimalKind kind = kind(record);
            return selected == AnimalKind.ALL || (kind.enabled(config) && selected == kind);
        }).toList();
    }

    static AnimalKind kind(AnimalBreedingHorse record) {
        String type = record.animalType().toLowerCase(Locale.ROOT);
        if (containsAny(type, "esel", "donkey", "maultier", "mule")) return AnimalKind.DONKEYS;
        if (containsAny(type, "rind", "rotvieh", "kuh", "cow")) return AnimalKind.COWS;
        if (containsAny(type, "brakel", "huhn", "hühner", "chicken")) return AnimalKind.CHICKENS;
        if (containsAny(type, "schwein", "pig")) return AnimalKind.PIGS;
        if (containsAny(type, "ziege", "goat")) return AnimalKind.GOATS;
        if (containsAny(type, "zaupel", "schaf", "sheep")) return AnimalKind.SHEEP;
        if (containsAny(type, "pferd", "horse", "ross", "gaul")
                || (type.isBlank() && (!record.speed().isBlank() || !record.jumpHeight().isBlank()
                || !record.coatColor().isBlank()))) return AnimalKind.HORSES;
        return AnimalKind.OTHER;
    }

    private static boolean containsAny(String type, String... aliases) {
        for (String alias : aliases) if (type.contains(alias)) return true;
        return false;
    }

    enum AnimalKind {
        ALL, HORSES, DONKEYS, COWS, CHICKENS, PIGS, GOATS, SHEEP, OTHER;

        String translationKey() { return "ottoextra.horses.filter." + name().toLowerCase(Locale.ROOT); }

        boolean enabled(OttoExtraConfig.AnimalInfo config) {
            return switch (this) {
                case ALL, HORSES, DONKEYS, OTHER -> true;
                case COWS -> config.overviewShowCows;
                case CHICKENS -> config.overviewShowChickens;
                case PIGS -> config.overviewShowPigs;
                case GOATS -> config.overviewShowGoats;
                case SHEEP -> config.overviewShowSheep;
            };
        }
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
        // Keep the reference values independent of the search text.
        Map<String, double[]> maxima = new LinkedHashMap<>();
        for (AnimalBreedingHorse horse : records) {
            double[] max = maxima.computeIfAbsent(horse.currentFarm().trim(), ignored -> new double[2]);
            Double speed = numericValue(horse.speed()), jump = numericValue(horse.jumpHeight());
            if (speed != null && jump != null) {
                max[0] = Math.max(max[0], speed);
                max[1] = Math.max(max[1], jump);
            }
        }
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
                        entry.getValue().stream().sorted(metricComparator(selection, maxima.get(entry.getKey()))
                                .thenComparing(byName)).toList()))
                .toList();
    }

    private static Comparator<AnimalBreedingHorse> metricComparator(SortSelection selection, double[] maxima) {
        if (selection.criteria().size() == 2) {
            return Comparator.comparing(horse -> combinedScore(horse, selection, maxima),
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if (selection.criteria().isEmpty()) return (left, right) -> 0;
        SortOrder order = selection.criteria().getFirst();
        Comparator<Double> values = order.ascending() ? Comparator.naturalOrder() : Comparator.reverseOrder();
        return Comparator.comparing(horse -> numericValue(order.isSpeed() ? horse.speed() : horse.jumpHeight()),
                Comparator.nullsLast(values));
    }

    private static Double combinedScore(AnimalBreedingHorse horse, SortSelection selection, double[] maxima) {
        Double speed = numericValue(horse.speed()), jump = numericValue(horse.jumpHeight());
        if (speed == null || jump == null) return null;
        double speedPart = maxima[0] == 0 ? 1 : speed / maxima[0];
        double jumpPart = maxima[1] == 0 ? 1 : jump / maxima[1];
        if (selection.metric(true).ascending()) speedPart = 1 - speedPart;
        if (selection.metric(false).ascending()) jumpPart = 1 - jumpPart;
        return (speedPart + jumpPart) / 2;
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

    /** Farm grouping is independent; two metrics contribute equally to the combined score. */
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
