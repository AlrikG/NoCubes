package io.github.cadiboo.nocubes.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.github.cadiboo.nocubes.util.ModReference.MOD_ID;

/**
 * Manages the registry of the of network packets (it does more than this but everything's handled automagically by forge)
 *
 * @author Cadiboo
 */
public final class ModNetworkManager {

	public static final String CHANNEL = StringUtils.substring(MOD_ID, 0, 20);
	public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
	private static final Logger LOGGER = LogManager.getLogger();
	static {
		if (CHANNEL.length() != MOD_ID.length()) {
			// Network Channel CAN'T be longer than 20 characters due to Minecraft & Forge's packet system
			LOGGER.error("Network Channel was clamped to 20 characters! {}, {}", CHANNEL, MOD_ID);
		}
	}

	public ModNetworkManager() {
		int networkIds = 0;

		/* Client -> Server */
//		NETWORK.registerMessage(CPacket___.class, CPacket___.class, networkIds++, Side.SERVER);
		/* Server -> Client */
//		NETWORK.registerMessage(SPacket___.class, SPacket___.class, networkIds++, Side.CLIENT);

	}

}
