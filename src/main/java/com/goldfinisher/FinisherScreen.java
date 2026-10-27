package com.goldfinisher;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Create-World-style tabbed settings. Every option is one field in {@link FinisherConfig} + one line in a tab below. */
public class FinisherScreen extends Screen {
	private static final int W = 150, H = 20, COL_GAP = 10, ROW_GAP = 4;
	private final @Nullable Screen parent;
	private @Nullable TabNavigationBar tabBar;
	private @Nullable HeaderAndFooterLayout layout;
	/** Survives rebuildWidgets() (type/color change, resize) so the user stays on the same tab. */
	private int selectedTab;

	public FinisherScreen(@Nullable Screen parent) {
		super(Component.translatable("goldfinisher.menu.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		List<Tab> tabs = List.of(
				new OptionsTab("general", this::general),
				new OptionsTab("finisher", this::finisher),
				new OptionsTab("effects", this::effects));
		TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget,
				t -> selectedTab = tabs.indexOf(t), t -> {});
		tabBar = TabNavigationBar.builder(tabManager, width).addTabs(tabs.toArray(Tab[]::new)).build();
		addRenderableWidget(tabBar);

		layout = new HeaderAndFooterLayout(this);
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);
		tabBar.selectTab(selectedTab, false);

		// same as CreateWorldScreen.repositionElements(); resizing rebuilds the whole screen
		tabBar.setWidth(width);
		tabBar.arrangeElements();
		int top = tabBar.getRectangle().bottom();
		tabManager.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
		layout.setHeaderHeight(top);
		layout.arrangeElements();
	}

	private void general(GridLayout.RowHelper rows) {
		FinisherConfig c = FinisherConfig.INSTANCE;
		rows.addChild(CycleButton.onOffBuilder(c.enabled).create(0, 0, W, H, label("enabled"), (b, v) -> c.enabled = v));
		rows.addChild(cycle("type", FinisherConfig.Type.values(), c.type, v -> {
			c.type = v;
			rebuildWidgets(); // the Finisher tab shows this type's options
		}));
		rows.addChild(cycle("trigger", FinisherConfig.Trigger.values(), c.trigger, v -> c.trigger = v));
		rows.addChild(new Slider(label("animation_speed"), 0.25, 3, c.animationSpeed,
				v -> c.animationSpeed = Math.round(v * 20) / 20.0, v -> String.format("%.2fx", v)));
		rows.addChild(Button.builder(label("preview"), b -> {
			FinisherConfig.save();
			minecraft.setScreen(null);
			GoldFinisher.preview();
		}).size(W, H).build(), 2, rows.newCellSettings().alignHorizontallyCenter());
		rows.addChild(new StringWidget(Component.translatable("goldfinisher.option.replay_hint").withStyle(ChatFormatting.GRAY), font).setMaxWidth(2 * W + COL_GAP),
				2, rows.newCellSettings().alignHorizontallyCenter().paddingTop(6));
	}

	private void finisher(GridLayout.RowHelper rows) {
		FinisherConfig c = FinisherConfig.INSTANCE;
		switch (c.type) {
			case CHAMPIONS -> {
				rows.addChild(CycleButton.<FinisherConfig.ChampionsVersion>builder(
								v -> label("champions_version." + v.name().substring(1)), c.championsVersion)
						.withValues(FinisherConfig.ChampionsVersion.values())
						.create(0, 0, W, H, label("champions_version"), (b, v) -> c.championsVersion = v));
				rows.addChild(duration(40, 200, c.championsDuration, v -> c.championsDuration = v));
				rows.addChild(cycle("color", FinisherConfig.Color.values(), c.color, v -> {
					c.color = v;
					rebuildWidgets(); // show/hide the RGB sliders
				}));
				rows.addChild(cycle("style", FinisherConfig.Style.values(), c.style, v -> c.style = v));
				if (c.color == FinisherConfig.Color.CUSTOM) rgb(rows, () -> c.customColor, v -> c.customColor = v);
				rows.addChild(new Slider(label("rise"), 0, 2, c.riseHeight,
						v -> c.riseHeight = Math.round(v * 10) / 10.0, v -> String.format("%.1f", v)));
				rows.addChild(angle("horizontal_angle", 180, c.championsHorizontalAngle, v -> c.championsHorizontalAngle = v));
				rows.addChild(angle("vertical_angle", 60, c.championsVerticalAngle, v -> c.championsVerticalAngle = v));
			}
			case REAVER -> {
				rows.addChild(duration(40, 200, c.reaverDuration, v -> c.reaverDuration = v));
				rows.addChild(cycle("style", FinisherConfig.Style.values(), c.reaverStyle, v -> c.reaverStyle = v));
				rows.addChild(cycle("reaver_color", FinisherConfig.ReaverColor.values(), c.reaverColor, v -> {
					c.reaverColor = v;
					rebuildWidgets(); // show/hide the RGB sliders
				}));
				if (c.reaverColor == FinisherConfig.ReaverColor.CUSTOM) {
					rows.addChild(new SpacerElement(W, H)); // keeps the RGB sliders together
					rgb(rows, () -> c.reaverCustomColor, v -> c.reaverCustomColor = v);
				}
				rows.addChild(new Slider(label("sink_depth"), 0.5, 3, c.sinkDepth,
						v -> c.sinkDepth = Math.round(v * 10) / 10.0, v -> String.format("%.1f", v)));
				rows.addChild(new Slider(label("sink_start"), 0.1, 0.8, c.sinkStart,
						v -> c.sinkStart = Math.round(v * 20) / 20.0, v -> String.format("%.0f%%", v * 100)));
				rows.addChild(new Slider(label("shiver"), 0, 2, c.shiver,
						v -> c.shiver = Math.round(v * 10) / 10.0, v -> String.format("%.1fx", v)));
				rows.addChild(angle("horizontal_angle", 180, c.reaverHorizontalAngle, v -> c.reaverHorizontalAngle = v));
				rows.addChild(angle("vertical_angle", 60, c.reaverVerticalAngle, v -> c.reaverVerticalAngle = v));
				rows.addChild(toggle("portal_ring", c.portalRing, v -> c.portalRing = v));
				rows.addChild(toggle("souls", c.souls, v -> c.souls = v));
				rows.addChild(toggle("pillar", c.pillar, v -> c.pillar = v));
			}
			case AEMONDIR -> {
				rows.addChild(duration(60, 240, c.aemondirDuration, v -> c.aemondirDuration = v));
				rows.addChild(cycle("style", FinisherConfig.Style.values(), c.aemondirStyle, v -> c.aemondirStyle = v));
				rows.addChild(new Slider(label("sword_scale"), 1.5, 4, c.swordScale,
						v -> c.swordScale = Math.round(v * 10) / 10.0, v -> String.format("%.1f", v)));
				rows.addChild(toggle("crystals", c.crystals, v -> c.crystals = v));
				rows.addChild(toggle("explosion", c.explosion, v -> c.explosion = v));
			}
		}
	}

	private void effects(GridLayout.RowHelper rows) {
		FinisherConfig c = FinisherConfig.INSTANCE;
		rows.addChild(cycle("particles", FinisherConfig.Particles.values(), c.particles, v -> c.particles = v));
		rows.addChild(toggle("sounds", c.sounds, v -> c.sounds = v));
		rows.addChild(toggle("totem_sound", c.totemSound, v -> c.totemSound = v));
		rows.addChild(toggle("totem_particles", c.totemParticles, v -> c.totemParticles = v));
		rows.addChild(toggle("glow", c.glow, v -> c.glow = v));
		rows.addChild(toggle("beam", c.beam, v -> c.beam = v));
	}

	@Override
	protected void setInitialFocus() {
		// like CreateWorldScreen: nothing focused until the user tabs/clicks
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return tabBar != null && tabBar.keyPressed(event) || super.keyPressed(event); // Ctrl+Tab / Ctrl+1..3
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (layout != null) g.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR,
				0, height - layout.getFooterHeight() - 2, 0, 0, width, 2, 32, 2);
	}

	@Override
	protected void renderMenuBackground(GuiGraphics g) {
		int header = layout == null ? 0 : layout.getHeaderHeight();
		g.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0, 0, width, header, 16, 16);
		renderMenuBackground(g, 0, header, width, height);
	}

	@Override
	public void onClose() {
		FinisherConfig.save();
		minecraft.setScreen(parent);
	}

	/**
	 * A 2-column grid inside a vanilla ScrollableLayout: centered in the tab area, and if it is taller than
	 * the area (small window / big GUI scale) it scrolls instead of running under the tab bar or footer.
	 */
	private final class OptionsTab implements Tab {
		private final Component title;
		private final ScrollableLayout scroll;

		OptionsTab(String key, Consumer<GridLayout.RowHelper> content) {
			title = Component.translatable("goldfinisher.tab." + key);
			GridLayout grid = new GridLayout().columnSpacing(COL_GAP).rowSpacing(ROW_GAP);
			content.accept(grid.createRowHelper(2));
			scroll = new ScrollableLayout(minecraft, grid, 0); // must come after the content: it collects the widgets now
		}

		@Override
		public Component getTabTitle() {
			return title;
		}

		@Override
		public Component getTabExtraNarration() {
			return Component.empty();
		}

		@Override
		public void visitChildren(Consumer<AbstractWidget> consumer) {
			scroll.visitWidgets(consumer);
		}

		@Override
		public void doLayout(ScreenRectangle area) {
			scroll.setMaxHeight(area.height() - 2 * 8);
			scroll.arrangeElements();
			FrameLayout.alignInRectangle(scroll, area, 0.5F, 1 / 6F);
		}
	}

	private static <T extends Enum<T>> CycleButton<T> cycle(String key, T[] values, T current, Consumer<T> set) {
		return CycleButton.<T>builder(v -> label(key + "." + v.name().toLowerCase()), current)
				.withValues(values)
				.create(0, 0, W, H, label(key), (b, v) -> set.accept(v));
	}

	private static CycleButton<Boolean> toggle(String key, boolean current, Consumer<Boolean> set) {
		return CycleButton.onOffBuilder(current).create(0, 0, W, H, label(key), (b, v) -> set.accept(v));
	}

	/** Duration in ticks, shown as seconds. */
	private static Slider duration(int min, int max, int current, IntConsumer set) {
		return new Slider(label("duration"), min, max, current, v -> set.accept((int) Math.round(v)), v -> String.format("%.1fs", v / 20));
	}

	/** Red/green/blue sliders for an RGB field (3 cells: fills one row and starts the next). */
	private static void rgb(GridLayout.RowHelper rows, IntSupplier get, IntConsumer set) {
		rows.addChild(channel("red", 16, get, set));
		rows.addChild(channel("green", 8, get, set));
		rows.addChild(channel("blue", 0, get, set));
	}

	/** 0-255 slider for one byte of an RGB config field. */
	private static Slider channel(String key, int shift, IntSupplier get, IntConsumer set) {
		return new Slider(label(key), 0, 255, (get.getAsInt() >> shift) & 0xFF,
				v -> set.accept((get.getAsInt() & ~(0xFF << shift)) | ((int) Math.round(v) << shift)),
				v -> String.valueOf(Math.round(v)));
	}

	/** Whole-degree slider over [-range, range]. */
	private static Slider angle(String key, double range, double current, DoubleConsumer set) {
		return new Slider(label(key), -range, range, current, v -> set.accept(Math.round(v)), v -> String.format("%.0f°", v));
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
