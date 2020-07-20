package io.github.cottonmc.resources;

import net.fabricmc.fabric.api.tag.TagRegistry;

import net.minecraft.block.Block;
import net.minecraft.tag.Tag;

public final class CottonResourcesTags {
	/**
	 * This tag will be replaced with an Identified type in the future.
	 */
	@Deprecated
	public static final Tag<Block> NATURAL_STONES = TagRegistry.block(CottonResources.common("natural_stones"));
	/**
	 * This tag will be replaced with an Identified type in the future.
	 */
	@Deprecated
	public static final Tag<Block> STRIP_COMMAND = TagRegistry.block(CottonResources.common("strip_command"));
}
