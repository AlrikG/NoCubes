package io.github.cadiboo.nocubes.client;

import io.github.cadiboo.nocubes.client.optifine.OptiFineCompatibility;
import io.github.cadiboo.nocubes.config.Config;
import io.github.cadiboo.nocubes.mesh.MeshGenerator;
import io.github.cadiboo.nocubes.util.ModProfiler;
import io.github.cadiboo.nocubes.util.pooled.cache.SmoothableCache;
import io.github.cadiboo.nocubes.util.pooled.cache.StateCache;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRender;
import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.client.renderer.chunk.ChunkRenderTask;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutableBlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.Region;
import net.minecraft.world.chunk.IChunk;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static io.github.cadiboo.nocubes.util.StateHolder.GRASS_BLOCK_DEFAULT;
import static io.github.cadiboo.nocubes.util.StateHolder.GRASS_BLOCK_SNOWY;
import static io.github.cadiboo.nocubes.util.StateHolder.PODZOL_SNOWY;
import static io.github.cadiboo.nocubes.util.StateHolder.SNOW_LAYER_DEFAULT;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static net.minecraft.util.BlockRenderLayer.CUTOUT;
import static net.minecraft.util.BlockRenderLayer.CUTOUT_MIPPED;
import static net.minecraft.util.math.MathHelper.clamp;

/**
 * Util that is only used on the Physical Client i.e. Rendering code
 *
 * @author Cadiboo
 */
@SuppressWarnings("WeakerAccess")
public final class ClientUtil {

	static final int[] NEGATIVE_1_8000 = new int[8000];
	private static final int[][] OFFSETS_ORDERED = {
			// check 6 immediate neighbours
			{+0, -1, +0},
			{+0, +1, +0},
			{-1, +0, +0},
			{+1, +0, +0},
			{+0, +0, -1},
			{+0, +0, +1},
			// check 12 non-immediate, non-corner neighbours
			{-1, -1, +0},
			{-1, +0, -1},
			{-1, +0, +1},
			{-1, +1, +0},
			{+0, -1, -1},
			{+0, -1, +1},
			// {+0, +0, +0}, // Don't check self
			{+0, +1, -1},
			{+0, +1, +1},
			{+1, -1, +0},
			{+1, +0, -1},
			{+1, +0, +1},
			{+1, +1, +0},
			// check 8 corner neighbours
			{+1, +1, +1},
			{+1, +1, -1},
			{-1, +1, +1},
			{-1, +1, -1},
			{+1, -1, +1},
			{+1, -1, -1},
			{-1, -1, +1},
			{-1, -1, -1},
	};
	static {
		Arrays.fill(ClientUtil.NEGATIVE_1_8000, -1);
	}

	/**
	 * @param red   the red value of the color, between 0x00 (decimal 0) and 0xFF (decimal 255)
	 * @param green the red value of the color, between 0x00 (decimal 0) and 0xFF (decimal 255)
	 * @param blue  the red value of the color, between 0x00 (decimal 0) and 0xFF (decimal 255)
	 * @return the color in ARGB format
	 */
	public static int colori(int red, int green, int blue) {

		red = clamp(red, 0x00, 0xFF);
		green = clamp(green, 0x00, 0xFF);
		blue = clamp(blue, 0x00, 0xFF);

		final int alpha = 0xFF;

		// 0x alpha red green blue
		// 0xaarrggbb

		// int colorRGBA = 0;
		// colorRGBA |= red << 16;
		// colorRGBA |= green << 8;
		// colorRGBA |= blue << 0;
		// colorRGBA |= alpha << 24;

		return blue | red << 16 | green << 8 | alpha << 24;

	}

	/**
	 * @param red   the red value of the color, 0F and 1F
	 * @param green the green value of the color, 0F and 1F
	 * @param blue  the blue value of the color, 0F and 1F
	 * @return the color in ARGB format
	 */
	public static int colorf(final float red, final float green, final float blue) {
		final int redInt = max(0, min(255, round(red * 255)));
		final int greenInt = max(0, min(255, round(green * 255)));
		final int blueInt = max(0, min(255, round(blue * 255)));
		return colori(redInt, greenInt, blueInt);
	}

	public static int getLightmapSkyLightCoordsFromPackedLightmapCoords(int packedLightmapCoords) {
		return (packedLightmapCoords >> 16) & 0xFFFF; // get upper 4 bytes
	}

	public static int getLightmapBlockLightCoordsFromPackedLightmapCoords(int packedLightmapCoords) {
		return packedLightmapCoords & 0xFFFF; // get lower 4 bytes
	}

