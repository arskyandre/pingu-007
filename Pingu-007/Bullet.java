public class Bullet {

    private double x, y;
    //owner sera usado para definir se a bala foi atirada pelo player ou por inimigos, para checar a colisao futuramente
    private final BulletOwner owner;
    private double largura, altura;
    private double velX, velY;
    final private double speed = 5.0;
    private boolean active = true;

    Bullet(double x, double y, double dirX, double dirY, BulletOwner owner) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.largura = 8;
        this.altura = 8;
        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        this.velX = (dirX / len) * speed;
        this.velY = (dirY / len) * speed;
    }

    public void update(double telaLargura, double telaAltura) {
        x += velX;
        y += velY;
        // Desativa ao sair da tela
        if (x < 0 || x + largura > telaLargura || y < 0 || y + altura > telaAltura)
            active = false;
    }

    public boolean isActive() {
        return active;
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

}
