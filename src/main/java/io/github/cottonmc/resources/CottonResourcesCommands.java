package io.github.cottonmc.resources;

import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

final class CottonResourcesCommands {
	static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
		CottonResourcesCommands.createStripCommand(dispatcher, dedicated);
	}

	private static void createStripCommand(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
		final RootCommandNode<ServerCommandSource> root = dispatcher.getRoot();
		final LiteralCommandNode<ServerCommandSource> strip = literal("strip")
				.requires(CottonResourcesCommands::levelThreeOperator)
				.executes(CottonResourcesCommands::executeStripCommand)
				.build();

		root.addChild(strip);
	}

	private static int executeStripCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		final ServerCommandSource source = context.getSource();
		final ServerPlayerEntity player = source.getPlayer();
		final ServerWorld world = source.getWorld();
		final Chunk chunk = world.getChunk(player.getBlockPos());

		// TODO: Translatable text?
		// TODO: CUI-Like Highlight mode?
		source.sendFeedback(new LiteralText("Stripping " + chunk.getPos() + "..."), true);

		// Lets be efficient, skip empty chunk sections
		final int height = (chunk.getHighestNonEmptySectionYOffset() + 1) * 16;
		final BlockPos chunkBlockPos = chunk.getPos().getCenterBlockPos();
		final BlockPos.Mutable pos = chunkBlockPos.mutableCopy();

		for (int y = height; y > 0; y--) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					BlockState toReplace = world.getBlockState(pos.set(chunkBlockPos.getX() + x, chunkBlockPos.getY() + y, chunkBlockPos.getZ() + z));

					// Skip air-like blocks
					if (toReplace.isAir()) {
						continue;
					}

					if (CottonResourcesTags.STRIP_COMMAND.contains(toReplace.getBlock())) {
						world.setBlockState(pos, Blocks.AIR.getDefaultState());
					}
				}
			}
		}

		context.getSource().sendFeedback(new LiteralText("Chunk stripped."), true);

		return 1;
	}

	private static boolean levelThreeOperator(ServerCommandSource source) {
		return source.hasPermissionLevel(3);
	}

	private CottonResourcesCommands() {
	}
}
