package de.ottoextra.animalinfo;

public record AnimalInfoSnapshot(String animalName, String gender, String mast,
                                 String breedingReady, String shearable, String milkable,
                                 String jumpHeight, String speed, String life) {

    public AnimalInfoSnapshot {
        animalName = clean(animalName);
        gender = clean(gender);
        mast = clean(mast);
        breedingReady = clean(breedingReady);
        shearable = clean(shearable);
        milkable = clean(milkable);
        jumpHeight = clean(jumpHeight);
        speed = clean(speed);
        life = clean(life);
    }

    public AnimalInfoSnapshot(String animalName, String gender, String mast,
                              String breedingReady, String shearable, String milkable,
                              String jumpHeight, String speed) {
        this(animalName, gender, mast, breedingReady, shearable, milkable,
                jumpHeight, speed, "");
    }

    public AnimalInfoSnapshot(String animalName, String gender, String breedingReady) {
        this(animalName, gender, "", breedingReady, "", "", "", "", "");
    }

    public AnimalInfoSnapshot(String animalName, String gender, String mast,
                              String breedingReady, String shearable, String milkable) {
        this(animalName, gender, mast, breedingReady, shearable, milkable,
                "", "", "");
    }

    public boolean complete() {
        return !animalName.isEmpty() && !gender.isEmpty() && !breedingReady.isEmpty();
    }

    public boolean hasShearable() {
        return !shearable.isEmpty();
    }

    public boolean hasMilkable() {
        return !milkable.isEmpty();
    }

    public boolean hasHorseStats() {
        return !jumpHeight.isEmpty() || !speed.isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
