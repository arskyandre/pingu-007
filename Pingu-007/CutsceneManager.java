import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class CutsceneManager {

    public enum Phase {
        NONE, OPENING, CLOSING
    }

    public enum CutsceneType {
        NONE, BOSS_INTRO, WALL_REVEAL
    }

    private CutsceneType type = CutsceneType.NONE;
    private Phase phase = Phase.NONE;
    private int timer = 0;
    
    private static final int DURATION = 100; // Duração do foco no boss antes/durante o diálogo
    private static final int TEXT_AREA_HEIGHT = 80;
    private String nomeBoss = "";

    // --- wall reveal state ---
    private static final int WALL_REVEAL_DURATION = 150;
    private int wallTimer = 0;
    private Rectangle2D.Double wallFadeRect;
    private Player wallRevealPlayer;
    private static final double WALL_SHAKE_AMPLITUDE_MAX = 2.5;
    private static final double WALL_SHAKE_SPEED = 1.2;

    private GameCore gameCore;
    private MorsaBoss bossRef; // Guarda a referência para ativar o Boss em sequência

    public CutsceneManager(GameCore GC) {
        gameCore = GC;
    }

    public static int getDuracaoTotal() {
        return WALL_REVEAL_DURATION + DURATION;
    }

    public void setBossRef(MorsaBoss boss) {
        this.bossRef = boss;
    }

    public void iniciar(String nomeBoss) {
        this.type = CutsceneType.BOSS_INTRO;
        this.nomeBoss = nomeBoss;
        this.phase = Phase.OPENING;
        this.timer = 0;
        gameCore.setCinematicBorderAnimation(Renderer.BorderState.IN);
    }

    public void iniciarWallFade(Rectangle2D.Double wallRect) {
        if (type == CutsceneType.WALL_REVEAL && wallRevealPlayer != null) return;
        this.type = CutsceneType.WALL_REVEAL;
        this.wallTimer = 0;
        this.wallFadeRect = wallRect;
        this.wallRevealPlayer = null;
    }

    public void iniciarWallRevealComCamera(Rectangle2D.Double wallRect, CameraManager camera, Player player) {
        this.type = CutsceneType.WALL_REVEAL;
        this.wallTimer = 0;
        this.wallFadeRect = wallRect;
        this.wallRevealPlayer = player;
        camera.focarEmRect(wallRect, (int) WALL_REVEAL_DURATION, gameCore.getWidth(), gameCore.getHeight());
        player.setBlockInputs(true);
    }

    public void update() {
        if (type == CutsceneType.BOSS_INTRO) {
            timer++;

            DialogueManager dm = gameCore.getDialogueManager();

            // Enquanto o diálogo estiver rolando, trava o timer e mantém o foco na morsa
            if (dm != null && dm.isAtivo()) {
                timer = DURATION - 1;
                if (bossRef != null) {
                    bossRef.iniciarCutsceneEntrada(2); // Força a câmera a renovar o foco no boss continuamente
                }
            } else if (timer >= DURATION) {
                // O diálogo acabou E o timer passou do mínimo? Encerra a cutscene
                phase = Phase.CLOSING;
                gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                
                phase = Phase.NONE;
                timer = 0;
                nomeBoss = "";
                type = CutsceneType.NONE;
                bossRef = null;
                if (wallRevealPlayer != null) {
                    wallRevealPlayer.setBlockInputs(false);
                    wallRevealPlayer = null;
                }
            }
        } else if (type == CutsceneType.WALL_REVEAL) {
            wallTimer++;
            
            // Quando a animação dos espinhos terminando de nascer acabar:
            if (wallTimer >= WALL_REVEAL_DURATION) {
                wallFadeRect = null;
                wallTimer = 0;
                
                // SE FOR A ARENA DO BOSS
                if (bossRef != null) {
                    this.type = CutsceneType.BOSS_INTRO;
                    this.phase = Phase.OPENING;
                    this.timer = 0;
                    this.nomeBoss = "Morsa Gigante, o terror do Ártico";
                    
                    // Move a câmera suavemente até o Boss
                    bossRef.iniciarCutsceneEntrada(DURATION);
                    
                    // Força o boss a rugir e tremer a tela imediatamente
                    bossRef.forçarRugidoAtivo(); 
                    
                    // Dispara a caixa de diálogo na tela
                    DialogueManager dm = gameCore.getDialogueManager();
                    if (dm != null) {
                        dm.iniciarDialogo(new String[] {
                            "MORSA GIGANTE: TA MALUCO PINGU, TA DOIDO DE INVADIR MEU CAFOFO?!",
                            "PINGU: Devolve minha irmã sua morsa maluca, vou te encher de furo!",
                            "MORSA GIGANTE: VOU TRANSFORMAR VOCÊ E SUA IRMÃ EM PICOLES!",
                            "Vem tranquilo."
                        });
                    }
                } else {
                    // Se for apenas uma arena comum, segue o fluxo antigo de encerramento
                    if (wallRevealPlayer != null) {
                        wallRevealPlayer.setBlockInputs(false);
                    }
                    if (gameCore.getArenaManager() != null && !gameCore.getArenaManager().existeCombateAtivo()) {
                        gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                    }
                    wallRevealPlayer = null;
                    type = CutsceneType.NONE;
                }
            }
        }
    }

    public boolean isAtiva() {
        return type == CutsceneType.BOSS_INTRO || (type == CutsceneType.WALL_REVEAL && wallRevealPlayer != null);
    }

    public boolean isWallFadeAtiva() {
        return type == CutsceneType.WALL_REVEAL;
    }

    public boolean isWallRevealAtiva(){
        return type == CutsceneType.WALL_REVEAL;
    }

    private double getWallShakeAmplitude() {
        double progress = wallTimer / (double) WALL_REVEAL_DURATION;
        return WALL_SHAKE_AMPLITUDE_MAX * Math.pow(1.0 - progress, 2.0);
    }

    public double getWallShakeX() {
        if (!isWallFadeAtiva()) return 0;
        return Math.sin(wallTimer * WALL_SHAKE_SPEED) * getWallShakeAmplitude();
    }

    public double getWallShakeY() {
        if (!isWallFadeAtiva()) return 0;
        return Math.cos(wallTimer * WALL_SHAKE_SPEED * 1.3) * getWallShakeAmplitude();
    }

    public Rectangle2D.Double getWallFadeRect() {
        return wallFadeRect;
    }

    public float getWallFadeAlpha() {
        if (!isWallFadeAtiva()) return 1f;
        return Math.min(1f, (float) wallTimer / (float) WALL_REVEAL_DURATION);
    }

    public void draw(Graphics2D g2, int telaLargura, int telaAltura) {
        if (type != CutsceneType.BOSS_INTRO) return;

        AffineTransform transformOriginal = g2.getTransform();
        g2.setTransform(new AffineTransform());

        if (nomeBoss != null && !nomeBoss.isEmpty()) {
            desenharNomeBoss(g2, telaLargura, telaAltura);
        }

        g2.setTransform(transformOriginal);
    }

    private void desenharNomeBoss(Graphics2D g2, int telaLargura, int telaAltura) {
        Object antialiasAntigo = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font fonteNome = new Font("Serif", Font.BOLD, 26);
        g2.setFont(fonteNome);
        FontMetrics fm = g2.getFontMetrics();
        int textoLargura = fm.stringWidth(nomeBoss);
        int textoX = (telaLargura - textoLargura) / 2;
        int textoY = (TEXT_AREA_HEIGHT / 2) + (fm.getAscent() / 2) - 4;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(nomeBoss, textoX + 2, textoY + 2);
        g2.setColor(new Color(230, 230, 230));
        g2.drawString(nomeBoss, textoX, textoY);

        if (antialiasAntigo != null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antialiasAntigo);
        }
    }
}
