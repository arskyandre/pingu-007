import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class BasicEnemy extends Enemy {
    private int dirS=0;
    private int animTick = 0;
    private int animIndex = 0;
    private BufferedImage[] Sprites;

    public BasicEnemy(double startX, double startY, double width, double height, int[][] lvlData) {
        super(startX, startY, width, height, lvlData);
        this.vidaMaxima = 30;
        this.vida = this.vidaMaxima;

        // Customizando a física herdada
        this.velocidadeMax = 2.0;
        this.aceleracao = 0.5;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);

        BufferedImage img = LoadSave.GetSpriteAtlas("lobo_sprite_sheet.png");
        Sprites = new BufferedImage[6];
        for(int j=0; j<6; j++){
            Sprites[j] = img.getSubimage(j*16, 0, 16, 16);
        }
    }

    @Override
    public void update(Player player) {
        // anda direto pro player
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 0) {
            velX += (dx / dist) * aceleracao;
            velY += (dy / dist) * aceleracao;
        }

        aplicarFisicaBasica();
        moveAndCollideWithMap(lvlData);

        // Checa dano Melee
        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }

        //direção esquerda(0) ou direita(1)
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
        
        animTick++;
        if(animTick >= 12){
            animTick = 0;
            animIndex++;
            if(animIndex >= 6)
                animIndex = 0;
        }
        g2.drawImage(Sprites[animIndex],xx,(int)y,inv * (int)width,(int)height,null);
        
    }
    
    @Override
    public void draw(Graphics2D g2){
        //para cancelar o draw dele
    }

}
