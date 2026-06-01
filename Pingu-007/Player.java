
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
//import static HelpMethods.CanMoveHere;

public class Player extends Entity {

    final private double largura, altura;

    private Direction direction = Direction.DOWN;

    // Física e Movimentação
    private double dirX = 0;
    private double dirY = 0;

    // Dash
    private boolean podeDash = true;
    private boolean emDash = false;
    private final int dashCooldown = 35;
    private int dashCooldownTimer = 0;
    private final int dashDuracao = 28;
    private int dashDuracaoTimer = 0;
    private final double dashForca = 22;
    private final double atritoDash = 0.90;
    private final double controleDash = 0.50;
    private double dashDirX = 0;
    private double dashDirY = 0;

    // Armas e balas
    private final BulletManager bulletmanager;
    private int armas = 1;
    private int pente = 15;
    private final int maxpente = 15;
    private int municao = 45;
    private int shootCooldownTimer = 0;
    private final int shootCooldown = 30;
    private int reloadCooldownTimer = 0;
    private final int reloadCooldown = 30;

    // iframes
    private int iFramesTimer = 0;
    private final int iFramesDanoDuration = 60;
    private final int iFramesDashGrace = 15;

    // TODO: digo isso por que acho que seria bom se parte do sprite
    // não colidisse com blocos especificamente acima dele
    // pra criar uma ilusão de 3d mais não tenho certeza
    // então por enquanto só hitbox
    private int[][] lvlData;

    public Player(double startX, double startY, double largura, double altura, BulletManager bulmgr) {
        this.x = startX;
        this.y = startY;
        this.aceleracao = 1.0;
        this.atritoPadrao = 0.85;
        this.velocidadeMax = 30;
        this.largura = largura;
        this.altura = altura;
        this.bulletmanager = bulmgr;
        this.bodyCollider = new Collider(0, altura / 2.0, largura, altura / 2.0);
        this.hurtbox = new Collider(0, 0, largura, altura);
        this.vidaMaxima = 100;
        this.vida = this.vidaMaxima;
    }

    //TEMPORARIO(TESTE): Adiciona loot de municao no chao com o clique direito, remover essa funcao e deletar a chamada no GameCore dps
    private int muniCOoldownTimer = 0;
    private final int muniCooldown = 60;

    public void testemunicao(InputManager input, int telaLargura, int telaAltura, LootManager lootmanager, CameraManager camera) {
        if (muniCOoldownTimer > 0) {
            muniCOoldownTimer--;
        }
        if (input.isMouseButtonPressed(MouseEvent.BUTTON3)) {
            if (muniCOoldownTimer == 0) {
                // Converte as coordenadas da tela para o mundo
                double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
                double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();

                // Instancia e spawna a municao na posição do mundo
                Ammo am = new Ammo(mouseXWorld, mouseYWorld, 32);
                lootmanager.spawn(am);
                muniCOoldownTimer = muniCooldown;
                System.out.println("Spawnou municao");
            }
        }
    }
    //FIM TESTE

    @Override
    public void receberDano(int dano) {
        if (iFramesTimer == 0 && !emDash) {
            super.receberDano(dano);
            iFramesTimer = iFramesDanoDuration;
            System.out.println("Player tomou dano! Vida: " + vida);
        }
    }

