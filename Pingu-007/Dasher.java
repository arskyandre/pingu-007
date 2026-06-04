
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Dasher extends Enemy {

    private enum Status {
        PERSEGUINDO, PREPARANDO, DASHING, COOLDOWN
    }
    private Status estadoAtual = Status.PERSEGUINDO;

    private int timer = 0;
    private double dashDirX = 0;
    private double dashDirY = 0;
    private int animTick = 0;
    private int animIndex = 0;
    private int dirS=0;
    

    // Configurações do Dasher
    private final double distAtivacao = 200.0; // distancia ele tenta atacar
    private final int tempoPreparo = 60;       // frames parado avisando o golpe
    private final int tempoDash = 25;          // frames de duração do dash
    private final int tempoCooldown = 120;     // frames antes de poder dar dash de novo
    private final double forcaDash = 22.0;     // Força / distancia do dash     
    private final double atritoDash = 0.94;

    public boolean interromperNoTiro = true;

    private BufferedImage[] Sprites;

    public Dasher(double startX, double startY, double width, double height, int[][] lvlData) {
        super(startX, startY, width, height, lvlData);
        this.vidaMaxima = 45;
        this.vida = this.vidaMaxima;

        this.velocidadeMax = 3.5;
        this.aceleracao = 0.8;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
        this.cor = Color.CYAN;

        BufferedImage img = LoadSave.GetSpriteAtlas("narval_sprite_sheet.png");
        Sprites = new BufferedImage[9];
        for(int j=0; j<2; j++){
            for(int i=0; i<4; i++){
                int index = j*4 + i;
                Sprites[index] = img.getSubimage(i * 16, j * 16, 16, 16);
            }
        }
        Sprites[8] = img.getSubimage(0, 32, 19, 16);
    }

    @Override
    public void update(Player player) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double pCenterX = player.getX() + player.getLargura() / 2.0;
        double pCenterY = player.getY() + player.getAltura() / 2.0;

        double dx = pCenterX - centerX;
        double dy = pCenterY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        // Controle da IA baseada em Estados
        switch (estadoAtual) {
            case PERSEGUINDO -> {
                if (dist < distAtivacao) {
                    estadoAtual = Status.PREPARANDO;
                    timer = tempoPreparo;
                    velX = 0;
                    velY = 0;
                } else if (dist > 0) {
                    // Anda até o jogador normalmente
                    velX += (dx / dist) * aceleracao;
                    velY += (dy / dist) * aceleracao;
                }
            }

            case PREPARANDO -> {
                timer--;
                // Fica parado carregando o ataque (Atrito zera o movimento)
                velX = 0;
                velY = 0;
                if (timer <= 0) {
                    estadoAtual = Status.DASHING;
                    timer = tempoDash;
                    if (dist > 0) {
                        dashDirX = dx / dist; // Trava a direção final pro dash
                        dashDirY = dy / dist;

                        velX = dashDirX * forcaDash;
                        velY = dashDirY * forcaDash;
                    }
                }
            }

            case DASHING -> {
                timer--;
                this.atritoPadrao = atritoDash;

                if (timer <= 0) {
                    estadoAtual = Status.COOLDOWN;
                    timer = tempoCooldown;
                }
            }

            case COOLDOWN -> {
                timer--;
                if (dist > 0) {
                    // Volta a perseguir de leve enquanto recarrega
                    velX += (dx / dist) * (aceleracao * 0.5);
                    velY += (dy / dist) * (aceleracao * 0.5);
                }
                if (timer <= 0) {
                    estadoAtual = Status.PERSEGUINDO;
                }
            }
        }

        double atritoSalvo = this.atritoPadrao;
        double velMaxSalva = this.velocidadeMax;

        // Destrava a velocidade e muda o atrito apenas durante o dash
        if (estadoAtual == Status.DASHING) {
            this.atritoPadrao = atritoDash;
            this.velocidadeMax = 45.0;
        }

        aplicarFisicaBasica();

        // Devolve os limites normais
        this.atritoPadrao = atritoSalvo;
        this.velocidadeMax = velMaxSalva;

        moveAndCollideWithMap(lvlData);

        // Causa dano no jogador
        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }
          
        if(velX > 0)
            dirS = 1;
        else if(velX < 0)
            dirS = 0;
    }

    @Override
    public void animate(Graphics2D g2){
        int xx = (int)x;
        int inv = 1;

        if(dirS == 0){
            inv = -1;
            xx =(int) (x + width);
        }
        

        if(estadoAtual == Status.DASHING){
            g2.drawImage(Sprites[8],xx,(int)y,inv * (int) (19 * width / 16),(int)height,null);
        }
        else{
            if(estadoAtual == Status.PREPARANDO){
                if(timer>54)
                    animIndex = 3;
                else if(timer>48)
                    animIndex = 4;
                else if(timer>42)
                    animIndex = 5;
                else if(timer>36)
                    animIndex = 6;
                else
                    animIndex = 7;
            }
            else{
                animTick++;
                if(animTick>=90){
                    animTick = 0;
                    animIndex++;
                    if(animIndex>=3)
                        animIndex = 0;
                }
            }
            g2.drawImage(Sprites[animIndex],xx,(int)y,inv * (int)width,(int)height,null);
        }
    }
    
    @Override
    public void draw(Graphics2D g2){
        //para cancelar o draw dele
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        // caso queira que ele seja imune a tiros durante o dash
        // if (estadoAtual == Estado.DASHING) return; 
        super.receberDano(dano, sourceX, sourceY, knockbackForce);
        // Stun se for atingindo no meio do dash, vale decidir se isso fica no jogo ou não
        if (interromperNoTiro && estadoAtual == Status.DASHING) {
            estadoAtual = Status.COOLDOWN;
            timer = tempoCooldown;
            velX = 0;
            velY = 0;
            System.out.println("Dasher atordoado no ar!");
        }
    }
}
