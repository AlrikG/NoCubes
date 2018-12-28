package io.github.cadiboo.nocubes.client;

import io.github.cadiboo.nocubes.NoCubes;
import io.github.cadiboo.nocubes.config.ModConfig;
import io.github.cadiboo.nocubes.util.ModReference;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkBlockEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkBlockRenderInLayerEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkBlockRenderInTypeEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkPostEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkPreEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static net.minecraftforge.fml.relauncher.Side.CLIENT;

/**
 * Subscribe to events that should be handled on the PHYSICAL CLIENT in this class
 *
 * @author Cadiboo
 */
@Mod.EventBusSubscriber(modid = ModReference.MOD_ID, value = CLIENT)
public final class ClientEventSubscriber {

	@SubscribeEvent
	public static void onRebuildChunkPreEvent(final RebuildChunkPreEvent event) {

		if (!NoCubes.isEnabled()) {
			return;
		}

		if (ModConfig.debug.debugEnabled) {
			return;
		}

		if (ModConfig.shouldExtendLiquids)
			ClientUtil.calculateExtendedLiquids(event);

		ModConfig.activeStableRenderingAlgorithm.renderPre(event);
	}

	@SubscribeEvent
	public static void onRebuildChunkBlockRenderInLayerEvent(final RebuildChunkBlockRenderInLayerEvent event) {

		if (!NoCubes.isEnabled()) {
			return;
		}

		if (ModConfig.debug.debugEnabled) {
			return;
		}

		ModConfig.activeStableRenderingAlgorithm.renderLayer(event);
	}

	@SubscribeEvent
	public static void onRebuildChunkBlockRenderInTypeEvent(final RebuildChunkBlockRenderInTypeEvent event) {

		if (!NoCubes.isEnabled()) {
			return;
		}

		if (ModConfig.debug.debugEnabled) {
			return;
		}

		ModConfig.activeStableRenderingAlgorithm.renderType(event);
	}

	@SubscribeEvent
	public static void onRebuildChunkBlockEvent(final RebuildChunkBlockEvent event) {

		if (!NoCubes.isEnabled()) {
			return;
		}

		if (ModConfig.debug.debugEnabled) {
			return;
		}

		if (ModConfig.shouldExtendLiquids)
			ClientUtil.handleExtendedLiquidRender(event);

		ModConfig.activeStableRenderingAlgorithm.renderBlock(event);
	}

	@SubscribeEvent
	public static void onRebuildChunkPostEvent(final RebuildChunkPostEvent event) {

		if (!NoCubes.isEnabled()) {
			return;
		}

		if (ModConfig.debug.debugEnabled) {
			return;
		}

		if (ModConfig.shouldExtendLiquids)
			ClientUtil.cleanupExtendedLiquids(event);

		ModConfig.activeStableRenderingAlgorithm.renderPost(event);
	}

}