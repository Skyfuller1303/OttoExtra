package de.ottoextra.resources;

record ResourceStatus(
        String status,
        int regenerationPercentage,
        String nextStageAt,
        String fullyRegeneratedAt,
        int secondsUntilNextStage,
        int secondsUntilFull) {
}
