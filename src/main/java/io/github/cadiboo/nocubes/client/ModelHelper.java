package io.github.cadiboo.nocubes.client;

import io.github.cadiboo.nocubes.util.ModProfiler;
import io.github.cadiboo.nocubes.util.StateHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static net.minecraft.util.Direction.DOWN;
import static net.minecraft.util.Direction.EAST;
import static net.minecraft.util.Direction.NORTH;
import static net.minecraft.util.Direction.SOUTH;
import static net.minecraft.util.Direction.UP;
import static net.minecraft.util.Direction.WEST;

/**
 * @author Cadiboo
 */
public final class ModelHelper {

	/**
	 * The order of {@link Direction} and null used in getQuads
	 */
	public static final Direction[] ENUMFACING_QUADS_ORDERED = {
			UP, null, DOWN, NORTH, EAST, SOUTH, WEST,
	};

	@Nullable
	public static List<BakedQuad> getQuads(BlockState state, final BlockPos pos, final BufferBuilder bufferBuilder, final IBlockReader blockAccess, final BlockRendererDispatcher blockRendererDispatcher, final IModelData modelData, final Random posRand, final BlockRenderLayer blockRenderLayer) {

//		try (final ModProfiler ignored = ModProfiler.get().start("getActualState")) {
//			try {
//				state = state.getActualState(blockAccess, pos);
//			} catch (Exception ignored1) {
//			}
//		}

		IBakedModel model = getModel(state, blockRendererDispatcher);

//		Object renderEnv = null;

//		if (OPTIFINE_INSTALLED) {
////		    RenderEnv renderEnv = bufferBuilder.getRenderEnv(blockAccess, state, pos);
//			renderEnv = BufferBuilderOF.getRenderEnv(bufferBuilder, blockAccess, state, pos);
//
//			model = BlockModelCustomizer.getRenderModel(model, state, renderEnv);
//		}

//		try (final ModProfiler ignored = ModProfiler.get().start("getExtendedState")) {
//			state = state.getBlock().getExtendedState(state, blockAccess, pos);
//		}

		for (int facingIndex = 0, enumfacing_quads_orderedLength = ENUMFACING_QUADS_ORDERED.length; facingIndex < enumfacing_quads_orderedLength; facingIndex++) {
			final Direction facing = ENUMFACING_QUADS_ORDERED[facingIndex];
			List<BakedQuad> quads = model.getQuads(state, facing, posRand, modelData);
			if (quads.isEmpty()) {
				continue;
			}

//			if (OPTIFINE_INSTALLED) {
//				try (final ModProfiler ignored = ModProfiler.get().start("getRenderQuads")) {
//					quads = BlockModelCustomizer.getRenderQuads(quads, blockAccess, state, pos, facing, blockRenderLayer, posRand, renderEnv);
//					if (quads.isEmpty()) {
//						continue;
//					}
//				}
//			}

			return quads;
		}

		return null;
	}

	/**
	 * Returns the model or the missing model if there isn't one
	 */
	@Nonnull
	public static IBakedModel getModel(final BlockState state, final BlockRendererDispatcher blockRendererDispatcher) {
		try (final ModProfiler ignored = ModProfiler.get().start("getModel")) {
//			if (DynamicTreesCompatibility.isRootyBlock(unextendedState)) {
//				return blockRendererDispatcher.getModelForState(StateHolder.GRASS_BLOCK_DEFAULT);
//			}
			if (ClientUtil.isStateSnow(state)) {
				return blockRendererDispatcher.getModelForState(StateHolder.SNOW_LAYER_DEFAULT);
			}
			return blockRendererDispatcher.getModelForState(state);
		}
	}

}
