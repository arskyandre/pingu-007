import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;


public class MorsaBoss extends Enemy{

    private BossMao maoEsquerda;
    private BossMao maoDireita;

    private int estadoAtual = 0;
    private final int IDLE = 0;
    private final int CHUVA_DE_GELO = 1;

    private double timerEstado = 0;
    private double timerSpawnGelo = 0;
    private double timerAtaque = 0;


    

    private BulletManager bulletManager;

    private ArrayList<AvisoGelo> avisosGelo = new ArrayList<>();

    public MorsaBoss(double startX, double startY, int[][] lvlData, BulletManager bulmgr, SoundManager sound){

        super(startX, startY, GameCore.tiles_size * 6, GameCore.tiles_size * 6, lvlData, sound);
        this.bulletManager = bulmgr;
        this.vida = 500;
        this.cor = Color.BLUE;
        this.aggroPermanente = true;
        this.bodyCollider = new Collider(0, 0, GameCore.tiles_size * 6, GameCore.tiles_size * 6);
      }

    public void vincularMaos(BossMao esquerda, BossMao direita){
      this.maoEsquerda = esquerda;
      this.maoDireita = direita;
    }

      @Override
      public void update(Player player, ArrayList<JumpLink> jumpLinks){
          

            velX = 0;
            velY = 0;


            timerAtaque += 1.0;
            timerEstado += 1.0;

            for(int i = avisosGelo.size() - 1; i >= 0; i--){
              AvisoGelo aviso = avisosGelo.get(i);
              aviso.update();

              if(aviso.isProntoParaCair()){

                if(bulletManager != null){
                  double tetoAbsolutoY = 32;

                  
                  bulletManager.shoot(aviso.targetX, tetoAbsolutoY, 0 , 1.8, BulletOwner.ENEMY);
                }
                avisosGelo.remove(i);
              }
            }
      switch (estadoAtual){
        case IDLE -> {
            if(timerEstado >= 180){
              timerEstado = 0;
              double rand = Math.random();

              if(rand < 0.4){

                estadoAtual = CHUVA_DE_GELO;
                timerSpawnGelo = 0;
              }
              else{
                comandarAtaqueMao();
              }
            }
        }

      case CHUVA_DE_GELO -> {
        this.cor = new Color(0, 150, 255);

        timerSpawnGelo += 1.0;

        if(timerSpawnGelo >= 15){
          timerSpawnGelo = 0;

          double variacaoX = (Math.random() * 1400) - 700;
          double alvoX = this.x + (this.width / 2) + variacaoX;
        

          int tileX = (int) (alvoX/ GameCore.tiles_size);
          double alvoY = this.y + this.height - 5;

          if(lvlData != null && lvlData.length > 0){
            if(tileX < 0)tileX = 0;
            if(tileX >= lvlData[0].length) tileX = lvlData[0].length - 1;
            for(int row = lvlData.length - 1;row >= 0; row--){
              if(TileProperties.isSolid(lvlData[row][tileX])){
                alvoY = row*GameCore.tiles_size;
                break;
              }
            }
          }

          avisosGelo.add(new AvisoGelo(alvoX, alvoY, 55));

        }
        if(timerEstado >= 240){
          this.cor = Color.BLUE;
          estadoAtual = IDLE;
          timerEstado = 0;
        }
      }
    }
  }

