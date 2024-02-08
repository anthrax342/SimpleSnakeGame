package SimpleSnakeGame;

import javax.swing.*;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener {
    private enum GameState {
        RUNNING, GAME_OVER
    }

    private final int WIDTH = 300;
    private final int HEIGHT = 300;
    private final int SCALE = 10;
    private final int ALL_DOTS = 900;
    private final int RAND_POS = 29;
    private final int DELAY = 70;
    private final int x[] = new int[ALL_DOTS];
    private final int y[] = new int[ALL_DOTS];
    private int dots;
    private int score;
    private int apple_x;
    private int apple_y;
    private int lastKey;
    private int bestScore = 0;
    private long lastFpsTime = System.currentTimeMillis();
    private int frameCount = 0;
    private int fps = 0;
    private int blueApple_x;
    private int blueApple_y;
    private boolean isNewHighScore = false;
    private long animationStartTime;
    private final long ANIMATION_DURATION = 3000;
    private boolean blueAppleVisible = false;
    private boolean blueAppleTimerCollision = false;
    private long blueAppleLastTime = 0;
    private long blueAppleTimeLeft = 0;
    private final int BLUE_APPLE_SCORE = 2;
    private boolean showBlueAppleHint = true;
    private boolean isNewHighScoreThisGame = false;
    private long hintStartTime;
    private final long HINT_DURATION = 3000;
    private boolean moving = false;
    private boolean firstGame = true;
    private final float realX[] = new float[ALL_DOTS];
    private final float realY[] = new float[ALL_DOTS];
    private Thread renderThread;
    private volatile boolean running = true;
    private GameState gameState = GameState.RUNNING;
    private Timer timer;
    private JButton restartButton;
    private boolean fpsCollision = false;

    public SnakeGame() {
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setDoubleBuffered(true);
        initRendering();
        initGame();
    }
    
    private void initRendering() {
        renderThread = new Thread(() -> {
            while (running) {
                repaint();
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        renderThread.start();
    }

    public void initGame() {
        dots = 3;
        score = 0;
        for (int z = 0; z < dots; z++) {
            x[z] = 50 - z * 10;
            y[z] = 50;
            realX[z] = x[z];
            realY[z] = y[z];
        }
        locateApple();
        timer = new Timer(DELAY, this);
        timer.start();
        hintStartTime = System.currentTimeMillis();

        restartButton = new JButton("Restart");
        restartButton.setBounds(WIDTH / 2 - 50, HEIGHT / 2 - 30, 100, 20);
        restartButton.addActionListener(e -> restartGame());
        this.add(restartButton);
        restartButton.setVisible(false);
    }
    
    private void playNewHighScoreAnimation(Graphics g) {
        if (isNewHighScore && System.currentTimeMillis() - animationStartTime <= ANIMATION_DURATION) {
            float alpha = (float) (Math.sin((System.currentTimeMillis() - animationStartTime) / 200.0) * 0.5 + 0.5);
            g.setColor(new Color(1.0f, 0.0f, 0.0f, alpha));
            g.setFont(new Font("Helvetica", Font.BOLD, 30));
            String text = "New Highscore!";
            FontMetrics metrics = g.getFontMetrics();
            int x = (WIDTH - metrics.stringWidth(text)) / 2;
            int y = HEIGHT / 3;
            g.drawString(text, x, y);
        } else {
            isNewHighScore = false;
        }
    }

    private void restartGame() {
        gameState = GameState.RUNNING;
        firstGame = false;
        isNewHighScoreThisGame = false;
        dots = 3;
        for (int z = 0; z < dots; z++) {
            x[z] = 50 - z * 10;
            y[z] = 50;
        }
        score = 0;
        locateApple();
        timer.start();
        restartButton.setVisible(false);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime > 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;
        }
        frameCount++;
        if (isNewHighScore) {
            playNewHighScoreAnimation(g);
        }

        drawHint(g);
        drawGame(g);

        g.setColor(fpsCollision ? Color.red : Color.white);
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        g.drawString("FPS: " + fps, 5, 15);
    }
    
    private void drawHint(Graphics g) {
        if (showBlueAppleHint) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - hintStartTime > HINT_DURATION) {
                showBlueAppleHint = false;
                return;
            }

            float alpha = 1.0f - (float) (currentTime - hintStartTime) / HINT_DURATION;
            alpha = Math.max(alpha, 0);

            g.setColor(new Color(1.0f, 1.0f, 1.0f, alpha));
            g.setFont(new Font("Helvetica", Font.BOLD, 18));
            String text = "Blue apples count double!";
            FontMetrics metrics = g.getFontMetrics();
            int x = (WIDTH - metrics.stringWidth(text)) / 2;
            int y = HEIGHT / 2;

            g.drawString(text, x, y);
        }
    }
    private void setNewHighScore() {
    	if (!isNewHighScore) {
            isNewHighScore = true;
            animationStartTime = System.currentTimeMillis();
        }
    }

    private void drawGame(Graphics g) {
        if (gameState == GameState.RUNNING) {
            g.setColor(Color.red);
            g.fillOval(apple_x, apple_y, SCALE, SCALE);

            for (int z = 0; z < dots; z++) {
                if (z == 0) {
                    g.setColor(Color.green);
                    g.fillRect(x[z], y[z], SCALE, SCALE);
                } else {
                    g.fillRect(x[z], y[z], SCALE, SCALE);
                }
            }

            if (blueAppleVisible) {
                g.setColor(Color.blue);
                g.fillOval(blueApple_x, blueApple_y, SCALE, SCALE);
                drawBlueAppleTimer(g);
            }

            g.setColor(Color.white);
            g.setFont(new Font("Helvetica", Font.BOLD, 14));
            g.drawString("Score: " + score, 5, HEIGHT - 5);

            if (bestScore > 0) {
                g.drawString("Best Score: " + bestScore, WIDTH - 100, HEIGHT - 5);
            }

            drawFPS(g);
            drawScoreCollision(g);

            Toolkit.getDefaultToolkit().sync();
        } else {
            gameOver(g);
        }
    }

    
    private void drawFPS(Graphics g) {
        String fpsText = "FPS: " + fps;
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(fpsText);
        int textHeight = metrics.getHeight();
        int fpsX = 5;
        int fpsY = 15;

        Rectangle fpsBounds = new Rectangle(fpsX, fpsY - textHeight, textWidth, textHeight);
        fpsCollision = false;

        for (int i = 0; i < dots; i++) {
            Rectangle snakeBounds = new Rectangle(x[i], y[i], SCALE, SCALE);

            if (fpsBounds.intersects(snakeBounds)) {
                fpsCollision = true;
                break;
            }
        }

        g.setColor(fpsCollision ? Color.red : Color.white);
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        g.drawString(fpsText, fpsX, fpsY);
    }


    private void drawScoreCollision(Graphics g) {
        FontMetrics metrics = g.getFontMetrics();
        Rectangle scoreTextBounds = new Rectangle(5, HEIGHT - 14, metrics.stringWidth("Score: " + score), 14);
        Rectangle bestScoreTextBounds = new Rectangle(WIDTH - 100, HEIGHT - 14, metrics.stringWidth("Best Score: " + bestScore), 14);

        for (int i = 0; i < dots; i++) {
            Rectangle snakeBounds = new Rectangle(x[i], y[i], SCALE, SCALE);

            if (snakeBounds.intersects(scoreTextBounds)) {
                g.setColor(Color.red);
                g.drawString("Score: " + score, 5, HEIGHT - 5);
                g.setColor(Color.white);
            }

            if ((snakeBounds.intersects(bestScoreTextBounds) && !firstGame) || (snakeBounds.intersects(bestScoreTextBounds) && bestScore > 0)) {
                g.setColor(Color.red);
                g.drawString("Best Score: " + bestScore, WIDTH - 100, HEIGHT - 5);
                g.setColor(Color.white);
            }
        }
    }
    
    private void drawBlueAppleTimer(Graphics g) {
        int timeLeftInSeconds = (int) Math.ceil(blueAppleTimeLeft / 1000.0);
        String timeText = timeLeftInSeconds + "s";

        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(timeText);
        int textHeight = metrics.getHeight();

        int textX = Math.min(blueApple_x, WIDTH - textWidth - 5);
        int textY = Math.max(blueApple_y, textHeight + 5);

        Rectangle timerBounds = new Rectangle(textX, textY - textHeight, textWidth, textHeight);
        blueAppleTimerCollision = false;

        for (int i = 0; i < dots; i++) {
            Rectangle snakeBounds = new Rectangle(x[i], y[i], SCALE, SCALE);

            if (timerBounds.intersects(snakeBounds)) {
                blueAppleTimerCollision = true;
                break;
            }
        }

        g.setColor(blueAppleTimerCollision ? Color.red : Color.white);
        g.setFont(new Font("Helvetica", Font.BOLD, 12));
        g.drawString(timeText, textX, textY);
    }


    private void gameOver(Graphics g) {
        if (gameState == GameState.GAME_OVER) {
            String msg = "Game Over";
            String scoreMsg = "Score: " + score;
            String bestScoreMsg = "Best Score: " + bestScore;

            if (score > bestScore) {
                bestScore = score;
            }

            Font font = new Font("Helvetica", Font.BOLD, 24);
            FontMetrics metrics = getFontMetrics(font);
            g.setColor(Color.white);
            g.setFont(font);
            g.drawString(msg, (WIDTH - metrics.stringWidth(msg)) / 2, HEIGHT / 2 - 20);
            g.drawString(scoreMsg, (WIDTH - metrics.stringWidth(scoreMsg)) / 2, HEIGHT / 2 + 20);
            g.drawString(bestScoreMsg, (WIDTH - metrics.stringWidth(bestScoreMsg)) / 2, HEIGHT / 2 + 60);

            restartButton.setVisible(true);
        }
    }

    private void checkApple() {
        if ((x[0] == apple_x) && (y[0] == apple_y)) {
            dots++;
            score++;
            locateApple();
        } else if (blueAppleVisible && (x[0] == blueApple_x) && (y[0] == blueApple_y)) {
            dots += 2;
            score += BLUE_APPLE_SCORE;
            blueAppleVisible = false;
            blueAppleLastTime = System.currentTimeMillis();
        }

        if (score > bestScore) {
            bestScore = score;
            if (!isNewHighScoreThisGame && !firstGame) {
                isNewHighScoreThisGame = true;
                setNewHighScore();
            }
        }
    }
    
    private float lerp(float start, float end, float t) {
        return start + t * (end - start);
    }
    
    private void move() {
        if (lastKey == KeyEvent.VK_LEFT) {
            x[0] -= SCALE;
        }
        if (lastKey == KeyEvent.VK_RIGHT) {
            x[0] += SCALE;
        }
        if (lastKey == KeyEvent.VK_UP) {
            y[0] -= SCALE;
        }
        if (lastKey == KeyEvent.VK_DOWN) {
            y[0] += SCALE;
        }

        float lerpAmount = 0.1f;
        realX[0] = lerp(realX[0], x[0], lerpAmount);
        realY[0] = lerp(realY[0], y[0], lerpAmount);

        for (int z = dots; z > 0; z--) {
            x[z] = x[z - 1];
            y[z] = y[z - 1];
            realX[z] = lerp(realX[z], x[z], lerpAmount);
            realY[z] = lerp(realY[z], y[z], lerpAmount);
        }
    }

    private void checkCollision() {
        if (moving) {
            for (int z = dots; z > 0; z--) {
                if ((z > 4) && (x[0] == x[z]) && (y[0] == y[z])) {
                    gameState = GameState.GAME_OVER;
                    return;
                }
            }
        }

        if (y[0] >= HEIGHT) {
            y[0] = 0;
            realY[0] = 0;
        }

        if (y[0] < 0) {
            y[0] = HEIGHT - SCALE;
            realY[0] = HEIGHT - SCALE;
        }

        if (x[0] >= WIDTH) {
            x[0] = 0;
            realX[0] = 0;
        }

        if (x[0] < 0) {
            x[0] = WIDTH - SCALE;
            realX[0] = WIDTH - SCALE;
        }

        if (gameState == GameState.GAME_OVER) {
            timer.stop();
        }
    }
    
    private boolean isAppleCollidingWithSnake(int appleX, int appleY) {
        for (int i = 0; i < dots; i++) {
            if (appleX == x[i] && appleY == y[i]) {
                return true;
            }
        }
        return false;
    }

    private boolean isAppleCollidingWithText(int appleX, int appleY) {
        FontMetrics metrics = getFontMetrics(getFont());
        Rectangle scoreBounds = new Rectangle(5, HEIGHT - 14, metrics.stringWidth("Score: " + score), 14);
        Rectangle fpsBounds = new Rectangle(5, 15 - metrics.getHeight(), metrics.stringWidth("FPS: " + fps), metrics.getHeight());

        Rectangle appleBounds = new Rectangle(appleX, appleY, SCALE, SCALE);
        return appleBounds.intersects(scoreBounds) || appleBounds.intersects(fpsBounds);
    }

    private void locateApple() {
        do {
            apple_x = (int) (Math.random() * RAND_POS) * SCALE;
            apple_y = (int) (Math.random() * RAND_POS) * SCALE;
        } while (isAppleCollidingWithSnake(apple_x, apple_y) || isAppleCollidingWithText(apple_x, apple_y));
    }

    private void locateBlueApple() {
        do {
            blueApple_x = (int) (Math.random() * RAND_POS) * SCALE;
            blueApple_y = (int) (Math.random() * RAND_POS) * SCALE;
        } while (isAppleCollidingWithSnake(blueApple_x, blueApple_y) || isAppleCollidingWithText(blueApple_x, blueApple_y));
    }
    
    private void updateBlueApple() {
        if (score >= 5) {
            if (!blueAppleVisible && System.currentTimeMillis() - blueAppleLastTime > randomTime(7000, 15000)) {
                locateBlueApple();
                blueAppleVisible = true;
                blueAppleLastTime = System.currentTimeMillis();
                blueAppleTimeLeft = randomTime(5000, 8000);
            } else if (blueAppleVisible) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - blueAppleLastTime > blueAppleTimeLeft) {
                    blueAppleVisible = false;
                    blueAppleLastTime = currentTime;
                } else {
                    blueAppleTimeLeft -= currentTime - blueAppleLastTime;
                    blueAppleLastTime = currentTime;
                }
            }
        }
    }

    private long randomTime(int minMillis, int maxMillis) {
        return (long) (Math.random() * (maxMillis - minMillis)) + minMillis;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.RUNNING && !moving) {
            moving = true;
            checkApple();
            updateBlueApple();
            checkCollision();
            move();
            moving = false;
        }
    }

    public void stopRendering() {
        running = false;
        try {
            renderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (gameState == GameState.RUNNING) {
                if (key == KeyEvent.VK_LEFT && lastKey != KeyEvent.VK_RIGHT) {
                    lastKey = KeyEvent.VK_LEFT;
                }
                if (key == KeyEvent.VK_RIGHT && lastKey != KeyEvent.VK_LEFT) {
                    lastKey = KeyEvent.VK_RIGHT;
                }
                if (key == KeyEvent.VK_UP && lastKey != KeyEvent.VK_DOWN) {
                    lastKey = KeyEvent.VK_UP;
                }
                if (key == KeyEvent.VK_DOWN && lastKey != KeyEvent.VK_UP) {
                    lastKey = KeyEvent.VK_DOWN;
                }
            }
        }
    }

    public static void main(String[] args) {
    	long startTime = System.currentTimeMillis();
    	SimpleDateFormat sdf = new SimpleDateFormat("ss.SSS");
        String formattedStartTime = sdf.format(new Date(startTime));
        System.out.println("Laoding ...");
        JFrame loadingFrame = new JFrame("Loading");
        JPanel loadingPanel = new JPanel();
        JLabel loadingLabel = new JLabel("Loading...");
        loadingLabel.setFont(new Font("Helvetica", Font.BOLD, 18));
        loadingPanel.add(loadingLabel);
        loadingFrame.add(loadingPanel);
        loadingFrame.setSize(300, 100);
        loadingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);

        loadingFrame.dispose();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SimpleSnakeGame v0.7");
            SnakeGame game = new SnakeGame();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            long endTime = System.currentTimeMillis();
            String formattedEndTime = sdf.format(new Date(endTime));
            System.out.println("Start " + formattedStartTime);
            System.out.println("End " + formattedEndTime);
            long elapsedTime = endTime - startTime;
            System.out.println("Loaded in: " + elapsedTime + " ms");
        });
    }
}
