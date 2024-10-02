import java.awt.*;
import javax.swing.*;
import java.awt.event.*; // needed for event handling

public class Snake {
  
  static final int SCREEN_SIZE_X=40;         // In units of snake sections.
  static final int SCREEN_SIZE_Y=30;
  
  final int MAX_SNAKE_LENGTH = 1000;
  
  // While a snake is created with a very large number of snake sections,
  // determined by the constant variable MAX_SNAKE_LENGTH, the actual
  // apparent length of the snake in the game will be controlled by the
  // variable snakeLength. At each time step, the program will only draw
  // snakeLength snake sections for each snake. 
  //
  // Each time the snake eats some "food" (a yellow box on the screen), 
  // the snakeLength for that snake will
  // grow by the value of the food, which is printed in the box representing the
  // food. 
  
  int snakeLength = 5;                      // Start snakes with length 5.
  SnakeSection [] snakeSecs = new SnakeSection[MAX_SNAKE_LENGTH];
   
  // These variables represent the direction the snake is going.
  // Each time step, the snake moves in the direction represented by these
  // variables. The program does this by adding these values to the previous
  // head position of the snake. For example, the snake goes left initially, 
  // since by adding -1 to the x value (dirX = -1) and adding 0 to the y value
  // (dirY=0), the head of the snake moves one square to the left.

  int dirX=-1;
  int dirY=0;
  
  Color color;               // Holds the color of the snake.
  
  public Snake(SnakeSection startPos,int dx,int dy,Color color) {
    // Here, we are creating a large number of snake sections (1000 of them) so
    // that we don't have to worry about creating them later.
    for (int i=0; i<MAX_SNAKE_LENGTH; i++) 
      snakeSecs[i]=new SnakeSection(0,0);
    
    // Set the color of the snake based upon the formal parameter.    
    this.color=color;
    
    // Here, we create and INITIALIZE the snake sections that are going to be visible
    // at the beginning. We give these locations using a starting position, and offsets
    // from the start position.
    //
    // NOTE: Strictly speaking, it is unnecessary to CREATE the snake sections below
    //       using the new command, since these snake sections have already been created
    //       in the code just above, and all we really want to do is initialize the coordinates
    //       of the snake sections. However, in this case, it is easiest to use the
    //       constructor to initialize the snake sections to the values we want. In order
    //       to use the constructor, we must call new, and thus create the same snake
    //       sections again. It is a little bit of wasted effort, but it won't hurt anything.

    for (int i=0; i<snakeLength; i++) 
      snakeSecs[i]=new SnakeSection(startPos.x+i*dx,startPos.y+i*dy);
  }
  
  // This method returns true if EITHER the head or the body of a snake matches the given coordinates (x,y).
  
  public boolean contains(int x,int y) {  
    SnakeSection s=new SnakeSection(x,y);
    return s.match(snakeSecs[0]) || checkBodyPositions(s);
  }

  // This method returns true if any snake section in the body of a snake matches the given SnakeSection s.
  
  public boolean checkBodyPositions(SnakeSection s) {
    boolean collision=false;
    for (int i=1; i<snakeLength; i++) {
      if (s.match(snakeSecs[i]))
        collision=true;
    }
    return collision;
  }

  public void move() {
    for (int i=snakeLength-1; i>0; i--)
      snakeSecs[i]=snakeSecs[i-1];
    
    int newX=(snakeSecs[1].x + dirX + SCREEN_SIZE_X) % SCREEN_SIZE_X;
    int newY=(snakeSecs[1].y + dirY + SCREEN_SIZE_Y) % SCREEN_SIZE_Y;
    snakeSecs[0]=new SnakeSection(newX,newY);
  }
  
  // A snake is painted by drawing a square for each snake section. Each square is 20 by 20 pixels.
  
  public void paint(Graphics g) { 
    g.setColor(color);
    for (int i=0; i<snakeLength; i++) {
      g.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
      g.drawRect(snakeSecs[i].x*20,snakeSecs[i].y*20,20,20);
    }
  }
}
import java.util.*;
import javax.imageio.ImageIO;
import java.util.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

