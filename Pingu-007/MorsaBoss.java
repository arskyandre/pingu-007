import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class MorsaBoss extends Enemy {

    private BossMao maoEsquerda;
    private BossMao maoDireita;

    // Posição de origem do corpo (Sua versão)
    private double xHome, yHome;

    // Sistema de Sprites e Direção
    private BufferedImage[] Sprites;
    private int Direita = 1;
    private int dirS = 1;
    private int[] idle = {0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 4, 5};
    private int[] rugidoSprites = {8, 9}; // Sprites do rugido
    private double animT = 0;
    private double timerVirar = 0;
    private BulletManager bulletManager;

    // --- Controle do Rugido ---
    private boolean rugindo = false;
    private double timerRugido = 0;
    private double cooldownRugido = 0;

    // --- Câmera (tremida ao rugir / foco de entrada na arena) ---
    private CameraManager camera;

    public MorsaBoss(double startX, double startY, int[][] lvlData, BulletManager bulmgr, SoundManager sound) {
        super(startX, startY, GameCore.tiles_size * 6, GameCore.tiles_size * 6, lvlData, sound);
        this.bulletManager = bulmgr;
        this.vida = 500;
        this.cor = Color.BLUE;
        this.aggroPermanente = true;
        this.bodyCollider = new Collider(0, 0, GameCore.tiles_size * 6, GameCore.tiles_size * 6);
        
        // Inicializando sua posição de origem
        this.xHome = startX;
        this.yHome = startY;

        // Inicializando o sistema de Sprites
        BufferedImage img = LoadSave.GetSpriteAtlas("MorsaBoss-Sheet.png");
        Sprites = new BufferedImage[10];
        for (int j = 0; j < 10; j++) {
            Sprites[j] = img.getSubimage(j * 96, 0, 96, 96);
        }
    }

    public void vincularMaos(BossMao esquerda, BossMao direita) {
        this.maoEsquerda = esquerda;
        this.maoDireita = direita;
    }

    @Override
    public void update(Player player, ArrayList<JumpLink> jumpLinks) {
        // Mantém o Boss estritamente travado na posição Home
        velX = 0;
        velY = 0;
        this.x = xHome;
        this.y = yHome;

        if (timerVirar > 0)
            timerVirar -= 1.0;

        // Controle do tempo do rugido ativo
        if (rugindo) {
            timerRugido -= 1.0;
            if (timerRugido <= 0) {
                rugindo = false;
                cooldownRugido = 300; // 5 segundos de recarga antes de rugir de novo (a 60fps)
            }
        } else {
            if (cooldownRugido > 0) {
                cooldownRugido -= 1.0;
            } else {
                // Ativa o Rugido!
                rugindo = true;
                timerRugido = 120; // O rugido dura 2 segundos (120 frames)
                animT = 0; // Reinicia o timer de animação
                
                // Ativa o efeito visual de tremer a câmera se ela estiver vinculada
                if (camera != null) {
                    camera.tremer(10, 60); // Exemplo: intensidade 10, por 60 frames
                }
                
                // Toca o som do rugido se houver gerenciador de áudio
                if (soundManager != null) {
                    // soundManager.playVoice(SoundManager.MORSA_ROAR); // Ajuste para a sua constante de som
                }
            }
        }

        // Lógica de virar o sprite baseada na posição do jogador (só vira se não estiver rugindo)
        if (!rugindo) {
            double playerCenterX = player.getX() + player.getLargura() / 2.0;
            double CenterX = x + (getLargura()/2); 
            if (CenterX < playerCenterX) {
                Direita = 1;
            } else if (CenterX > playerCenterX) {
                Direita = 0;
            }
        }
    }

    public void vincularCamera(CameraManager camera) {
        this.camera = camera;
    }

    public void iniciarCutsceneEntrada(int duracaoFrames) {
        if (camera != null) {
            double centroBossX = this.x + (this.width / 2);
            double centroBossY = this.y + (this.height / 2);
            camera.focarEm(centroBossX, centroBossY, duracaoFrames);
        }
    }

    @Override
    public void animate(Graphics2D g, double delta) {
        int index = 0;
        int xx = (int) x;
        int inv = 1;

        // Transição/Animação de virar para o lado
        if (dirS != Direita && timerVirar <= 0 && !rugindo) {
            timerVirar = 40;
        }
        
        if (timerVirar > 0 && !rugindo) {
            if (timerVirar > 30) {
                index = 6;
            } else if (timerVirar > 20) {
                index = 7;
            } else if (timerVirar > 10) {
                index = 7;
                dirS = Direita;
            } else {
                index = 6;
            }
        } else {
            if (rugindo) {
                // Animação de Rugido (Alterna rápido entre frames 8 e 9)
                animT += 5 * delta;
                if (animT >= 2)
                    animT = 0;
                index = rugidoSprites[(int) animT];
                if(timerRugido > 110)
                  index = 6;
                else if(timerRugido > 100)
                  index = 7;
                else if(timerRugido < 10)
                  index = 6;
                else if(timerRugido < 20)
                  index = 7;
            } else {
                // Animação padrão (Idle)
                animT += 3 * delta;
                if (animT >= 12)
                    animT = 0;
                index = idle[(int) animT];
            }
        }

        // Inverte o sprite horizontalmente se necessário
        if (dirS == 0) {
            xx = (int) x + (int) width;
            inv = -1;
        }

        // Desenha o sprite correto
        if (Sprites[index] != null) {
            g.drawImage(Sprites[index], xx, (int) y, inv * (int) width, (int) height, null);
        }
    }

    @Override
    public void draw(Graphics2D g) {
        desenharBarradevida(g);
    }

    private void desenharBarradevida(Graphics2D g) {
        int vidaMaxima = 500;
        int largura = (int) width;
        int altura = 14;
        int barraX = (int) x;
        int barraY = (int) y - altura - 8;

        double proporcao = Math.max(0, Math.min(1.0, this.vida / (double) vidaMaxima));

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barraX, barraY, largura, altura);

        g.setColor(Color.RED);
        g.fillRect(barraX, barraY, (int) (largura * proporcao), altura);

        g.setColor(Color.WHITE);
        g.drawRect(barraX, barraY, largura, altura);
        g.drawString("MORSA GIGANTE", barraX, barraY - 4);
    }

    public void forçarRugidoAtivo(){
      this.rugindo = true;
      this.timerRugido = 120;
      this.animT = 0;
      if(this.camera != null){
        this.camera.tremer(10,60);
      }
    }

    @Override
    public Collider getHurtbox() {
        return this.bodyCollider;
    }
}

