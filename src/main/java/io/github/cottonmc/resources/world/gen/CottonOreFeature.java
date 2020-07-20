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

package io.github.cottonmc.resources.world.gen;

import io.github.cottonmc.resources.CottonResources;
import io.github.cottonmc.resources.CottonResourcesTags;
import io.github.cottonmc.resources.oregen.OreGenerationSettings;
import io.github.cottonmc.resources.oregen.OreVoteConfig;
import io.github.cottonmc.resources.oregen.OregenResourceListener;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CottonOreFeature extends Feature<DefaultFeatureConfig> {
	public static final CottonOreFeature COTTON_ORE = Registry.register(Registry.FEATURE, "cotton:ore", new CottonOreFeature());

	public static Clump[] SPHERES = {
			Clump.of(1), Clump.of(2), Clump.of(3), Clump.of(4), Clump.of(5), Clump.of(6), Clump.of(7), Clump.of(8), Clump.of(9)
	};

	public CottonOreFeature() {
		super(DefaultFeatureConfig.CODEC);
	}

	@Override
	public boolean generate(ServerWorldAccess world, ChunkGenerator chunkGenerator, Random random, BlockPos pos, DefaultFeatureConfig featureConfig) {
		OreVoteConfig config = OregenResourceListener.getConfig();

		if (config.ores.isEmpty()) return true; // We have nothing to generate.

		Chunk toGenerateIn = world.getChunk(pos);

		BiomeArray biomeArray = toGenerateIn.getBiomeArray();

		if (biomeArray == null) {
			CottonResources.LOGGER.error("BiomeArray was null during generation.");
			return false; // I have no idea why this would be null but in the context of generating a chunk I am pretty sure the game would have this already.
		}

		final Biome biome = biomeArray.getBiomeForNoiseGen(pos.getX(), pos.getY(), pos.getZ());

		//CottonResources.LOGGER.error("Generating into "+toGenerateIn.getPos()+" <- "+config.ores);
		for (String s : config.ores) {
			OreGenerationSettings settings = config.generators.get(s);

			if (settings == null) continue;

			//For now, spit debug info
			if (settings.ores.isEmpty()) {
				//CottonResources.LOGGER.error("Empty ore settings");
				continue;
			}

			int clusters = settings.cluster_count;

			if (clusters < 1) clusters = 1;

			if (settings.cluster_size <= 0) settings.cluster_size = 1;

			int blocksGenerated = 0;

			for (int i = 0; i < clusters; i++) {
				//Pick an epicenter
				//int maxCluster = 7;
				int overbleed = 0; //Increase to allow ore deposits to overlap South/East chunks by this amount

				int radius = (int) Math.log(settings.cluster_size) + 1;
				if (radius > 7) radius = 7; //radius can't go past 7 without adding some overbleed

				for (int j = 0; j < SPHERES.length; j++) { //find the smallest clump in our vocabulary which expresses the number of ores
					Clump clump = SPHERES[j];

					if (clump.size() >= settings.cluster_size) {
						radius = j + 1;
						break;
					}
				}

				int clusterX = random.nextInt(16 + overbleed - (radius * 2)) + radius;
				int clusterZ = random.nextInt(16 + overbleed - (radius * 2)) + radius;
				int heightRange = settings.max_height - settings.min_height;

				if (heightRange < 1) heightRange = 1;

				int clusterY = random.nextInt(heightRange) + settings.min_height;

				clusterX += toGenerateIn.getPos().getStartX();
				clusterZ += toGenerateIn.getPos().getStartZ();

				int generatedThisCluster = generateVeinPartGaussianClump(s, world, clusterX, clusterY, clusterZ, settings.cluster_size, radius, settings.ores, 85, random);
				blocksGenerated += generatedThisCluster;
			}

		}

		return false;
	}

	protected int generateVeinPartGaussianClump(String resourceName, ServerWorldAccess world, int x, int y, int z, int clumpSize, int radius, Set<BlockState> states, int density, Random rand) {
		int radIndex = radius - 1;
		Clump clump = (radIndex < SPHERES.length) ? SPHERES[radIndex].copy() : Clump.of(radius);

		//int rad2 = radius * radius;
		BlockState[] blocks = states.toArray(new BlockState[states.size()]);
		int replaced = 0;

		for (int i = 0; i < clump.size(); i++) {
			if (clump.isEmpty()) break;
			BlockPos pos = clump.removeGaussian(rand, x, y, z);

			if (replace(world, pos.getX(), pos.getY(), pos.getZ(), resourceName, blocks, rand)) {
				replaced++;

				if (replaced >= clumpSize) return replaced;
			}
		}

		return replaced;
	}

	public boolean replace(ServerWorldAccess world, int x, int y, int z, String resource, BlockState[] states, Random rand) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockState toReplace = world.getBlockState(pos);
		HashMap<String, String> replacementSpecs = OregenResourceListener.getConfig().replacements.get(resource);

		if (replacementSpecs != null) {
			//CottonResources.LOGGER.debug("Activating replacementSpecs for resource "+resource);
			for (Map.Entry<String, String> entry : replacementSpecs.entrySet()) {
				if (test(toReplace.getBlock(), entry.getKey())) {
					BlockState replacement = getBlockState(entry.getValue(), rand);

					if (replacement == null) continue;

					world.setBlockState(pos, replacement, 3);
					return true;
				}
			}

			return false; //There are replacements defined for this resource, but none could be applied.
		} else {
			if (!CottonResourcesTags.NATURAL_STONES.contains(toReplace.getBlock())) return false; //Fixes surface copper

			BlockState replacement = states[rand.nextInt(states.length)];
			world.setBlockState(pos, replacement, 3);
			return true;
		}
	}

	public boolean test(Block block, String spec) {
		if (spec.startsWith("#")) {
			Tag<Block> tag = BlockTags.getTagGroup().getTagOrEmpty(new Identifier(spec.substring(1)));

			if (tag == null) return false;

			return tag.contains(block);
		} else {
			Block b = Registry.BLOCK.get(new Identifier(spec));

			if (b == Blocks.AIR) return false;

			return block == b;
		}
	}

	public BlockState getBlockState(String spec, Random rnd) {
		if (spec.startsWith("#")) {
			Tag<Block> tag = BlockTags.getTagGroup().getTagOrEmpty(new Identifier(spec.substring(1)));

			if (tag == null) return null;

			return tag.getRandom(rnd).getDefaultState();
		} else {
			Block b = Registry.BLOCK.get(new Identifier(spec));

			if (b == Blocks.AIR) return null;

			return b.getDefaultState();
		}
	}
}