	/**
	 * Returns a state and sets the texturePooledMutablePos to the pos it found
	 *
	 * @return a state
	 */
	@Nonnull
	public static BlockState getTexturePosAndState(
			final int posX, final int posY, final int posZ,
			@Nonnull final PooledMutableBlockPos texturePooledMutablePos,
			@Nonnull final StateCache stateCache,
			@Nonnull final SmoothableCache smoothableCache,
			final byte relativePosX, final byte relativePosY, final byte relativePosZ,
			final boolean tryForBetterTexturesSnow, final boolean tryForBetterTexturesGrass
	) {

		final boolean[] smoothableCacheArray = smoothableCache.getSmoothableCache();
		final BlockState[] blockCacheArray = stateCache.getBlockStates();

		final int stateStartPaddingX = stateCache.startPaddingX;
		final int stateStartPaddingY = stateCache.startPaddingY;
		final int stateStartPaddingZ = stateCache.startPaddingZ;

		final int smoothableStartPaddingX = smoothableCache.startPaddingX;
		final int smoothableStartPaddingY = smoothableCache.startPaddingY;
		final int smoothableStartPaddingZ = smoothableCache.startPaddingZ;


		if (Config.betterTextures) {
			if (tryForBetterTexturesSnow) {
				try (final ModProfiler ignored = ModProfiler.get().start("getTexturePosAndState-tryForBetterTextures-snow")) {
					BlockState betterTextureState = blockCacheArray[stateCache.getIndex(
							relativePosX + stateStartPaddingX,
							relativePosY + stateStartPaddingY,
							relativePosZ + stateStartPaddingZ
					)];

					if (isStateSnow(betterTextureState)) {
						texturePooledMutablePos.setPos(posX, posY, posZ);
						return betterTextureState;
					}
					for (int[] offset : OFFSETS_ORDERED) {
						betterTextureState = blockCacheArray[stateCache.getIndex(
								relativePosX + offset[0] + stateStartPaddingX,
								relativePosY + offset[1] + stateStartPaddingY,
								relativePosZ + offset[2] + stateStartPaddingZ
						)];
						if (isStateSnow(betterTextureState)) {
							texturePooledMutablePos.setPos(posX + offset[0], posY + offset[1], posZ + offset[2]);
							return betterTextureState;
						}
					}
				}
			}
			if (tryForBetterTexturesGrass) {
				try (final ModProfiler ignored = ModProfiler.get().start("getTexturePosAndState-tryForBetterTextures-grass")) {
					BlockState betterTextureState = blockCacheArray[stateCache.getIndex(
							relativePosX + stateStartPaddingX,
							relativePosY + stateStartPaddingY,
							relativePosZ + stateStartPaddingZ
					)];

					if (isStateGrass(betterTextureState)) {
						texturePooledMutablePos.setPos(posX, posY, posZ);
						return betterTextureState;
					}
					for (int[] offset : OFFSETS_ORDERED) {
						betterTextureState = blockCacheArray[stateCache.getIndex(
								relativePosX + offset[0] + stateStartPaddingX,
								relativePosY + offset[1] + stateStartPaddingY,
								relativePosZ + offset[2] + stateStartPaddingZ
						)];
						if (isStateGrass(betterTextureState)) {
							texturePooledMutablePos.setPos(posX + offset[0], posY + offset[1], posZ + offset[2]);
							return betterTextureState;
						}
					}
				}
			}
		}

		try (final ModProfiler ignored = ModProfiler.get().start("getTexturePosAndState")) {

			// If pos passed in is smoothable return state from that pos
			if (smoothableCacheArray[smoothableCache.getIndex(
					relativePosX + smoothableStartPaddingX,
					relativePosY + smoothableStartPaddingY,
					relativePosZ + smoothableStartPaddingZ
			)]) {
				texturePooledMutablePos.setPos(posX, posY, posZ);
				return blockCacheArray[stateCache.getIndex(
						relativePosX + stateStartPaddingX,
						relativePosY + stateStartPaddingY,
						relativePosZ + stateStartPaddingZ
				)];
			}

			// Start at state of pos passed in
			BlockState state = blockCacheArray[stateCache.getIndex(
					relativePosX + stateStartPaddingX,
					relativePosY + stateStartPaddingY,
					relativePosZ + stateStartPaddingZ
			)];

			for (int[] offset : OFFSETS_ORDERED) {
				if (smoothableCacheArray[smoothableCache.getIndex(
						relativePosX + offset[0] + smoothableStartPaddingX,
						relativePosY + offset[1] + smoothableStartPaddingY,
						relativePosZ + offset[2] + smoothableStartPaddingZ
				)]) {
					texturePooledMutablePos.setPos(posX + offset[0], posY + offset[1], posZ + offset[2]);
					state = blockCacheArray[stateCache.getIndex(
							relativePosX + offset[0] + stateStartPaddingX,
							relativePosY + offset[1] + stateStartPaddingY,
							relativePosZ + offset[2] + stateStartPaddingZ
					)];
					break;
				}
			}
			return state;
		}
	}

	public static boolean isStateSnow(final BlockState checkState) {
		if (checkState == SNOW_LAYER_DEFAULT) return true;
		if (checkState == GRASS_BLOCK_SNOWY) return true;
		return checkState == PODZOL_SNOWY;
	}