class BossMao extends Enemy {

    private MorsaBoss corpoPrincipal;
    private double xHome, yHome;
    private int estado = 0;
    private double timerEstado = 0;
    private double targetX, targetY;

    // Controle do Ataque (Esmagamento)
    private boolean ladoEsquerdo;
    private double centroAlvoX, centroAlvoY;

    public BossMao(double startX, double startY, int[][] lvlData, SoundManager sound, MorsaBoss corpo) {
        super(startX, startY, GameCore.tiles_size * 1.5, GameCore.tiles_size * 1.5, lvlData, sound);
        this.bodyCollider = new Collider(-1, 0, GameCore.tiles_size * 1.5, GameCore.tiles_size * 1.5);
        this.xHome = startX;
        this.yHome = startY;
        this.cor = Color.RED;
        this.corpoPrincipal = corpo;
        this.vida = 150;
    }

    public void iniciarAtaque() {
        if (estado == 0) {
            estado = 1;
            timerEstado = 0;
        }
    }

    public void iniciarEsmagamento(double alvoX, double alvoY, boolean ladoEsquerdo) {
        if (estado == 0) {
            estado = 3;
            timerEstado = 0;
            this.ladoEsquerdo = ladoEsquerdo;
            this.centroAlvoX = alvoX;
            this.centroAlvoY = alvoY;

            double offsetLateral = GameCore.tiles_size * 3;
            double offsetAltura = GameCore.tiles_size * 4;

            this.targetX = alvoX + (ladoEsquerdo ? -offsetLateral : offsetLateral);
            this.targetY = alvoY - offsetAltura;
        }
    }

    @Override
    public void update(Player player, ArrayList<JumpLink> jumpLinks) {
        if (corpoPrincipal == null || corpoPrincipal.isDead()) {
            this.vida = 0;
            return;
        }

        switch (estado) {
            case 0:
                timerEstado += 0.05;
                this.y = yHome + Math.sin(timerEstado) * 6;
                this.x = xHome;
                break;

            case 1:
                if (timerEstado == 0) {
                    targetX = player.getX();
                    targetY = player.getY();
                }

                timerEstado += 1;

                double dx = targetX - this.x;
                double dy = targetY - this.y;
                double dist = Math.hypot(dx, dy);

                if (dist > 15 && timerEstado < 45) {
                    this.x += (dx / dist) * 9.0;
                    this.y += (dy / dist) * 9.0;
                } else {
                    estado = 2;
                }
                break;

            case 2:
                double dxHome = xHome - this.x;
                double dyHome = yHome - this.y;
                double distHome = Math.hypot(dxHome, dyHome);

                if (distHome > 5) {
                    this.x += (dxHome / distHome) * 5.0;
                    this.y += (dyHome / distHome) * 5.0;
                } else {
                    this.x = xHome;
                    this.y = yHome;
                    estado = 0;
                }
                break;

            case 3:
                timerEstado += 1;

                double dxSubir = targetX - this.x;
                double dySubir = targetY - this.y;
                double distSubir = Math.hypot(dxSubir, dySubir);

                if (distSubir > 15 && timerEstado < 50) {
                    this.x += (dxSubir / distSubir) * 10.0;
                    this.y += (dySubir / distSubir) * 10.0;
                } else {
                    estado = 4;
                    timerEstado = 0;
                    targetX = centroAlvoX;
                    targetY = centroAlvoY;
                }
                break;

            case 4:
                timerEstado += 1;

                double dxBater = targetX - this.x;
                double dyBater = targetY - this.y;
                double distBater = Math.hypot(dxBater, dyBater);

                if (distBater > 15 && timerEstado < 30) {
                    this.x += (dxBater / distBater) * 16.0;
                    this.y += (dyBater / distBater) * 16.0;
                } else {
                    estado = 2;
                    timerEstado = 0;
                }
                break;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(this.cor);
        g.fillRect((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public Collider getHurtbox() {
        return this.bodyCollider;
    }
}
