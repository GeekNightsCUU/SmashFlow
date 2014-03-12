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
import com.sun.org.apache.xml.internal.security.Init;

public class SmashFlow implements ApplicationListener {
	private OrthographicCamera camera;
	private ShapeRenderer shapeRenderer;
	
	// Render objects
	private SpriteBatch batch;
	private Texture texture;
	private Sprite sprite;
	
	private FPSLogger fpslogger;
	
	// World sections
	private float currentColor = 0.0f;
	private final int width = 120; // Default 110
	private final int height = 60; // Default 60
	private int pixelSize = 7;
	
	private int offsetX = 0;
	private int offsetY = 0;
	
	float[][] worldMap = new float[width][];
	float[] row = new float[height];
	boolean[][] unitsToMove = new boolean[width][];
	Point2d currentUnitToMove = new Point2d(0, 0);
	
	Point2d movementCursor = new Point2d(0, 0);
	Point2d direction = new Point2d(); // Used in movement to avoid GC
	Point2d currentParticleDir = new Point2d();
	int currentDistance = 0;
	int bestDistance = 0;
	Point2d[] movements = new Point2d[8];
	
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
			unitsToMove[current_column] = new boolean[height];
		}
		
		// Create some units
		//worldMap[(int)(width / 2)][(int)height / 2] = 1.0f;
		for (int x = 0; x < 20; ++x) {
			for (int y = 0; y < 20; ++y) {
				worldMap[x][y] = 1.0f;
			}
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
		
		// Init the 8 directions around a point
		initMovements();
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

		// Apply the "physics" engine
		//createRain();
		//applyGravity();
		moveUnits();
		clearUnitsToMove();
		
		// Render units
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Filled);
				
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				currentColor = worldMap[x][y];
				
				// Only draw the pixel if it has a color
				if (worldMap[x][y] != 0.0f) {
					shapeRenderer.setColor(currentColor, currentColor, currentColor, 1);
					shapeRenderer.rect((x * pixelSize) + offsetX,
							(y * pixelSize) + offsetY,
							pixelSize, pixelSize);
				}
			}
		}
		
		// Draw border around the play space 
		shapeRenderer.setColor(0.0f, 0.0f, 0.0f, 0.1f); // Black
		shapeRenderer.rect(offsetX - pixelSize, // Bottom
				offsetY - pixelSize,
				pixelSize * (width + 1), pixelSize);
		shapeRenderer.rect(offsetX - pixelSize, // Top
				offsetY + (pixelSize * height),
				pixelSize * (width + 1), pixelSize);
		shapeRenderer.rect(offsetX - pixelSize, // Left
				offsetY - pixelSize,
				pixelSize, pixelSize * (height + 1));
		shapeRenderer.rect(offsetX + (pixelSize * width), // Right
				offsetY - pixelSize,
				pixelSize, pixelSize * (height + 2));
		
		// Draw current cursor - A cross around the movement cursor
		shapeRenderer.setColor(0.5f, 0, 0, 1);
		shapeRenderer.rect(((movementCursor.x + 1) * pixelSize) + offsetX,
				(movementCursor.y * pixelSize) + offsetY,
				pixelSize, pixelSize);
		shapeRenderer.rect(((movementCursor.x - 1) * pixelSize) + offsetX,
				(movementCursor.y * pixelSize) + offsetY,
				pixelSize, pixelSize);
		shapeRenderer.rect((movementCursor.x * pixelSize) + offsetX,
				((movementCursor.y + 1) * pixelSize) + offsetY,
				pixelSize, pixelSize);
		shapeRenderer.rect((movementCursor.x * pixelSize) + offsetX,
				((movementCursor.y - 1) * pixelSize) + offsetY,
				pixelSize, pixelSize);
		
		shapeRenderer.end();
		
		// Check touches
		if (Gdx.input.isTouched(0)) {
			
			int touchX = Gdx.input.getX();
			int touchY = Gdx.input.getY();
			
			//Gdx.app.log("touch", "X: " + touchX + " Y:" + touchY);
			
			Point2d point2d = getLocalCoordinates(touchX, touchY);
			boolean valid_point = point2d.x >= 0
					&& point2d.x < width
					&& point2d.y >= 0
					&& point2d.y < height
					;
					
			if (valid_point) {
				// Create unit in the selected position
				//worldMap[point2d.x][point2d.y] = 1.0f;
				
				// Move the cursor to the selected position
				movementCursor = point2d;
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
		//Gdx.app.log("local_coord", "X: " + pointX + " Y:" + pointY);
		
		return new Point2d(pointX, pointY);
	}
	
	/*
	private void applyGravity() {
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
	
	private void createRain() {
		for (int current_column = 0; current_column < width; ++current_column) {
			// Create random rain-drops in the second row (the first is used as cleaning)
			// Using stupid 10% chance of creating a rain drop
			if (java.lang.Math.random() * 10 < 1) {
				worldMap[current_column][height - 1] = 1;
			}
		}
	}
	*/
	
	private void moveUnits() {
		// Mark the units we're going to move
		// To avoid race conditions where the same unit is moved many times
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// Only move units with value (not empty)
				if (worldMap[x][y] != 0.0f) {
					unitsToMove[x][y] = true; 
				}
			}
		}
		
		// Now, move the units
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				if (unitsToMove[x][y]) {
					currentUnitToMove.x = x;
					currentUnitToMove.y = y;
					moveUnitToCursor(currentUnitToMove);
				}
			}
		}
	}
	
	private void moveUnitToCursor(Point2d unit) {
		// Initialize with the current position of the particle 
		currentParticleDir.x = 0;
		currentParticleDir.y = 0;
		
		//Gdx.app.log("Current particle", unit.x + " " + unit.y);
		
		int xDist = unit.x - movementCursor.x;
		int yDist = unit.y - movementCursor.y;
		
		bestDistance = (int)(Math.pow(xDist, 2) + Math.pow(yDist, 2));
		//Gdx.app.log("Best distance", bestDistance + "");
		
		// Check the distances of each of the available movements
		for (int currentMovement = 0; currentMovement < movements.length; ++currentMovement) {
			int xMove = movements[currentMovement].x;
			int yMove = movements[currentMovement].y;
			
			int xDest = unit.x + xMove;
			int yDest = unit.y + yMove;
			//Gdx.app.log("Current destination", xDest + " " + yDest);
			
			// For default get infinite distance
			currentDistance = Integer.MAX_VALUE - 1;
			
			// Check we're in the allowed space
			if (xDest >= 0 && xDest < width && yDest >= 0 && yDest < height) {
				
				if (worldMap[xDest][yDest] != 1.0f) { // If the space is empty
					// Calculate the base point and one of the 8 movements evaluated
					currentDistance = (int)(Math.pow(xDist + xMove, 2) + Math.pow(yDist + yMove, 2));
					//Gdx.app.log("current_dist", currentDistance + " " + xMove + " " + yMove);
				}
			}
			
			
			if (currentDistance < bestDistance) {
				currentParticleDir.x = movements[currentMovement].x;
				currentParticleDir.y = movements[currentMovement].y;
				bestDistance = currentDistance;
				//Gdx.app.log("best_distance", bestDistance + "");
				//Gdx.app.log("best_distance", "X: " + xMove + " Y: " + yMove);
			}
		}
		
		// If the particle is going to be moved, copy to the new place and delete the current one
		if (currentParticleDir.x != 0 || currentParticleDir.y != 0) {
			worldMap[unit.x + currentParticleDir.x][unit.y + currentParticleDir.y] = 1.0f;
			worldMap[unit.x][unit.y] = 0.0f;
		}
		
	}
	
	private void moveUnitToCursor_old(Point2d unit) {
		int deltaX = movementCursor.x - unit.x;
		int deltaY = movementCursor.y - unit.y;
		
		int destX = 0;
		int destY = 0;
		
		float tan = 0.0f;
		if (deltaX != 0) {
			tan = deltaY / deltaX; 
		} else {
			tan = deltaY / 0.001f;
		}
		
		// Stupid interpolation to calculate movement - Didn't work
		if (tan >= 0.4142f && tan < 2.4142f) {
			if (deltaX > 0) {
				direction.x = 1;
				direction.y = 1; 
			} else {
				direction.x = -1;
				direction.y = -1; 
			}
		}
		
		if (tan >= -2.4142f && tan < -0.4142f) {
			if (deltaX > 0) {
				direction.x = 1;
				direction.y = -1; 
			} else {
				direction.x = -1;
				direction.y = 1; 
			}
		}
		
		if (tan >= 2.4142f || tan < -2.4142f) {
			if (deltaY > 0) {
				direction.x = 0;
				direction.y = 1; 
			} else {
				direction.x = 0;
				direction.y = -1; 
			}
		}
		
		if (tan >= -0.4142f && tan < 0.4142f) {
			if (deltaX > 0) {
				direction.x = 1;
				direction.y = 0; 
			} else {
				direction.x = -1;
				direction.y = 0; 
			}
		}
		
		if (deltaX == 0 && deltaY == 0) {
			direction.x = 0;
			direction.y = 0; 
		}
		
		destX = unit.x + direction.x;
		destY = unit.y + direction.y;
		
		// If we're within the game area and the space is empty 
		if (destX >= 0 && destX < width && destY >= 0 && destY < height) {
			if (worldMap[destX][destY] == 0.0f) {
				worldMap[destX][destY] = 1.0f;
				worldMap[unit.x][unit.y] = 0.0f;
			} else { // Stupid algorithm to move in other ways if there are collisions
				destX = unit.x;
				destY = unit.y + direction.y;
				
				if (worldMap[destX][destY] == 0.0f) {
					worldMap[destX][destY] = 1.0f;
					worldMap[unit.x][unit.y] = 0.0f;
				} else {
					destX = unit.x + direction.x;
					destY = unit.y;
					
					if (worldMap[destX][destY] == 0.0f) {
						worldMap[destX][destY] = 1.0f;
						worldMap[unit.x][unit.y] = 0.0f;
					}
				}
			}
		}
	}
	//*/
	
	private void clearUnitsToMove() {
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				unitsToMove[x][y] = false;
			}
		}
	}
	
	private void initMovements() {
		movements[0] = new Point2d(0, 1);
		movements[1] = new Point2d(1, 1);
		movements[2] = new Point2d(1, 0);
		movements[3] = new Point2d(1, -1);
		movements[4] = new Point2d(0, -1);
		movements[5] = new Point2d(-1, -1);
		movements[6] = new Point2d(-1, 0);
		movements[7] = new Point2d(-1, 1);
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