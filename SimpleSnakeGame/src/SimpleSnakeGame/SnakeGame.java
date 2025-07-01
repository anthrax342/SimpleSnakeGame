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
	private java.util.List<Particle> particles = new java.util.ArrayList<>();
    private enum GameState {
        RUNNING, GAME_OVER, PAUSED
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
    private boolean gameWon = false;
    private Timer timer;
    private JButton restartButton;
    private boolean fpsCollision = false;
    private Color[] segmentColors = new Color[ALL_DOTS];
    private boolean isVictoryAnimation = false;
    private long victoryAnimationStart = 0;
    private final long VICTORY_ANIMATION_DURATION = 3000;
    private JButton pauseExitButton;
    private JButton pauseRestartButton;
    private JButton resumeButton;

    public SnakeGame() {
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setFocusable(true);
        requestFocusInWindow();
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
            segmentColors[z] = Color.green;
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
        
        pauseExitButton = new JButton("Exit");
        pauseExitButton.setBounds(WIDTH / 2 - 50, HEIGHT / 2, 100, 20);
        pauseExitButton.addActionListener(e -> System.exit(0));
        this.add(pauseExitButton);
        pauseExitButton.setVisible(false);
        
        pauseRestartButton = new JButton("Restart");
        pauseRestartButton.setBounds(WIDTH / 2 - 50, HEIGHT / 2 - 30, 100, 20);
        pauseRestartButton.addActionListener(e -> restartGame());
        this.add(pauseRestartButton);
        pauseRestartButton.setVisible(false);
        
        resumeButton = new JButton("Resume");
        resumeButton.setBounds(WIDTH / 2 - 50, HEIGHT / 2 - 60, 100, 20);
        resumeButton.addActionListener(e -> resumeGame());
        this.add(resumeButton);
        resumeButton.setVisible(false);
    }
    
    private void pauseGame() {
    	if (gameState == GameState.RUNNING) {
    		gameState = GameState.PAUSED;
    		timer.stop();
    		resumeButton.setVisible(true);
    		pauseExitButton.setVisible(true);
    		pauseRestartButton.setVisible(true);
    		restartButton.setVisible(false);
    	}
    }
    
    private void resumeGame() {
    	if (gameState == GameState.PAUSED) {
    		gameState = GameState.RUNNING;
    		timer.start();
    		resumeButton.setVisible(false);
    		pauseExitButton.setVisible(false);
    		pauseRestartButton.setVisible(false);
    	}
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
    
    private void playVictoryAnimation(Graphics g) {
        long t = System.currentTimeMillis() - victoryAnimationStart;
        if (t > VICTORY_ANIMATION_DURATION) {
            isVictoryAnimation = false;
            return;
        }
        float alpha = 0.6f + 0.4f * (float)Math.sin(t / 200.0);
        g.setColor(new Color(1.0f, 0.8f, 0.0f, alpha));
        g.setFont(new Font("Helvetica", Font.BOLD, 40));
        String text = "Victory!";
        FontMetrics m = g.getFontMetrics();
        int tx = (WIDTH - m.stringWidth(text))/2;
        int ty = HEIGHT/2;
        g.drawString(text, tx, ty);
    }

    private void restartGame() {
    	restartButton.setVisible(false);
    	pauseExitButton.setVisible(false);
    	pauseRestartButton.setVisible(false);
    	
        gameState = GameState.RUNNING;
        gameWon = false;
        firstGame = false;
        isNewHighScoreThisGame = false;
        moving = false;
        
        lastKey = KeyEvent.VK_RIGHT;
        
        dots = 3;
        for (int z = 0; z < dots; z++) {
            x[z] = 50 - z * 10;
            y[z] = 50;
        }
        
        score = 0;
        locateApple();
        blueAppleVisible = false;
        blueAppleLastTime = 0;
        blueAppleTimeLeft = 0;
        
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (gameState == GameState.PAUSED) {
        	g.setColor(new Color(0, 0, 0, 150));
        	g.fillRect(0, 0, WIDTH, HEIGHT);
        	
        	g.setColor(Color.white);
        	g.setFont(new Font("Helvetica", Font.BOLD, 36));
        	String msg = "PAUSED";
        	FontMetrics fm = g.getFontMetrics();
        	int tx = (WIDTH - fm.stringWidth(msg)) / 2;
        	int ty = HEIGHT / 3;
        	g.drawString(msg, tx, ty);
        	
        	return;
        }

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

        if (gameState == GameState.RUNNING) {
            drawGame(g);
            drawFPS(g);
        }
        else {
            if (gameWon && isVictoryAnimation) {
                playVictoryAnimation(g);
            }
            else {
                gameOver(g);
            }
        }
        
        updateAndDrawParticles(g);
    }
    
    private void updateAndDrawParticles(Graphics g) {
    	java.util.Iterator<Particle> it = particles.iterator();
    	while (it.hasNext()) {
    		Particle p = it.next();
    		if (!p.isAlive()) {
    			it.remove();
    			continue;
    		}
    		
    		p.update();
    		
    		g.setColor(p.color);
    		g.fillRect((int)p.x, (int)p.y, 2, 2);
    	}
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
        if (gameState != GameState.RUNNING) {
            return;
        } 
        
        g.setColor(Color.red);
        g.fillOval(apple_x, apple_y, SCALE, SCALE);

        for (int z = 0; z < dots; z++) {
            g.setColor(segmentColors[z]);
            g.fillRect(x[z], y[z], SCALE, SCALE);
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
            String msg = gameWon ? "Victory!" : "Game Over";
            String scoreMsg = "Score: " + score;
            String bestScoreMsg = "Best Score: " + bestScore;

            if (score > bestScore) {
                bestScore = score;
            }

            Font font = new Font("Helvetica", Font.BOLD, 24);
            FontMetrics metrics = getFontMetrics(font);
            g.setColor(Color.white);
            g.setFont(font);

            int xMsg = (WIDTH - metrics.stringWidth(msg)) / 2;
            int yMsg = HEIGHT / 2 - 20;
            g.drawString(msg, xMsg, yMsg);

            g.drawString(scoreMsg, (WIDTH - metrics.stringWidth(scoreMsg)) / 2, HEIGHT / 2 + 20);
            g.drawString(bestScoreMsg, (WIDTH - metrics.stringWidth(bestScoreMsg)) / 2, HEIGHT / 2 + 60);

            restartButton.setVisible(true);
        }
    }

    private void checkApple() {
    	if ((x[0] == apple_x) && (y[0] == apple_y)) {
    	    dots++;
    	    score++;
    	    segmentColors[0] = Color.red;
    	    spawnParticles(apple_x + SCALE / 2, apple_y + SCALE / 2, Color.red);
    	    locateApple();
    	    
    	    if (dots == ALL_DOTS)
    	    {
    	    	triggerVictory();
    	    	return;
    	    }
    	} else if (blueAppleVisible && (x[0] == blueApple_x) && (y[0] == blueApple_y)) {
    	    dots += 2;
    	    score += BLUE_APPLE_SCORE;
    	    segmentColors[0] = Color.blue;
    	    segmentColors[1] = Color.blue;
    	    blueAppleVisible = false;
    	    blueAppleLastTime = System.currentTimeMillis();
    	    spawnParticles(blueApple_x + SCALE / 2, blueApple_y + SCALE / 2, Color.blue);
    	}

        if (score > bestScore) {
            bestScore = score;
            if (!isNewHighScoreThisGame && !firstGame) {
                isNewHighScoreThisGame = true;
                setNewHighScore();
            }
        }
    }
    
    private void spawnParticles(int centerX, int centerY, Color color) {
    	for (int i = 0; i < 15; i++) {
    		double angle = Math.random() * 2 * Math.PI;
    		double speed = Math.random() * 2 + 1;
    		float dx = (float) (Math.cos(angle) * speed);
    		float dy = (float) (Math.sin(angle) * speed);
    		int life = 20 + (int)(Math.random() * 10);
    		particles.add(new Particle(centerX, centerY, dx, dy, life, color));
     	}
    }
    
    private void triggerVictory() {
        gameWon = true;
        gameState = GameState.GAME_OVER;
        timer.stop();
        restartButton.setVisible(true);
        
        isVictoryAnimation = true;
        victoryAnimationStart = System.currentTimeMillis();
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
        
        if (x[0] >= WIDTH) {
            x[0] = 0;
        } else if (x[0] < 0) {
            x[0] = WIDTH - SCALE;
        }

        if (y[0] >= HEIGHT) {
            y[0] = 0;
        } else if (y[0] < 0) {
            y[0] = HEIGHT - SCALE;
        }

        for (int z = dots; z > 0; z--) {
            segmentColors[z] = segmentColors[z - 1];
            x[z] = x[(z - 1)];
            y[z] = y[(z - 1)];
        }
        segmentColors[0] = Color.green;
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

    private boolean isAppleCollidingWithAnyText(int appleX, int appleY) {
        if (showBlueAppleHint) {
            FontMetrics hintMetrics = getFontMetrics(new Font("Helvetica", Font.BOLD, 18));
            String hintText = "Blue apples count double!";
            int hintTextWidth = hintMetrics.stringWidth(hintText);
            int hintTextX = (WIDTH - hintTextWidth) / 2;
            int hintTextY = HEIGHT / 2;
            Rectangle hintTextBounds = new Rectangle(hintTextX, hintTextY - hintMetrics.getHeight(), hintTextWidth, hintMetrics.getHeight());
            Rectangle appleBounds = new Rectangle(appleX, appleY, SCALE, SCALE);
            if (appleBounds.intersects(hintTextBounds)) {
                return true;
            }
        }

        FontMetrics fpsMetrics = getFontMetrics(new Font("Helvetica", Font.BOLD, 14));
        String fpsText = "FPS: " + fps;
        Rectangle fpsBounds = new Rectangle(5, 15 - fpsMetrics.getHeight(), fpsMetrics.stringWidth(fpsText), fpsMetrics.getHeight());
        if (new Rectangle(appleX, appleY, SCALE, SCALE).intersects(fpsBounds)) {
            return true;
        }

        FontMetrics scoreMetrics = getFontMetrics(new Font("Helvetica", Font.BOLD, 14));
        String scoreText = "Score: " + score;
        Rectangle scoreBounds = new Rectangle(5, HEIGHT - 5 - scoreMetrics.getHeight(), scoreMetrics.stringWidth(scoreText), scoreMetrics.getHeight());
        String bestScoreText = "Best Score: " + bestScore;
        Rectangle bestScoreBounds = new Rectangle(WIDTH - 100, HEIGHT - 5 - scoreMetrics.getHeight(), scoreMetrics.stringWidth(bestScoreText), scoreMetrics.getHeight());
        if (new Rectangle(appleX, appleY, SCALE, SCALE).intersects(scoreBounds) || new Rectangle(appleX, appleY, SCALE, SCALE).intersects(bestScoreBounds)) {
            return true;
        }

        return false;
    }

    private void locateApple() {
        do {
            apple_x = (int) (Math.random() * RAND_POS) * SCALE;
            apple_y = (int) (Math.random() * RAND_POS) * SCALE;
        } while (isAppleCollidingWithSnake(apple_x, apple_y) || isAppleCollidingWithAnyText(apple_x, apple_y));
    }

    private void locateBlueApple() {
        do {
            blueApple_x = (int) (Math.random() * RAND_POS) * SCALE;
            blueApple_y = (int) (Math.random() * RAND_POS) * SCALE;
        } while (isAppleCollidingWithSnake(blueApple_x, blueApple_y) || isAppleCollidingWithAnyText(blueApple_x, blueApple_y));
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
            
            if (key == KeyEvent.VK_ESCAPE) {
            	if (gameState == GameState.RUNNING) {
            		pauseGame();
            	} else if (gameState == GameState.PAUSED) {
            		resumeGame();
            	}
            	return;
            }
            
            //Debug Victory Screen button
            if (key == KeyEvent.VK_V) {
                triggerVictory();
                return;
            }

            if (gameState == GameState.RUNNING) {
                if (key == KeyEvent.VK_LEFT && lastKey != KeyEvent.VK_RIGHT) {
                    lastKey = KeyEvent.VK_LEFT;
                }
                else if (key == KeyEvent.VK_RIGHT && lastKey != KeyEvent.VK_LEFT) {
                    lastKey = KeyEvent.VK_RIGHT;
                }
                else if (key == KeyEvent.VK_UP && lastKey != KeyEvent.VK_DOWN) {
                    lastKey = KeyEvent.VK_UP;
                }
                else if (key == KeyEvent.VK_DOWN && lastKey != KeyEvent.VK_UP) {
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
            JFrame frame = new JFrame("SimpleSnakeGame v0.8");
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

class Particle {
	float x, y;
	float dx, dy;
	int life;
	Color color;
	
	public Particle(float x, float y, float dx, float dy, int life, Color color) {
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
		this.life = life;
		this.color = color;
	}
	
	public void update() {
		x += dx;
		y += dy;
		
		if (x < 0) {
			x = 0;
			dx = -dx;
		} else if (x > 300) {
			x = 300;
			dx = -dx;
		}
		
		if (y < 0) {
			y = 0;
			dy = -dy;
		} else if (y > 300) {
			y = 300;
			dy = -dy;
		}
		
		life--;
	}
	
	public boolean isAlive() {
		return life > 0;
	}
}