	private static boolean isStateGrass(final BlockState checkState) {
		return checkState == GRASS_BLOCK_DEFAULT;
	}

	@Nonnull
	public static BlockRenderLayer getCorrectRenderLayer(@Nonnull final BlockState state) {
		return getCorrectRenderLayer(state.getBlock().getRenderLayer());
	}

	@Nonnull
	public static BlockRenderLayer getCorrectRenderLayer(@Nonnull final IFluidState state) {
		return getCorrectRenderLayer(state.getRenderLayer());
	}

	@Nonnull
	public static BlockRenderLayer getCorrectRenderLayer(@Nonnull final BlockRenderLayer blockRenderLayer) {
		switch (blockRenderLayer) {
			default:
			case SOLID:
			case TRANSLUCENT:
				return blockRenderLayer;
			case CUTOUT_MIPPED:
				return Minecraft.getInstance().gameSettings.mipmapLevels == 0 ? CUTOUT : CUTOUT_MIPPED;
			case CUTOUT:
				return Minecraft.getInstance().gameSettings.mipmapLevels != 0 ? CUTOUT_MIPPED : CUTOUT;
		}
	}

	public static BufferBuilder startOrContinueBufferBuilder(final ChunkRenderTask generator, final int blockRenderLayerOrdinal, final CompiledChunk compiledChunk, final BlockRenderLayer blockRenderLayer, ChunkRender renderChunk, BlockPos renderChunkPosition) {
		final BufferBuilder bufferBuilder = generator.getRegionRenderCacheBuilder().getBuilder(blockRenderLayerOrdinal);
		if (!compiledChunk.isLayerStarted(blockRenderLayer)) {
			compiledChunk.setLayerStarted(blockRenderLayer);
			renderChunk.preRenderBlocks(bufferBuilder, renderChunkPosition);
		}
		return bufferBuilder;
	}

	public static void tryReloadRenderers() {
		final WorldRenderer worldRenderer = Minecraft.getInstance().worldRenderer;
		if (worldRenderer != null) {
			worldRenderer.loadRenderers();
		}
	}

	/**
	 * @param chunkPos the chunk position as a {@link BlockPos}
	 * @param blockPos the {@link BlockPos}
	 * @return the position relative to the chunkPos
	 */
	public static byte getRelativePos(final int chunkPos, final int blockPos) {
		final int blockPosChunkPos = (blockPos >> 4) << 4;
		if (chunkPos == blockPosChunkPos) { // if blockpos is in chunkpos's chunk
			return getRelativePos(blockPos);
		} else {
			// can be anything. usually between -1 and 16
			return (byte) (blockPos - chunkPos);
		}
	}

	/**
	 * @param blockPos the {@link BlockPos}
	 * @return the position (between 0-15) relative to the blockPos's chunk position
	 */
	public static byte getRelativePos(final int blockPos) {
		return (byte) (blockPos & 15);
	}

	public static IChunk getChunk(final int currentChunkPosX, final int currentChunkPosZ, final IEnviromentBlockReader reader) {
//		if (reader instanceof IWorld) { // This should never be the case...
//			return ((IWorld) reader).getChunk(currentChunkPosX, currentChunkPosZ);
//		} else
		if (reader instanceof ChunkRenderCache) {
			ChunkRenderCache renderChunkCache = (ChunkRenderCache) reader;
			final int x = currentChunkPosX - renderChunkCache.chunkStartX;
			final int z = currentChunkPosZ - renderChunkCache.chunkStartZ;
			return renderChunkCache.chunks[x][z];
		} else if (OptiFineCompatibility.isChunkCacheOF(reader)) {
			Region region = OptiFineCompatibility.getRegion(reader);
			final int x = currentChunkPosX - region.chunkX;
			final int z = currentChunkPosZ - region.chunkZ;
			return region.chunks[x][z];
		}
		throw new IllegalStateException("Should Not Reach Here!");
	}

	/**
	 * We add 1 because idk (it fixes seams in between chunks)
	 * and then surface nets needs another +1 because reasons
	 */
	public static byte getMeshSizeX(final int initialSize, final MeshGenerator meshGenerator) {
		return (byte) (initialSize + meshGenerator.getSizeXExtension());
	}

	/**
	 * We add 1 because idk (it fixes seams in between chunks)
	 * and then surface nets needs another +1 because reasons
	 */
	public static byte getMeshSizeY(final int initialSize, final MeshGenerator meshGenerator) {
		return (byte) (initialSize + meshGenerator.getSizeYExtension());
	}

	/**
	 * We add 1 because idk (it fixes seams in between chunks)
	 * and then surface nets needs another +1 because reasons
	 */
	public static byte getMeshSizeZ(final int initialSize, final MeshGenerator meshGenerator) {
		return (byte) (initialSize + meshGenerator.getSizeZExtension());
	}

}
