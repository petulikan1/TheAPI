package me.devtec.theapi.bukkit.nms;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
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
import org.bukkit.craftbukkit.v1_20_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

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
import me.devtec.theapi.bukkit.xseries.XMaterial;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatClickable.EnumClickAction;
import net.minecraft.network.chat.ChatHexColor;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.ChatHoverable.EnumHoverAction;
import net.minecraft.network.chat.ChatModifier;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.a;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.b;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.PacketPlayInWindowClick;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutExperience;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.network.protocol.game.PacketPlayOutResourcePackSend;
import net.minecraft.network.protocol.game.PacketPlayOutRespawn;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardObjective;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardScore;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.network.protocol.status.ServerPing;
import net.minecraft.network.protocol.status.ServerPing.ServerData;
import net.minecraft.network.protocol.status.ServerPing.ServerPingPlayerSample;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.InventoryClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFalling;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ITileEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.IBlockDataHolder;
import net.minecraft.world.level.block.state.properties.IBlockState;
import net.minecraft.world.level.chunk.Chunk.EnumTileEntityState;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.criteria.IScoreboardCriteria.EnumScoreboardHealthDisplay;

public class v1_20_R1 implements NmsProvider {
	private static final MinecraftServer server = MinecraftServer.getServer();
	private static final sun.misc.Unsafe unsafe = (sun.misc.Unsafe) Ref.getNulled(Ref.field(sun.misc.Unsafe.class, "theUnsafe"));
	private static final IChatBaseComponent empty = IChatBaseComponent.b("");

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
		if (isPaperChunkRework)
			return ((CraftChunk) chunk).getHandle(ChunkStatus.n);
		return Ref.invoke(chunk, getNmsChunkHandle);
	}

	@Override
	public int getEntityId(Object entity) {
		return ((net.minecraft.world.entity.Entity) entity).af();
	}

	@Override
	public Object getScoreboardAction(Action type) {
		return type == Action.CHANGE ? ScoreboardServer.Action.a : ScoreboardServer.Action.b;
	}

	@Override
	public Object getEnumScoreboardHealthDisplay(DisplayType type) {
		return type == DisplayType.INTEGER ? EnumScoreboardHealthDisplay.a : EnumScoreboardHealthDisplay.b;
	}

	@Override
	public Object getNBT(ItemStack itemStack) {
		return ((net.minecraft.world.item.ItemStack) asNMSItem(itemStack)).v();
	}

	@Override
	public Object parseNBT(String json) {
		if (json == null)
			return new NBTTagCompound();
		try {
			return MojangsonParser.a(json);
		} catch (Exception e) {
			return new NBTTagCompound();
		}
	}

	@Override
	public ItemStack setNBT(ItemStack stack, Object nbt) {
		if (nbt instanceof NBTEdit)
			nbt = ((NBTEdit) nbt).getNBT();
		net.minecraft.world.item.ItemStack i = (net.minecraft.world.item.ItemStack) asNMSItem(stack);
		i.c((NBTTagCompound) nbt);
		return asBukkitItem(i);
	}

	@Override
	public Object asNMSItem(ItemStack stack) {
		if (stack == null)
			return net.minecraft.world.item.ItemStack.b;
		return CraftItemStack.asNMSCopy(stack);
	}

	@Override
	public ItemStack asBukkitItem(Object stack) {
		return CraftItemStack.asBukkitCopy(stack == null ? net.minecraft.world.item.ItemStack.b : (net.minecraft.world.item.ItemStack) stack);
	}

	@Override
	public int getContainerId(Object container) {
		return ((Container) container).j;
	}

	@Override
	public Object packetResourcePackSend(String url, String hash, boolean requireRP, Component prompt) {
		return new PacketPlayOutResourcePackSend(url, hash, requireRP, prompt == null ? null : (IChatBaseComponent) this.toIChatBaseComponent(prompt));
	}

	@Override
	public Object packetSetSlot(int container, int slot, int changeId, Object itemStack) {
		return new PacketPlayOutSetSlot(container, changeId, slot, (net.minecraft.world.item.ItemStack) (itemStack == null ? asNMSItem(null) : itemStack));
	}

	@Override
	public Object packetEntityMetadata(int entityId, Object dataWatcher, boolean bal) {
		return new PacketPlayOutEntityMetadata(entityId, ((net.minecraft.network.syncher.DataWatcher) dataWatcher).c());
	}

	@Override
	public Object packetEntityDestroy(int... ids) {
		return new PacketPlayOutEntityDestroy(ids);
	}

	@Override
	public Object packetSpawnEntity(Object entity, int id) {
		return new PacketPlayOutSpawnEntity((net.minecraft.world.entity.Entity) entity, id);
	}

	@Override
	public Object packetNamedEntitySpawn(Object player) {
		return new PacketPlayOutNamedEntitySpawn((EntityHuman) player);
	}

	@Override
	public Object packetSpawnEntityLiving(Object entityLiving) {
		return new PacketPlayOutSpawnEntity((EntityLiving) entityLiving);
	}

	@Override
	public Object packetPlayerListHeaderFooter(Component header, Component footer) {
		return new PacketPlayOutPlayerListHeaderFooter((IChatBaseComponent) this.toIChatBaseComponent(header), (IChatBaseComponent) this.toIChatBaseComponent(footer));
	}

	@Override
	public Object packetBlockChange(int x, int y, int z, Object iblockdata, int data) {
		return new PacketPlayOutBlockChange(new BlockPosition(x, y, z), iblockdata == null ? Blocks.a.n() : (IBlockData) iblockdata);
	}

	@Override
	public Object packetScoreboardObjective() {
		try {
			return v1_20_R1.unsafe.allocateInstance(PacketPlayOutScoreboardObjective.class);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Object packetScoreboardDisplayObjective(int id, Object scoreboardObjective) {
		return new PacketPlayOutScoreboardDisplayObjective(id, scoreboardObjective == null ? null : (ScoreboardObjective) scoreboardObjective);
	}

	@Override
	public Object packetScoreboardTeam() {
		try {
			return v1_20_R1.unsafe.allocateInstance(PacketPlayOutScoreboardTeam.class);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Object packetScoreboardScore(Action action, String player, String line, int score) {
		return new PacketPlayOutScoreboardScore((net.minecraft.server.ScoreboardServer.Action) getScoreboardAction(action), player, line, score);
	}

	@Override
	public Object packetTitle(TitleAction action, Component text, int fadeIn, int stay, int fadeOut) {
		switch (action) {
		case ACTIONBAR:
			return new ClientboundSetActionBarTextPacket((IChatBaseComponent) this.toIChatBaseComponent(text));
		case TITLE:
			return new ClientboundSetTitleTextPacket((IChatBaseComponent) this.toIChatBaseComponent(text));
		case SUBTITLE:
			return new ClientboundSetSubtitleTextPacket((IChatBaseComponent) this.toIChatBaseComponent(text));
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
		return new ClientboundSystemChatPacket((IChatBaseComponent) chatBase, false);
	}

	@Override
	public Object packetChat(ChatType type, Component text, UUID uuid) {
		return new ClientboundSystemChatPacket((IChatBaseComponent) this.toIChatBaseComponent(text), false);
	}

	@Override
	public void postToMainThread(Runnable runnable) {
		v1_20_R1.server.execute(runnable);
	}

	@Override
	public Object getMinecraftServer() {
		return v1_20_R1.server;
	}

	@Override
	public Thread getServerThread() {
		return v1_20_R1.server.ag;
	}

	@Override
	public double[] getServerTPS() {
		return v1_20_R1.server.recentTps;
	}

	private IChatBaseComponent convert(Component c) {
		if (c instanceof ComponentItem || c instanceof ComponentEntity)
			return IChatBaseComponent.a(Json.writer().simpleWrite(c.toJsonMap()));
		IChatMutableComponent current = IChatBaseComponent.b(c.getText());
		ChatModifier modif = current.a();
		if (c.getColor() != null && !c.getColor().isEmpty())
			if (c.getColor().startsWith("#"))
				modif = modif.a(ChatHexColor.a(c.getColor()));
			else
				modif = modif.a(EnumChatFormat.a(c.colorToChar()));
		if (c.getClickEvent() != null)
			modif = modif.a(new ChatClickable(EnumClickAction.a(c.getClickEvent().getAction().name().toLowerCase()), c.getClickEvent().getValue()));
		if (c.getHoverEvent() != null)
			switch (c.getHoverEvent().getAction()) {
			case SHOW_ENTITY:
				try {
					ComponentEntity compoundTag = (ComponentEntity) c.getHoverEvent().getValue();
					IChatBaseComponent component = compoundTag.getName() == null ? null : (IChatBaseComponent) toIChatBaseComponent(compoundTag.getName());
					EntityTypes<?> entityType = BuiltInRegistries.h.a(new MinecraftKey(compoundTag.getType()));
					modif = modif.a(new ChatHoverable(EnumHoverAction.c, new ChatHoverable.b(entityType, compoundTag.getId(), component)));
				} catch (Exception commandSyntaxException) {
				}
				break;
			case SHOW_ITEM:
				try {
					ComponentItem compoundTag = (ComponentItem) c.getHoverEvent().getValue();
					net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(
							CraftMagicNumbers.getItem(XMaterial.matchXMaterial(compoundTag.getId()).orElse(XMaterial.AIR).parseMaterial()), compoundTag.getCount());
					if (compoundTag.getNbt() != null)
						stack.c((NBTTagCompound) parseNBT(compoundTag.getNbt()));
					modif = modif.a(new ChatHoverable(EnumHoverAction.b, new ChatHoverable.c(stack)));
				} catch (Exception commandSyntaxException) {
				}
				break;
			default:
				modif = modif.a(new ChatHoverable(EnumHoverAction.a, (IChatBaseComponent) this.toIChatBaseComponent(c.getHoverEvent().getValue())));
				break;
			}
		modif = modif.a(c.isBold());
		modif = modif.b(c.isItalic());
		modif = modif.e(c.isObfuscated());
		modif = modif.c(c.isUnderlined());
		modif = modif.d(c.isStrikethrough());
		current.b(modif);
		return current;
	}

	@Override
	public Object[] toIChatBaseComponents(List<Component> components) {
		List<IChatBaseComponent> chat = new ArrayList<>();
		chat.add(IChatBaseComponent.b(""));
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
		return chat.toArray(new IChatBaseComponent[0]);
	}

	private void addConverted(List<IChatBaseComponent> chat, List<Component> extra) {
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
			return new IChatBaseComponent[] { empty };
		if (co instanceof ComponentItem || co instanceof ComponentEntity)
			return new IChatBaseComponent[] { IChatBaseComponent.b(Json.writer().simpleWrite(co.toJsonMap())) };
		List<IChatBaseComponent> chat = new ArrayList<>();
		chat.add(IChatBaseComponent.b(""));
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
		return chat.toArray(new IChatBaseComponent[0]);
	}

	@Override
	public Object toIChatBaseComponent(Component co) {
		if (co == null)
			return empty;
		if (co instanceof ComponentItem || co instanceof ComponentEntity)
			return IChatBaseComponent.b(Json.writer().simpleWrite(co.toJsonMap()));
		IChatMutableComponent main = IChatBaseComponent.b("");
		List<IChatBaseComponent> chat = new ArrayList<>();
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
		main.c().addAll(chat);
		return main.c().isEmpty() ? empty : main;
	}

	@Override
	public Object toIChatBaseComponent(List<Component> cc) {
		IChatMutableComponent main = IChatBaseComponent.b("");
		for (Component c : cc)
			main.c().add((IChatBaseComponent) this.toIChatBaseComponent(c));
		return main.c().isEmpty() ? empty : main;
	}

	@Override
	public Object chatBase(String json) {
		return IChatBaseComponent.ChatSerializer.a(json);
	}

	@Override
	public Component fromIChatBaseComponent(Object componentObject) {
		if (componentObject == null)
			return Component.EMPTY_COMPONENT;
		IChatBaseComponent component = (IChatBaseComponent) componentObject;
		Object result = Ref.invoke(component.b(), "a");
		Component comp = new Component(result == null ? "" : (String) result);
		ChatModifier modif = component.a();
		if (modif.a() != null)
			comp.setColor(modif.a().b());

		if (modif.h() != null)
			comp.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(modif.h().a().name()), modif.h().b()));

		if (modif.i() != null)
			switch (HoverEvent.Action.valueOf(modif.i().a().b().toUpperCase())) {
			case SHOW_ENTITY: {
				net.minecraft.network.chat.ChatHoverable.b hover = modif.i().a(EnumHoverAction.c);
				ComponentEntity compEntity = new ComponentEntity(hover.a.j().a(), hover.b);
				if (hover.c != null)
					compEntity.setName(fromIChatBaseComponent(hover.c));
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY, compEntity));
				break;
			}
			case SHOW_ITEM: {
				net.minecraft.network.chat.ChatHoverable.c hover = modif.i().a(EnumHoverAction.b);
				ComponentItem compEntity = new ComponentItem(CraftMagicNumbers.getMaterial(hover.a().d()).name(), hover.a().L());
				if (hover.a().v() != null)
					compEntity.setNbt(hover.a().v().toString());
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, compEntity));
				break;
			}
			default:
				IChatBaseComponent hover = modif.i().a(EnumHoverAction.a);
				comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, fromIChatBaseComponent(hover)));
				break;
			}
		comp.setBold(modif.b());
		comp.setItalic(modif.c());
		comp.setObfuscated(modif.d());
		comp.setUnderlined(modif.e());
		comp.setStrikethrough(modif.f());

		if (!component.c().isEmpty()) {
			List<Component> extra = new ArrayList<>();
			for (IChatBaseComponent base : component.c())
				extra.add(fromIChatBaseComponent(base));
			comp.setExtra(extra);
		}
		return comp;
	}

	@Override
	public BlockDataStorage toMaterial(Object blockOrIBlockData) {
		if (blockOrIBlockData instanceof Block) {
			IBlockData data = ((Block) blockOrIBlockData).n();
			return new BlockDataStorage(CraftMagicNumbers.getMaterial(data.b()), (byte) 0, asString(data));
		}
		if (blockOrIBlockData instanceof IBlockData) {
			IBlockData data = (IBlockData) blockOrIBlockData;
			return new BlockDataStorage(CraftMagicNumbers.getMaterial(data.b()), (byte) 0, asString(data));
		}
		return new BlockDataStorage(Material.AIR);
	}

	private String asString(IBlockData data) {
		StringBuilder stateString = new StringBuilder();
		if (!data.C().isEmpty()) {
			stateString.append('[');
			stateString.append(data.C().entrySet().stream().map(IBlockDataHolder.a).collect(Collectors.joining(",")));
			stateString.append(']');
		}
		return stateString.toString();
	}

	@Override
	public Object toIBlockData(BlockDataStorage material) {
		if (material == null || material.getType() == null || material.getType() == Material.AIR)
			return Blocks.a.n();
		Block block = CraftMagicNumbers.getBlock(material.getType());
		return readArgument(block, material);
	}

	@Override
	public Object toBlock(BlockDataStorage material) {
		if (material == null || material.getType() == null || material.getType() == Material.AIR)
			return Blocks.a;
		Block block = CraftMagicNumbers.getBlock(material.getType());
		return readArgument(block, material).b();
	}

	private IBlockData readArgument(Block block, BlockDataStorage material) {
		IBlockData ib = block.n();
		return writeData(ib, ib.b().l(), material.getData());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static IBlockData writeData(IBlockData ib, BlockStateList blockStateList, String string) {
		if (string == null || string.trim().isEmpty())
			return ib;

		String key = "";
		String value = "";
		int set = 0;

		for (int i = 1; i < string.length() - 1; ++i) {
			char c = string.charAt(i);
			if (c == ',') {
				IBlockState ibj = blockStateList.a(key);
				if (ibj != null) {
					Optional optional = ibj.b(value);
					if (optional.isPresent())
						ib = ib.a(ibj, (Comparable) optional.get());
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
			IBlockState ibj = blockStateList.a(key);
			if (ibj != null) {
				Optional optional = ibj.b(value);
				if (optional.isPresent())
					ib = ib.a(ibj, (Comparable) optional.get());
			}
		}
		return ib;
	}

	@Override
	public ItemStack toItemStack(BlockDataStorage material) {
		Item item = CraftMagicNumbers.getItem(material.getType(), ParseUtils.getShort(material.getData()));
		return CraftItemStack.asBukkitCopy(item.ae_());
	}

	static Method getNmsChunkHandle = Ref.method(CraftChunk.class, "getHandle");

	@Override
	public Object getChunk(World world, int x, int z) {
		if (isPaperChunkRework)
			return ((CraftChunk) world.getChunkAt(x, z)).getHandle(ChunkStatus.n);
		return Ref.invoke(world.getChunkAt(x, z), getNmsChunkHandle);
	}

	@Override
	public void setBlock(Object objChunk, int x, int y, int z, Object IblockData, int data) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;
		WorldServer world = chunk.r;
		int highY = chunk.e(y);
		if (highY < 0)
			return;
		ChunkSection sc = chunk.b(highY);
		if (sc == null)
			return;
		BlockPosition pos = new BlockPosition(x, y, z);

		IBlockData iblock = IblockData == null ? Blocks.a.n() : (IBlockData) IblockData;

		boolean onlyModifyState = iblock.b() instanceof ITileEntity;

		// REMOVE TILE ENTITY IF NOT SAME TYPE
		TileEntity ent = chunk.k.get(pos);
		if (ent != null) {
			boolean shouldSkip = true;
			if (!onlyModifyState)
				shouldSkip = false;
			else if (onlyModifyState && !ent.q().b().getClass().equals(iblock.b().getClass())) {
				shouldSkip = false;
				onlyModifyState = false;
			}
			if (!shouldSkip)
				chunk.d(pos);
		}

		IBlockData old = sc.a(x & 15, y & 15, z & 15, iblock, false);

		// ADD TILE ENTITY
		if (iblock.b() instanceof ITileEntity && !onlyModifyState) {
			ent = ((ITileEntity) iblock.b()).a(pos, iblock);
			chunk.k.put(pos, ent);
			ent.a(chunk.r);
			Object packet = ent.h();
			BukkitLoader.getPacketHandler().send(chunk.r.getWorld().getPlayers(), packet);
		}

		// MARK CHUNK TO SAVE
		chunk.a(true);

		// POI
		if (!world.preventPoiUpdated)
			world.a(pos, old, iblock);
	}

	@Override
	public void updatePhysics(Object objChunk, int x, int y, int z, Object iblockdata) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;

		BlockPosition blockPos = new BlockPosition(x, y, z);

		doPhysicsAround(chunk.r, blockPos, ((IBlockData) iblockdata).b());
	}

	private void doPhysicsAround(WorldServer world, BlockPosition blockposition, Block block) {
		doPhysics(world, blockposition.g(), block, blockposition); // west
		doPhysics(world, blockposition.h(), block, blockposition); // east
		doPhysics(world, blockposition.d(), block, blockposition); // down
		doPhysics(world, blockposition.c(), block, blockposition); // up
		doPhysics(world, blockposition.e(), block, blockposition); // north
		doPhysics(world, blockposition.f(), block, blockposition); // south
	}

	private void doPhysics(WorldServer world, BlockPosition blockposition, Block block, BlockPosition blockposition1) {
		IBlockData state = world.a_(blockposition);
		state.a(world, blockposition, block, blockposition1, false);
		if (state.b() instanceof BlockFalling)
			((BlockFalling) state.b()).b(state, world, blockposition, block.n(), false);
	}

	@Override
	public void updateLightAt(Object objChunk, int x, int y, int z) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;
		chunk.r.k().a().a(new BlockPosition(x, y, z));
	}

	private static boolean isPaperChunkRework = Ref.getClass("io.papermc.paper.chunk.system.scheduling.ChunkHolderManager") != null;
	private static Method getBlockStateFinal = Ref.method(net.minecraft.world.level.chunk.Chunk.class, "getBlockStateFinal", int.class, int.class, int.class);

	@Override
	public Object getBlock(Object objChunk, int x, int y, int z) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;
		if (isPaperChunkRework)
			return Ref.invoke(chunk, getBlockStateFinal, x, y, z); // Modern getting of blocks, Thx PaperSpigot!
		int highY = chunk.e(y);
		if (highY < 0)
			return Blocks.a.n();
		ChunkSection sc = chunk.b(highY);
		if (sc == null)
			return Blocks.a.n();
		return sc.h().a(x & 15, y & 15, z & 15);
	}

	@Override
	public byte getData(Object chunk, int x, int y, int z) {
		return 0;
	}

	@Override
	public String getNBTOfTile(Object objChunk, int x, int y, int z) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;
		return chunk.a(new BlockPosition(x, y, z), EnumTileEntityState.a).n().toString();
	}

	@Override
	public void setNBTToTile(Object objChunk, int x, int y, int z, String nbt) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;
		TileEntity ent = chunk.a(new BlockPosition(x, y, z), EnumTileEntityState.a);
		NBTTagCompound parsedNbt = (NBTTagCompound) parseNBT(nbt);
		parsedNbt.a("x", x);
		parsedNbt.a("y", y);
		parsedNbt.a("z", z);
		ent.a(parsedNbt);
		Object packet = ent.h();
		BukkitLoader.getPacketHandler().send(chunk.r.getWorld().getPlayers(), packet);
	}

	@Override
	public boolean isTileEntity(Object objChunk, int x, int y, int z) {
		net.minecraft.world.level.chunk.Chunk chunk = (net.minecraft.world.level.chunk.Chunk) objChunk;
		return chunk.a(new BlockPosition(x, y, z), EnumTileEntityState.a) != null;
	}

	@Override
	public int getCombinedId(Object IblockDataOrBlock) {
		return Block.i((IBlockData) IblockDataOrBlock);
	}

	@Override
	public Object blockPosition(int blockX, int blockY, int blockZ) {
		return new BlockPosition(blockX, blockY, blockZ);
	}

	@Override
	public Object toIBlockData(Object data) {
		return ((CraftBlockData) data).getState();
	}

	@Override
	public Object toIBlockData(BlockState state) {
		return CraftMagicNumbers.getBlock(state.getType(), state.getRawData());
	}

	static Field bukkitChunk = Ref.field(net.minecraft.world.level.chunk.Chunk.class, "bukkitChunk");

	@Override
	public Chunk toBukkitChunk(Object nmsChunk) {
		if (isPaperChunkRework)
			return new CraftChunk((net.minecraft.world.level.chunk.Chunk) nmsChunk);
		return (Chunk) Ref.get(nmsChunk, bukkitChunk);
	}

	@Override
	public int getPing(Player player) {
		return ((EntityPlayer) getPlayer(player)).f;
	}

	@Override
	public Object getPlayerConnection(Player player) {
		return ((EntityPlayer) getPlayer(player)).c;
	}

	private Field playerNetwork = Ref.field(PlayerConnection.class, "h");

	@Override
	public Object getConnectionNetwork(Object playercon) {
		return Ref.get(playercon, playerNetwork);
	}

	@Override
	public Object getNetworkChannel(Object network) {
		return ((NetworkManager) network).m;
	}

	@Override
	public Object packetOpenWindow(int id, String legacy, int size, Component title) {
		Containers<?> windowType = Containers.a;
		switch (size) {
		case 0: {
			windowType = Containers.h;
			break;
		}
		case 18: {
			windowType = Containers.b;
			break;
		}
		case 27: {
			windowType = Containers.c;
			break;
		}
		case 36: {
			windowType = Containers.d;
			break;
		}
		case 45: {
			windowType = Containers.e;
			break;
		}
		case 54: {
			windowType = Containers.f;
			break;
		}
		}
		return new PacketPlayOutOpenWindow(id, windowType, (IChatBaseComponent) this.toIChatBaseComponent(title));
	}

	@Override
	public void closeGUI(Player player, Object container, boolean closePacket) {
		if (closePacket)
			BukkitLoader.getPacketHandler().send(player, new PacketPlayOutCloseWindow(BukkitLoader.getNmsProvider().getContainerId(container)));
		EntityPlayer nmsPlayer = (EntityPlayer) getPlayer(player);
		nmsPlayer.bR = nmsPlayer.bQ;
		((Container) container).transferTo(nmsPlayer.bR, (CraftPlayer) player);
	}

	@Override
	public void setSlot(Object container, int slot, Object item) {
		((Container) container).b(slot, (net.minecraft.world.item.ItemStack) item);
	}

	@Override
	public void setGUITitle(Player player, Object container, String legacy, int size, Component title) {
		int id = ((Container) container).j;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, legacy, size, title));
		net.minecraft.world.item.ItemStack carried = ((Container) container).g();
		if (!carried.b())
			BukkitLoader.getPacketHandler().send(player, new PacketPlayOutSetSlot(id, getContainerStateId(container), -1, carried));
		int slot = 0;
		for (net.minecraft.world.item.ItemStack item : ((Container) container).c()) {
			if (slot == size)
				break;
			if (!item.b())
				BukkitLoader.getPacketHandler().send(player, new PacketPlayOutSetSlot(id, getContainerStateId(container), slot, item));
			++slot;
		}
	}

	@Override
	public void openGUI(Player player, Object container, String legacy, int size, Component title) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		int id = ((Container) container).j;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, legacy, size, title));
		nmsPlayer.bR.transferTo((Container) container, (CraftPlayer) player);
		nmsPlayer.bR = (Container) container;
		nmsPlayer.a((Container) container);
		((Container) container).checkReachable = false;
	}

	@Override
	public void openAnvilGUI(Player player, Object container, Component title) {
		openGUI(player, container, "minecraft:anvil", 0, title);
	}

	public Object createContainer(Inventory inv, Player player) {
		if (inv.getType() == InventoryType.ANVIL) {
			ContainerAnvil container = new ContainerAnvil(((CraftPlayer) player).getHandle().nextContainerCounter(), ((CraftPlayer) player).getHandle().fN(), new ContainerAccess() {

				@Override
				public <T> Optional<T> a(BiFunction<net.minecraft.world.level.World, BlockPosition, T> getter) {
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
					container.b(slot++).d((net.minecraft.world.item.ItemStack) asNMSItem(stack));
			});
			return container;
		}
		return new CraftContainer(inv, ((CraftPlayer) player).getHandle(), ((CraftPlayer) player).getHandle().nextContainerCounter());
	}

	@Override
	public Object getSlotItem(Object container, int slot) {
		return slot < 0 ? null : ((Container) container).b(slot).e();
	}

	@Override
	public String getAnvilRenameText(Object anvil) {
		return ((ContainerAnvil) anvil).v;
	}

	public static int c(final int quickCraftData) {
		return quickCraftData >> 2 & 0x3;
	}

	public static int d(final int quickCraftData) {
		return quickCraftData & 0x3;
	}

	@Override
	public boolean processInvClickPacket(Player player, HolderGUI gui, Object provPacket) {
		PacketPlayInWindowClick packet = (PacketPlayInWindowClick) provPacket;
		int slot = packet.c();

		Object container = gui.getContainer(player);
		if (container == null)
			return false;

		int id = packet.a();
		int mouseClick = packet.d();
		InventoryClickType type = packet.g();

		if (packet.c() < -1 && packet.c() != -999)
			return true;

		Container c = (Container) container;
		EntityHuman nPlayer = ((CraftPlayer) player).getHandle();

		ItemStack newItem;
		ItemStack oldItem;
		switch (type) {
		case a: // PICKUP
			oldItem = asBukkitItem(getSlotItem(container, slot));
			newItem = asBukkitItem(c.g());
			if (slot > 0 && mouseClick != 0) {
				if (c.g().b()) { // pickup half
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
		case b: // QUICK_MOVE
			newItem = asBukkitItem(c.g());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case c:// SWAP
			newItem = asBukkitItem(nPlayer.fN().a(mouseClick));
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case d:// CLONE
			newItem = asBukkitItem(getSlotItem(container, slot));
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case e:// THROW
			if (c.g().b() && slot >= 0) {
				Slot slot3 = c.b(slot);
				newItem = asBukkitItem(slot3.e());
				if (mouseClick != 0 || newItem.getAmount() - 1 <= 0)
					newItem = new ItemStack(Material.AIR);
				else
					newItem.setAmount(newItem.getAmount() - 1);
			} else
				newItem = asBukkitItem(c.g());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case f:// QUICK_CRAFT
			newItem = asBukkitItem(c.g());
			oldItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(getSlotItem(container, slot));
			break;
		case g:// PICKUP_ALL
			newItem = asBukkitItem(c.g());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		default:
			newItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(packet.e());
			oldItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(packet.e());
			break;
		}

		if (oldItem.getType() == Material.AIR && newItem.getType() == Material.AIR)
			return true;

		boolean cancel = false;
		int gameSlot = slot > gui.size() - 1 ? InventoryUtils.convertToPlayerInvSlot(slot - gui.size()) : slot;

		ClickType clickType = InventoryUtils.buildClick(type == InventoryClickType.f ? 1 : type == InventoryClickType.b ? 2 : 0, mouseClick);
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
		int statusId = c.j();
		BukkitLoader.getPacketHandler().send(player, packetSetSlot(-1, -1, statusId, c.g()));
		switch (type) {
		case d:
			break;
		case c:
		case b:
		case g:
			c.b();
			break;
		default:
			BukkitLoader.getPacketHandler().send(player, packetSetSlot(id, slot, statusId, c.b(packet.c()).e()));
			break;
		}
		return true;
	}

	private void processEvent(Container c, InventoryClickType type, HolderGUI gui, Player player, int slot, int gameSlot, ItemStack newItem, ItemStack oldItem, PacketPlayInWindowClick packet,
			int mouseClick, ClickType clickType, EntityHuman nPlayer) {
		c.h();
		switch (type) {
		case b: {
			ItemStack[] contents = slot < gui.size() ? player.getInventory().getStorageContents() : gui.getInventory().getStorageContents();
			boolean interactWithResultSlot = false;
			if (gui instanceof AnvilGUI && slot < gui.size() && slot == 2)
				if (c.b(2).a(nPlayer))
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
						nPlayer.fN().a(modif.getKey(), (net.minecraft.world.item.ItemStack) asNMSItem(modif.getValue()));
					if (remaining == 0) {
						c.b(gameSlot).d((net.minecraft.world.item.ItemStack) asNMSItem(null));
						if (interactWithResultSlot) {
							c.b(0).d((net.minecraft.world.item.ItemStack) asNMSItem(null));
							c.b(1).d((net.minecraft.world.item.ItemStack) asNMSItem(null));
						}
					} else {
						newItem.setAmount(remaining);
						c.b(gameSlot).d((net.minecraft.world.item.ItemStack) asNMSItem(newItem));
					}
				} else {
					for (Entry<Integer, ItemStack> modif : modified.entrySet())
						c.b(modif.getKey()).d((net.minecraft.world.item.ItemStack) asNMSItem(modif.getValue())); // Visual & Nms side
					// Plugin & Bukkit side
					gui.getInventory().setStorageContents(contents);
					if (remaining == 0)
						nPlayer.fN().a(gameSlot, (net.minecraft.world.item.ItemStack) asNMSItem(null));
					else {
						newItem.setAmount(remaining);
						nPlayer.fN().a(gameSlot, (net.minecraft.world.item.ItemStack) asNMSItem(newItem));
					}
				}
			c.i();
			return;
		}
		default:
			processClick(gui, gui.getNotInterableSlots(player), c, slot, mouseClick, type, nPlayer);
			break;
		}
		postToMainThread(() -> {
			if (type != InventoryClickType.f && (c.a().equals(Containers.h) || c.a().equals(Containers.u)))
				c.b();
			for (final it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<net.minecraft.world.item.ItemStack> entry : Int2ObjectMaps.fastIterable(packet.f()))
				c.b(entry.getIntKey(), entry.getValue());
			c.a(packet.e());
			c.i();
			if (packet.h() != c.j())
				c.e();
			else
				c.d();
		});
	}

	private Method addAmount = Ref.method(Slot.class, "b", int.class);
	private Method checkItem = Ref.method(Container.class, "a", EntityHuman.class, ClickAction.class, Slot.class, net.minecraft.world.item.ItemStack.class, net.minecraft.world.item.ItemStack.class);

	@SuppressWarnings("unchecked")
	private void processClick(HolderGUI gui, List<Integer> ignoredSlots, Container container, int slotIndex, int button, InventoryClickType actionType, EntityHuman player) {
		if (actionType == InventoryClickType.f)
			processDragMove(gui, container, player, slotIndex, button);
		else {
			int u = (int) Ref.get(container, containerU);
			int j = getContainerStateId(container);
			Set<Slot> mod = (Set<Slot>) Ref.get(container, containerV);
			if (u != 0) {
				Ref.set(container, containerU, u = 0);
				mod.clear();
			} else if (actionType == InventoryClickType.a && (button == 0 || button == 1)) {
				ClickAction clickaction = button == 0 ? ClickAction.a : ClickAction.b;
				if (slotIndex == -999) {
					if (!container.g().b())
						if (clickaction == ClickAction.a) {
							net.minecraft.world.item.ItemStack carried = container.g();
							container.b(net.minecraft.world.item.ItemStack.b);
							postToMainThread(() -> player.a(carried, true));
						} else
							postToMainThread(() -> player.a(container.g().a(1), true));
				} else if (actionType == InventoryClickType.b) {
					if (slotIndex < 0)
						return;

					Slot slot = container.b(slotIndex);
					if (!slot.a(player))
						return;

					net.minecraft.world.item.ItemStack itemstack;
					for (itemstack = container.a(player, slotIndex); !itemstack.b() && net.minecraft.world.item.ItemStack.b(slot.e(), itemstack); itemstack = container.a(player, slotIndex)) {
					}
				} else {
					if (slotIndex < 0)
						return;
					Slot slot = container.b(slotIndex);
					net.minecraft.world.item.ItemStack itemstack = slot.e();
					net.minecraft.world.item.ItemStack itemstack4 = container.g();
					player.a(itemstack4, slot.e(), clickaction);
					if (!(boolean) Ref.invoke(container, checkItem, player, clickaction, slot, itemstack, itemstack4))
						if (itemstack.b()) {
							if (!itemstack4.b()) {
								int i2 = clickaction == ClickAction.a ? itemstack4.L() : 1;
								net.minecraft.world.item.ItemStack stack = slot.b(itemstack4, i2);
								container.b(stack);
							}
						} else if (slot.a(player))
							if (itemstack4.b()) {
								int i2 = clickaction == ClickAction.a ? itemstack.L() : (itemstack.L() + 1) / 2;
								Optional<net.minecraft.world.item.ItemStack> optional = slot.a(i2, Integer.MAX_VALUE, player);
								optional.ifPresent(i -> {
									container.b(i);
									slot.a(player, i);
								});
							} else if (slot.a(itemstack4)) {
								if (net.minecraft.world.item.ItemStack.c(itemstack, itemstack4)) {
									int i2 = clickaction == ClickAction.a ? itemstack4.L() : 1;
									net.minecraft.world.item.ItemStack stack = slot.b(itemstack4, i2);
									container.b(stack);
								} else if (itemstack4.L() <= slot.a_(itemstack4)) {
									container.b(itemstack);
									slot.d(itemstack4);
								}
							} else if (net.minecraft.world.item.ItemStack.c(itemstack, itemstack4)) {
								Optional<net.minecraft.world.item.ItemStack> optional2 = slot.a(itemstack.L(), itemstack4.g() - itemstack4.L(), player);
								optional2.ifPresent(i -> {
									itemstack.g(i.L());
									slot.a(player, i);
								});
							}
					slot.d();
					if (player instanceof EntityPlayer && slot.a() != 64) {
						BukkitLoader.getPacketHandler().send((Player) player.getBukkitEntity(), BukkitLoader.getNmsProvider().packetSetSlot(j, slot.e, container.k(), slot.e()));
						if (container.getBukkitView().getType() == InventoryType.WORKBENCH || container.getBukkitView().getType() == InventoryType.CRAFTING)
							BukkitLoader.getPacketHandler().send((Player) player.getBukkitEntity(), BukkitLoader.getNmsProvider().packetSetSlot(j, 0, container.k(), container.b(0).e()));
					}
				}
			} else if (actionType == InventoryClickType.c) {
				if (slotIndex < 0)
					return;
				PlayerInventory playerinventory = player.fN();
				Slot slot3 = container.b(slotIndex);
				net.minecraft.world.item.ItemStack itemstack2 = playerinventory.a(button);
				net.minecraft.world.item.ItemStack itemstack = slot3.e();
				if (!itemstack2.b() || !itemstack.b())
					if (itemstack2.b()) {
						if (slot3.a(player)) {
							playerinventory.a(button, itemstack);
							Ref.invoke(slot3, addAmount, itemstack.L());
							slot3.d(net.minecraft.world.item.ItemStack.b);
							slot3.a(player, itemstack);
						}
					} else if (itemstack.b()) {
						if (slot3.a(itemstack2)) {
							int j2 = slot3.a_(itemstack2);
							if (itemstack2.L() > j2)
								slot3.d(itemstack2.a(j2));
							else {
								playerinventory.a(button, net.minecraft.world.item.ItemStack.b);
								slot3.d(itemstack2);
							}
						}
					} else if (slot3.a(player) && slot3.a(itemstack2)) {
						int j2 = slot3.a_(itemstack2);
						if (itemstack2.L() > j2) {
							slot3.d(itemstack2.a(j2));
							slot3.a(player, itemstack);
							if (!playerinventory.e(itemstack))
								postToMainThread(() -> player.a(itemstack, true));
						} else {
							playerinventory.a(button, itemstack);
							slot3.d(itemstack2);
							slot3.a(player, itemstack);
						}
					}
			} else if (actionType == InventoryClickType.d && player.fO().d && container.g().b() && slotIndex >= 0) {
				Slot slot3 = container.b(slotIndex);
				if (slot3.f()) {
					net.minecraft.world.item.ItemStack itemstack2 = slot3.e();
					container.b(itemstack2.c(itemstack2.g()));
				}
			} else if (actionType == InventoryClickType.e && container.g().b() && slotIndex >= 0) {
				Slot slot3 = container.b(slotIndex);
				int m = button == 0 ? 1 : slot3.e().L();
				net.minecraft.world.item.ItemStack itemstack = slot3.b(m, Integer.MAX_VALUE, player);
				postToMainThread(() -> player.a(itemstack, true));
			} else if (actionType == InventoryClickType.g && slotIndex >= 0) {
				final Slot slot3 = container.i.get(slotIndex);
				final net.minecraft.world.item.ItemStack itemstack2 = container.g();
				if (!itemstack2.b() && (!slot3.f() || !slot3.a(player))) {
					List<Integer> ignoreSlots = ignoredSlots == null ? Collections.emptyList() : ignoredSlots;
					List<Integer> corruptedSlots = ignoredSlots == null ? Collections.emptyList() : new ArrayList<>();
					Map<Integer, ItemStack> modifiedSlots = new HashMap<>();
					Map<Integer, ItemStack> modifiedSlotsPlayerInv = new HashMap<>();
					final int l = button == 0 ? 0 : container.i.size() - 1;
					final int j2 = button == 0 ? 1 : -1;
					for (int i2 = 0; i2 < 2; ++i2)
						for (int slot = l; slot >= 0 && slot < container.i.size() && itemstack2.L() < itemstack2.g(); slot += j2) {
							final Slot slot4 = container.i.get(slot);
							if (slot4.f() && Container.a(slot4, itemstack2, true) && slot4.a(player) && container.a(itemstack2, slot4)) {
								final net.minecraft.world.item.ItemStack itemstack5 = slot4.e();
								if (i2 != 0 || itemstack5.L() != itemstack5.g()) {
									if (slot < gui.size() && ignoreSlots.contains(slot)) {
										corruptedSlots.add(slot);
										continue;
									}
									final net.minecraft.world.item.ItemStack itemstack6 = slot4.b(itemstack5.L(), itemstack2.g() - itemstack2.L(), player);
									itemstack2.g(itemstack6.L());
									int gameSlot = slot > gui.size() - 1 ? InventoryUtils.convertToPlayerInvSlot(slot - gui.size()) : slot;
									if (slot < gui.size())
										modifiedSlots.put(gameSlot, asBukkitItem(slot4.e()));
									else
										modifiedSlotsPlayerInv.put(gameSlot, asBukkitItem(slot4.e()));
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

	private Field containerU = Ref.field(Container.class, "u"), containerV = Ref.field(Container.class, "v"), containerT = Ref.field(Container.class, "t");

	@SuppressWarnings("unchecked")
	private void processDragMove(HolderGUI gui, Container container, EntityHuman player, int slot, int mouseClick) {
		int previous = (int) Ref.get(container, containerU);
		int u = d(mouseClick);
		Set<Slot> mod = (Set<Slot>) Ref.get(container, containerV);
		if ((previous != 1 || u != 2) && previous != u || container.g().b()) {
			mod.clear();
			u = 0;
		} else
			switch (u) {
			case 0: {
				int t = c(mouseClick);
				Ref.set(container, containerT, t);
				if (Container.a(t, player)) {
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
				final Slot bslot = container.b(slot);
				final net.minecraft.world.item.ItemStack itemstack = container.g();
				if (Container.a(bslot, itemstack, true) && bslot.a(itemstack) && (t == 2 || itemstack.L() > mod.size()) && container.b(bslot))
					mod.add(bslot);
				break;
			}
			case 2:
				if (!mod.isEmpty()) {
					final net.minecraft.world.item.ItemStack itemstack2 = container.g().p();
					if (itemstack2.b()) {
						mod.clear();
						Ref.set(container, containerU, 0);
						return;
					}
					int t = (int) Ref.get(container, containerT);
					int l = container.g().L();
					final Iterator<Slot> iterator = mod.iterator();
					final Map<Integer, net.minecraft.world.item.ItemStack> draggedSlots = new HashMap<>();
					while (iterator.hasNext()) {
						final Slot slot2 = iterator.next();
						final net.minecraft.world.item.ItemStack itemstack3 = container.g();
						if (slot2 != null && Container.a(slot2, itemstack3, true) && slot2.a(itemstack3) && (t == 2 || itemstack3.L() >= mod.size()) && container.b(slot2)) {
							final int j1 = slot2.f() ? slot2.e().L() : 0;
							final int k1 = Math.min(itemstack2.g(), slot2.a_(itemstack2));
							final int l2 = Math.min(Container.a(mod, t, itemstack2) + j1, k1);
							l -= l2 - j1;
							draggedSlots.put(slot2.e, itemstack2.c(l2));
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
					container.b(CraftItemStack.asNMSCopy(newcursor));
					if (!guiSlots.isEmpty() || !playerSlots.isEmpty())
						gui.onMultipleIteract((Player) player.getBukkitEntity(), guiSlots, playerSlots);
					for (final Entry<Integer, net.minecraft.world.item.ItemStack> dslot : draggedSlots.entrySet())
						view.setItem(dslot.getKey(), CraftItemStack.asBukkitCopy(dslot.getValue()));
					if (container.g() != null)
						container.b();
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
		PacketStatusOutServerInfo status = (PacketStatusOutServerInfo) packet;
		ServerPing ping = status.a();

		List<GameProfileHandler> gameProfiles = new ArrayList<>();
		for (GameProfile profile : ping.b().get().c())
			gameProfiles.add(fromGameProfile(profile));

		IChatBaseComponent motd = IChatBaseComponent.a("");
		Optional<ServerPingPlayerSample> players = Optional.empty();
		Optional<ServerData> onlineCount = Optional.empty();
		Optional<ServerPing.a> serverIcon = Optional.empty();
		boolean enforceSecureProfile = ping.e();

		String favicon = "server-icon.png";
		ServerListPingEvent event = new ServerListPingEvent(getOnlinePlayers().size(), Bukkit.getMaxPlayers(), gameProfiles, Bukkit.getMotd(), favicon,
				((InetSocketAddress) ((Channel) channel).remoteAddress()).getAddress(), ping.c().get().b(), ping.c().get().c());
		EventManager.call(event);
		if (event.isCancelled())
			return true;
		ServerPingPlayerSample playerSample = new ServerPingPlayerSample(event.getMaxPlayers(), event.getOnlinePlayers(), new ArrayList<>());
		if (event.getPlayersText() != null)
			for (GameProfileHandler s : event.getPlayersText())
				playerSample.c().add(new GameProfile(s.getUUID(), s.getUsername()));
		players = Optional.of(playerSample);

		if (event.getMotd() != null)
			motd = (IChatBaseComponent) this.toIChatBaseComponent(ComponentAPI.fromString(event.getMotd()));
		if (event.getVersion() != null)
			onlineCount = Optional.of(new ServerData(event.getVersion(), event.getProtocol()));
		if (event.getFalvicon() != null)
			if (!event.getFalvicon().equals("server-icon.png") && new File(event.getFalvicon()).exists()) {
				BufferedImage var1;
				try {
					var1 = ImageIO.read(new File(event.getFalvicon()));
					Preconditions.checkState(var1.getWidth() == 64, "Must be 64 pixels wide");
					Preconditions.checkState(var1.getHeight() == 64, "Must be 64 pixels high");
					ByteArrayOutputStream var2 = new ByteArrayOutputStream();
					ImageIO.write(var1, "PNG", var2);
					serverIcon = Optional.of(new ServerPing.a(var2.toByteArray()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else
				serverIcon = ping.d();
		Ref.set(status, "a", new ServerPing(motd, players, onlineCount, serverIcon, enforceSecureProfile));
		return false;
	}

	@Override
	public Object getNBT(Entity entity) {
		return ((CraftEntity) entity).getHandle().f(new NBTTagCompound());
	}

	@Override
	public Object setString(Object nbt, String path, String value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setInteger(Object nbt, String path, int value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setDouble(Object nbt, String path, double value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setLong(Object nbt, String path, long value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setShort(Object nbt, String path, short value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setFloat(Object nbt, String path, float value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setBoolean(Object nbt, String path, boolean value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setIntArray(Object nbt, String path, int[] value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setByteArray(Object nbt, String path, byte[] value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public Object setNBTBase(Object nbt, String path, Object value) {
		((NBTTagCompound) nbt).a(path, (NBTBase) value);
		return nbt;
	}

	@Override
	public String getString(Object nbt, String path) {
		return ((NBTTagCompound) nbt).l(path);
	}

	@Override
	public int getInteger(Object nbt, String path) {
		return ((NBTTagCompound) nbt).h(path);
	}

	@Override
	public double getDouble(Object nbt, String path) {
		return ((NBTTagCompound) nbt).i(path);
	}

	@Override
	public long getLong(Object nbt, String path) {
		return ((NBTTagCompound) nbt).i(path);
	}

	@Override
	public short getShort(Object nbt, String path) {
		return ((NBTTagCompound) nbt).g(path);
	}

	@Override
	public float getFloat(Object nbt, String path) {
		return ((NBTTagCompound) nbt).j(path);
	}

	@Override
	public boolean getBoolean(Object nbt, String path) {
		return ((NBTTagCompound) nbt).e(path);
	}

	@Override
	public int[] getIntArray(Object nbt, String path) {
		return ((NBTTagCompound) nbt).n(path);
	}

	@Override
	public byte[] getByteArray(Object nbt, String path) {
		return ((NBTTagCompound) nbt).m(path);
	}

	@Override
	public Object getNBTBase(Object nbt, String path) {
		return ((NBTTagCompound) nbt).c(path);
	}

	@Override
	public Set<String> getKeys(Object nbt) {
		return ((NBTTagCompound) nbt).e();
	}

	@Override
	public boolean hasKey(Object nbt, String path) {
		return ((NBTTagCompound) nbt).e(path);
	}

	@Override
	public void removeKey(Object nbt, String path) {
		((NBTTagCompound) nbt).r(path);
	}

	@Override
	public Object setByte(Object nbt, String path, byte value) {
		((NBTTagCompound) nbt).a(path, value);
		return nbt;
	}

	@Override
	public byte getByte(Object nbt, String path) {
		return ((NBTTagCompound) nbt).f(path);
	}

	@Override
	public Object getDataWatcher(Entity entity) {
		return ((CraftEntity) entity).getHandle().aj();
	}

	@Override
	public Object getDataWatcher(Object entity) {
		return ((net.minecraft.world.entity.Entity) entity).aj();
	}

	@Override
	public int incrementStateId(Object container) {
		return ((Container) container).k();
	}

	@Override
	public Object packetEntityHeadRotation(Entity entity) {
		return new PacketPlayOutEntityHeadRotation((net.minecraft.world.entity.Entity) getEntity(entity), (byte) (entity.getLocation().getYaw() * 256F / 360F));
	}

	@Override
	public Object packetHeldItemSlot(int slot) {
		return new PacketPlayOutHeldItemSlot(slot);
	}

	@Override
	public Object packetExp(float exp, int total, int toNextLevel) {
		return new PacketPlayOutExperience(exp, total, toNextLevel);
	}

	@Override
	public Object packetPlayerInfo(PlayerInfoType type, Player player) {
		a action = null;
		switch (type) {
		case ADD_PLAYER:
			action = a.a;
			break;
		case REMOVE_PLAYER:
			return new ClientboundPlayerInfoRemovePacket(Arrays.asList(player.getUniqueId()));
		case UPDATE_DISPLAY_NAME:
			action = a.f;
			break;
		case UPDATE_GAME_MODE:
			action = a.c;
			break;
		case UPDATE_LATENCY:
			action = a.e;
			break;
		}
		return new ClientboundPlayerInfoUpdatePacket(action, (EntityPlayer) getPlayer(player));
	}

	static boolean MODERN_CLIENTBOUND_PACKET = Ref.constructor(ClientboundPlayerInfoUpdatePacket.class, EnumSet.class, List.class) != null ? false : true;
	static Field setField, listField;
	static Constructor<?> clientboundConstructor;

	static {
		if (MODERN_CLIENTBOUND_PACKET) {
			setField = Ref.field(ClientboundPlayerInfoUpdatePacket.class, "a");
			listField = Ref.field(ClientboundPlayerInfoUpdatePacket.class, "b");
		} else
			clientboundConstructor = Ref.constructor(ClientboundPlayerInfoUpdatePacket.class, EnumSet.class, List.class);
	}

	@Override
	public Object packetPlayerInfo(PlayerInfoType type, GameProfileHandler gameProfile, int latency, GameMode gameMode, Component playerName) {
		a action = null;
		switch (type) {
		case ADD_PLAYER:
			action = a.a;
			break;
		case REMOVE_PLAYER:
			return new ClientboundPlayerInfoRemovePacket(Arrays.asList(gameProfile.getUUID()));
		case UPDATE_DISPLAY_NAME:
			action = a.f;
			break;
		case UPDATE_GAME_MODE:
			action = a.c;
			break;
		case UPDATE_LATENCY:
			action = a.e;
			break;
		}
		EnumSet<a> set = EnumSet.of(action);
		List<b> list = new ArrayList<>();
		ClientboundPlayerInfoUpdatePacket packet;
		if (MODERN_CLIENTBOUND_PACKET) {
			packet = (ClientboundPlayerInfoUpdatePacket) Ref.newUnsafeInstance(ClientboundPlayerInfoUpdatePacket.class);
			Ref.set(packet, setField, set);
			Ref.set(packet, listField, list);
		} else
			packet = (ClientboundPlayerInfoUpdatePacket) Ref.newInstance(clientboundConstructor, set, list);
		packet.c().add(new b(gameProfile.getUUID(), (GameProfile) toGameProfile(gameProfile), true, latency, gameMode == null ? EnumGamemode.a : EnumGamemode.a(gameMode.name().toLowerCase()),
				(IChatBaseComponent) (playerName == null ? toIChatBaseComponent(new Component(gameProfile.getUsername())) : toIChatBaseComponent(playerName)), null));
		return packet;
	}

	@Override
	public Object packetPosition(double x, double y, double z, float yaw, float pitch) {
		return new PacketPlayOutPosition(x, y, z, yaw, pitch, Collections.emptySet(), 0);
	}

	@Override
	public Object packetRespawn(Player player) {
		EntityPlayer entityPlayer = (EntityPlayer) getPlayer(player);

		WorldServer worldserver = entityPlayer.x();
		return new PacketPlayOutRespawn(worldserver.aa(), worldserver.ac(), BiomeManager.a(worldserver.A()), entityPlayer.e.b(), entityPlayer.e.c(), worldserver.af(), worldserver.z(), (byte) 1,
				entityPlayer.gm(), entityPlayer.ar());
	}

	@Override
	public String getProviderName() {
		return "1_20_R1 (1.20)";
	}

	@Override
	public int getContainerStateId(Object container) {
		return ((Container) container).j();
	}

	@Override
	public void loadParticles() {
		for (Entry<ResourceKey<Particle<?>>, Particle<?>> s : BuiltInRegistries.k.g())
			me.devtec.theapi.bukkit.game.particles.Particle.identifier.put(s.getKey().a().a(), s.getValue());
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
		return ((EntityPlayer) nmsPlayer).fM();
	}

}
