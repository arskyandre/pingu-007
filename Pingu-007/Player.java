import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

public class Player {

    private double x, y;
    final private double largura, altura;

    private Direction direction = Direction.DOWN;

    // Física e Movimentação
    private double velX = 0;
    private double velY = 0;
    private final double aceleracao = 1.0;
    private final double atritoNormal = 0.85;
    private final double velocidadeMax = 30;
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

    public Player(double x, double y, double largura, double altura, BulletManager bulmgr) {
        this.x = x;
        this.y = y;
        this.bulletmanager = bulmgr;
        this.largura = largura;
        this.altura = altura;
    }

    //TEMPORARIO(TESTE): Adiciona loot de municao no chao com o clique direito, remover essa funcao e deletar a chamada no GameCore dps
    private int muniCOoldownTimer = 0;
    private final int muniCooldown = 60;
    public void testemunicao(InputManager input, int telaLargura, int telaAltura, LootManager lootmanager){
        if (muniCOoldownTimer > 0) {
            muniCOoldownTimer--;
        }
        if (input.isMouseButtonPressed(MouseEvent.BUTTON3)) {
            if (input.isMouseButtonPressed(MouseEvent.BUTTON3) && muniCOoldownTimer == 0 ) {
                double mouseX = input.getMouseX();
                double mouseY = input.getMouseY();
                //instancia e spawna a municao
                Ammo am = new Ammo(mouseX, mouseY, 32);
                lootmanager.spawn(am);
                muniCOoldownTimer = muniCooldown;
                System.out.println("Spawnou municao");
            }

        }
    }
    //FIM TESTE

    public void update(InputManager input, int telaLargura, int telaAltura) {

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
            }
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
        if (input.isMouseButtonPressed(MouseEvent.BUTTON1)) {
            if (input.isMouseButtonPressed(MouseEvent.BUTTON1) && shootCooldownTimer == 0 && pente > 0) {
                double mouseX = input.getMouseX();
                double mouseY = input.getMouseY();
                double centerX = x + largura / 2.0;
                double centerY = y + altura / 2.0;
                bulletmanager.shoot(centerX, centerY, mouseX - centerX, mouseY - centerY, BulletOwner.PLAYER);
                shootCooldownTimer = shootCooldown;
                pente--;
                System.out.println(
                        "Atirou, pente: " + String.valueOf(pente) + " municao total: " + String.valueOf(municao));
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
        bulletmanager.update(telaLargura, telaAltura);

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
        velX = Math.max(-velocidadeMax, Math.min(velX, velocidadeMax));
        velY = Math.max(-velocidadeMax, Math.min(velY, velocidadeMax));

        x += velX;
        y += velY;

        // Aplicação de Atrito
        double atritoAtual = emDash ? atritoDash : atritoNormal;
        velX *= atritoAtual;
        velY *= atritoAtual;

        if (Math.abs(velX) < 0.01) {
            velX = 0;
        }
        if (Math.abs(velY) < 0.01) {
            velY = 0;
        }

        // Colisões com as bordas da tela
        if (x < 0) {
            x = 0;
            velX = 0;
        }
        if (y < 0) {
            y = 0;
            velY = 0;
        }
        if (x + largura > telaLargura) {
            x = telaLargura - largura;
            velX = 0;
        }
        if (y + altura > telaAltura) {
            y = telaAltura - altura;
            velY = 0;
        }

        updatePlayerDirection(input.getMouseX(), input.getMouseY());
    }

    private void updatePlayerDirection(int mouseX, int mouseY) {
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

    public void addMunicao(int qtd){
        municao += qtd;
        System.out.println("coletou " + String.valueOf(qtd) + " total: " + String.valueOf(municao));
    }

    // Getters para a Renderização
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

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
}
