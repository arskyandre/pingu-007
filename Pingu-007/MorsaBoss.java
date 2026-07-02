import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


public class MorsaBoss extends Enemy{

    private BossMao maoEsquerda;
    private BossMao maoDireita;
    private double timerAtaque = 0;

    public MorsaBoss(double startX, double startY, int[][] lvlData, SoundManager sound){

        super(startX, startY, GameCore.tiles_size * 3, GameCore.tiles_size * 3, lvlData, sound);

        this.vida = 500;
        this.cor = Color.BLUE;
        this.aggroPermanente = true;
        this.bodyCollider = new Collider(0, 0, GameCore.tiles_size * 3, GameCore.tiles_size * 3);
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
            if(timerAtaque >= 150){
                comandarAtaque();
                timerAtaque = 0;
              }
        }

          private void comandarAtaque(){
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
        }
      @Override
      public Collider getHurtbox(){
        return this.bodyCollider;
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
