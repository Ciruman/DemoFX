/*
 * Copyright (c) 2015 Chris Newland.
 * Licensed under https://github.com/chriswhocodes/demofx/blob/master/LICENSE-BSD
 */
package com.chrisnewland.demofx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class DemoFXApplication extends Application
{
	private static String[] args;

	public static void main(String[] args)
	{
		DemoFXApplication.args = args;
		Application.launch(args);
	}

	private GraphicsContext gc;

	private Label statsLabel;
	private Label fxLabel;

	private List<IEffect> effects;

	@Override
	public void start(final Stage stage) throws Exception
	{
		DemoConfig config = DemoConfig.parseArgs(args);

		if (config == null)
		{
			System.err.print(DemoConfig.getUsageError());
			System.exit(-1);
		}
                
                DemoFX demoFX = new DemoFX();

		runDemo(stage, config);
	}

	private void runDemo(final Stage stage, final DemoConfig config)
	{
		Canvas canvas = new Canvas(config.getWidth(), config.getHeight());

		gc = canvas.getGraphicsContext2D();

		try
		{
			if (config.isUseScriptedDemoConfig())
			{
				effects = ScriptedDemoConfig.getEffects(gc, config);
			}
			else
			{
				effects = EffectFactory.getEffects(gc, config);
			}
		}
		catch (RuntimeException re)
		{
			re.printStackTrace();
			System.err.println(re.getMessage());
			System.err.print(DemoConfig.getUsageError());
			System.exit(-1);
		}

		BorderPane root = new BorderPane();

		Scene scene;

		int topHeight = 50;

		scene = new Scene(root, config.getWidth(), config.getHeight() + topHeight);

		final String BLACK_BG_STYLE = "-fx-background-color:black;";
		final String FONT_STYLE = "-fx-font-family:monospace; -fx-font-size:16px; -fx-text-fill:white;";

		statsLabel = new Label();
		statsLabel.setStyle(FONT_STYLE);
		statsLabel.setAlignment(Pos.BASELINE_LEFT);
		statsLabel.prefWidthProperty().bind(root.widthProperty());

		fxLabel = new Label();
		fxLabel.setStyle(FONT_STYLE);
		fxLabel.setAlignment(Pos.BASELINE_LEFT);
		fxLabel.prefWidthProperty().bind(root.widthProperty());
		fxLabel.setText(getFXLabelText(config));

		root.setStyle(BLACK_BG_STYLE);

		VBox vbox = new VBox();
		vbox.setMinHeight(topHeight);
		vbox.getChildren().add(statsLabel);
		vbox.getChildren().add(fxLabel);

		root.setTop(vbox);
		root.setCenter(canvas);

		stage.setTitle("DemoFX performance test platform by @chriswhocodes");
		stage.setScene(scene);
		stage.show();

		stage.setOnCloseRequest(new EventHandler<WindowEvent>()
		{
			@Override
			public void handle(WindowEvent arg0)
			{
				for (IEffect effect : effects)
				{
					effect.stop();
				}
			}
		});

		DemoAnimationTimer timer = new DemoAnimationTimer(gc, statsLabel, effects);
		timer.start();
	}

	private String getFXLabelText(DemoConfig config)
	{
		StringBuilder builder = new StringBuilder();

		builder.append("Order: ").append(getPrismTryOrder());
		builder.append(" | Pipeline: ").append(getUsedPipeline());
		builder.append(" | Lookups: ");

		StringBuilder lookupBuilder = new StringBuilder();

		boolean anyLookups = false;

		if (config.isLookupRandom())
		{
			anyLookups = true;
			lookupBuilder.append("rand").append(",");
		}

		if (config.isLookupSqrt())
		{
			anyLookups = true;
			lookupBuilder.append("sqrt").append(",");
		}

		if (config.isLookupTrig())
		{
			anyLookups = true;
			lookupBuilder.append("trig");
		}

		if (!anyLookups)
		{
			lookupBuilder.append("none");
		}
		else if (lookupBuilder.charAt(lookupBuilder.length() - 1) == ',')
		{
			lookupBuilder.deleteCharAt(lookupBuilder.length() - 1);
		}

		builder.append(lookupBuilder.toString());

		return builder.toString();
	}

	private String getUsedPipeline()
	{
		GraphicsPipeline pipeline = GraphicsPipeline.getPipeline();
		return pipeline.getClass().getName();
	}

	@SuppressWarnings("unchecked")
	private String getPrismTryOrder()
	{
		// Java 7 returns String[]
		// Java 8 returns List<String>
		Object result = PrismSettings.tryOrder;

		List<String> tryOrderList = new ArrayList<>();

		if (result instanceof String[])
		{
			tryOrderList.addAll(Arrays.asList((String[]) result));
		}
		else if (result instanceof List)
		{
			tryOrderList.addAll((List<String>) result);
		}

		StringBuilder builder = new StringBuilder();

		for (String str : tryOrderList)
		{
			builder.append(str).append(",");
		}

		if (builder.length() > 0)
		{
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder.toString();
	}
}
