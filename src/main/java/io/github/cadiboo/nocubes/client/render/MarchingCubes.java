package io.github.cadiboo.nocubes.client.render;

import io.github.cadiboo.nocubes.client.ClientUtil;
import io.github.cadiboo.nocubes.config.ModConfig;
import io.github.cadiboo.nocubes.util.ModUtil;
import io.github.cadiboo.nocubes.util.Vec3;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkBlockEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkBlockRenderInLayerEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkBlockRenderInTypeEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkPostEvent;
import io.github.cadiboo.renderchunkrebuildchunkhooks.event.RebuildChunkPreEvent;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * Implementation of the MarchingCubes algorithm in Minecraft
 *
 * @author Cadiboo
 * @see "http://mikolalysenko.github.io/Isosurface/js/marchingcubes.js"
 */
public final class MarchingCubes {

	private static final int[] EDGE_TABLE = {
			0x0, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
			0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
			0x190, 0x99, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
			0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
			0x230, 0x339, 0x33, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
			0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
			0x3a0, 0x2a9, 0x1a3, 0xaa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
			0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
			0x460, 0x569, 0x663, 0x76a, 0x66, 0x16f, 0x265, 0x36c,
			0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
			0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff, 0x3f5, 0x2fc,
			0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
			0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55, 0x15c,
			0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
			0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc,
			0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
			0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
			0xcc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
			0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
			0x15c, 0x55, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
			0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
			0x2fc, 0x3f5, 0xff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
			0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
			0x36c, 0x265, 0x16f, 0x66, 0x76a, 0x663, 0x569, 0x460,
			0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
			0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa, 0x1a3, 0x2a9, 0x3a0,
			0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
			0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33, 0x339, 0x230,
			0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
			0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99, 0x190,
			0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
			0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0
	};

