final class PlayerProgress {

    private static boolean rewardUnlocked;

    private int totalEnemyCount;
    private int currentEnemyCount;
    private int shooterEnemyCount;
    private int loboEnemyCount;
    private int jumperEnemyCount;
    private int dasherEnemyCount;
    private int bomberEnemyCount;

    private int coins;
    private int bait;
    private int weapons = 1;
    private int keys;
    private int totalCollectedKeys;
    private boolean helmet;
    private boolean checkpointRequested;

    static void setRewardUnlocked(boolean unlocked) {
        rewardUnlocked = unlocked;
    }

    static boolean isRewardUnlocked() {
        return rewardUnlocked;
    }

    void reset() {
        totalEnemyCount = 0;
        currentEnemyCount = 0;
        shooterEnemyCount = 0;
        loboEnemyCount = 0;
        jumperEnemyCount = 0;
        dasherEnemyCount = 0;
        bomberEnemyCount = 0;
        coins = 0;
        bait = 0;
        weapons = 1;
        keys = 0;
        totalCollectedKeys = 0;
        helmet = false;
        checkpointRequested = false;
        rewardUnlocked = false;
    }

    void addEnemyCount(int count) {
        totalEnemyCount += count;
        currentEnemyCount += count;
    }

    int getTotalEnemyCount() {
        return totalEnemyCount;
    }

    void setTotalEnemyCount(int count) {
        totalEnemyCount = count;
    }

    int getCurrentEnemyCount() {
        return currentEnemyCount;
    }

    void setCurrentEnemyCount(int count) {
        currentEnemyCount = count;
    }

    int getShooterEnemyCount() {
        return shooterEnemyCount;
    }

    void addShooterEnemyCount(int count) {
        shooterEnemyCount += count;
    }

    int getLoboEnemyCount() {
        return loboEnemyCount;
    }

    void setLoboEnemyCount(int count) {
        loboEnemyCount = count;
    }

    void addLoboEnemyCount(int count) {
        loboEnemyCount += count;
    }

    int getJumperEnemyCount() {
        return jumperEnemyCount;
    }

    void addJumperEnemyCount(int count) {
        jumperEnemyCount += count;
    }

    int getDasherEnemyCount() {
        return dasherEnemyCount;
    }

    void addDasherEnemyCount(int count) {
        dasherEnemyCount += count;
    }

    int getBomberEnemyCount() {
        return bomberEnemyCount;
    }

    void addBomberEnemyCount(int count) {
        bomberEnemyCount += count;
    }

    void addCoins(int amount) {
        coins += amount;
    }

    int getCoins() {
        return coins;
    }

    void setCoins(int coins) {
        this.coins = coins;
    }

    void addBait(int amount) {
        bait += amount;
    }

    int getBait() {
        return bait;
    }

    void addKey(int amount) {
        keys += amount;
        if (amount > 0) {
            totalCollectedKeys += amount;
        }
    }

    int getKeys() {
        return keys;
    }

    void restoreKeys(int keys) {
        this.keys = keys;
    }

    int getTotalCollectedKeys() {
        return totalCollectedKeys;
    }

    void equipWeapon() {
        weapons++;
    }

    int getWeapons() {
        return weapons;
    }

    boolean hasHelmet() {
        return helmet;
    }

    void setHelmet(boolean helmet) {
        this.helmet = helmet;
    }

    void requestCheckpoint() {
        checkpointRequested = true;
    }

    void clearCheckpointRequest() {
        checkpointRequested = false;
    }

    boolean isCheckpointRequested() {
        return checkpointRequested;
    }
}
