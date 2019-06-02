/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package io.github.cadiboo.nocubes.util;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class DistExecutor {

	private DistExecutor() {
	}

	/**
	 * Run the callable in the supplier only on the specified {@link Side}
	 *
	 * @param dist  The dist to run on
	 * @param toRun A supplier of the callable to run (Supplier wrapper to ensure classloading only on the appropriate dist)
	 * @param <T>   The return type from the callable
	 * @return The callable's result
	 */
	public static <T> T callWhenOn(Side dist, Supplier<Callable<T>> toRun) {
		if (dist == FMLCommonHandler.instance().getSide()) {
			try {
				return toRun.get().call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public static void runWhenOn(Side dist, Supplier<Runnable> toRun) {
		if (dist == FMLCommonHandler.instance().getSide()) {
			toRun.get().run();
		}
	}

	public static <T> T runForDist(Supplier<Supplier<T>> clientTarget, Supplier<Supplier<T>> serverTarget) {
		switch (FMLCommonHandler.instance().getSide()) {
			case CLIENT:
				return clientTarget.get().get();
			case SERVER: // DEDICATED_SERVER
				return serverTarget.get().get();
			default:
				throw new IllegalArgumentException("UNSIDED?");
		}
	}

}