	private static final int[][] TRIANGLE_TABLE = {
			{},
			{0, 8, 3},
			{0, 1, 9},
			{1, 8, 3, 9, 8, 1},
			{1, 2, 10},
			{0, 8, 3, 1, 2, 10},
			{9, 2, 10, 0, 2, 9},
			{2, 8, 3, 2, 10, 8, 10, 9, 8},
			{3, 11, 2},
			{0, 11, 2, 8, 11, 0},
			{1, 9, 0, 2, 3, 11},
			{1, 11, 2, 1, 9, 11, 9, 8, 11},
			{3, 10, 1, 11, 10, 3},
			{0, 10, 1, 0, 8, 10, 8, 11, 10},
			{3, 9, 0, 3, 11, 9, 11, 10, 9},
			{9, 8, 10, 10, 8, 11},
			{4, 7, 8},
			{4, 3, 0, 7, 3, 4},
			{0, 1, 9, 8, 4, 7},
			{4, 1, 9, 4, 7, 1, 7, 3, 1},
			{1, 2, 10, 8, 4, 7},
			{3, 4, 7, 3, 0, 4, 1, 2, 10},
			{9, 2, 10, 9, 0, 2, 8, 4, 7},
			{2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4},
			{8, 4, 7, 3, 11, 2},
			{11, 4, 7, 11, 2, 4, 2, 0, 4},
			{9, 0, 1, 8, 4, 7, 2, 3, 11},
			{4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1},
			{3, 10, 1, 3, 11, 10, 7, 8, 4},
			{1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4},
			{4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3},
			{4, 7, 11, 4, 11, 9, 9, 11, 10},
			{9, 5, 4},
			{9, 5, 4, 0, 8, 3},
			{0, 5, 4, 1, 5, 0},
			{8, 5, 4, 8, 3, 5, 3, 1, 5},
			{1, 2, 10, 9, 5, 4},
			{3, 0, 8, 1, 2, 10, 4, 9, 5},
			{5, 2, 10, 5, 4, 2, 4, 0, 2},
			{2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8},
			{9, 5, 4, 2, 3, 11},
			{0, 11, 2, 0, 8, 11, 4, 9, 5},
			{0, 5, 4, 0, 1, 5, 2, 3, 11},
			{2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5},
			{10, 3, 11, 10, 1, 3, 9, 5, 4},
			{4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10},
			{5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3},
			{5, 4, 8, 5, 8, 10, 10, 8, 11},
			{9, 7, 8, 5, 7, 9},
			{9, 3, 0, 9, 5, 3, 5, 7, 3},
			{0, 7, 8, 0, 1, 7, 1, 5, 7},
			{1, 5, 3, 3, 5, 7},
			{9, 7, 8, 9, 5, 7, 10, 1, 2},
			{10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3},
			{8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2},
			{2, 10, 5, 2, 5, 3, 3, 5, 7},
			{7, 9, 5, 7, 8, 9, 3, 11, 2},
			{9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11},
			{2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7},
			{11, 2, 1, 11, 1, 7, 7, 1, 5},
			{9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11},
			{5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0},
			{11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0},
			{11, 10, 5, 7, 11, 5},
			{10, 6, 5},
			{0, 8, 3, 5, 10, 6},
			{9, 0, 1, 5, 10, 6},
			{1, 8, 3, 1, 9, 8, 5, 10, 6},
			{1, 6, 5, 2, 6, 1},
			{1, 6, 5, 1, 2, 6, 3, 0, 8},
			{9, 6, 5, 9, 0, 6, 0, 2, 6},
			{5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8},
			{2, 3, 11, 10, 6, 5},
			{11, 0, 8, 11, 2, 0, 10, 6, 5},
			{0, 1, 9, 2, 3, 11, 5, 10, 6},
			{5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11},
			{6, 3, 11, 6, 5, 3, 5, 1, 3},
			{0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6},
			{3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9},
			{6, 5, 9, 6, 9, 11, 11, 9, 8},
			{5, 10, 6, 4, 7, 8},
			{4, 3, 0, 4, 7, 3, 6, 5, 10},
			{1, 9, 0, 5, 10, 6, 8, 4, 7},
			{10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4},
			{6, 1, 2, 6, 5, 1, 4, 7, 8},
			{1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7},
			{8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6},
			{7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9},
			{3, 11, 2, 7, 8, 4, 10, 6, 5},
			{5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11},
			{0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6},
			{9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6},
			{8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6},
			{5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11},
			{0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7},
			{6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9},
			{10, 4, 9, 6, 4, 10},
			{4, 10, 6, 4, 9, 10, 0, 8, 3},
			{10, 0, 1, 10, 6, 0, 6, 4, 0},
			{8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10},
			{1, 4, 9, 1, 2, 4, 2, 6, 4},
			{3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4},
			{0, 2, 4, 4, 2, 6},
			{8, 3, 2, 8, 2, 4, 4, 2, 6},
			{10, 4, 9, 10, 6, 4, 11, 2, 3},
			{0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6},
			{3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10},
			{6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1},
			{9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3},
			{8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1},
			{3, 11, 6, 3, 6, 0, 0, 6, 4},
			{6, 4, 8, 11, 6, 8},
			{7, 10, 6, 7, 8, 10, 8, 9, 10},
			{0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10},
			{10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0},
			{10, 6, 7, 10, 7, 1, 1, 7, 3},
			{1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7},
			{2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9},
			{7, 8, 0, 7, 0, 6, 6, 0, 2},
			{7, 3, 2, 6, 7, 2},
			{2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7},
			{2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7},
			{1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11},
			{11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1},
			{8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6},
			{0, 9, 1, 11, 6, 7},
			{7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0},
			{7, 11, 6},
			{7, 6, 11},
			{3, 0, 8, 11, 7, 6},
			{0, 1, 9, 11, 7, 6},
			{8, 1, 9, 8, 3, 1, 11, 7, 6},
			{10, 1, 2, 6, 11, 7},
			{1, 2, 10, 3, 0, 8, 6, 11, 7},
			{2, 9, 0, 2, 10, 9, 6, 11, 7},
			{6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8},
			{7, 2, 3, 6, 2, 7},
			{7, 0, 8, 7, 6, 0, 6, 2, 0},
			{2, 7, 6, 2, 3, 7, 0, 1, 9},
			{1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6},
			{10, 7, 6, 10, 1, 7, 1, 3, 7},
			{10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8},
			{0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7},
			{7, 6, 10, 7, 10, 8, 8, 10, 9},
			{6, 8, 4, 11, 8, 6},
			{3, 6, 11, 3, 0, 6, 0, 4, 6},
			{8, 6, 11, 8, 4, 6, 9, 0, 1},
			{9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6},
			{6, 8, 4, 6, 11, 8, 2, 10, 1},
			{1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6},
			{4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9},
			{10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3},
			{8, 2, 3, 8, 4, 2, 4, 6, 2},
			{0, 4, 2, 4, 6, 2},
			{1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8},
			{1, 9, 4, 1, 4, 2, 2, 4, 6},
			{8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1},
			{10, 1, 0, 10, 0, 6, 6, 0, 4},
			{4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3},
			{10, 9, 4, 6, 10, 4},
			{4, 9, 5, 7, 6, 11},
			{0, 8, 3, 4, 9, 5, 11, 7, 6},
			{5, 0, 1, 5, 4, 0, 7, 6, 11},
			{11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5},
			{9, 5, 4, 10, 1, 2, 7, 6, 11},
			{6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5},
			{7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2},
			{3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6},
			{7, 2, 3, 7, 6, 2, 5, 4, 9},
			{9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7},
			{3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0},
			{6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8},
			{9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7},
			{1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4},
			{4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10},
			{7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10},
			{6, 9, 5, 6, 11, 9, 11, 8, 9},
			{3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5},
			{0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11},
			{6, 11, 3, 6, 3, 5, 5, 3, 1},
			{1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6},
			{0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10},
			{11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5},
			{6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3},
			{5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2},
			{9, 5, 6, 9, 6, 0, 0, 6, 2},
			{1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8},
			{1, 5, 6, 2, 1, 6},
			{1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6},
			{10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0},
			{0, 3, 8, 5, 6, 10},
			{10, 5, 6},
			{11, 5, 10, 7, 5, 11},
			{11, 5, 10, 11, 7, 5, 8, 3, 0},
			{5, 11, 7, 5, 10, 11, 1, 9, 0},
			{10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1},
			{11, 1, 2, 11, 7, 1, 7, 5, 1},
			{0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11},
			{9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7},
			{7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2},
			{2, 5, 10, 2, 3, 5, 3, 7, 5},
			{8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5},
			{9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2},
			{9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2},
			{1, 3, 5, 3, 7, 5},
			{0, 8, 7, 0, 7, 1, 1, 7, 5},
			{9, 0, 3, 9, 3, 5, 5, 3, 7},
			{9, 8, 7, 5, 9, 7},
			{5, 8, 4, 5, 10, 8, 10, 11, 8},
			{5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0},
			{0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5},
			{10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4},
			{2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8},
			{0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11},
			{0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5},
			{9, 4, 5, 2, 11, 3},
			{2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4},
			{5, 10, 2, 5, 2, 4, 4, 2, 0},
			{3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9},
			{5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2},
			{8, 4, 5, 8, 5, 3, 3, 5, 1},
			{0, 4, 5, 1, 0, 5},
			{8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5},
			{9, 4, 5},
			{4, 11, 7, 4, 9, 11, 9, 10, 11},
			{0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11},
			{1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11},
			{3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4},
			{4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2},
			{9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3},
			{11, 7, 4, 11, 4, 2, 2, 4, 0},
			{11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4},
			{2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9},
			{9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7},
			{3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10},
			{1, 10, 2, 8, 7, 4},
			{4, 9, 1, 4, 1, 7, 7, 1, 3},
			{4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1},
			{4, 0, 3, 7, 4, 3},
			{4, 8, 7},
			{9, 10, 8, 10, 11, 8},
			{3, 0, 9, 3, 9, 11, 11, 9, 10},
			{0, 1, 10, 0, 10, 8, 8, 10, 11},
			{3, 1, 10, 11, 3, 10},
			{1, 2, 11, 1, 11, 9, 9, 11, 8},
			{3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9},
			{0, 2, 11, 8, 0, 11},
			{3, 2, 11},
			{2, 3, 8, 2, 8, 10, 10, 8, 9},
			{9, 10, 2, 0, 9, 2},
			{2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8},
			{1, 10, 2},
			{1, 3, 8, 9, 1, 8},
			{0, 9, 1},
			{0, 3, 8},
			{}
	};

