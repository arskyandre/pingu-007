
import java.awt.*;
import java.awt.image.BufferStrategy;
import javax.swing.*;

public class GameCore extends Canvas implements Runnable {

    JFrame frame;
    boolean running = true;

    // Componentes do Jogo
    private final Player player;
    private BulletManager bulletmanager;
    private LootManager lootmanager;
    private final InputManager input;
    private final Renderer renderer;
    // Coisas do Level Creator
    private LevelManager levelManager;

    //

    public final static int tiles_default_size = 16;
    public final static float scale = 3f;
    public final static int tiles_in_width = 26;
    public final static int tiles_in_height = 14;
    public final static int tiles_size = (int) (tiles_default_size * scale);
    public final static int game_width = tiles_size * tiles_in_width;
    public final static int game_height = tiles_size * tiles_in_height;

    public GameCore() {
        bulletmanager = new BulletManager();
        lootmanager = new LootManager();
        setPreferredSize(new Dimension(game_width, game_height));
        setBackground(Color.BLACK);

        // Inicialização dos Módulos
        input = new InputManager();
        player = new Player(380, 560, 70, 70, bulletmanager);
        renderer = new Renderer();
        levelManager = new LevelManager(this);

        addKeyListener(input);
        addMouseMotionListener(input);
        addMouseListener(input);

        setFocusable(true);
        requestFocus();
    }

    public void update() {
        //REMOVER
        player.testemunicao(input, getWidth(), getHeight(), lootmanager);
        // Passa a responsabilidade de atualização para as entidades
        player.update(input, getWidth(), getHeight());

        lootmanager.update(player);
        levelManager.update();
    }

    public void render(BufferStrategy bs) {

        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
                // Repassa o Graphics2D para o renderizador
                renderer.renderizar(g2, player, input, getWidth(), getHeight(), levelManager, bulletmanager, lootmanager);

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