          private void comandarAtaqueMao(){
              double rand = Math.random();
              if(rand < 0.5 && maoEsquerda != null && !maoEsquerda.isDead()){
                  maoEsquerda.iniciarAtaque();
                }
                else if(maoDireita != null && !maoDireita.isDead()){
                    maoDireita.iniciarAtaque();
                  }
            }
      @Override
      public void draw(Graphics2D g){
          g.setColor(this.cor);
          g.fillRect((int)x, (int)y, (int)width, (int)height);

          g.setColor(Color.WHITE);
          g.drawString("MORSA: " + this.vida, (int)x, (int)y - 10);

          if(estadoAtual == CHUVA_DE_GELO){
            g.setColor(Color.CYAN);
            g.drawString("* ROAAAARRRR *", (int) x + 30, (int) y + ((int) height / 2));
          }
          for(AvisoGelo aviso: avisosGelo){
            aviso.draw(g);
          }
        }
      @Override
      public Collider getHurtbox(){
        return this.bodyCollider;
      }
  }

  class AvisoGelo {
    public double targetX, targetY;
    private int timer = 0;
    private int tempoAvisoMax;
    private double tamanhoSombra = 10;

    public AvisoGelo(double x, double y, int tempoAvisoMax){
      this.targetX = x;
      this.targetY = y;
      this.tempoAvisoMax = tempoAvisoMax;
    }
    public void update(){
      timer++;

      if(timer <= tempoAvisoMax){
        double progresso = (double) timer / tempoAvisoMax;
        tamanhoSombra = 10 + (progresso * 22);
      }
    }
  

      public boolean isProntoParaCair(){
        return timer >= tempoAvisoMax;
      }

  public void draw(Graphics2D g){
    g.setColor(new Color(255, 0, 0, 120));
    int drawX = (int) (targetX - (tamanhoSombra / 2));
    int drawY = (int) (targetY - 5);

    g.fill(new Ellipse2D.Double(drawX, drawY, tamanhoSombra, 8));

    g.setColor(new Color(255, 0, 0, 40));
    g.drawLine((int)targetX, (int)targetY, (int)targetX, (int)targetY - 800);
  }
}

  class BossMao extends Enemy{

      private MorsaBoss corpoPrincipal;
      private double xHome, yHome;
      private int estado = 0;
      private double timerEstado = 0;
      private double targetX, targetY;

      public BossMao(double startX, double startY, int[][] lvlData, SoundManager sound, MorsaBoss corpo){
          
          super(startX, startY, GameCore.tiles_size * 1.5, GameCore.tiles_size * 1.5, lvlData, sound);
          this.bodyCollider = new Collider(-1, 0, GameCore.tiles_size * 1.5, GameCore.tiles_size * 1.5);
          this.xHome = startX;
          this.yHome = startY;
          this.cor = Color.RED;
          this.corpoPrincipal = corpo;
          this.vida = 150;
        
        }

      public void iniciarAtaque(){
          if(estado == 0){
              estado = 1;
              timerEstado = 0;
            }
        }

        @Override
        public void update(Player player, ArrayList<JumpLink> jumpLinks){

            if(corpoPrincipal == null || corpoPrincipal.isDead()){
                this.vida = 0;
                return;
              }

              switch(estado){
                  case 0:
                  timerEstado += 0.05;
                  this.y = yHome + Math.sin(timerEstado) * 6;
                  this.x = xHome;
                  break;

                  case 1:
                  if(timerEstado == 0){

                      targetX = player.getX();
                      targetY= player.getY();

                    }

                    timerEstado += 1;

                    double dx = targetX - this.x;
                    double dy = targetY - this.y;
                    double dist = Math.hypot(dx, dy);

                    if(dist > 15 && timerEstado < 45){
                        this.x += (dx/dist) * 9.0;
                        this.y += (dy/dist) * 9.0;
                      }
                      else{
                          estado = 2;
                          
                        }
                    
                      break;


                      case 2:

                      double dxHome = xHome - this.x;
                      double dyHome = yHome - this.y;
                      double distHome = Math.hypot(dxHome, dyHome);

                      if(distHome > 5){
                          this.x += (dxHome / distHome) * 5.0;
                          this.y += (dyHome/ distHome) * 5.0;
                        }
                        else{
                            this.x = xHome;
                            this.y = yHome;
                            estado = 0;
                          }
                          break;
                }
          }
        @Override
        public void draw(Graphics2D g){
          g.setColor(this.cor);
          g.fillRect((int)x, (int)y, (int)width, (int)height);
        }
      @Override
      public Collider getHurtbox(){
        return this.bodyCollider;
      }
    }
