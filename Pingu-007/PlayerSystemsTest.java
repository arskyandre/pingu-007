public class PlayerSystemsTest {

    public static void main(String[] args) {
        testDefaultReloadDuration();
        testFasterReloadDuration();
        testAutomaticReloadDelay();
        testShootingIsBlockedDuringReload();
        testProgressReset();
        System.out.println("PlayerSystemsTest: OK");
    }

    private static void testDefaultReloadDuration() {
        PlayerCombat combat = new PlayerCombat(null, null);
        combat.restoreAmmo(45, 0);
        combat.requestReload();

        advanceReload(combat, 29);
        check(combat.isReloading(), "A recarga normal terminou antes de 30 updates");
        combat.updateAfterInput();

        check(!combat.isReloading(), "A recarga normal nao terminou em 30 updates");
        check(combat.getMagazine() == 15, "A recarga normal nao completou o pente");
        check(combat.getReserveAmmo() == 30, "A recarga normal consumiu municao incorreta");
    }

    private static void testFasterReloadDuration() {
        PlayerCombat combat = new PlayerCombat(null, null);
        combat.setFasterReload(true);
        combat.restoreAmmo(45, 0);
        combat.requestReload();

        advanceReload(combat, 14);
        check(combat.isReloading(), "A recarga rapida terminou antes de 15 updates");
        combat.updateAfterInput();

        check(!combat.isReloading(), "A recarga rapida nao terminou em 15 updates");
    }

    private static void testAutomaticReloadDelay() {
        PlayerCombat combat = new PlayerCombat(null, null);
        combat.restoreAmmo(45, 0);
        combat.scheduleAutomaticReloadIfNeeded();

        for (int i = 0; i < 34; i++) {
            combat.updateCooldownsBeforeInput();
        }
        check(!combat.isReloading(), "A recarga automatica iniciou antes de 35 updates");

        combat.updateCooldownsBeforeInput();
        check(combat.isReloading(), "A recarga automatica nao iniciou em 35 updates");
    }

    private static void testShootingIsBlockedDuringReload() {
        PlayerCombat combat = new PlayerCombat(null, null);
        combat.restoreAmmo(45, 1);
        combat.requestReload();

        check(!combat.tryShoot(0, 0, 1, 0), "Foi possivel disparar durante a recarga");
        check(combat.getMagazine() == 1, "Um disparo bloqueado consumiu municao");
    }

    private static void testProgressReset() {
        PlayerProgress progress = new PlayerProgress();
        progress.addCoins(20);
        progress.addBait(4);
        progress.addKey(2);
        progress.addEnemyCount(3);
        progress.setHelmet(true);
        progress.requestCheckpoint();
        PlayerProgress.setRewardUnlocked(true);

        progress.reset();

        check(progress.getCoins() == 0, "O reset preservou moedas");
        check(progress.getBait() == 0, "O reset preservou iscas");
        check(progress.getKeys() == 0, "O reset preservou chaves");
        check(progress.getTotalCollectedKeys() == 0, "O reset preservou o total de chaves");
        check(progress.getTotalEnemyCount() == 0, "O reset preservou abates");
        check(!progress.hasHelmet(), "O reset preservou o capacete");
        check(!progress.isCheckpointRequested(), "O reset preservou o pedido de checkpoint");
        check(!PlayerProgress.isRewardUnlocked(), "O reset preservou a recompensa");
    }

    private static void advanceReload(PlayerCombat combat, int updates) {
        for (int i = 0; i < updates; i++) {
            combat.updateAfterInput();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
