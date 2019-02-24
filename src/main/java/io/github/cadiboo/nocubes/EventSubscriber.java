package io.github.cadiboo.nocubes;

import io.github.cadiboo.nocubes.config.ModConfig;
import io.github.cadiboo.nocubes.util.FaceList;
import io.github.cadiboo.nocubes.util.ModReference;
import io.github.cadiboo.nocubes.util.ModUtil;
import io.github.cadiboo.nocubes.world.ModWorldEventListener;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static io.github.cadiboo.nocubes.util.ModUtil.TERRAIN_SMOOTHABLE;
import static net.minecraft.util.math.BlockPos.PooledMutableBlockPos;

/**
 * Subscribe to events that should be handled on both PHYSICAL sides in this class
 */
@Mod.EventBusSubscriber(modid = ModReference.MOD_ID)
public final class EventSubscriber {

	@SubscribeEvent
	public static void onWorldLoadEvent(final WorldEvent.Load event) {

		final IWorld iworld = event.getWorld();
		//WTF
		if (iworld instanceof World) {
			((World) iworld).addEventListener(new ModWorldEventListener());
		}

	}

	@SubscribeEvent
	public static void getCollisionBoxes(final GetCollisionBoxesEvent event) {
		event.getCollisionBoxesList().clear();

		final BlockPos entityPos = event.getEntity().getPosition();
		final int entityX = entityPos.getX();
		final int entityY = entityPos.getY();
		final int entityZ = entityPos.getZ();
		final IWorld world = event.getWorld();

		final float[] grid = new float[8];
		try (final PooledMutableBlockPos pos = PooledMutableBlockPos.retain()) {

			int gridIndex = 0;
			for (int zOffset = 0; zOffset < 2; ++zOffset) {
				for (int yOffset = 0; yOffset < 2; ++yOffset) {
					for (int xOffset = 0; xOffset < 2; ++xOffset, ++gridIndex) {
						pos.setPos(entityX + xOffset, entityY + yOffset, entityZ + zOffset);
						final IBlockState state = world.getBlockState(pos);
						final float p = ModUtil.getIndividualBlockDensity(TERRAIN_SMOOTHABLE.isSmoothable(state), state, world, pos);
						grid[gridIndex] = p;
					}
				}
			}

		}

		try (
				final FaceList faces = ModConfig.getMeshGenerator().generateBlock(new byte[]{
						(byte) ((entityX - (entityX >> 4)) << 4),
						(byte) ((entityY - (entityY >> 4)) << 4),
						(byte) ((entityZ - (entityZ >> 4)) << 4)
				}, grid)
		) {
			faces.forEach(
					event.getCollisionBoxesList().add(new AxisAlignedBB())
			);
		}

	}

}
