
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
// TODO: adicionar ponteiro do mouse
// TODO: adicionar sistema de sprites influenciados pela camera/mouse

public class Moviment extends JPanel {

    final private Quadrado quadrado;

    public Moviment() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);

        quadrado = new Quadrado(380, 560, 70, 70);

        setFocusable(true);
        addKeyListener(new Teclado());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.WHITE);

        g2.fill(new Rectangle2D.Double(
                quadrado.getX(),
                quadrado.getY(),
                quadrado.getLargura(),
                quadrado.getAltura()
        ));
    }

    boolean[] teclas = new boolean[256];

    class Teclado extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            teclas[e.getKeyCode()] = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            teclas[e.getKeyCode()] = false;
        }
    }

    double velX = 0, velY = 0, aceleracao = 1.0, atritoNormal = 0.85, velocidadeMax = 30;

    boolean podeDash = true, emDash = false;
    int dashCooldown = 35, dashCooldownTimer = 0, dashDuracao = 28, dashDuracaoTimer = 0;
    double dashForca = 22, atritoDash = 0.90, controleDash = 0.50;

    public void update() {

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

        if (teclas[KeyEvent.VK_D]) {
            velX += aceleracao * controleAtual;
        }
        if (teclas[KeyEvent.VK_A]) {
            velX -= aceleracao * controleAtual;
        }
        if (teclas[KeyEvent.VK_W]) {
            velY -= aceleracao * controleAtual;
        }
        if (teclas[KeyEvent.VK_S]) {
            velY += aceleracao * controleAtual;
        }

        if (teclas[KeyEvent.VK_SPACE] && podeDash && !emDash) {
            double dirX = 0;
            double dirY = 0;

            if (teclas[KeyEvent.VK_D]) {
                dirX++;
            }
            if (teclas[KeyEvent.VK_A]) {
                dirX--;
            }
            if (teclas[KeyEvent.VK_S]) {
                dirY++;
            }
            if (teclas[KeyEvent.VK_W]) {
                dirY--;
            }

            if (dirX != 0 || dirY != 0) {
                double tamanho = Math.sqrt(dirX * dirX + dirY * dirY);

                dirX /= tamanho;
                dirY /= tamanho;

                velX += dirX * dashForca;
                velY += dirY * dashForca;

                podeDash = false;
                emDash = true;

                dashDuracaoTimer = dashDuracao;
                dashCooldownTimer = dashCooldown;
            }
        }

        velX = Math.max(-velocidadeMax, Math.min(velX, velocidadeMax));

        velY = Math.max(-velocidadeMax, Math.min(velY, velocidadeMax));

        double x = quadrado.getX();
        double y = quadrado.getY();

        x += velX;
        y += velY;

        boolean movendo = teclas[KeyEvent.VK_W] || teclas[KeyEvent.VK_A] || teclas[KeyEvent.VK_S] || teclas[KeyEvent.VK_D];
        double atritoAtual = emDash ? (movendo ? atritoDash : 0.95) : atritoNormal;

        velX *= atritoAtual;
        velY *= atritoAtual;

        if (Math.abs(velX) < 0.01) {
            velX = 0;
        }

        if (Math.abs(velY) < 0.01) {
            velY = 0;
        }

        if (x + quadrado.getLargura() > getWidth()) {
            x = getWidth() - quadrado.getLargura();
            velX = 0;
        }

        if (x < 0) {
            x = 0;
            velX = 0;
        }

        if (y + quadrado.getAltura() > getHeight()) {
            y = getHeight() - quadrado.getAltura();
            velY = 0;
        }

        if (y < 0) {
            y = 0;
            velY = 0;
        }

        quadrado.setPosicao(x, y);
    }

    public void game_loop() {

        Timer timer = new Timer(16, e -> {
            update();
            repaint();
        });

        timer.start();
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Jogo do Quadrado");

        Moviment jogo = new Moviment();

        frame.add(jogo);
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        jogo.game_loop();
    }
}
