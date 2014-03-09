package com.plentmx.smashflow;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main {
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Smash Flow Alpha 1";
		cfg.useGL20 = true;
		cfg.width = 1024;
		cfg.height = 700;
		
		new LwjglApplication(new SmashFlow(), cfg);
	}
}
