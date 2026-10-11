package com.wildspell.mobs.entity;

public final class EelTestAccess {
    private EelTestAccess() {
    }

    public static boolean settingOffForDen(ElectricEel eel) {
        return !eel.lurking && eel.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof ElectricEel.LurkGoal);
    }
}