	private static final int[][] CUBE_VERTS = {
			{0, 0, 0},
			{1, 0, 0},
			{1, 1, 0},
			{0, 1, 0},
			{0, 0, 1},
			{1, 0, 1},
			{1, 1, 1},
			{0, 1, 1}
	};
	private static final int[][] EDGE_INDEX = {
			{0, 1},
			{1, 2},
			{2, 3},
			{3, 0},
			{4, 5},
			{5, 6},
			{6, 7},
			{7, 4},
			{0, 4},
			{1, 5},
			{2, 6},
			{3, 7}
	};

	public static void renderPre(final RebuildChunkPreEvent event) {

		final BlockPos renderChunkPos = event.getRenderChunkPosition();
		final RenderChunk renderChunk = event.getRenderChunk();
		final int[] c = {renderChunkPos.getX(), renderChunkPos.getY(), renderChunkPos.getZ()};
		final int[] x = {0, 0, 0};
		final float[] grid = new float[8];
		final int[] edges = new int[12];
		final int[] dims = {16, 16, 16};
		int n = 0;
		final ArrayList<float[]> vertices = new ArrayList<>();

		final BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
		final BlockPos.PooledMutableBlockPos pooledMutablePos = BlockPos.PooledMutableBlockPos.retain();
		final ChunkCache cache = event.getChunkCache();

		//March over the volume
		for (x[2] = 0; x[2] < dims[2]; ++x[2], n += dims[0]) {
			for (x[1] = 0; x[1] < dims[1]; ++x[1], ++n) {
				for (x[0] = 0; x[0] < dims[0]; ++x[0], ++n) {
					pos.setPos(c[0] + x[0], c[1] + x[1], c[2] + x[2]);
					//For each cell, compute cube mask
					int cube_index = 0;
					for (int i = 0; i < 8; ++i) {
						int[] v = CUBE_VERTS[i];
//						float s = data[n + v[0] + dims[0] * (v[1] + dims[1] * v[2])];
						pooledMutablePos.setPos(c[0] + x[0] + v[0], c[1] + x[1] + v[1], c[2] + x[2] + v[2]);
						float s = ModUtil.getBlockDensity(pooledMutablePos, cache);
						grid[i] = s;
						cube_index |= (s > 0) ? 1 << i : 0;
					}
					//Compute vertices
					int edge_mask = EDGE_TABLE[cube_index];
					if (edge_mask == 0) {
						continue;
					}
					for (int i = 0; i < 12; ++i) {
						if ((edge_mask & (1 << i)) == 0) {
							continue;
						}
						edges[i] = vertices.size();
						float[] nv = {0, 0, 0};
						int[] e = EDGE_INDEX[i];
						int[] p0 = CUBE_VERTS[e[0]], p1 = CUBE_VERTS[e[1]];
						float a = grid[e[0]], b = grid[e[1]], d = a - b, t = 0;
						if (Math.abs(d) > 1e-6) {
							t = a / d;
						}
						for (int j = 0; j < 3; ++j) {
							nv[j] = (c[j] + x[j] + p0[j]) + t * (p1[j] - p0[j]);
						}
						if (ModConfig.offsetVertices)
							ModUtil.offsetVertex(nv);
						vertices.add(nv);
					}

					final BlockRenderData renderData = ClientUtil.getBlockRenderData(pos, cache);

					final BlockRenderLayer blockRenderLayer = renderData.getBlockRenderLayer();
					final int red = renderData.getRed();
					final int green = renderData.getGreen();
					final int blue = renderData.getBlue();
					final int alpha = renderData.getAlpha();
					final float minU = renderData.getMinU();
					final float maxU = renderData.getMaxU();
					final float minV = renderData.getMinV();
					final float maxV = renderData.getMaxV();
					final int lightmapSkyLight = renderData.getLightmapSkyLight();
					final int lightmapBlockLight = renderData.getLightmapBlockLight();

					final BufferBuilder bufferBuilder = event.getGenerator().getRegionRenderCacheBuilder().getWorldRendererByLayer(blockRenderLayer);
					final CompiledChunk compiledChunk = event.getCompiledChunk();

					if (!compiledChunk.isLayerStarted(blockRenderLayer)) {
						compiledChunk.setLayerStarted(blockRenderLayer);
						ClientUtil.compiledChunk_setLayerUsed(compiledChunk, blockRenderLayer);
						ClientUtil.renderChunk_preRenderBlocks(renderChunk, bufferBuilder, renderChunkPos);
					}

					//Add faces
					int[] f = TRIANGLE_TABLE[cube_index];
					for (int i = 0; i < f.length; i += 3) {
//						faces.push({edges[f[i]], edges[f[i+1]], edges[f[i+2]]});
//						final float[] vert0 = vertices.get(edges[f[i]]), vert1 = vertices.get(edges[f[i + 1]]), vert2 = vertices.get(edges[f[i + 2]]);
						// legit wtf cunt why do I have to swap them???
						final float[] vert0 = vertices.get(edges[f[i + 2]]), vert1 = vertices.get(edges[f[i + 1]]), vert2 = vertices.get(edges[f[i]]);

						final Vec3 vertex0 = new Vec3(vert0),
								vertex1 = new Vec3(vert1),
								vertex2 = new Vec3(vert2);

						//pretend they're quads & try and get good textures
						if (i % 6 == 0) {
							//bottom right triangle (if facing north)
							bufferBuilder.pos(vertex0.xCoord, vertex0.yCoord, vertex0.zCoord).color(red, green, blue, alpha).tex(maxU, maxV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
							bufferBuilder.pos(vertex0.xCoord, vertex0.yCoord, vertex0.zCoord).color(red, green, blue, alpha).tex(maxU, maxV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
							bufferBuilder.pos(vertex1.xCoord, vertex1.yCoord, vertex1.zCoord).color(red, green, blue, alpha).tex(maxU, minV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
							bufferBuilder.pos(vertex2.xCoord, vertex2.yCoord, vertex2.zCoord).color(red, green, blue, alpha).tex(minU, maxV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
						} else {
							//top left triangle (if facing north)
							bufferBuilder.pos(vertex0.xCoord, vertex0.yCoord, vertex0.zCoord).color(red, green, blue, alpha).tex(minU, maxV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
							bufferBuilder.pos(vertex0.xCoord, vertex0.yCoord, vertex0.zCoord).color(red, green, blue, alpha).tex(minU, maxV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
							bufferBuilder.pos(vertex1.xCoord, vertex1.yCoord, vertex1.zCoord).color(red, green, blue, alpha).tex(maxU, minV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
							bufferBuilder.pos(vertex2.xCoord, vertex2.yCoord, vertex2.zCoord).color(red, green, blue, alpha).tex(minU, minV).lightmap(lightmapSkyLight, lightmapBlockLight).endVertex();
						}
					}

				}
			}
		}
		pos.release();
		pooledMutablePos.release();
	}

	public static void renderLayer(final RebuildChunkBlockRenderInLayerEvent event) {

//		if (ModUtil.shouldSmooth(event.getBlockState())) {
//			event.setResult(Event.Result.DENY);
//			event.setCanceled(true);
//		}

	}

	public static void renderType(final RebuildChunkBlockRenderInTypeEvent event) {

//		if (ModUtil.shouldSmooth(event.getBlockState())) {
//			event.setResult(Event.Result.DENY);
//			event.setCanceled(true);
//		}

	}

	public static void renderBlock(final RebuildChunkBlockEvent event) {

		event.setCanceled(ModUtil.shouldSmooth(event.getBlockState()));

	}

	@Nullable
	public static Vec3[] getPoints(BlockPos pooledMutablePos, IBlockAccess world) {
		return null;
	}

	public static void renderPost(final RebuildChunkPostEvent event) {

	}

}