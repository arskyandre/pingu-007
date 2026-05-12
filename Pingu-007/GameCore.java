
import java.awt.*;
import java.awt.image.BufferStrategy;
import javax.swing.*;

public class GameCore extends Canvas implements Runnable {

    JFrame frame;
    boolean running = true;

    // Componentes do Jogo
    private final Player player;
    private final InputManager input;
    private final Renderer renderer;

    public GameCore() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);

        // Inicialização dos Módulos
        input = new InputManager();
        player = new Player(380, 560, 70, 70);
        renderer = new Renderer();

        addKeyListener(input);
        addMouseMotionListener(input);

        setFocusable(true);
        requestFocus();
    }

    public void update() {
        // Passa a responsabilidade de atualização para as entidades
        player.update(input, getWidth(), getHeight());
    }

    public void render(BufferStrategy bs) {
        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();

                // Repassa o Graphics2D para o renderizador
                renderer.renderizar(g2, player, input, getWidth(), getHeight());

                g2.dispose();
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void run() {
        createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerFrame;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            render(bs);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public static void main(String[] args) {
        GameCore game = new GameCore();
        game.frame = new JFrame("Pingu 007 (ALPHA)");
        game.frame.add(game);
        game.frame.pack();
        game.frame.setLocationRelativeTo(null);
        game.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.frame.setResizable(false);
        game.frame.setVisible(true);
        game.start();
    }
}
