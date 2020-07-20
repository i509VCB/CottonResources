package io.github.cottonmc.resources;

import io.github.cottonmc.resources.world.gen.CottonOreFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CottonResourcesMod implements ModInitializer {
	private static final Logger LOGGER = LogManager.getLogger("CottonResources");

	@Override
	public void onInitialize() {
		CottonResources.init();

		CommandRegistrationCallback.EVENT.register(CottonResourcesCommands::register);
		BuiltinResources.init();
		CottonOreFeatures.init();

		RegistryEntryAddedCallback.event(CottonResources.RESOURCE_TYPES).register((rawId, identifier, resourceType) -> {
			resourceType.registerAll();
		});
	}
}
