package me.devtec.theapi.bukkit.nms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftContainer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import me.devtec.shared.Pair;
import me.devtec.shared.Ref;
import me.devtec.shared.components.ClickEvent;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.components.ComponentEntity;
import me.devtec.shared.components.ComponentItem;
import me.devtec.shared.components.HoverEvent;
import me.devtec.shared.events.EventManager;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.events.ServerListPingEvent;
import me.devtec.theapi.bukkit.game.BlockDataStorage;
import me.devtec.theapi.bukkit.gui.AnvilGUI;
import me.devtec.theapi.bukkit.gui.GUI.ClickType;
import me.devtec.theapi.bukkit.gui.HolderGUI;
import me.devtec.theapi.bukkit.nms.GameProfileHandler.PropertyHandler;
import me.devtec.theapi.bukkit.nms.utils.InventoryUtils;
import me.devtec.theapi.bukkit.nms.utils.InventoryUtils.DestinationType;
import me.devtec.theapi.bukkit.packetlistener.PacketContainer;
import me.devtec.theapi.bukkit.xseries.XMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.HoverEvent.EntityTooltipInfo;
import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerStatus.Favicon;
import net.minecraft.network.protocol.status.ServerStatus.Players;
import net.minecraft.network.protocol.status.ServerStatus.Version;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class v1_20_6 implements NmsProvider {
	private static final MinecraftServer server = MinecraftServer.getServer();
	private static final net.minecraft.network.chat.Component empty = net.minecraft.network.chat.Component.literal("");
	private static final CommandBuildContext dispatcher = Commands.createValidationContext(VanillaRegistries.createLookup());

	@Override
	public Collection<? extends Player> getOnlinePlayers() {
		return Bukkit.getOnlinePlayers();
	}

	@Override
	public Object getEntity(Entity entity) {
		return ((CraftEntity) entity).getHandle();
	}

	@Override
	public Object getEntityLiving(LivingEntity entity) {
		return ((CraftLivingEntity) entity).getHandle();
	}

	@Override
	public Object getPlayer(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	@Override
	public Object getWorld(World world) {
		return ((CraftWorld) world).getHandle();
	}

	@Override
	public Object getChunk(Chunk chunk) {
		return ((CraftChunk) chunk).getHandle(ChunkStatus.FULL);
	}

	@Override
	public int getEntityId(Object entity) {
		return ((net.minecraft.world.entity.Entity) entity).getId();
	}

	@Override
	public Object getScoreboardAction(Action type) {
		return type == Action.CHANGE ? ClientboundSetPlayerTeamPacket.Action.ADD : ClientboundSetPlayerTeamPacket.Action.REMOVE;
	}

	@Override
	public Object getEnumScoreboardHealthDisplay(DisplayType type) {
		return type == DisplayType.INTEGER ? ObjectiveCriteria.RenderType.INTEGER : ObjectiveCriteria.RenderType.HEARTS;
	}

	@Override
	public Object getNBT(ItemStack itemStack) {
		net.minecraft.world.item.ItemStack item = (net.minecraft.world.item.ItemStack) asNMSItem(itemStack);
		if (item.isEmpty())
			return new CompoundTag();
		CustomData data = item.get(DataComponents.CUSTOM_DATA);
		if (data != null)
			return data.copyTag();
		return null;
	}

	@Override
	public Object parseNBT(String json) {
		if (json == null)
			return new CompoundTag();
		try {
			return TagParser.parseTag(json);
		} catch (Exception e) {
			return new CompoundTag();
		}
	}

	@Override
	public ItemStack setNBT(ItemStack stack, Object nbt) {
		if (nbt instanceof NBTEdit)
			nbt = ((NBTEdit) nbt).getNBT();
		net.minecraft.world.item.ItemStack item = (net.minecraft.world.item.ItemStack) asNMSItem(stack);
		item.set(DataComponents.CUSTOM_DATA, CustomData.of((CompoundTag) nbt));
		return asBukkitItem(item);
	}

	@Override
	public Object asNMSItem(ItemStack stack) {
		if (stack == null)
			return net.minecraft.world.item.ItemStack.EMPTY;
		return CraftItemStack.asNMSCopy(stack);
	}

	@Override
	public ItemStack asBukkitItem(Object stack) {
		return CraftItemStack.asBukkitCopy(stack == null ? net.minecraft.world.item.ItemStack.EMPTY : (net.minecraft.world.item.ItemStack) stack);
	}

	@Override
	public int getContainerId(Object container) {
		return ((AbstractContainerMenu) container).containerId;
	}

	@Override
	public Object packetResourcePackSend(String url, String hash, boolean requireRP, Component prompt) {
		return new ClientboundResourcePackPushPacket(UUID.randomUUID(), url, hash, requireRP,
				prompt == null ? null : prompt == null ? Optional.empty() : Optional.of((net.minecraft.network.chat.Component) this.toIChatBaseComponent(prompt)));
	}

	@Override
	public Object packetSetSlot(int container, int slot, int changeId, Object itemStack) {
		return new ClientboundContainerSetSlotPacket(container, changeId, slot, (net.minecraft.world.item.ItemStack) (itemStack == null ? asNMSItem(null) : itemStack));
	}

	@Override
	public Object packetEntityMetadata(int entityId, Object dataWatcher, boolean bal) {
		return new ClientboundSetEntityDataPacket(entityId, ((SynchedEntityData) dataWatcher).packAll());
	}

	@Override
	public Object packetEntityDestroy(int... ids) {
		return new ClientboundRemoveEntitiesPacket(ids);
	}

	@Override
	public Object packetSpawnEntity(Object entity, int id) {
		return new ClientboundAddEntityPacket((net.minecraft.world.entity.Entity) entity, id);
	}

	@Override
	public Object packetNamedEntitySpawn(Object player) {
		return new ClientboundAddEntityPacket((net.minecraft.world.entity.player.Player) player);
	}

	@Override
	public Object packetSpawnEntityLiving(Object entityLiving) {
		return new ClientboundAddEntityPacket((net.minecraft.world.entity.LivingEntity) entityLiving);
	}

	@Override
	public Object packetPlayerListHeaderFooter(Component header, Component footer) {
		return new ClientboundTabListPacket((net.minecraft.network.chat.Component) toIChatBaseComponent(header), (net.minecraft.network.chat.Component) this.toIChatBaseComponent(footer));
	}

	@Override
	public Object packetBlockChange(int x, int y, int z, Object iblockdata, int data) {
		return new ClientboundBlockUpdatePacket(new BlockPos(x, y, z), iblockdata == null ? Blocks.AIR.defaultBlockState() : (net.minecraft.world.level.block.state.BlockState) iblockdata);
	}

	@Override
	public Object packetScoreboardObjective() {
		return Ref.newUnsafeInstance(ClientboundSetObjectivePacket.class);
	}

	@Override
	public Object packetScoreboardDisplayObjective(int id, Object scoreboardObjective) {
		return new ClientboundSetDisplayObjectivePacket(DisplaySlot.values()[id], scoreboardObjective == null ? null : (Objective) scoreboardObjective);
	}

	@Override
	public Object packetScoreboardTeam() {
		return Ref.newUnsafeInstance(ClientboundSetPlayerTeamPacket.class);
	}

	@Override
	public Object packetScoreboardScore(Action action, String player, String line, int score) {
		return new ClientboundSetScorePacket(line, player, score, Optional.ofNullable(null), Optional.ofNullable(null));
	}

	@Override
	public Object packetTitle(TitleAction action, Component text, int fadeIn, int stay, int fadeOut) {
		switch (action) {
		case ACTIONBAR:
			return new ClientboundSetActionBarTextPacket((net.minecraft.network.chat.Component) this.toIChatBaseComponent(text));
		case TITLE:
			return new ClientboundSetTitleTextPacket((net.minecraft.network.chat.Component) this.toIChatBaseComponent(text));
		case SUBTITLE:
			return new ClientboundSetSubtitleTextPacket((net.minecraft.network.chat.Component) this.toIChatBaseComponent(text));
		case TIMES:
			return new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
		case CLEAR:
		case RESET:
			return new ClientboundClearTitlesPacket(true);
		}
		return null;
	}

	@Override
	public Object packetChat(ChatType type, Object chatBase, UUID uuid) {
		return new ClientboundSystemChatPacket((net.minecraft.network.chat.Component) chatBase, false);
	}

	@Override
	public Object packetChat(ChatType type, Component text, UUID uuid) {
		return new ClientboundSystemChatPacket((net.minecraft.network.chat.Component) this.toIChatBaseComponent(text), false);
	}

	@Override
	public void postToMainThread(Runnable runnable) {
		v1_20_6.server.execute(runnable);
	}

	@Override
	public Object getMinecraftServer() {
		return v1_20_6.server;
	}

	@Override
	public Thread getServerThread() {
		return v1_20_6.server.serverThread;
	}

	@SuppressWarnings("removal")
	@Override
	public double[] getServerTPS() {
		return v1_20_6.server.recentTps;
	}

	private net.minecraft.network.chat.Component convert(Component c) {
		if (c instanceof ComponentItem || c instanceof ComponentEntity)
			return net.minecraft.network.chat.Component.Serializer.fromJson(Json.writer().simpleWrite(c.toJsonMap()), dispatcher);
		MutableComponent current = net.minecraft.network.chat.Component.literal(c.getText());
		Style modif = current.getStyle();
		if (c.getColor() != null && !c.getColor().isEmpty())
			if (c.getColor().startsWith("#"))
				modif = modif.withColor(TextColor.fromRgb(Integer.decode(c.getColor())));
			else
				modif = modif.withColor(ChatFormatting.getByCode(c.colorToChar()));
		if (c.getClickEvent() != null)
			modif = modif.withClickEvent(
					new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.valueOf(c.getClickEvent().getAction().name()), c.getClickEvent().getValue()));
		if (c.getHoverEvent() != null)
			switch (c.getHoverEvent().getAction()) {
			case SHOW_ENTITY:
				try {
					ComponentEntity compoundTag = (ComponentEntity) c.getHoverEvent().getValue();
					net.minecraft.network.chat.Component component = compoundTag.getName() == null ? null : (net.minecraft.network.chat.Component) toIChatBaseComponent(compoundTag.getName());
					EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(compoundTag.getType()));
					modif = modif.withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY,
							new net.minecraft.network.chat.HoverEvent.EntityTooltipInfo(entityType, compoundTag.getId(), component)));
				} catch (Exception commandSyntaxException) {
				}
				break;
			case SHOW_ITEM:
				try {

					ComponentItem compoundTag = (ComponentItem) c.getHoverEvent().getValue();
					net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(
							CraftMagicNumbers.getItem(XMaterial.matchXMaterial(compoundTag.getId()).orElse(XMaterial.AIR).parseMaterial()), compoundTag.getCount());
					if (compoundTag.getNbt() != null) {
						CompoundTag nbt = (CompoundTag) parseNBT(compoundTag.getNbt());
						if (!nbt.contains("id"))
							nbt.putString("id",
									BuiltInRegistries.ITEM.getKey(CraftMagicNumbers.getItem(XMaterial.matchXMaterial(compoundTag.getId()).orElse(XMaterial.AIR).parseMaterial())).toString());
						if (!nbt.contains("count"))
							nbt.putInt("count", compoundTag.getCount());
						stack = net.minecraft.world.item.ItemStack.parseOptional(dispatcher, nbt);
					}
					modif = modif.withHoverEvent(
							new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM, new net.minecraft.network.chat.HoverEvent.ItemStackInfo(stack)));
				} catch (Exception commandSyntaxException) {
				}
				break;
			default:
				modif = modif.withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
						(net.minecraft.network.chat.Component) this.toIChatBaseComponent(c.getHoverEvent().getValue())));
				break;
			}
		modif = modif.withBold(c.isBold());
		modif = modif.withItalic(c.isItalic());
		modif = modif.withObfuscated(c.isObfuscated());
		modif = modif.withUnderlined(c.isUnderlined());
		modif = modif.withStrikethrough(c.isStrikethrough());
		current.setStyle(modif);
		return current;
	}

	@Override
	public Object[] toIChatBaseComponents(List<Component> components) {
		List<net.minecraft.network.chat.Component> chat = new ArrayList<>();
		chat.add(net.minecraft.network.chat.Component.literal(""));
		for (Component c : components) {
			if (c.getText() == null || c.getText().isEmpty()) {
				if (c.getExtra() != null)
					addConverted(chat, c.getExtra());
				continue;
			}
			chat.add(convert(c));
			if (c.getExtra() != null)
				addConverted(chat, c.getExtra());
		}
		return chat.toArray(new net.minecraft.network.chat.Component[0]);
	}

	private void addConverted(List<net.minecraft.network.chat.Component> chat, List<Component> extra) {
		for (Component c : extra) {
			if (c.getText() == null || c.getText().isEmpty()) {
				if (c.getExtra() != null)
					addConverted(chat, c.getExtra());
				continue;
			}
			chat.add(convert(c));
			if (c.getExtra() != null)
				addConverted(chat, c.getExtra());
		}
	}

	@Override
	public Object[] toIChatBaseComponents(Component co) {
		if (co == null)
			return new net.minecraft.network.chat.Component[] { empty };
		if (co instanceof ComponentItem || co instanceof ComponentEntity)
			return new net.minecraft.network.chat.Component[] { net.minecraft.network.chat.Component.Serializer.fromJson(Json.writer().simpleWrite(co.toJsonMap()), dispatcher) };
		List<net.minecraft.network.chat.Component> chat = new ArrayList<>();
		chat.add(net.minecraft.network.chat.Component.literal(""));
		if (co.getText() != null && !co.getText().isEmpty())
			chat.add(convert(co));
		if (co.getExtra() != null)
			for (Component c : co.getExtra()) {
				if (c.getText() == null || c.getText().isEmpty()) {
					if (c.getExtra() != null)
						addConverted(chat, c.getExtra());
					continue;
				}
				chat.add(convert(c));
				if (c.getExtra() != null)
					addConverted(chat, c.getExtra());
			}
		return chat.toArray(new net.minecraft.network.chat.Component[0]);
	}

	@Override
	public Object toIChatBaseComponent(Component co) {
		if (co == null)
			return empty;
		if (co instanceof ComponentItem || co instanceof ComponentEntity)
			return net.minecraft.network.chat.Component.Serializer.fromJson(Json.writer().simpleWrite(co.toJsonMap()), dispatcher);
		MutableComponent main = net.minecraft.network.chat.Component.literal("");
		List<net.minecraft.network.chat.Component> chat = new ArrayList<>();
		if (co.getText() != null && !co.getText().isEmpty())
			chat.add(convert(co));
		if (co.getExtra() != null)
			for (Component c : co.getExtra()) {
				if (c.getText() == null || c.getText().isEmpty()) {
					if (c.getExtra() != null)
						addConverted(chat, c.getExtra());
					continue;
				}
				chat.add(convert(c));
				if (c.getExtra() != null)
					addConverted(chat, c.getExtra());
			}
		main.getSiblings().addAll(chat);
		return main.getSiblings().isEmpty() ? empty : main;
	}

	@Override
	public Object toIChatBaseComponent(List<Component> cc) {
		MutableComponent main = net.minecraft.network.chat.Component.literal("");
		for (Component c : cc)
			main.getSiblings().add((net.minecraft.network.chat.Component) this.toIChatBaseComponent(c));
		return main.getSiblings().isEmpty() ? empty : main;
	}

	@Override
	public Object chatBase(String json) {
		return net.minecraft.network.chat.Component.Serializer.fromJson(json, dispatcher);
	}

	@Override
	public Component fromIChatBaseComponent(Object componentObject) {
		if (componentObject == null)
			return Component.EMPTY_COMPONENT;
		net.minecraft.network.chat.Component component = (net.minecraft.network.chat.Component) componentObject;
		Object result = Ref.invoke(component.getContents(), "text");
		Component comp = new Component(result == null ? "" : (String) result);
		Style modif = component.getStyle();
		if (modif.getColor() != null)
			comp.setColor(modif.getColor().serialize());

		if (modif.getClickEvent() != null)
			comp.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(modif.getClickEvent().getAction().name()), modif.getClickEvent().getValue()));

		if (modif.getHoverEvent() != null)
			switch (HoverEvent.Action.valueOf(modif.getHoverEvent().getAction().getSerializedName().toUpperCase())) {
			case SHOW_ENTITY: {
				EntityTooltipInfo hover = modif.getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY);
				ComponentEntity compEntity = new ComponentEntity(hover.type.toString(), hover.id);
				if (hover.name.isPresent())
					compEntity.setName(fromIChatBaseComponent(hover.name.get()));
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, compEntity));
				break;
			}
			case SHOW_ITEM: {
				ItemStackInfo hover = modif.getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM);
				ComponentItem compEntity = new ComponentItem(CraftMagicNumbers.getMaterial(hover.getItemStack().getItem()).name(), hover.getItemStack().getCount());
				if (hover.getItemStack().getTags() != null)
					compEntity.setNbt(hover.getItemStack().save(dispatcher).toString());
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, compEntity));
				break;
			}
			default:
				net.minecraft.network.chat.Component hover = modif.getHoverEvent().getValue(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT);
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, fromIChatBaseComponent(hover)));
				break;
			}
		comp.setBold(modif.isBold());
		comp.setItalic(modif.isItalic());
		comp.setObfuscated(modif.isObfuscated());
		comp.setUnderlined(modif.isUnderlined());
		comp.setStrikethrough(modif.isStrikethrough());

		if (!component.getSiblings().isEmpty()) {
			List<Component> extra = new ArrayList<>();
			for (net.minecraft.network.chat.Component base : component.getSiblings())
				extra.add(fromIChatBaseComponent(base));
			comp.setExtra(extra);
		}
		return comp;
	}

	@Override
	public BlockDataStorage toMaterial(Object blockOrIBlockData) {
		if (blockOrIBlockData instanceof Block) {
			net.minecraft.world.level.block.state.BlockState data = ((Block) blockOrIBlockData).defaultBlockState();
			return new BlockDataStorage(CraftMagicNumbers.getMaterial(data.getBlock()), (byte) 0, asString(data));
		}
		if (blockOrIBlockData instanceof net.minecraft.world.level.block.state.BlockState) {
			net.minecraft.world.level.block.state.BlockState data = (net.minecraft.world.level.block.state.BlockState) blockOrIBlockData;
			return new BlockDataStorage(CraftMagicNumbers.getMaterial(data.getBlock()), (byte) 0, asString(data));
		}
		return new BlockDataStorage(Material.AIR);
	}

	private String asString(net.minecraft.world.level.block.state.BlockState data) {
		StringBuilder stateString = new StringBuilder();
		if (!data.getProperties().isEmpty()) {
			stateString.append('[');
			stateString.append(data.getValues().entrySet().stream().map(StateHolder.PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
			stateString.append(']');
		}
		return stateString.toString();
	}

	@Override
	public Object toIBlockData(BlockDataStorage material) {
		if (material == null || material.getType() == null || material.getType() == Material.AIR)
			return Blocks.AIR.defaultBlockState();
		Block block = CraftMagicNumbers.getBlock(material.getType());
		return readArgument(block, material);
	}

	@Override
	public Object toBlock(BlockDataStorage material) {
		if (material == null || material.getType() == null || material.getType() == Material.AIR)
			return Blocks.AIR;
		Block block = CraftMagicNumbers.getBlock(material.getType());
		return readArgument(block, material).getBlock();
	}

	private net.minecraft.world.level.block.state.BlockState readArgument(Block block, BlockDataStorage material) {
		net.minecraft.world.level.block.state.BlockState ib = block.defaultBlockState();
		return writeData(ib, ib.getBlock().getStateDefinition(), material.getData());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static net.minecraft.world.level.block.state.BlockState writeData(net.minecraft.world.level.block.state.BlockState ib, StateDefinition blockStateList, String string) {
		if (string == null || string.trim().isEmpty())
			return ib;

		String key = "";
		String value = "";
		int set = 0;

		for (int i = 1; i < string.length() - 1; ++i) {
			char c = string.charAt(i);
			if (c == ',') {
				net.minecraft.world.level.block.state.properties.Property ibj = blockStateList.getProperty(key);
				if (ibj != null) {
					Optional optional = ibj.getValue(value);
					if (optional.isPresent())
						ib = ib.setValue(ibj, (Comparable) optional.get());
				}
				key = "";
				value = "";
				set = 0;
				continue;
			}
			if (c == '=') {
				set = 1;
				continue;
			}
			if (set == 0)
				key += c;
			else
				value += c;
		}
		if (set == 1) {
			net.minecraft.world.level.block.state.properties.Property ibj = blockStateList.getProperty(key);
			if (ibj != null) {
				Optional optional = ibj.getValue(value);
				if (optional.isPresent())
					ib = ib.setValue(ibj, (Comparable) optional.get());
			}
		}
		return ib;
	}

	@Override
	public ItemStack toItemStack(BlockDataStorage material) {
		Item item = CraftMagicNumbers.getItem(material.getType(), ParseUtils.getShort(material.getData()));
		return CraftItemStack.asBukkitCopy(item.getDefaultInstance());
	}

	@Override
	public Object getChunk(World world, int x, int z) {
		return ((CraftChunk) world.getChunkAt(x, z)).getHandle(ChunkStatus.FULL);
	}

	@Override
	public void setBlock(Object objChunk, int x, int y, int z, Object IblockData, int data) {
		LevelChunk chunk = (LevelChunk) objChunk;
		ServerLevel world = chunk.level;
		int highY = chunk.getSectionIndex(y);
		if (highY < 0)
			return;
		LevelChunkSection sc = chunk.getSection(highY);
		if (sc == null)
			return;
		BlockPos pos = new BlockPos(x, y, z);

		net.minecraft.world.level.block.state.BlockState iblock = IblockData == null ? Blocks.AIR.defaultBlockState() : (net.minecraft.world.level.block.state.BlockState) IblockData;

		boolean onlyModifyState = iblock.getBlock() instanceof EntityBlock;

		// REMOVE TILE ENTITY IF NOT SAME TYPE
		BlockEntity ent = chunk.blockEntities.get(pos);
		if (ent != null) {
			boolean shouldSkip = true;
			if (!onlyModifyState)
				shouldSkip = false;
			else if (onlyModifyState && !ent.getType().isValid(iblock)) {
				shouldSkip = false;
				onlyModifyState = false;
			}
			if (!shouldSkip)
				chunk.removeBlockEntity(pos);
		}

		net.minecraft.world.level.block.state.BlockState old = sc.setBlockState(x & 15, y & 15, z & 15, iblock, false);

		// ADD TILE ENTITY
		if (iblock.getBlock() instanceof EntityBlock && !onlyModifyState) {
			ent = ((EntityBlock) iblock.getBlock()).newBlockEntity(pos, iblock);
			chunk.blockEntities.put(pos, ent);
			ent.setLevel(world);
			Object packet = ent.getUpdatePacket();
			BukkitLoader.getPacketHandler().send(chunk.level.getWorld().getPlayers(), packet);
		}

		// MARK CHUNK TO SAVE
		chunk.setUnsaved(true);

		// POI
		if (!world.preventPoiUpdated)
			world.onBlockStateChange(pos, old, iblock);
	}

	@Override
	public void updatePhysics(Object objChunk, int x, int y, int z, Object iblockdata) {
		LevelChunk chunk = (LevelChunk) objChunk;
		BlockPos blockPos = new BlockPos(x, y, z);
		doPhysicsAround(chunk.level, blockPos, ((net.minecraft.world.level.block.state.BlockState) iblockdata).getBlock());
	}

	private void doPhysicsAround(ServerLevel world, BlockPos BlockPos, Block block) {
		doPhysics(world, BlockPos.west(), block, BlockPos); // west
		doPhysics(world, BlockPos.east(), block, BlockPos); // east
		doPhysics(world, BlockPos.below(), block, BlockPos); // down
		doPhysics(world, BlockPos.above(), block, BlockPos); // up
		doPhysics(world, BlockPos.north(), block, BlockPos); // north
		doPhysics(world, BlockPos.south(), block, BlockPos); // south
	}

	private static final Method callPhysics = Ref.method(FallingBlock.class, "onPlace", net.minecraft.world.level.block.state.BlockState.class, Level.class, BlockPos.class,
			net.minecraft.world.level.block.state.BlockState.class, boolean.class);

	private void doPhysics(ServerLevel world, BlockPos BlockPos, Block block, BlockPos BlockPos1) {

		net.minecraft.world.level.block.state.BlockState state = world.getBlockState(BlockPos);
		state.handleNeighborChanged(world, BlockPos, block, BlockPos1, false);
		if (state.getBlock() instanceof FallingBlock)
			Ref.invoke(state.getBlock(), callPhysics, state, world, BlockPos, block.defaultBlockState(), false);
	}

	@Override
	public void updateLightAt(Object objChunk, int x, int y, int z) {
		LevelChunk chunk = (LevelChunk) objChunk;
		chunk.level.getChunkSource().getLightEngine().checkBlock(new BlockPos(x, y, z));
	}

	@Override
	public Object getBlock(Object objChunk, int x, int y, int z) {
		LevelChunk chunk = (LevelChunk) objChunk;
		return chunk.getBlockState(x, y, z);
	}

	@Override
	public byte getData(Object chunk, int x, int y, int z) {
		return 0;
	}

	@Override
	public String getNBTOfTile(Object objChunk, int x, int y, int z) {
		LevelChunk chunk = (LevelChunk) objChunk;
		return chunk.getBlockEntity(new BlockPos(x, y, z), EntityCreationType.IMMEDIATE).saveWithFullMetadata(dispatcher).toString();
	}

	@Override
	public void setNBTToTile(Object objChunk, int x, int y, int z, String nbt) {
		LevelChunk chunk = (LevelChunk) objChunk;
		BlockEntity ent = chunk.getBlockEntity(new BlockPos(x, y, z), EntityCreationType.IMMEDIATE);
		CompoundTag parsedNbt = (CompoundTag) parseNBT(nbt);
		parsedNbt.putInt("x", x);
		parsedNbt.putInt("y", y);
		parsedNbt.putInt("z", z);
		ent.loadWithComponents(parsedNbt, dispatcher);
		Object packet = ent.getUpdatePacket();
		BukkitLoader.getPacketHandler().send(chunk.level.getWorld().getPlayers(), packet);
	}

	@Override
	public boolean isTileEntity(Object objChunk, int x, int y, int z) {
		LevelChunk chunk = (LevelChunk) objChunk;
		return chunk.getBlockEntity(new BlockPos(x, y, z), EntityCreationType.IMMEDIATE) != null;
	}

	@Override
	public int getCombinedId(Object IblockDataOrBlock) {
		return Block.getId((net.minecraft.world.level.block.state.BlockState) IblockDataOrBlock);
	}

	@Override
	public Object blockPosition(int blockX, int blockY, int blockZ) {
		return new BlockPos(blockX, blockY, blockZ);
	}

	@Override
	public Object toIBlockData(Object data) {
		return ((CraftBlockData) data).getState();
	}

	@Override
	public Object toIBlockData(BlockState state) {
		return CraftMagicNumbers.getBlock(state.getType(), state.getRawData());
	}

	@Override
	public Chunk toBukkitChunk(Object nmsChunk) {
		return new CraftChunk((LevelChunk) nmsChunk);
	}

	@Override
	public int getPing(Player player) {
		return ((ServerGamePacketListenerImpl) getPlayerConnection(player)).latency();
	}

	@Override
	public Object getPlayerConnection(Player player) {
		return ((ServerPlayer) getPlayer(player)).connection;
	}

	private static Field networkManagerField = Ref.field(ServerCommonPacketListenerImpl.class, Connection.class);

	@Override
	public Object getConnectionNetwork(Object playercon) {
		return Ref.get(playercon, networkManagerField);
	}

	@Override
	public Object getNetworkChannel(Object network) {
		return ((Connection) network).channel;
	}

	@Override
	public Object packetOpenWindow(int id, String legacy, int size, Component title) {

		MenuType<?> windowType = MenuType.GENERIC_9x1;
		switch (size) {
		case 0: {
			windowType = MenuType.ANVIL;
			break;
		}
		case 18: {
			windowType = MenuType.GENERIC_9x2;
			break;
		}
		case 27: {
			windowType = MenuType.GENERIC_9x3;
			break;
		}
		case 36: {
			windowType = MenuType.GENERIC_9x4;
			break;
		}
		case 45: {
			windowType = MenuType.GENERIC_9x5;
			break;
		}
		case 54: {
			windowType = MenuType.GENERIC_9x6;
			break;
		}
		}
		return new ClientboundOpenScreenPacket(id, windowType, (net.minecraft.network.chat.Component) this.toIChatBaseComponent(title));
	}

	@Override
	public void closeGUI(Player player, Object container, boolean closePacket) {
		if (closePacket)
			BukkitLoader.getPacketHandler().send(player, new ClientboundContainerClosePacket(((AbstractContainerMenu) container).containerId));
		net.minecraft.world.entity.player.Player nmsPlayer = (net.minecraft.world.entity.player.Player) getPlayer(player);
		nmsPlayer.containerMenu = nmsPlayer.inventoryMenu;
		((AbstractContainerMenu) container).transferTo(nmsPlayer.containerMenu, (CraftPlayer) player);
	}

	@Override
	public void setSlot(Object container, int slot, Object item) {
		((AbstractContainerMenu) container).setItem(slot, ((AbstractContainerMenu) container).getStateId(), (net.minecraft.world.item.ItemStack) item);
	}

	@Override
	public void setGUITitle(Player player, Object container, String legacy, int size, Component title) {
		int id = ((AbstractContainerMenu) container).containerId;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, legacy, size, title));
		net.minecraft.world.item.ItemStack carried = ((AbstractContainerMenu) container).getCarried();
		if (!carried.isEmpty())
			BukkitLoader.getPacketHandler().send(player, new ClientboundContainerSetSlotPacket(id, getContainerStateId(container), -1, carried));
		int slot = 0;
		for (net.minecraft.world.item.ItemStack item : ((AbstractContainerMenu) container).getItems()) {
			if (slot == size)
				break;
			if (!item.isEmpty())
				BukkitLoader.getPacketHandler().send(player, new ClientboundContainerSetSlotPacket(id, getContainerStateId(container), slot, item));
			++slot;
		}
	}

	@Override
	public void openGUI(Player player, Object container, String legacy, int size, Component title) {
		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		int id = ((AbstractContainerMenu) container).containerId;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, legacy, size, title));
		nmsPlayer.containerMenu.transferTo((AbstractContainerMenu) container, (CraftPlayer) player);
		nmsPlayer.containerMenu = (AbstractContainerMenu) container;
		postToMainThread(() -> nmsPlayer.initMenu((AbstractContainerMenu) container));
		((AbstractContainerMenu) container).checkReachable = false;
	}

	@Override
	public void openAnvilGUI(Player player, Object container, Component title) {
		openGUI(player, container, "minecraft:anvil", 0, title);
	}

	@Override
	public Object createContainer(Inventory inv, Player player) {
		if (inv.getType() == InventoryType.ANVIL) {
			AnvilMenu container = new AnvilMenu(((CraftPlayer) player).getHandle().nextContainerCounter(), ((CraftPlayer) player).getHandle().getInventory(), new ContainerLevelAccess() {

				@Override
				public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> getter) {
					return Optional.empty();
				}

				@Override
				public Location getLocation() {
					return null;
				}
			});
			postToMainThread(() -> {
				int slot = 0;
				for (ItemStack stack : inv.getContents())
					container.getSlot(slot++).set((net.minecraft.world.item.ItemStack) asNMSItem(stack));
			});
			container.checkReachable = false;
			return container;
		}
		return new CraftContainer(inv, ((CraftPlayer) player).getHandle(), ((CraftPlayer) player).getHandle().nextContainerCounter());
	}

	@Override
	public Object getSlotItem(Object container, int slot) {
		return slot < 0 ? null : ((AbstractContainerMenu) container).getSlot(slot).getItem();
	}

	@Override
	public String getAnvilRenameText(Object anvil) {
		return ((AnvilMenu) anvil).itemName;
	}

	public static int c(final int quickCraftData) {
		return quickCraftData >> 2 & 0x3;
	}

	public static int d(final int quickCraftData) {
		return quickCraftData & 0x3;
	}

	@Override
	public boolean processInvClickPacket(Player player, HolderGUI gui, Object provPacket) {
		ServerboundContainerClickPacket packet = (ServerboundContainerClickPacket) provPacket;
		int slot = packet.getSlotNum();

		Object container = gui.getContainer(player);
		if (container == null)
			return false;

		int id = packet.getContainerId();
		int mouseClick = packet.getButtonNum();
		net.minecraft.world.inventory.ClickType type = packet.getClickType();
		AbstractContainerMenu c = (AbstractContainerMenu) container;

		if (slot < -1 && slot != -999)
			return true;

		net.minecraft.world.entity.player.Player nPlayer = ((CraftPlayer) player).getHandle();

		ItemStack newItem;
		ItemStack oldItem;
		switch (type) {
		case PICKUP: // PICKUP
			oldItem = asBukkitItem(getSlotItem(container, slot));
			newItem = asBukkitItem(c.getCarried());
			if (slot > 0 && mouseClick != 0) {
				if (c.getCarried().isEmpty()) { // pickup half
					newItem = oldItem.clone();
					if (oldItem.getAmount() == 1)
						newItem = new ItemStack(Material.AIR);
					else
						newItem.setAmount(Math.max(1, oldItem.getAmount() / 2));
				} else
				// drop
				if (oldItem.isSimilar(newItem) || oldItem.getType() == Material.AIR)
					newItem.setAmount(oldItem.getType() == Material.AIR ? 1 : oldItem.getAmount() + 1);
			} else if (slot > 0 && mouseClick == 0) // drop
				if (oldItem.isSimilar(newItem))
					newItem.setAmount(Math.min(newItem.getAmount() + oldItem.getAmount(), newItem.getMaxStackSize()));
			break;
		case QUICK_MOVE: // QUICK_MOVE
			newItem = asBukkitItem(c.getCarried());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case SWAP:// SWAP
			newItem = asBukkitItem(nPlayer.getInventory().getItem(mouseClick));
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case CLONE:// CLONE
			newItem = asBukkitItem(getSlotItem(container, slot));
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case THROW:// THROW
			if (c.getCarried().isEmpty() && slot >= 0) {
				Slot slot3 = c.getSlot(slot);
				newItem = asBukkitItem(slot3.getItem());
				if (mouseClick != 0 || newItem.getAmount() - 1 <= 0)
					newItem = new ItemStack(Material.AIR);
				else
					newItem.setAmount(newItem.getAmount() - 1);
			} else
				newItem = asBukkitItem(c.getCarried());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case QUICK_CRAFT:// QUICK_CRAFT
			newItem = asBukkitItem(c.getCarried());
			oldItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(getSlotItem(container, slot));
			break;
		case PICKUP_ALL:// PICKUP_ALL
			newItem = asBukkitItem(c.getCarried());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		default:
			newItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(packet.getCarriedItem());
			oldItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(packet.getCarriedItem());
			break;
		}

		if (oldItem.getType() == Material.AIR && newItem.getType() == Material.AIR)
			return true;

		boolean cancel = false;
		int gameSlot = slot > gui.size() - 1 ? InventoryUtils.convertToPlayerInvSlot(slot - gui.size()) : slot;

		ClickType clickType = InventoryUtils.buildClick(type == net.minecraft.world.inventory.ClickType.QUICK_CRAFT ? 1 : type == net.minecraft.world.inventory.ClickType.QUICK_MOVE ? 2 : 0,
				mouseClick);
		if (slot > -1) {
			if (!cancel)
				cancel = InventoryUtils.useItem(player, gui, slot, clickType);
			if (!gui.isInsertable())
				cancel = true;

			if (!cancel)
				cancel = gui.onInteractItem(player, newItem, oldItem, clickType, gameSlot, slot < gui.size());
			else
				gui.onInteractItem(player, newItem, oldItem, clickType, gameSlot, slot < gui.size());
		}
		if (!cancel) {
			if (gui instanceof AnvilGUI) { // Event
				final ItemStack newItemFinal = newItem;
				postToMainThread(() -> {
					processEvent(c, type, gui, player, slot, gameSlot, newItemFinal, oldItem, packet, mouseClick, clickType, nPlayer);
				});
			} else
				processEvent(c, type, gui, player, slot, gameSlot, newItem, oldItem, packet, mouseClick, clickType, nPlayer);
			return true;
		}
		// MOUSE
		int statusId = c.getStateId();
		BukkitLoader.getPacketHandler().send(player, packetSetSlot(-1, -1, statusId, c.getCarried()));
		switch (type) {
		case CLONE:
			break;
		case SWAP:
		case QUICK_MOVE:
		case PICKUP_ALL:
			c.sendAllDataToRemote();
			break;
		default:
			BukkitLoader.getPacketHandler().send(player, packetSetSlot(id, slot, statusId, c.getSlot(slot).getItem()));
			break;
		}
		return true;
	}

	private void processEvent(AbstractContainerMenu c, net.minecraft.world.inventory.ClickType type, HolderGUI gui, Player player, int slot, int gameSlot, ItemStack newItem, ItemStack oldItem,
			ServerboundContainerClickPacket packet, int mouseClick, ClickType clickType, net.minecraft.world.entity.player.Player nPlayer) {
		c.suppressRemoteUpdates();
		switch (type) {
		case QUICK_MOVE: {
			ItemStack[] contents = slot < gui.size() ? player.getInventory().getStorageContents() : gui.getInventory().getStorageContents();
			boolean interactWithResultSlot = false;
			if (gui instanceof AnvilGUI && slot < gui.size() && slot == 2)
				if (c.getSlot(2).allowModification(nPlayer))
					interactWithResultSlot = true;
				else
					return;
			Pair result = slot < gui.size()
					? InventoryUtils.shift(slot, player, gui, clickType, gui instanceof AnvilGUI && slot != 2 ? DestinationType.PLAYER_FROM_ANVIL : DestinationType.PLAYER, null, contents, oldItem)
					: InventoryUtils.shift(slot, player, gui, clickType, DestinationType.GUI, gui.getNotInterableSlots(player), contents, oldItem);
			@SuppressWarnings("unchecked")
			Map<Integer, ItemStack> modified = (Map<Integer, ItemStack>) result.getValue();
			int remaining = (int) result.getKey();

			if (!modified.isEmpty())
				if (slot < gui.size()) {
					for (Entry<Integer, ItemStack> modif : modified.entrySet())
						nPlayer.getInventory().setItem(modif.getKey(), (net.minecraft.world.item.ItemStack) asNMSItem(modif.getValue()));
					if (remaining == 0) {
						c.getSlot(gameSlot).set((net.minecraft.world.item.ItemStack) asNMSItem(null));
						if (interactWithResultSlot) {
							c.getSlot(0).set((net.minecraft.world.item.ItemStack) asNMSItem(null));
							c.getSlot(1).set((net.minecraft.world.item.ItemStack) asNMSItem(null));
						}
					} else {
						newItem.setAmount(remaining);
						c.getSlot(gameSlot).set((net.minecraft.world.item.ItemStack) asNMSItem(newItem));
					}
				} else {
					for (Entry<Integer, ItemStack> modif : modified.entrySet())
						c.getSlot(modif.getKey()).set((net.minecraft.world.item.ItemStack) asNMSItem(modif.getValue())); // Visual & Nms side
					// Plugin & Bukkit side
					gui.getInventory().setStorageContents(contents);
					if (remaining == 0)
						nPlayer.getInventory().setItem(gameSlot, (net.minecraft.world.item.ItemStack) asNMSItem(null));
					else {
						newItem.setAmount(remaining);
						nPlayer.getInventory().setItem(gameSlot, (net.minecraft.world.item.ItemStack) asNMSItem(newItem));
					}
				}
			c.resumeRemoteUpdates();
			return;
		}
		default:
			processClick(gui, gui.getNotInterableSlots(player), c, slot, mouseClick, type, nPlayer);
			break;
		}
		postToMainThread(() -> {
			if (type != net.minecraft.world.inventory.ClickType.QUICK_CRAFT && (c.getType().equals(MenuType.ANVIL) || c.getType().equals(MenuType.SMITHING)))
				c.sendAllDataToRemote();
			for (final it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<net.minecraft.world.item.ItemStack> entry : Int2ObjectMaps.fastIterable(packet.getChangedSlots()))
				c.setItem(entry.getIntKey(), packet.getStateId(), entry.getValue());
			c.setCarried(packet.getCarriedItem());
			c.resumeRemoteUpdates();
			if (packet.getStateId() != c.getStateId())
				c.broadcastFullState();
			else
				c.broadcastChanges();
		});
	}

	private Method addAmount = Ref.method(Slot.class, "onSwapCraft", int.class);
	private Method checkItem = Ref.method(AbstractContainerMenu.class, "tryItemClickBehaviourOverride", net.minecraft.world.entity.player.Player.class, ClickAction.class, Slot.class,
			net.minecraft.world.item.ItemStack.class, net.minecraft.world.item.ItemStack.class);

	@SuppressWarnings("unchecked")
	private void processClick(HolderGUI gui, List<Integer> ignoredSlots, AbstractContainerMenu container, int slotIndex, int button, net.minecraft.world.inventory.ClickType actionType,
			net.minecraft.world.entity.player.Player player) {
		if (actionType == net.minecraft.world.inventory.ClickType.QUICK_CRAFT)
			processDragMove(gui, container, player, slotIndex, button);
		else {
			int u = (int) Ref.get(container, containerU);
			int j = getContainerStateId(container);
			Set<Slot> mod = (Set<Slot>) Ref.get(container, containerV);
			if (u != 0) {
				Ref.set(container, containerU, u = 0);
				mod.clear();
			} else if (actionType == net.minecraft.world.inventory.ClickType.PICKUP && (button == 0 || button == 1)) {
				ClickAction clickaction = button == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
				if (slotIndex == -999) {
					if (!container.getCarried().isEmpty())
						if (clickaction == ClickAction.PRIMARY) {
							net.minecraft.world.item.ItemStack carried = container.getCarried();
							container.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
							postToMainThread(() -> player.drop(carried, true));
						} else
							postToMainThread(() -> player.drop(container.getCarried().split(1), true));
				} else {
					if (slotIndex < 0)
						return;
					Slot slot = container.getSlot(slotIndex);
					net.minecraft.world.item.ItemStack itemstack = slot.getItem();
					net.minecraft.world.item.ItemStack itemstack4 = container.getCarried();
					player.updateTutorialInventoryAction(itemstack4, slot.getItem(), clickaction);
					if (!(boolean) Ref.invoke(container, checkItem, player, clickaction, slot, itemstack, itemstack4))
						if (itemstack.isEmpty()) {
							if (!itemstack4.isEmpty()) {
								int i2 = clickaction == ClickAction.PRIMARY ? itemstack4.getCount() : 1;
								net.minecraft.world.item.ItemStack stack = slot.safeInsert(itemstack4, i2);
								container.setCarried(stack);
							}
						} else if (slot.allowModification(player))
							if (itemstack4.isEmpty()) {
								int i2 = clickaction == ClickAction.PRIMARY ? itemstack.getCount() : (itemstack.getCount() + 1) / 2;
								Optional<net.minecraft.world.item.ItemStack> optional = slot.tryRemove(i2, 2147483647, player);
								optional.ifPresent(i -> {
									container.setCarried(i);
									slot.onTake(player, i);
								});
							} else if (slot.mayPlace(itemstack4)) {
								if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(itemstack, itemstack4)) {
									int i2 = clickaction == ClickAction.PRIMARY ? itemstack4.getCount() : 1;
									net.minecraft.world.item.ItemStack stack = slot.safeInsert(itemstack4, i2);
									container.setCarried(stack);
								} else if (itemstack4.getCount() <= slot.getMaxStackSize(itemstack4)) {
									container.setCarried(itemstack);
									slot.safeInsert(itemstack4);
								}
							} else if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(itemstack, itemstack4)) {
								Optional<net.minecraft.world.item.ItemStack> optional2 = slot.tryRemove(itemstack.getCount(), itemstack4.getMaxStackSize() - itemstack4.getCount(), player);
								optional2.ifPresent(i -> {
									itemstack.grow(i.getCount());
									slot.onTake(player, i);
								});
							}
					slot.setChanged();
					if (player instanceof net.minecraft.world.entity.player.Player && slot.getMaxStackSize() != 64) {
						BukkitLoader.getPacketHandler().send((Player) player.getBukkitEntity(),
								BukkitLoader.getNmsProvider().packetSetSlot(j, slot.index, container.incrementStateId(), slot.getItem()));
						if (container.getBukkitView().getType() == InventoryType.WORKBENCH || container.getBukkitView().getType() == InventoryType.CRAFTING)
							BukkitLoader.getPacketHandler().send((Player) player.getBukkitEntity(),
									BukkitLoader.getNmsProvider().packetSetSlot(j, 0, container.incrementStateId(), container.getSlot(0).getItem()));
					}
				}
			} else if (actionType == net.minecraft.world.inventory.ClickType.SWAP) {
				if (slotIndex < 0)
					return;
				net.minecraft.world.entity.player.Inventory playerinventory = player.getInventory();
				Slot slot3 = container.getSlot(slotIndex);
				net.minecraft.world.item.ItemStack itemstack2 = playerinventory.getItem(button);
				net.minecraft.world.item.ItemStack itemstack = slot3.getItem();
				if (!itemstack2.isEmpty() || !itemstack.isEmpty())
					if (itemstack2.isEmpty()) {
						if (slot3.allowModification(player)) {
							playerinventory.setItem(button, itemstack);
							Ref.invoke(slot3, addAmount, itemstack.getCount());
							slot3.set(net.minecraft.world.item.ItemStack.EMPTY);
							slot3.onTake(player, itemstack);
						}
					} else if (itemstack.isEmpty()) {
						if (slot3.mayPlace(itemstack2)) {
							int j2 = slot3.getMaxStackSize(itemstack2);
							if (itemstack2.getCount() > j2)
								slot3.set(itemstack2.split(j2));
							else {
								playerinventory.setItem(button, net.minecraft.world.item.ItemStack.EMPTY);
								slot3.set(itemstack2);
							}
						}
					} else if (slot3.allowModification(player) && slot3.mayPlace(itemstack2)) {
						int j2 = slot3.getMaxStackSize(itemstack2);
						if (itemstack2.getCount() > j2) {
							slot3.set(itemstack2.split(j2));
							slot3.onTake(player, itemstack);
							if (!playerinventory.add(itemstack))
								postToMainThread(() -> player.drop(itemstack, true));
						} else {
							playerinventory.setItem(button, itemstack);
							slot3.set(itemstack2);
							slot3.onTake(player, itemstack);
						}
					}
			} else if (actionType == net.minecraft.world.inventory.ClickType.CLONE && player.getAbilities().instabuild && container.getCarried().isEmpty() && slotIndex >= 0) {
				Slot slot3 = container.getSlot(slotIndex);
				if (slot3.hasItem()) {
					net.minecraft.world.item.ItemStack itemstack2 = slot3.getItem();
					container.setCarried(itemstack2.copyWithCount(itemstack2.getMaxStackSize()));
				}
			} else if (actionType == net.minecraft.world.inventory.ClickType.THROW && container.getCarried().isEmpty() && slotIndex >= 0) {
				Slot slot3 = container.getSlot(slotIndex);
				int m = button == 0 ? 1 : slot3.getItem().getCount();
				net.minecraft.world.item.ItemStack itemstack = slot3.safeTake(m, 2147483647, player);
				postToMainThread(() -> player.drop(itemstack, true));
			} else if (actionType == net.minecraft.world.inventory.ClickType.PICKUP_ALL && slotIndex >= 0) {
				final Slot slot3 = container.slots.get(slotIndex);
				final net.minecraft.world.item.ItemStack itemstack2 = container.getCarried();
				if (!itemstack2.isEmpty() && (!slot3.hasItem() || !slot3.allowModification(player))) {
					List<Integer> ignoreSlots = ignoredSlots == null ? Collections.emptyList() : ignoredSlots;
					List<Integer> corruptedSlots = ignoredSlots == null ? Collections.emptyList() : new ArrayList<>();
					Map<Integer, ItemStack> modifiedSlots = new HashMap<>();
					Map<Integer, ItemStack> modifiedSlotsPlayerInv = new HashMap<>();
					final int l = button == 0 ? 0 : container.slots.size() - 1;
					final int j2 = button == 0 ? 1 : -1;
					for (int i2 = 0; i2 < 2; ++i2)
						for (int slot = l; slot >= 0 && slot < container.slots.size() && itemstack2.getCount() < itemstack2.getMaxStackSize(); slot += j2) {
							final Slot slot4 = container.slots.get(slot);
							if (slot4.hasItem() && AbstractContainerMenu.canItemQuickReplace(slot4, itemstack2, true) && slot4.allowModification(player)
									&& container.canTakeItemForPickAll(itemstack2, slot4)) {
								final net.minecraft.world.item.ItemStack itemstack5 = slot4.getItem();
								if (i2 != 0 || itemstack5.getCount() != itemstack5.getMaxStackSize()) {
									if (slot < gui.size() && ignoreSlots.contains(slot)) {
										corruptedSlots.add(slot);
										continue;
									}
									final net.minecraft.world.item.ItemStack itemstack6 = slot4.safeTake(itemstack5.getCount(), itemstack2.getMaxStackSize() - itemstack2.getCount(), player);
									itemstack2.grow(itemstack6.getCount());
									int gameSlot = slot > gui.size() - 1 ? InventoryUtils.convertToPlayerInvSlot(slot - gui.size()) : slot;
									if (slot < gui.size())
										modifiedSlots.put(gameSlot, asBukkitItem(slot4.getItem()));
									else
										modifiedSlotsPlayerInv.put(gameSlot, asBukkitItem(slot4.getItem()));
								}
							}
						}
					if (slotIndex < gui.size())
						modifiedSlots.put(slotIndex, new ItemStack(Material.AIR));
					else
						modifiedSlotsPlayerInv.put(InventoryUtils.convertToPlayerInvSlot(slotIndex - gui.size()), new ItemStack(Material.AIR));
					if (!modifiedSlots.isEmpty() || !modifiedSlotsPlayerInv.isEmpty())
						gui.onMultipleIteract((Player) player.getBukkitEntity(), modifiedSlots, modifiedSlotsPlayerInv);
					for (int s : corruptedSlots)
						BukkitLoader.getPacketHandler().send((Player) player.getBukkitEntity(), BukkitLoader.getNmsProvider().packetSetSlot(BukkitLoader.getNmsProvider().getContainerId(container), s,
								getContainerStateId(container), BukkitLoader.getNmsProvider().getSlotItem(container, s)));
				}
			}
		}
	}

	private Field containerU = Ref.field(AbstractContainerMenu.class, "quickcraftStatus"), containerV = Ref.field(AbstractContainerMenu.class, "quickcraftSlots"),
			containerT = Ref.field(AbstractContainerMenu.class, "quickcraftType");

	@SuppressWarnings("unchecked")
	private void processDragMove(HolderGUI gui, AbstractContainerMenu container, net.minecraft.world.entity.player.Player player, int slot, int mouseClick) {
		int previous = (int) Ref.get(container, containerU);
		int u = d(mouseClick);
		Set<Slot> mod = (Set<Slot>) Ref.get(container, containerV);
		if ((previous != 1 || u != 2) && previous != u || container.getCarried().isEmpty()) {
			mod.clear();
			u = 0;
		} else
			switch (u) {
			case 0: {
				int t = c(mouseClick);
				Ref.set(container, containerT, t);
				if (AbstractContainerMenu.isValidQuickcraftType(t, player)) {
					u = 1;
					mod.clear();
				} else {
					mod.clear();
					u = 0;
				}
				break;
			}
			case 1: {
				if (slot < 0) {
					Ref.set(container, containerU, u);
					return; // nothing
				}
				int t = (int) Ref.get(container, containerT);
				final Slot bslot = container.getSlot(slot);
				final net.minecraft.world.item.ItemStack itemstack = container.getCarried();
				if (AbstractContainerMenu.canItemQuickReplace(bslot, itemstack, true) && bslot.mayPlace(itemstack) && (t == 2 || itemstack.getCount() > mod.size()) && container.canDragTo(bslot))
					mod.add(bslot);
				break;
			}
			case 2:
				if (!mod.isEmpty()) {
					final net.minecraft.world.item.ItemStack itemstack2 = container.getCarried().copy();
					if (itemstack2.isEmpty()) {
						mod.clear();
						Ref.set(container, containerU, 0);
						return;
					}
					int t = (int) Ref.get(container, containerT);
					int l = container.getCarried().getCount();
					final Map<Integer, net.minecraft.world.item.ItemStack> draggedSlots = new HashMap<>();
					for (Slot slot2 : mod) {
						final net.minecraft.world.item.ItemStack itemstack3 = container.getCarried();
						if (slot2 != null && AbstractContainerMenu.canItemQuickReplace(slot2, itemstack3, true) && slot2.mayPlace(itemstack3) && (t == 2 || itemstack3.getCount() >= mod.size())
								&& container.canDragTo(slot2)) {
							final int j1 = slot2.hasItem() ? slot2.getItem().getCount() : 0;
							final int k1 = Math.min(itemstack2.getMaxStackSize(), slot2.getMaxStackSize(itemstack2));
							final int l2 = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(mod, t, itemstack2) + j1, k1);
							l -= l2 - j1;
							draggedSlots.put(slot2.index, itemstack2.copyWithCount(l2));
						}
					}
					final InventoryView view = container.getBukkitView();
					final org.bukkit.inventory.ItemStack newcursor = CraftItemStack.asCraftMirror(itemstack2);
					newcursor.setAmount(l);
					final Map<Integer, org.bukkit.inventory.ItemStack> guiSlots = new HashMap<>();
					final Map<Integer, org.bukkit.inventory.ItemStack> playerSlots = new HashMap<>();
					for (final Entry<Integer, net.minecraft.world.item.ItemStack> ditem : draggedSlots.entrySet())
						if (ditem.getKey() < gui.size())
							guiSlots.put(ditem.getKey(), CraftItemStack.asBukkitCopy(ditem.getValue()));
						else {
							int finalSlot = ditem.getKey() - gui.size();
							if (finalSlot >= 27)
								finalSlot -= 27;
							else
								finalSlot += 9;
							playerSlots.put(finalSlot, CraftItemStack.asBukkitCopy(ditem.getValue()));
						}
					container.setCarried(CraftItemStack.asNMSCopy(newcursor));
					if (!guiSlots.isEmpty() || !playerSlots.isEmpty())
						gui.onMultipleIteract((Player) player.getBukkitEntity(), guiSlots, playerSlots);
					for (final Entry<Integer, net.minecraft.world.item.ItemStack> dslot : draggedSlots.entrySet())
						view.setItem(dslot.getKey(), CraftItemStack.asBukkitCopy(dslot.getValue()));
					if (container.getCarried() != null)
						container.sendAllDataToRemote();
				}
				mod.clear();
				u = 0;
			default:
				mod.clear();
				u = 0;
				break;
			}
		Ref.set(container, containerU, u);
	}

	@Override
	public boolean processServerListPing(String player, Object channel, Object packet) {
		if (packet instanceof PacketContainer) {
			PacketContainer container = (PacketContainer) packet;
			ClientboundStatusResponsePacket status = (ClientboundStatusResponsePacket) container.getPacket();
			ServerStatus ping = status.status();
			List<GameProfileHandler> gameProfiles = new ArrayList<>();
			if (ping.players().isPresent())
				for (GameProfile profile : ping.players().get().sample())
					gameProfiles.add(fromGameProfile(profile));

			net.minecraft.network.chat.Component motd = net.minecraft.network.chat.Component.literal("");
			Optional<Players> players = Optional.empty();
			Optional<Favicon> serverIcon = Optional.empty();
			Optional<Version> version = ping.version();
			boolean enforceSecureProfile = ping.enforcesSecureChat();

			String favicon = "server-icon.png";
			ServerListPingEvent event = new ServerListPingEvent(getOnlinePlayers().size(), Bukkit.getMaxPlayers(), gameProfiles, Bukkit.getMotd(), favicon,
					((InetSocketAddress) ((Channel) channel).remoteAddress()).getAddress(), ping.version().get().name(), ping.version().get().protocol());
			EventManager.call(event);
			if (event.isCancelled()) {
				container.setCancelled(true);
				return true;
			}
			Players playerSample = new Players(event.getMaxPlayers(), event.getOnlinePlayers(), new ArrayList<>());
			if (event.getSlots() != null)
				for (GameProfileHandler s : event.getSlots())
					playerSample.sample().add(new GameProfile(s.getUUID(), s.getUsername()));
			players = Optional.of(playerSample);

			if (event.getMotd() != null)
				motd = (net.minecraft.network.chat.Component) this.toIChatBaseComponent(ComponentAPI.fromString(event.getMotd()));
			if (event.getVersion() != null)
				version = Optional.of(new Version(event.getVersion(), event.getProtocol()));
			if (event.getFavicon() != null)
				if (!event.getFavicon().equals("server-icon.png") && new File(event.getFavicon()).exists()) {
					BufferedImage var1;
					try {
						var1 = ImageIO.read(new File(event.getFavicon()));
						Preconditions.checkState(var1.getWidth() == 64, "Must be 64 pixels wide");
						Preconditions.checkState(var1.getHeight() == 64, "Must be 64 pixels high");
						ByteArrayOutputStream var2 = new ByteArrayOutputStream();
						ImageIO.write(var1, "PNG", var2);
						serverIcon = Optional.of(new Favicon(var2.toByteArray()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else
					serverIcon = ping.favicon();
			container.setPacket(new ClientboundStatusResponsePacket(new ServerStatus(motd, players, version, serverIcon, enforceSecureProfile)));
			return false;
		}
		JavaPlugin.getPlugin(BukkitLoader.class).getLogger().warning("You are using outdated version of TheAPI, please update TheAPI to the latest version!");
		return false;
	}

	@Override
	public Object getNBT(Entity entity) {
		return ((CraftEntity) entity).getHandle().saveWithoutId(new CompoundTag());
	}

	@Override
	public Object setString(Object nbt, String path, String value) {
		((CompoundTag) nbt).putString(path, value);
		return nbt;
	}

	@Override
	public Object setInteger(Object nbt, String path, int value) {
		((CompoundTag) nbt).putInt(path, value);
		return nbt;
	}

	@Override
	public Object setDouble(Object nbt, String path, double value) {
		((CompoundTag) nbt).putDouble(path, value);
		return nbt;
	}

	@Override
	public Object setLong(Object nbt, String path, long value) {
		((CompoundTag) nbt).putLong(path, value);
		return nbt;
	}

	@Override
	public Object setShort(Object nbt, String path, short value) {
		((CompoundTag) nbt).putShort(path, value);
		return nbt;
	}

	@Override
	public Object setFloat(Object nbt, String path, float value) {
		((CompoundTag) nbt).putFloat(path, value);
		return nbt;
	}

	@Override
	public Object setBoolean(Object nbt, String path, boolean value) {
		((CompoundTag) nbt).putBoolean(path, value);
		return nbt;
	}

	@Override
	public Object setIntArray(Object nbt, String path, int[] value) {
		((CompoundTag) nbt).putIntArray(path, value);
		return nbt;
	}

	@Override
	public Object setByteArray(Object nbt, String path, byte[] value) {
		((CompoundTag) nbt).putByteArray(path, value);
		return nbt;
	}

	@Override
	public Object setNBTBase(Object nbt, String path, Object value) {
		((CompoundTag) nbt).put(path, (Tag) value);
		return nbt;
	}

	@Override
	public String getString(Object nbt, String path) {
		return ((CompoundTag) nbt).getString(path);
	}

	@Override
	public int getInteger(Object nbt, String path) {
		return ((CompoundTag) nbt).getInt(path);
	}

	@Override
	public double getDouble(Object nbt, String path) {
		return ((CompoundTag) nbt).getDouble(path);
	}

	@Override
	public long getLong(Object nbt, String path) {
		return ((CompoundTag) nbt).getLong(path);
	}

	@Override
	public short getShort(Object nbt, String path) {
		return ((CompoundTag) nbt).getShort(path);
	}

	@Override
	public float getFloat(Object nbt, String path) {
		return ((CompoundTag) nbt).getFloat(path);
	}

	@Override
	public boolean getBoolean(Object nbt, String path) {
		return ((CompoundTag) nbt).getBoolean(path);
	}

	@Override
	public int[] getIntArray(Object nbt, String path) {
		return ((CompoundTag) nbt).getIntArray(path);
	}

	@Override
	public byte[] getByteArray(Object nbt, String path) {
		return ((CompoundTag) nbt).getByteArray(path);
	}

	@Override
	public Object getNBTBase(Object nbt, String path) {
		return ((CompoundTag) nbt).get(path);
	}

	@Override
	public Set<String> getKeys(Object nbt) {
		return ((CompoundTag) nbt).getAllKeys();
	}

	@Override
	public boolean hasKey(Object nbt, String path) {
		return ((CompoundTag) nbt).contains(path);
	}

	@Override
	public void removeKey(Object nbt, String path) {
		((CompoundTag) nbt).remove(path);
	}

	@Override
	public Object setByte(Object nbt, String path, byte value) {
		((CompoundTag) nbt).putByte(path, value);
		return nbt;
	}

	@Override
	public byte getByte(Object nbt, String path) {
		return ((CompoundTag) nbt).getByte(path);
	}

	@Override
	public Object getDataWatcher(Entity entity) {
		return ((CraftEntity) entity).getHandle().getEntityData();
	}

	@Override
	public Object getDataWatcher(Object entity) {
		return ((net.minecraft.world.entity.Entity) entity).getEntityData();
	}

	@Override
	public int incrementStateId(Object container) {
		return ((AbstractContainerMenu) container).incrementStateId();
	}

	@Override
	public Object packetEntityHeadRotation(Entity entity) {
		return new ClientboundRotateHeadPacket((net.minecraft.world.entity.Entity) getEntity(entity), (byte) (entity.getLocation().getYaw() * 256F / 360F));
	}

	@Override
	public Object packetHeldItemSlot(int slot) {
		return new ClientboundSetCarriedItemPacket(slot);
	}

	@Override
	public Object packetExp(float exp, int total, int toNextLevel) {
		return new ClientboundSetExperiencePacket(exp, total, toNextLevel);
	}

	@Override
	public Object packetPlayerInfo(PlayerInfoType type, Player player) {
		ClientboundPlayerInfoUpdatePacket.Action action = null;
		switch (type) {
		case ADD_PLAYER:
			action = ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER;
			break;
		case REMOVE_PLAYER:
			return new ClientboundPlayerInfoRemovePacket(Arrays.asList(player.getUniqueId()));
		case UPDATE_DISPLAY_NAME:
			action = ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME;
			break;
		case UPDATE_GAME_MODE:
			action = ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE;
			break;
		case UPDATE_LATENCY:
			action = ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY;
			break;
		}
		return new ClientboundPlayerInfoUpdatePacket(action, (ServerPlayer) getPlayer(player));
	}

	@Override
	public Object packetPlayerInfo(PlayerInfoType type, GameProfileHandler gameProfile, int latency, GameMode gameMode, Component playerName) {
		ClientboundPlayerInfoUpdatePacket.Action action = null;
		switch (type) {
		case ADD_PLAYER:
			action = ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER;
			break;
		case REMOVE_PLAYER:
			return new ClientboundPlayerInfoRemovePacket(Arrays.asList(gameProfile.getUUID()));
		case UPDATE_DISPLAY_NAME:
			action = ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME;
			break;
		case UPDATE_GAME_MODE:
			action = ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE;
			break;
		case UPDATE_LATENCY:
			action = ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY;
			break;
		}

		EnumSet<ClientboundPlayerInfoUpdatePacket.Action> set = EnumSet.of(action);
		List<ClientboundPlayerInfoUpdatePacket.Entry> list = Arrays.asList(new ClientboundPlayerInfoUpdatePacket.Entry(gameProfile.getUUID(), (GameProfile) toGameProfile(gameProfile), true, latency,
				gameMode == null ? GameType.SURVIVAL : GameType.byName(gameMode.name().toLowerCase()),
				(net.minecraft.network.chat.Component) (playerName == null ? toIChatBaseComponent(new Component(gameProfile.getUsername())) : toIChatBaseComponent(playerName)), null));
		return new ClientboundPlayerInfoUpdatePacket(set, list);
	}

	@Override
	public Object packetPosition(double x, double y, double z, float yaw, float pitch) {
		return new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, true);
	}

	@Override
	public Object packetRespawn(Player player) {
		ServerPlayer entityPlayer = (ServerPlayer) getPlayer(player);
		return new ClientboundRespawnPacket(entityPlayer.createCommonSpawnInfo(entityPlayer.serverLevel()), (byte) 1);
	}

	@Override
	public String getProviderName() {
		return "PaperMC 1.20.6";
	}

	@Override
	public int getContainerStateId(Object container) {
		return ((AbstractContainerMenu) container).getStateId();
	}

	@Override
	public void loadParticles() {
		for (Entry<ResourceKey<ParticleType<?>>, ParticleType<?>> s : BuiltInRegistries.PARTICLE_TYPE.entrySet())
			me.devtec.theapi.bukkit.game.particles.Particle.identifier.put(s.getKey().location().getPath(), s.getValue());
	}

	@Override
	public Object toGameProfile(GameProfileHandler gameProfileHandler) {
		GameProfile profile = new GameProfile(gameProfileHandler.getUUID(), gameProfileHandler.getUsername());
		for (Entry<String, PropertyHandler> entry : gameProfileHandler.getProperties().entrySet())
			profile.getProperties().put(entry.getKey(), new Property(entry.getValue().getName(), entry.getValue().getValues(), entry.getValue().getSignature()));
		return profile;
	}

	private Field name = Ref.field(Property.class, "name"), value = Ref.field(Property.class, "value"), signature = Ref.field(Property.class, "signature");

	@Override
	public GameProfileHandler fromGameProfile(Object gameProfile) {
		GameProfile profile = (GameProfile) gameProfile;
		GameProfileHandler handler = GameProfileHandler.of(profile.getName(), profile.getId());
		for (Entry<String, Property> entry : profile.getProperties().entries())
			handler.getProperties().put(entry.getKey(),
					PropertyHandler.of((String) Ref.get(entry.getValue(), name), (String) Ref.get(entry.getValue(), value), (String) Ref.get(entry.getValue(), signature)));
		return handler;
	}

	@Override
	public Object getGameProfile(Object nmsPlayer) {
		return ((net.minecraft.world.entity.player.Player) nmsPlayer).getGameProfile();
	}

}
