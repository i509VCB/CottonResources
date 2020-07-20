/*
 * MIT License
 *
 * Copyright (c) 2018-2020 The Cotton Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.cottonmc.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.mojang.serialization.Lifecycle;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.cottonmc.resources.config.CottonResourcesConfig;
import io.github.cottonmc.resources.oregen.OreGenerationSettings;
import io.github.cottonmc.resources.type.ResourceType;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

public class CottonResources {
	public static final String COMMON = "c";
	public static final String MODID = "cotton-resources";
	public static final Logger LOGGER = LogManager.getLogger("CottonResources");
	public static CottonResourcesConfig CONFIG = new CottonResourcesConfig(); //ConfigManager.loadConfig(CottonResourcesConfig.class);
	public static final RegistryKey<Registry<ResourceType>> RESOURCE_TYPE_KEY = RegistryKey.ofRegistry(id("resource_types"));
	public static final Registry<ResourceType> RESOURCE_TYPES = new SimpleRegistry<>(RESOURCE_TYPE_KEY, Lifecycle.experimental()); // TODO: Mark stable at release
	public static final Jankson JANKSON = JanksonFactory.builder()
			.registerTypeAdapter(OreGenerationSettings.class, OreGenerationSettings::deserialize)
			.build();

	public static ItemGroup ITEM_GROUP = FabricItemGroupBuilder.build(CottonResources.id("resources"), () -> new ItemStack(BuiltinResources.COPPER.getGear().orElseThrow(IllegalStateException::new)));
	public static Item.Settings ITEM_GROUP_SETTINGS = new Item.Settings().group(CottonResources.ITEM_GROUP);

	private static final String[] MACHINE_AFFIXES = new String[]{"gear", "plate"};

	public static SoundEvent METAL_STEP_SOUND;
	public static BlockSoundGroup METAL_SOUND_GROUP;

	static void init() {
		METAL_STEP_SOUND = Registry.register(Registry.SOUND_EVENT, "block.cotton-resources.metal.step", new SoundEvent(CottonResources.common("block.cotton-resources.metal.step")));
		METAL_SOUND_GROUP = new BlockSoundGroup(1.0F, 1.5F, SoundEvents.BLOCK_METAL_BREAK, METAL_STEP_SOUND, SoundEvents.BLOCK_METAL_PLACE, SoundEvents.BLOCK_METAL_HIT, SoundEvents.BLOCK_METAL_FALL);

		CottonResources.RESOURCE_TYPES.stream().forEach(ResourceType::registerAll);

		// Track new registrations and register them.

		File file = new File(FabricLoader.getInstance().getConfigDirectory(), "CottonResources.json5");

		if (file.exists()) {
			CONFIG = loadConfig();
		}

		saveConfig(CONFIG);
	}

	private static CottonResourcesConfig loadConfig() {
		File file = new File(FabricLoader.getInstance().getConfigDirectory(), "CottonResources.json5");

		try {
			JsonObject json = JANKSON.load(file);
			CottonResources.LOGGER.info("Loading: " + json);
			CottonResourcesConfig loading = JANKSON.fromJson(json, CottonResourcesConfig.class);
			CottonResources.LOGGER.info("Loaded Map: " + loading.generators);
			//Manually reload oregen because BiomeSpec and DimensionSpec can be fussy

			JsonObject oregen = json.getObject("generators");

			if (oregen != null) {
				CottonResources.LOGGER.info("RELOADING " + oregen.size() + " entries");

				for (Map.Entry<String, JsonElement> entry : oregen.entrySet()) {
					if (entry.getValue() instanceof JsonObject) {
						OreGenerationSettings settings = OreGenerationSettings.deserialize((JsonObject) entry.getValue());
						loading.generators.put(entry.getKey(), settings);
					}
				}
			}

			CottonResources.LOGGER.info("RELOADED Map: " + loading.generators);

			return loading;
		} catch (IOException | SyntaxError e) {
			e.printStackTrace();
		}

		return new CottonResourcesConfig();
	}

	private static void saveConfig(CottonResourcesConfig config) {
		File file = new File(FabricLoader.getInstance().getConfigDirectory(), "CottonResources.json5");

		JsonElement json = JANKSON.toJson(config);

		try (FileOutputStream out = new FileOutputStream(file, false)) {
			out.write(json.toJson(JsonGrammar.JSON5).getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			LOGGER.error("Could not write config", ex);
		}
	}

	/**
	 * Creates an identifier within the {@code common} namespace.
	 *
	 * <p>The common namespace is {@code c}.
	 *
	 * @param path the path
	 * @return an identifier
	 */
	public static Identifier common(String path) {
		return new Identifier(CottonResources.COMMON, path);
	}

	/**
	 * Creates an identifier within Cotton Resources' namespace.
	 *
	 * @param path the path
	 * @return an identifier
	 */
	public static Identifier id(String path) {
		return new Identifier(CottonResources.MODID, path);
	}
}
