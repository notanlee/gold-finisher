package com.goldfinisher;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class FinisherScreen extends Screen {
	private static final int W = 150, H = 20, GAP = 2;
	private final @Nullable Screen parent;

	public FinisherScreen(@Nullable Screen parent) {
		super(Component.translatable("goldfinisher.menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		FinisherConfig c = FinisherConfig.INSTANCE;
		List<AbstractWidget> w = new ArrayList<>();

		// To add an option: a field in FinisherConfig + one line here.
		w.add(CycleButton.onOffBuilder(c.enabled).create(0, 0, W, H, label("enabled"), (b, v) -> c.enabled = v));
		w.add(CycleButton.<FinisherConfig.Trigger>builder(v -> label("trigger." + v.name().toLowerCase()), c.trigger)
				.withValues(FinisherConfig.Trigger.values())
				.create(0, 0, W, H, label("trigger"), (b, v) -> c.trigger = v));
		w.add(CycleButton.<FinisherConfig.Type>builder(v -> label("type." + v.name().toLowerCase()), c.type)
				.withValues(FinisherConfig.Type.values())
				.create(0, 0, W, H, label("type"), (b, v) -> c.type = v));
		w.add(CycleButton.<FinisherConfig.Color>builder(v -> label("color." + v.name().toLowerCase()), c.color)
				.withValues(FinisherConfig.Color.values())
				.create(0, 0, W, H, label("color"), (b, v) -> {
					c.color = v;
					rebuildWidgets(); // show/hide the RGB sliders
				}));
		if (c.color == FinisherConfig.Color.CUSTOM) {
			w.add(channel("red", 16));
			w.add(channel("green", 8));
			w.add(channel("blue", 0));
		}
		w.add(new Slider(label("duration"), 20, 100, c.durationTicks,
				v -> c.durationTicks = (int) Math.round(v), v -> String.format("%.1fs", v / 20)));
		w.add(new Slider(label("rise"), 0, 2, c.riseHeight,
				v -> c.riseHeight = Math.round(v * 10) / 10.0, v -> String.format("%.1f", v)));
		w.add(new Slider(label("rise_angle"), 0, 60, c.riseAngle,
				v -> c.riseAngle = Math.round(v), v -> String.format("%.0f°", v)));
		w.add(new Slider(label("animation_speed"), 0.25, 3, c.animationSpeed,
				v -> c.animationSpeed = Math.round(v * 20) / 20.0, v -> String.format("%.2fx", v)));
		w.add(CycleButton.<FinisherConfig.Style>builder(v -> label("style." + v.name().toLowerCase()), c.style)
				.withValues(FinisherConfig.Style.values())
				.create(0, 0, W, H, label("style"), (b, v) -> c.style = v));
		w.add(CycleButton.onOffBuilder(c.glow).create(0, 0, W, H, label("glow"), (b, v) -> c.glow = v));
		w.add(CycleButton.onOffBuilder(c.beam).create(0, 0, W, H, label("beam"), (b, v) -> c.beam = v));
		w.add(CycleButton.<FinisherConfig.Particles>builder(v -> label("particles." + v.name().toLowerCase()), c.particles)
				.withValues(FinisherConfig.Particles.values())
				.create(0, 0, W, H, label("particles"), (b, v) -> c.particles = v));
		w.add(CycleButton.onOffBuilder(c.sounds).create(0, 0, W, H, label("sounds"), (b, v) -> c.sounds = v));
		w.add(CycleButton.onOffBuilder(c.totemSound).create(0, 0, W, H, label("totem_sound"), (b, v) -> c.totemSound = v));
		w.add(Button.builder(label("preview"), b -> {
			FinisherConfig.save();
			minecraft.setScreen(null);
			GoldFinisher.preview();
		}).size(W, H).build());

		int left = width / 2 - W - GAP / 2, top = 28;
		for (int i = 0; i < w.size(); i++) {
			AbstractWidget widget = w.get(i);
			widget.setPosition(left + (i % 2) * (W + GAP), top + (i / 2) * (H + GAP));
			addRenderableWidget(widget);
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(width / 2 - 100, height - 28, 200, H).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(font, title, width / 2, 15, 0xFFFFFFFF);
		g.drawCenteredString(font, label("replay_hint"), width / 2, height - 44, 0xFFAAAAAA);
	}

	@Override
	public void onClose() {
		FinisherConfig.save();
		minecraft.setScreen(parent);
	}

	/** 0-255 slider for one byte of customColor. */
	private static Slider channel(String key, int shift) {
		FinisherConfig c = FinisherConfig.INSTANCE;
		return new Slider(label(key), 0, 255, (c.customColor >> shift) & 0xFF,
				v -> c.customColor = (c.customColor & ~(0xFF << shift)) | ((int) Math.round(v) << shift),
				v -> String.valueOf(Math.round(v)));
	}

	private static Component label(String key) {
		return Component.translatable("goldfinisher.option." + key);
	}

	/** Slider over [min, max] that writes straight into a config field. */
	private static class Slider extends AbstractSliderButton {
		private final Component name;
		private final double min, max;
		private final DoubleConsumer setter;
		private final DoubleFunction<String> format;

		Slider(Component name, double min, double max, double current, DoubleConsumer setter, DoubleFunction<String> format) {
			super(0, 0, W, H, Component.empty(), (current - min) / (max - min));
			this.name = name;
			this.min = min;
			this.max = max;
			this.setter = setter;
			this.format = format;
			updateMessage();
		}

		private double real() {
			return min + value * (max - min);
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.empty().append(name).append(": " + format.apply(real())));
		}

		@Override
		protected void applyValue() {
			setter.accept(real());
		}
	}
}