class Game extends JPanel {
    private Timer timer;
    private Snake snake;
    private Point cherry;
    private int points = 0;
    private int best = 0;
    private BufferedImage image;
    private GameStatus status;
    private boolean didLoadCherryImage = true;

    private static Font FONT_M = new Font("MV Boli", Font.PLAIN, 24);
    private static Font FONT_M_ITALIC = new Font("MV Boli", Font.ITALIC, 24);
    private static Font FONT_L = new Font("MV Boli", Font.PLAIN, 84);
    private static Font FONT_XL = new Font("MV Boli", Font.PLAIN, 150);
    private static int WIDTH = 760;
    private static int HEIGHT = 520;
    private static int DELAY = 50;

    // Constructor
    public Game() {
        try {
            image = ImageIO.read(new File("cherry.png"));
        } catch (IOException e) {
          didLoadCherryImage = false;
        }

        addKeyListener(new KeyListener());
        setFocusable(true);
        setBackground(new Color(130, 205, 71));
        setDoubleBuffered(true);

        snake = new Snake(WIDTH / 2, HEIGHT / 2);
        status = GameStatus.NOT_STARTED;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        render(g);

        Toolkit.getDefaultToolkit().sync();
    }

    // Render the game
    private void update() {
        snake.move();

        if (cherry != null && snake.getHead().intersects(cherry, 20)) {
            snake.addTail();
            cherry = null;
            points++;
        }

        if (cherry == null) {
            spawnCherry();
        }

        checkForGameOver();
    }
    
    private void reset() {
        points = 0;
        cherry = null;
        snake = new Snake(WIDTH / 2, HEIGHT / 2);
        setStatus(GameStatus.RUNNING);
    }
    
    private void setStatus(GameStatus newStatus) {
        switch(newStatus) {
            case RUNNING:
                timer = new Timer();
                timer.schedule(new GameLoop(), 0, DELAY);
                break;
            case PAUSED:
                timer.cancel();
            case GAME_OVER:
                timer.cancel();
                best = points > best ? points : best;
                break;
        }

        status = newStatus;
    }

    private void togglePause() { 
        setStatus(status == GameStatus.PAUSED ? GameStatus.RUNNING : GameStatus.PAUSED);
    }

    // Check if the snake has hit the wall or itself
    private void checkForGameOver() { 
        Point head = snake.getHead();
        boolean hitBoundary = head.getX() <= 20
            || head.getX() >= WIDTH + 10
            || head.getY() <= 40
            || head.getY() >= HEIGHT + 30;

        boolean ateItself = false;

        for(Point t : snake.getTail()) {
            ateItself = ateItself || head.equals(t);
        }

        if (hitBoundary || ateItself) {
            setStatus(GameStatus.GAME_OVER);
        }
    }

    // Spawn a cherry at a random location
    public void drawCenteredString(Graphics g, String text, Font font, int y) { 
        FontMetrics metrics = g.getFontMetrics(font);
        int x = (WIDTH - metrics.stringWidth(text)) / 2;

        g.setFont(font);
        g.drawString(text, x, y);
    }

    private void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.setFont(FONT_M);

        if (status == GameStatus.NOT_STARTED) {
          drawCenteredString(g2d, "SNAKE", FONT_XL, 200);
          drawCenteredString(g2d, "GAME", FONT_XL, 300);
          drawCenteredString(g2d, "Press  any  key  to  begin", FONT_M_ITALIC, 330);

          return;
        }

        Point p = snake.getHead();

        g2d.drawString("SCORE: " + String.format ("%02d", points), 20, 30);
        g2d.drawString("BEST: " + String.format ("%02d", best), 630, 30);

        if (cherry != null) {
          if (didLoadCherryImage) {
            g2d.drawImage(image, cherry.getX(), cherry.getY(), 60, 60, null);
          } else {
            g2d.setColor(Color.BLACK);
            g2d.fillOval(cherry.getX(), cherry.getY(), 10, 10);
            g2d.setColor(Color.BLACK);
          }
        }

        if (status == GameStatus.GAME_OVER) {
            drawCenteredString(g2d, "Press  enter  to  start  again", FONT_M_ITALIC, 330);
            drawCenteredString(g2d, "GAME OVER", FONT_L, 300);
        }

        if (status == GameStatus.PAUSED) {
            g2d.drawString("Paused", 600, 14);
        }

