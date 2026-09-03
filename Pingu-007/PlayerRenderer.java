import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class PlayerRenderer {

    private static final int SPRITE_SIZE = 16;
    private static final int DRAW_SIZE = 48;
    private static final int DRAW_SCALE = 3;
    private static final int WALK_ANIMATION_TICK = 12;

    private final BufferedImage[] playerSprites;
    private final BufferedImage[] gunSprites;

    private double shadowLength = 42.0;
    private float shadowOpacity = 0.42f;
    private int animationIndex;
    private double animationTick;
    private int animationRow;
    private int walkFramePhase;
    private int spriteOverrideIndex = -1;
    private int spriteOverrideTimer;

    PlayerRenderer() {
        BufferedImage atlas = LoadSave.GetSpriteAtlas("images/pingu_sprite_sheet.png");
        playerSprites = new BufferedImage[24];
        gunSprites = new BufferedImage[4];
        loadSprites(atlas);
    }

    private void loadSprites(BufferedImage atlas) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 7; column++) {
                int index = row * 7 + column;
                playerSprites[index] = atlas.getSubimage(
                        column * SPRITE_SIZE, row * SPRITE_SIZE, SPRITE_SIZE, SPRITE_SIZE);
            }
        }

        playerSprites[21] = atlas.getSubimage(0, 48, SPRITE_SIZE, SPRITE_SIZE);
        playerSprites[22] = atlas.getSubimage(16, 48, SPRITE_SIZE, SPRITE_SIZE);
        playerSprites[23] = atlas.getSubimage(32, 48, SPRITE_SIZE, SPRITE_SIZE);
        gunSprites[0] = atlas.getSubimage(48, 48, SPRITE_SIZE, SPRITE_SIZE);
        gunSprites[1] = atlas.getSubimage(64, 48, SPRITE_SIZE, SPRITE_SIZE);
        gunSprites[2] = atlas.getSubimage(80, 48, SPRITE_SIZE, SPRITE_SIZE);
        gunSprites[3] = atlas.getSubimage(96, 48, SPRITE_SIZE, SPRITE_SIZE);
    }

    void update() {
        if (spriteOverrideTimer > 0) {
            spriteOverrideTimer--;
            if (spriteOverrideTimer == 0) {
                spriteOverrideIndex = -1;
            }
        } else {
            spriteOverrideIndex = -1;
        }
    }

    void draw(Graphics2D graphics, double delta, Player player,
            PlayerCombat combat, PlayerFishing fishing) {
        SpriteSelection sprite = selectPlayerSprite(delta, player);
        VisualState visual = createVisualState(player, combat, sprite);

        ProjectedShadow.drawForEntity(graphics, player.getX(), player.getY(), DRAW_SIZE, DRAW_SIZE,
                shadowLength, shadowOpacity,
                new ProjectedShadow.Part(visual.playerFrame,
                        visual.playerX, visual.playerY, visual.playerWidth, DRAW_SIZE),
                new ProjectedShadow.Part(visual.gunFrame,
                        visual.gunX, visual.gunY, visual.gunWidth, visual.gunHeight));

        if (visual.gunBehindPlayer) {
            drawGun(graphics, visual);
        }
        graphics.drawImage(visual.playerFrame, visual.playerX, visual.playerY,
                visual.playerWidth, DRAW_SIZE, null);
        if (!visual.gunBehindPlayer) {
            drawGun(graphics, visual);
        }

        fishing.draw(graphics);
    }

    private SpriteSelection selectPlayerSprite(double delta, Player player) {
        int flip = 1;
        int drawX = (int) player.getX();

        if (player.getDashDurationRemaining() > 0) {
            if (player.getDashDirX() < 0) {
                flip = -1;
                drawX = (int) (player.getX() + GameCore.tiles_size);
            }
            animationIndex = getDashAnimationIndex(player.getDashDurationRemaining());
            animationRow = getDashAnimationRow(player);
        } else {
            if (player.getDirection() == Direction.LEFT) {
                flip = -1;
                drawX = (int) (player.getX() + GameCore.tiles_size);
            }
            updateWalkAnimation(delta);
            animationRow = getWalkAnimationRow(player.getDirection());
        }

        if (!player.isMoving() && !player.isEmDash()) {
            animationIndex = 0;
        }
        if (player.getDamageAnimationTimer() > 0) {
            animationIndex = 0;
            animationRow = getDamageAnimationRow(player.getDirection());
        }

        int spriteIndex = isSpriteOverrideActive()
                ? spriteOverrideIndex
                : animationRow + animationIndex;
        return new SpriteSelection(spriteIndex, flip, drawX);
    }

    private int getDashAnimationIndex(int dashDurationRemaining) {
        if (dashDurationRemaining > 26) {
            return 0;
        }
        if (dashDurationRemaining > 24) {
            return 1;
        }
        if (dashDurationRemaining > 22) {
            return 2;
        }
        return 3;
    }

    private int getDashAnimationRow(Player player) {
        if (player.getDashDirX() != 0) {
            return 10;
        }
        return player.getDashDirY() < 0 ? 17 : 3;
    }

    private void updateWalkAnimation(double delta) {
        animationTick += 60f * delta;
        if (animationTick < WALK_ANIMATION_TICK) {
            return;
        }

        animationTick = 0;
        walkFramePhase = (walkFramePhase + 1) % 4;
        animationIndex = switch (walkFramePhase) {
            case 1 -> 1;
            case 3 -> 2;
            default -> 0;
        };
    }

    private int getWalkAnimationRow(Direction direction) {
        return switch (direction) {
            case DOWN -> 0;
            case UP -> 14;
            case LEFT, RIGHT -> 7;
        };
    }

    private int getDamageAnimationRow(Direction direction) {
        return switch (direction) {
            case DOWN -> 21;
            case UP -> 23;
            case LEFT, RIGHT -> 22;
        };
    }

    private boolean isSpriteOverrideActive() {
        return spriteOverrideTimer > 0
                && spriteOverrideIndex >= 0
                && spriteOverrideIndex < playerSprites.length;
    }

    private VisualState createVisualState(Player player, PlayerCombat combat, SpriteSelection sprite) {
        int gunFlip = 1;
        int gunAnchorX = (int) player.getX();
        double gunAngle = player.getAimAngle();

        if (gunAngle > Math.PI / 2 || gunAngle < -Math.PI / 2) {
            gunFlip = -1;
            gunAnchorX = (int) player.getX() + DRAW_SIZE;
            gunAngle = gunAngle > 0 ? Math.PI - gunAngle : -Math.PI - gunAngle;
        }

        int gunIndex = combat.getGunType() == Player.GunType.SHOTGUN ? 2 : 0;
        if (combat.isShotAnimationActive()) {
            gunIndex++;
        }

        BufferedImage gunFrame = HelpMethods.rotateImageByDegrees(gunSprites[gunIndex], gunAngle);
        int gap = (gunFrame.getWidth() * DRAW_SCALE - DRAW_SIZE) / 2;
        int gunY = (int) player.getY() - gap + 6;
        int gunOffset = combat.getGunType() == Player.GunType.PISTOL ? 20 : 12;
        int gunX = gunAnchorX - (gap - gunOffset) * gunFlip;

        return new VisualState(
                playerSprites[sprite.index], sprite.drawX, (int) player.getY(), DRAW_SIZE * sprite.flip,
                gunFrame, gunX, gunY,
                gunFrame.getWidth() * DRAW_SCALE * gunFlip,
                gunFrame.getHeight() * DRAW_SCALE,
                combat.getGunType() == Player.GunType.PISTOL || player.getDirection() == Direction.UP);
    }

    private void drawGun(Graphics2D graphics, VisualState visual) {
        graphics.drawImage(visual.gunFrame, visual.gunX, visual.gunY,
                visual.gunWidth, visual.gunHeight, null);
    }

    void setShadowLength(double length) {
        shadowLength = Math.max(0.0, length);
    }

    void setShadowOpacity(float opacity) {
        shadowOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    void setTemporarySpriteOverride(int spriteIndex, int durationUpdates) {
        this.spriteOverrideIndex = spriteIndex;
        this.spriteOverrideTimer = Math.max(0, durationUpdates);
        if (this.spriteOverrideTimer == 0) {
            this.spriteOverrideIndex = -1;
        }
    }

    void resetTransientState() {
        spriteOverrideIndex = -1;
        spriteOverrideTimer = 0;
    }

    private static final class SpriteSelection {
        private final int index;
        private final int flip;
        private final int drawX;

        private SpriteSelection(int index, int flip, int drawX) {
            this.index = index;
            this.flip = flip;
            this.drawX = drawX;
        }
    }

    private static final class VisualState {
        private final BufferedImage playerFrame;
        private final int playerX;
        private final int playerY;
        private final int playerWidth;
        private final BufferedImage gunFrame;
        private final int gunX;
        private final int gunY;
        private final int gunWidth;
        private final int gunHeight;
        private final boolean gunBehindPlayer;

        private VisualState(BufferedImage playerFrame, int playerX, int playerY, int playerWidth,
                BufferedImage gunFrame, int gunX, int gunY, int gunWidth, int gunHeight,
                boolean gunBehindPlayer) {
            this.playerFrame = playerFrame;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerWidth = playerWidth;
            this.gunFrame = gunFrame;
            this.gunX = gunX;
            this.gunY = gunY;
            this.gunWidth = gunWidth;
            this.gunHeight = gunHeight;
            this.gunBehindPlayer = gunBehindPlayer;
        }
    }
}
