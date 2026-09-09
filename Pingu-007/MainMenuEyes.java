import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class MainMenuEyes {
    private static final double largura_img = 1920.0;
    private static final double altura_img = 1080.0;
    private static final int tamX = 12;
    private static final int tamY = 11;
    private static final double vel=0.18;

    private final Eye left = new Eye(548, 209, 41, 33); //x,y(centro) / x,y(raio maximo)

    private final Eye right = new Eye(916, 216, 41, 33); //x,y(centro) / x,y(raio maximo)

    private BufferedImage pupil;

    public MainMenuEyes() {
        try {
            BufferedImage img = ImageIO.read(new File("images/hud/menu_pupil.png"));



            if (img == null || img.getWidth() < 960 || img.getHeight() < 486) {
                throw new IOException("pupila invalida");
            }



            pupil = new BufferedImage(tamX, tamY, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = pupil.createGraphics();


            try {
                g.drawImage(img, 0, 0, tamX, tamY, 948, 475, 960, 486, null);
            } finally {
                g.dispose();
            }
        } catch (IOException e) {
            System.err.println("Não foi possível carregar a pupila do menu: " + e.getMessage());
        }
    }

    public void update(int mx, int my, int largura, int altura) {

        if(pupil == null || largura <= 0 || altura <= 0) 
        return;



        double xx=mx * largura_img / largura;
        double yy = my * altura_img / altura;
        left.update(xx, yy);
        right.update(xx, yy);
    }

    public void render(Graphics2D g2, int largura, int altura) {
        if(pupil == null || largura <= 0 || altura <= 0) 
          return;


        Graphics2D g = (Graphics2D) g2.create();
        try {

            g.scale(largura / largura_img, altura / altura_img);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            left.draw(g, pupil);
            right.draw(g, pupil);
        } finally {
            g.dispose();
        }
    }

    private static final class Eye {
        private final double centerX, centerY, radiusX, radiusY;
        private double offsetX,offsetY;

        Eye(double centerX, double centerY, double radiusX, double radiusY) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
        }

        void update(double mx, double my) {

            double sobra = Math.hypot((tamX / 2.0 + 1) / radiusX, (tamY / 2.0 + 1) / radiusY);




            double ab=radiusX * (1-sobra);
            double bc = radiusY * (1-sobra);
            double vaiX = (mx - centerX) * 0.12;
            double vaiY = (my - centerY) * 0.12;
            double distt = Math.hypot(vaiX / ab, vaiY / bc);



            if(distt > 1){
                vaiX /= distt;
                vaiY /= distt;
            }



            offsetX += (vaiX - offsetX) * vel;
            offsetY += (vaiY - offsetY) * vel;
        }



        void draw(Graphics2D g,BufferedImage bolinha){
            int x = (int) Math.round(centerX + offsetX - tamX / 2.0);
            int y = (int) Math.round(centerY + offsetY - tamY / 2.0);
            g.drawImage(bolinha, x, y, null);
        }
    }
}