        g2d.setColor(new Color(33, 70, 199));
        g2d.fillRect(p.getX(), p.getY(), 10, 10);

        for(int i = 0, size = snake.getTail().size(); i < size; i++) {
            Point t = snake.getTail().get(i);

            g2d.fillRect(t.getX(), t.getY(), 10, 10);
        }

        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(20, 40, WIDTH, HEIGHT);
    }

    // spawn cherry in random position
    public void spawnCherry() {
        cherry = new Point((new Random()).nextInt(WIDTH - 60) + 20,
            (new Random()).nextInt(HEIGHT - 60) + 40);
    }

    // game loop
    private class KeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (status == GameStatus.RUNNING) {
                switch(key) {
                    case KeyEvent.VK_LEFT: snake.turn(Direction.LEFT); break;
                    case KeyEvent.VK_RIGHT: snake.turn(Direction.RIGHT); break;
                    case KeyEvent.VK_UP: snake.turn(Direction.UP); break;
                    case KeyEvent.VK_DOWN: snake.turn(Direction.DOWN); break;
                }
            }

            if (status == GameStatus.NOT_STARTED) {
                setStatus(GameStatus.RUNNING);
            }

            if (status == GameStatus.GAME_OVER && key == KeyEvent.VK_ENTER) {
                reset();
            }

            if (key == KeyEvent.VK_P) {
                togglePause();
            }
        }
    }

    private class GameLoop extends java.util.TimerTask {
        public void run() {
            update();
            repaint();
        }
    }
}


enum GameStatus 
{ 
    NOT_STARTED, RUNNING, PAUSED, GAME_OVER
}

// direction of snake
enum Direction { 
    UP, DOWN, LEFT, RIGHT;
    
    public boolean isX() {
        return this == LEFT || this == RIGHT;
    }
    
    public boolean isY() {
        return this == UP || this == DOWN;
    }
}


class Point {
    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point p) {
        this.x = p.getX();
        this.y = p.getY();
    }

    public void move(Direction d, int value) {
        switch(d) {
            case UP: this.y -= value; break;
            case DOWN: this.y += value; break;
            case RIGHT: this.x += value; break;
            case LEFT: this.x -= value; break;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Point setX(int x) {
        this.x = x;

        return this;
    }

    public Point setY(int y) {
        this.y = y;

        return this;
    }

    public boolean equals(Point p) {
        return this.x == p.getX() && this.y == p.getY();
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public boolean intersects(Point p) {
        return intersects(p, 10);
    }

    public boolean intersects(Point p, int tolerance) {
        int diffX = Math.abs(x - p.getX());
        int diffY = Math.abs(y - p.getY());

        return this.equals(p) || (diffX <= tolerance && diffY <= tolerance);
    }
}

class Snake {
    private Direction direction;
    private Point head;
    private ArrayList<Point> tail;
    
    public Snake(int x, int y) {
        this.head = new Point(x, y);
        this.direction = Direction.RIGHT;
        this.tail = new ArrayList<Point>();
        
        this.tail.add(new Point(0, 0));
        this.tail.add(new Point(0, 0));
        this.tail.add(new Point(0, 0));
    }

    public void move() {
        ArrayList<Point> newTail = new ArrayList<Point>();
        
        for (int i = 0, size = tail.size(); i < size; i++) {
            Point previous = i == 0 ? head : tail.get(i - 1);

            newTail.add(new Point(previous.getX(), previous.getY()));
        }
        
        this.tail = newTail;
        
        this.head.move(this.direction, 10);
    }
    
    public void addTail() {
        this.tail.add(new Point(-10, -10));
    }
    
    public void turn(Direction d) {       
        if (d.isX() && direction.isY() || d.isY() && direction.isX()) {
           direction = d; 
        }       
    }
    
    public ArrayList<Point> getTail() {
        return this.tail;
    }
    
    public Point getHead() {
        return this.head;
    }
}

public class Main extends JFrame {
    public Main() {
        initUI();
    }

    private void initUI() {
        add(new Game());

        setTitle("Snake");
        setSize(800, 610);

        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            Main ex = new Main();
            ex.setVisible(true);
        });
    }
}