    public void update(InputManager input, int telaLargura, int telaAltura, CameraManager camera) {
        // Gerenciamento dos Timers de Dash
        if (!podeDash) {
            dashCooldownTimer--;
            if (dashCooldownTimer <= 0) {
                podeDash = true;
            }
        }

        if (emDash) {
            dashDuracaoTimer--;
            if (dashDuracaoTimer <= 0) {
                emDash = false;
                iFramesTimer = iFramesDashGrace;
            }
        }
        // Timer de iFrames
        if (iFramesTimer > 0) {
            iFramesTimer--;
        }

        double controleAtual = emDash ? controleDash : 1.0;

        // Leitura de Entradas
        boolean andaX = false, andaY = false;
        if (input.isKeyPressed(KeyEvent.VK_D)) {
            velX += aceleracao * controleAtual;
            dirX = 1;
            andaX = true;
        }
        if (input.isKeyPressed(KeyEvent.VK_A)) {
            velX -= aceleracao * controleAtual;
            dirX = -1;
            andaX = true;
        }
        if (!andaX || (input.isKeyPressed(KeyEvent.VK_D) && input.isKeyPressed(KeyEvent.VK_A))) {
            dirX = 0;
        }

        if (input.isKeyPressed(KeyEvent.VK_S)) {
            velY += aceleracao * controleAtual;
            dirY = 1;
            andaY = true;
        }
        if (input.isKeyPressed(KeyEvent.VK_W)) {
            velY -= aceleracao * controleAtual;
            dirY = -1;
            andaY = true;
        }
        if (!andaY || (input.isKeyPressed(KeyEvent.VK_W) && input.isKeyPressed(KeyEvent.VK_S))) {
            dirY = 0;
        }

        // Atirar e recarregar
        if (shootCooldownTimer > 0) {
            shootCooldownTimer--;
        }

        double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
        double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();

        if (input.isMouseButtonPressed(MouseEvent.BUTTON1)) {
            if (shootCooldownTimer == 0 && pente > 0) {
                double centerX = x + largura / 2.0;
                double centerY = y + altura / 2.0;

                bulletmanager.shoot(centerX, centerY, mouseXWorld - centerX, mouseYWorld - centerY, BulletOwner.PLAYER);
                shootCooldownTimer = shootCooldown;
                pente--;
                System.out.println("Atirou, pente: " + pente + " municao total: " + municao);
            }
        }

        if (input.isKeyPressed(KeyEvent.VK_R)) {
            if (municao == 0) {
                System.out.println("sem municao");
            } else {
                int diff = maxpente - pente;
                if (municao >= diff) {
                    pente = maxpente;
                    municao -= diff;
                } else {
                    pente += municao;
                    municao = 0;
                }
            }
        }

        // Lógica de Dash
        if (input.isKeyPressed(KeyEvent.VK_SPACE) && podeDash && !emDash) {

            if (dirX != 0 || dirY != 0) {
                double tamanho = Math.sqrt(dirX * dirX + dirY * dirY);
                dirX /= tamanho;
                dirY /= tamanho;

                velX += dirX * dashForca;
                velY += dirY * dashForca;

                dashDirX = dirX;
                dashDirY = dirY;

                podeDash = false;
                emDash = true;

                dashCooldownTimer = dashCooldown;
                dashDuracaoTimer = dashDuracao;
            }
        }

        // Limitação de Velocidade e Aplicação na Posição
        double atritoSalvo = this.atritoPadrao;
        // Se estiver no dash, muda o atrito temporariamente para deslizar mais
        if (emDash) {
            this.atritoPadrao = atritoDash;
        }
        aplicarFisicaBasica();
        // Retorna o atrito normal
        this.atritoPadrao = atritoSalvo;

        //COLISAO Com Tiles
        moveAndCollideWithMap(lvlData);

        // Colisões com as bordas do Mapa
        int mapaLargura = lvlData[0].length * GameCore.tiles_size;
        int mapaAltura = lvlData.length * GameCore.tiles_size;

        if (x < 0) {
            x = 0;
            velX = 0;
        }
        if (y < 0) {
            y = 0;
            velY = 0;
        }
        if (x + largura > mapaLargura) {
            x = mapaLargura - largura;
            velX = 0;
        }
        if (y + altura > mapaAltura) {
            y = mapaAltura - altura;
            velY = 0;
        }

        updatePlayerDirection(mouseXWorld, mouseYWorld);
    }

    private void updatePlayerDirection(double mouseX, double mouseY) {
        double centerX = x + largura / 2.0;
        double centerY = y + altura / 2.0;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        double angle = Math.toDegrees(Math.atan2(dy, dx));

        if (angle < 0) {
            angle += 360;
        }

        if (angle >= 45 && angle < 135) {
            direction = Direction.DOWN;
        } else if (angle >= 135 && angle < 225) {
            direction = Direction.LEFT;
        } else if (angle >= 225 && angle < 315) {
            direction = Direction.UP;
        } else {
            direction = Direction.RIGHT;
        }
    }

    public void addMunicao(int qtd) {
        municao += qtd;
        System.out.println("coletou " + String.valueOf(qtd) + " total: " + String.valueOf(municao));
    }

    //pegar info do mapa
    public void loadLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
    }

    // Getters para a Renderização
    public double getLargura() {
        return largura;
    }

    public double getAltura() {
        return altura;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isEmDash() {
        return emDash;
    }

    public double getDashDirX() {
        return dashDirX;
    }

    public double getDashDirY() {
        return dashDirY;
    }

    public Boolean isMovendo(){
        return (velX > 0.2 || velX < -0.2 ) || (velY > 0.2 || velY < -0.2);
    }

}
