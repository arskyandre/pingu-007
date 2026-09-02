
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public class InteractiveMapObject extends MapObject {

    private boolean playerCanInteract = false;
    private DialogueManager dialogueManager;
    private GameCore gameCore;
    private String acao;
    private boolean isAberto_Concluido = false;
    private boolean starEvent = false;

    public InteractiveMapObject(TiledObject tObj, DialogueManager dm, GameCore GC) {
        super(tObj);
        this.dialogueManager = dm;
        this.gameCore = GC;
        this.acao = tObj.acao != null ? tObj.acao.trim() : "";
        // if ("abrir_portao".equalsIgnoreCase(this.acao)) {
        //     System.out.println("\n=== DEBUG PORTÃO: NASCIMENTO ===");
        //     System.out.println("RAW (Tiled) -> X: " + tObj.x + " | Y: " + tObj.y + " | W: " + tObj.width + " | H: " + tObj.height);
        //     System.out.println("SCALED (Java) -> X: " + this.getX() + " | Y: " + this.getY() + " | W: " + this.getLargura() + " | H: " + this.getAltura());
        //     if (this.getHitbox() != null) {
        //         System.out.println("Hitbox Final -> " + this.getHitbox().getBounds2D());
        //     } else {
        //         System.out.println("Hitbox Final -> NULL");
        //     }
        // }
    }

    public void update(Player player) {
        if (isAberto_Concluido) {
            playerCanInteract = false;
            return;
        }

        if (player.getHurtbox() != null) {
            java.awt.geom.Rectangle2D bounds;

            if (getHitbox() != null) {
                bounds = getHitbox().getBounds2D();
            } else {
                bounds = new java.awt.geom.Rectangle2D.Double(getX(), getY(), getLargura(), getAltura());
            }

            bounds.setRect(bounds.getX() - 10, bounds.getY() - 10,
                    bounds.getWidth() + 20, bounds.getHeight() + 20);

            java.awt.geom.Rectangle2D playerBounds = new java.awt.geom.Rectangle2D.Double(
                    player.getX() + player.getHurtbox().getOffsetX(),
                    player.getY() + player.getHurtbox().getOffsetY(),
                    player.getHurtbox().getWidth(),
                    player.getHurtbox().getHeight());

            playerCanInteract = bounds.intersects(playerBounds);
        }
    }

    public boolean tryInteract(Player player, int chavesDoPlayer) {

        if (!playerCanInteract || isAberto_Concluido) {
            return false;
        }

        if ("trocar_mapa".equalsIgnoreCase(acao)) {
            return true;
        }

        if ("star".equalsIgnoreCase(acao) && !starEvent) {
            starEvent = true;
            dialogueManager.iniciarDialogo(new String[]{
                "PINGU ME AJUDE! ENFIARAM UMA ARVORE NO MEU BUTICO SOCORRO AAAAAAA"}, null,
                    new BufferedImage[]{GameCore.star_portrait});
                this.data.gid += 1;
                LoadSave.applyGidData(this.data);
                this.setSprite(this.data.sprite);
            dialogueManager.setAoTerminarDialogo(()->{
                player.metodoInutil();
                this.data.gid += 1;
                LoadSave.applyGidData(this.data);
                this.setSprite(this.data.sprite);
                dialogueManager.iniciarDialogo(new String[]{
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}, null,
                        new BufferedImage[]{GameCore.star_portrait});
                dialogueManager.setAoTerminarDialogo(()->{
                    this.data.gid -= 3;
                LoadSave.applyGidData(this.data);
                this.setSprite(this.data.sprite);
                });
                //grita e muda pra uma arvore normal
            });
            
            return true;
        }

        if ("abrir_portao".equalsIgnoreCase(acao)) {
            if (chavesDoPlayer >= 3) {
                isAberto_Concluido = true;
                // System.out.println("\n=== DEBUG PORTÃO: ABRINDO ===");
                // System.out.println("ANTES (Tiled) -> X: " + this.data.x + " | Y: " + this.data.y + " | GID: " + this.data.gid);

                this.data.gid += 1;
                LoadSave.applyGidData(this.data);

                // System.out.println("DEPOIS (LoadSave) -> X: " + this.data.x + " | Y: " + this.data.y + " | GID: " + this.data.gid);
                // if (this.data.hitbox != null) {
                //     System.out.println("Nova Hitbox (Raw) -> " + this.data.hitbox.getBounds2D());
                // }
                // this.updateTransformFromData();
                this.setSprite(this.data.sprite);

                if (this.data.hitbox != null) {
                    AffineTransform tx = new AffineTransform();

                    tx.translate(this.getX(), this.getY());
                    tx.scale(GameCore.scale, GameCore.scale);
                    tx.translate(-this.data.x, -this.data.y);

                    Shape novaHitboxMundo = tx.createTransformedShape(this.data.hitbox);

                    this.setHitboxNoMundo(novaHitboxMundo);
                    this.setSolid(this.data.collision);
                    // System.out.println("Hitbox (Escalada) -> " + this.getHitbox().getBounds2D());
                } else {
                    this.setHitboxNoMundo(null);
                    this.setSolid(false);
                    // System.out.println("Hitbox (Escalada) -> NULL (Sem colisão)");
                }

                dialogueManager.iniciarDialogo(new String[]{
                    "RADIO: Você conseguiu! O portão abriu."}, null,
                        new BufferedImage[]{GameCore.cellphone_image});
                return true;
            }

            String qtdChaves = chavesDoPlayer == 0 ? "Você ainda não tem nenhuma chave."
                    : chavesDoPlayer == 1 ? "Você possui 1 chave." : "Você possui 2 chaves.";

            dialogueManager.iniciarDialogo(new String[]{
                "RADIO: De acordo com as nossas informações, você precisará de 3 chaves para abrir esse portão.",
                "RADIO: " + qtdChaves}, new BufferedImage[]{GameCore.cellphone_image});
            return true;
        }

        if ("pescar".equalsIgnoreCase(acao)) {
            System.out.println("Iniciando minigame de pesca!");
            return true;
        }

        return false;
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        super.draw(g2, delta);

        // Desenha [E]
        if (playerCanInteract && !isAberto_Concluido) {
            String prompt = "[E]";
            g2.setFont(GameCore.pixelFont.deriveFont(7f));
            FontMetrics fm = g2.getFontMetrics();

            int px = (int) (getX() + getLargura() / 2.0 - fm.stringWidth(prompt) / 2.0);
            int py = (int) getY() - 10;

            g2.setColor(new Color(0, 0, 0, 100)); // Sombra
            g2.drawString(prompt, px + 1, py + 1);

            g2.setColor(new Color(20, 77, 55)); // Cor principal
            g2.drawString(prompt, px, py);
        }
    }
}
