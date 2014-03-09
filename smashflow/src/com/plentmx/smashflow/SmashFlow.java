package com.plentmx.smashflow;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.FPSLogger;

// Render imports
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class SmashFlow implements ApplicationListener {
	private OrthographicCamera camera;
	private ShapeRenderer shapeRenderer;
	
	// Render objects
	private SpriteBatch batch;
	private Texture texture;
	private Sprite sprite;
	
	private FPSLogger fpslogger;
	
	// World settions
	private float currentColor = 0.0f;
	private final int width = 110; // 110
	private final int height = 60; // 60
	private int pixelSize = 8;
	
	private int offsetX = 0;
	private int offsetY = 0;
	
	float[][] worldMap = new float[width][];
	float[] row = new float[height];
	
	@Override
	public void create() {		
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		// Show the screen size of the device
		Gdx.app.log("screen_size", "Width: " + w + " Height:" + h);
		
		camera = new OrthographicCamera(w, h);
		shapeRenderer = new ShapeRenderer();
		fpslogger = new FPSLogger();
		
		// Calculate the center of our screen
		offsetX = (int)(-width * pixelSize / 2);
		offsetY = (int)(-height * pixelSize / 2);
		
		// Create the 2d world map
		for (int current_column = 0; current_column < width; ++current_column) {
			// Insert rows
			worldMap[current_column] = new float[height];
		}
		
		// Create the background
		batch = new SpriteBatch();
		
		texture = new Texture(Gdx.files.internal("data/blue_texture_waves.png"));
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		
		TextureRegion region = new TextureRegion(texture, 0, 0, 1200, 780);
		
		sprite = new Sprite(region);
		sprite.setSize(sprite.getWidth(), sprite.getHeight());
		sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);
		sprite.setPosition(-sprite.getWidth() / 2, -sprite.getHeight() / 2);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		// Draw background
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		sprite.draw(batch);
		batch.end();
		
		// Update state of the worldMap
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// Uncomment this to get black and white noise
				//worldMap[x][y] = (float)java.lang.Math.random();
			}
		}

		createRain(worldMap);
		applyGravity(worldMap);
		
		
		// Render units
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Filled);
				
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				currentColor = worldMap[x][y];
				
				// Only draw the pixel if has a color
				if (worldMap[x][y] != 0.0f) {
					shapeRenderer.setColor(currentColor, currentColor, currentColor, 1);
					shapeRenderer.rect((x * pixelSize) + offsetX,
							(y * pixelSize) + offsetY,
							pixelSize, pixelSize);
				}
			}
		}
		
		shapeRenderer.end();
		
		// Check touches
		if (Gdx.input.isTouched(0)) {
			
			int touchX = Gdx.input.getX();
			int touchY = Gdx.input.getY();
			
			Gdx.app.log("touch", "X: " + touchX + " Y:" + touchY);
			
			Point2d point2d = getLocalCoordinates(touchX, touchY);
			boolean valid_point = point2d.x >= 0
					&& point2d.x < width
					&& point2d.y >= 0
					&& point2d.y < height
					;
					
			if (valid_point) {
				worldMap[point2d.x][point2d.y] = 1.0f; 
			}
		}
		
		// Show the FPS in console
		fpslogger.log();
	}
	
	@Override
	public void dispose() {

	}
	
	/**
	 * Convert a touch in screen to the clicked unit
	 * @param x
	 * @param y
	 * @return Point2d
	 */
	private Point2d getLocalCoordinates(int x, int y) {
		int w = (int)Gdx.graphics.getWidth();
		int h = (int)Gdx.graphics.getHeight();
				
		int centerX = w / 2;
		int centerY = h / 2;
		
		// Calculate the unit we're selecting
		int pointX = (x - offsetX - centerX) / pixelSize;
		int pointY = -(y + offsetY - centerY) / pixelSize;
		Gdx.app.log("local_coord", "X: " + pointX + " Y:" + pointY);
		
		return new Point2d(pointX, pointY);
	}
	
	private void applyGravity(float[][] worldMap) {
		for (int current_row = 0; current_row < height - 1; ++current_row) {
			for (int current_column = 0; current_column < width; ++current_column) {
				// Copy the unit from the upper row
				worldMap[current_column][current_row] = worldMap[current_column][current_row + 1];
			}
		}
		
		for (int current_column = 0; current_column < width; ++current_column) {
			// Clean the units in the upper row (last in the array)
			worldMap[current_column][height - 1] = 0.0f;
		}
	}
	
	private void createRain(float[][] worldMap) {
		for (int current_column = 0; current_column < width; ++current_column) {
			// Create random rain-drops in the second row (the first is used as cleaning)
			// Using stupid 10% chance of creating a rain drop
			if (java.lang.Math.random() * 10 < 1) {
				worldMap[current_column][height - 1] = 1;
			}
		}
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
