package me.devtec.theapi.bukkit.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_14_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import io.netty.channel.Channel;
import me.devtec.shared.Ref;
import me.devtec.shared.components.ClickEvent;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.components.HoverEvent;
import me.devtec.shared.events.EventManager;
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
import me.devtec.theapi.bukkit.tablist.TabEntry;
import me.devtec.theapi.bukkit.tablist.Tablist;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockFalling;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.BlockStateList;
import net.minecraft.server.v1_14_R1.Blocks;
import net.minecraft.server.v1_14_R1.ChatClickable;
import net.minecraft.server.v1_14_R1.ChatClickable.EnumClickAction;
import net.minecraft.server.v1_14_R1.ChatComponentText;
import net.minecraft.server.v1_14_R1.ChatHoverable;
import net.minecraft.server.v1_14_R1.ChatHoverable.EnumHoverAction;
import net.minecraft.server.v1_14_R1.ChatMessageType;
import net.minecraft.server.v1_14_R1.ChatModifier;
import net.minecraft.server.v1_14_R1.Chunk.EnumTileEntityState;
import net.minecraft.server.v1_14_R1.ChunkSection;
import net.minecraft.server.v1_14_R1.Container;
import net.minecraft.server.v1_14_R1.ContainerAccess;
import net.minecraft.server.v1_14_R1.ContainerAnvil;
import net.minecraft.server.v1_14_R1.Containers;
import net.minecraft.server.v1_14_R1.EntityHuman;
import net.minecraft.server.v1_14_R1.EntityLiving;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.EnumChatFormat;
import net.minecraft.server.v1_14_R1.EnumGamemode;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.IBlockState;
import net.minecraft.server.v1_14_R1.IChatBaseComponent;
import net.minecraft.server.v1_14_R1.IRegistry;
import net.minecraft.server.v1_14_R1.IScoreboardCriteria.EnumScoreboardHealthDisplay;
import net.minecraft.server.v1_14_R1.ITileEntity;
import net.minecraft.server.v1_14_R1.InventoryClickType;
import net.minecraft.server.v1_14_R1.Item;
import net.minecraft.server.v1_14_R1.MathHelper;
import net.minecraft.server.v1_14_R1.MinecraftKey;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.MojangsonParser;
import net.minecraft.server.v1_14_R1.NBTBase;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.NetworkManager;
import net.minecraft.server.v1_14_R1.NonNullList;
import net.minecraft.server.v1_14_R1.PacketPlayInWindowClick;
import net.minecraft.server.v1_14_R1.PacketPlayOutBlockChange;
import net.minecraft.server.v1_14_R1.PacketPlayOutChat;
import net.minecraft.server.v1_14_R1.PacketPlayOutCloseWindow;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_14_R1.PacketPlayOutExperience;
import net.minecraft.server.v1_14_R1.PacketPlayOutHeldItemSlot;
import net.minecraft.server.v1_14_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_14_R1.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_14_R1.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_14_R1.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_14_R1.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_14_R1.PacketPlayOutPosition;
import net.minecraft.server.v1_14_R1.PacketPlayOutResourcePackSend;
import net.minecraft.server.v1_14_R1.PacketPlayOutRespawn;
import net.minecraft.server.v1_14_R1.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_14_R1.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_14_R1.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_14_R1.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_14_R1.PacketPlayOutSetSlot;
import net.minecraft.server.v1_14_R1.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_14_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_14_R1.PacketPlayOutTitle;
import net.minecraft.server.v1_14_R1.PacketPlayOutTitle.EnumTitleAction;
import net.minecraft.server.v1_14_R1.PacketPlayOutTransaction;
import net.minecraft.server.v1_14_R1.PacketStatusOutServerInfo;
import net.minecraft.server.v1_14_R1.PlayerConnection;
import net.minecraft.server.v1_14_R1.PlayerInventory;
import net.minecraft.server.v1_14_R1.ScoreboardObjective;
import net.minecraft.server.v1_14_R1.ScoreboardServer;
import net.minecraft.server.v1_14_R1.ServerPing;
import net.minecraft.server.v1_14_R1.ServerPing.ServerData;
import net.minecraft.server.v1_14_R1.ServerPing.ServerPingPlayerSample;
import net.minecraft.server.v1_14_R1.Slot;
import net.minecraft.server.v1_14_R1.TileEntity;
import net.minecraft.server.v1_14_R1.WorldServer;

public class v1_14_R1 implements NmsProvider {
	private static final MinecraftServer server = MinecraftServer.getServer();
	private static Field pos = Ref.field(PacketPlayOutBlockChange.class, "a");
	private static final ChatComponentText empty = new ChatComponentText("");

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
		return ((CraftChunk) chunk).getHandle();
	}

	@Override
	public int getEntityId(Object entity) {
		return ((net.minecraft.server.v1_14_R1.Entity) entity).getId();
	}

	@Override
	public Object getScoreboardAction(Action type) {
		return ScoreboardServer.Action.valueOf(type.name());
	}

	@Override
	public Object getEnumScoreboardHealthDisplay(DisplayType type) {
		return EnumScoreboardHealthDisplay.valueOf(type.name());
	}

	@Override
	public Object getNBT(ItemStack itemStack) {
		return ((net.minecraft.server.v1_14_R1.ItemStack) asNMSItem(itemStack)).getOrCreateTag();
	}

	@Override
	public Object parseNBT(String json) {
		try {
			return MojangsonParser.parse(json);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public ItemStack setNBT(ItemStack stack, Object nbt) {
		if (nbt instanceof NBTEdit)
			nbt = ((NBTEdit) nbt).getNBT();
		net.minecraft.server.v1_14_R1.ItemStack i = (net.minecraft.server.v1_14_R1.ItemStack) asNMSItem(stack);
		i.setTag((NBTTagCompound) nbt);
		return asBukkitItem(i);
	}

	@Override
	public Object asNMSItem(ItemStack stack) {
		if (stack == null)
			return net.minecraft.server.v1_14_R1.ItemStack.a;
		return CraftItemStack.asNMSCopy(stack);
	}

	@Override
	public ItemStack asBukkitItem(Object stack) {
		return CraftItemStack.asBukkitCopy(stack == null ? net.minecraft.server.v1_14_R1.ItemStack.a : (net.minecraft.server.v1_14_R1.ItemStack) stack);
	}

	@Override
	public Object packetOpenWindow(int id, String legacy, int size, Component title) {
		Containers<?> windowType = Containers.GENERIC_9X1;
		switch (size) {
		case 0: {
			windowType = Containers.ANVIL;
			break;
		}
		case 18: {
			windowType = Containers.GENERIC_9X2;
			break;
		}
		case 27: {
			windowType = Containers.GENERIC_9X3;
			break;
		}
		case 36: {
			windowType = Containers.GENERIC_9X4;
			break;
		}
		case 45: {
			windowType = Containers.GENERIC_9X5;
			break;
		}
		case 54: {
			windowType = Containers.GENERIC_9X6;
			break;
		}
		}
		return new PacketPlayOutOpenWindow(id, windowType, (IChatBaseComponent) this.toIChatBaseComponent(title));
	}

	@Override
	public int getContainerId(Object container) {
		return ((Container) container).windowId;
	}

	@Override
	public Object packetResourcePackSend(String url, String hash, boolean requireRP, Component prompt) {
		return new PacketPlayOutResourcePackSend(url, hash);
	}

	@Override
	public Object packetSetSlot(int container, int slot, int stateId, Object itemStack) {
		return new PacketPlayOutSetSlot(container, slot, (net.minecraft.server.v1_14_R1.ItemStack) (itemStack == null ? asNMSItem(null) : itemStack));
	}

	public Object packetSetSlot(int container, int slot, Object itemStack) {
		return this.packetSetSlot(container, slot, 0, itemStack);
	}

	@Override
	public Object packetEntityMetadata(int entityId, Object dataWatcher, boolean bal) {
		return new PacketPlayOutEntityMetadata(entityId, (net.minecraft.server.v1_14_R1.DataWatcher) dataWatcher, bal);
	}

	@Override
	public Object packetEntityDestroy(int... ids) {
		return new PacketPlayOutEntityDestroy(ids);
	}

	@Override
	public Object packetSpawnEntity(Object entity, int id) {
		return new PacketPlayOutSpawnEntity((net.minecraft.server.v1_14_R1.Entity) entity, id);
	}

	@Override
	public Object packetNamedEntitySpawn(Object player) {
		return new PacketPlayOutNamedEntitySpawn((EntityHuman) player);
	}

	@Override
	public Object packetSpawnEntityLiving(Object entityLiving) {
		return new PacketPlayOutSpawnEntityLiving((EntityLiving) entityLiving);
	}

	@Override
	public Object packetPlayerListHeaderFooter(Component header, Component footer) {
		PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
		packet.header = (IChatBaseComponent) this.toIChatBaseComponent(header);
		packet.footer = (IChatBaseComponent) this.toIChatBaseComponent(footer);
		return packet;
	}

	@Override
	public Object packetBlockChange(int x, int y, int z, Object iblockdata, int data) {
		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange();
		packet.block = iblockdata == null ? Blocks.AIR.getBlockData() : (IBlockData) iblockdata;
		try {
			v1_14_R1.pos.set(packet, new BlockPosition(x, y, z));
		} catch (Exception e) {
		}
		return packet;
	}

	@Override
	public Object packetScoreboardObjective() {
		return new PacketPlayOutScoreboardObjective();
	}

	@Override
	public Object packetScoreboardDisplayObjective(int id, Object scoreboardObjective) {
		return new PacketPlayOutScoreboardDisplayObjective(id, scoreboardObjective == null ? null : (ScoreboardObjective) scoreboardObjective);
	}

	@Override
	public Object packetScoreboardTeam() {
		return new PacketPlayOutScoreboardTeam();
	}

	@Override
	public Object packetScoreboardScore(Action action, String player, String line, int score) {
		return new PacketPlayOutScoreboardScore((ScoreboardServer.Action) getScoreboardAction(action), player, line, score);
	}

	@Override
	public Object packetTitle(TitleAction action, Component text, int fadeIn, int stay, int fadeOut) {
		return new PacketPlayOutTitle(EnumTitleAction.valueOf(action.name()), (IChatBaseComponent) this.toIChatBaseComponent(text), fadeIn, stay, fadeOut);
	}

	@Override
	public Object packetChat(ChatType type, Object chatBase, UUID uuid) {
		return new PacketPlayOutChat((IChatBaseComponent) chatBase, ChatMessageType.valueOf(type.name()));
	}

	@Override
	public Object packetChat(ChatType type, Component text, UUID uuid) {
		return this.packetChat(type, this.toIChatBaseComponent(text), uuid);
	}

	@Override
	public void postToMainThread(Runnable runnable) {
		v1_14_R1.server.executeSync(runnable);
	}

	@Override
	public Object getMinecraftServer() {
		return v1_14_R1.server;
	}

	@Override
	public Thread getServerThread() {
		return v1_14_R1.server.serverThread;
	}

	@Override
	public double[] getServerTPS() {
		return v1_14_R1.server.recentTps;
	}

	private IChatBaseComponent convert(Component c) {
		ChatComponentText current = new ChatComponentText(c.getText());
		ChatModifier modif = current.getChatModifier();
		if (c.getColor() != null)
			modif.setColor(EnumChatFormat.c(c.getColor()));
		if (c.getClickEvent() != null)
			modif.setChatClickable(new ChatClickable(EnumClickAction.valueOf(c.getClickEvent().getAction().name()), c.getClickEvent().getValue()));
		if (c.getHoverEvent() != null)
			modif.setChatHoverable(new ChatHoverable(EnumHoverAction.valueOf(c.getHoverEvent().getAction().name()), (IChatBaseComponent) this.toIChatBaseComponent(c.getHoverEvent().getValue())));
		modif.setBold(c.isBold());
		modif.setItalic(c.isItalic());
		modif.setRandom(c.isObfuscated());
		modif.setUnderline(c.isUnderlined());
		modif.setStrikethrough(c.isStrikethrough());
		return current.setChatModifier(modif);
	}

	@Override
	public Object[] toIChatBaseComponents(List<Component> components) {
		List<IChatBaseComponent> chat = new ArrayList<>();
		chat.add(new ChatComponentText(""));
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
		List<IChatBaseComponent> chat = new ArrayList<>();
		chat.add(new ChatComponentText(""));
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
		ChatComponentText main = new ChatComponentText("");
		List<IChatBaseComponent> chat = new ArrayList<>();
		if (co.getText() != null && !co.getText().isEmpty())
			chat.add(convert(co));
		if (co.getExtra() != null)
			for (Component c : co.getExtra()) {
				chat.add(convert(c));
				if (c.getExtra() != null)
					addConverted(chat, c.getExtra());
			}
		main.getSiblings().addAll(chat);
		return main.getSiblings().isEmpty() ? v1_14_R1.empty : main;
	}

	@Override
	public Object toIChatBaseComponent(List<Component> cc) {
		ChatComponentText main = new ChatComponentText("");
		for (Component c : cc)
			main.getSiblings().add((IChatBaseComponent) this.toIChatBaseComponent(c));
		return main.getSiblings().isEmpty() ? v1_14_R1.empty : main;
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
		if (component.getText().isEmpty()) {
			Component comp = new Component("");
			if (!component.getSiblings().isEmpty()) {
				List<Component> extra = new ArrayList<>();
				for (IChatBaseComponent base : component.getSiblings())
					extra.add(fromIChatBaseComponent(base));
				comp.setExtra(extra);
			}
			return comp;
		}
		Component comp = new Component(component.getText());
		ChatModifier modif = component.getChatModifier();
		if (modif.getColor() != null)
			comp.setColor(modif.getColor().name().toLowerCase());

		if (modif.getClickEvent() != null)
			comp.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(modif.getClickEvent().a().name()), modif.getClickEvent().b()));

		if (modif.getHoverEvent() != null)
			comp.setHoverEvent(new HoverEvent(HoverEvent.Action.valueOf(modif.getHoverEvent().a().b()), fromIChatBaseComponent(modif.getHoverEvent().b())));

		comp.setBold(modif.isBold());
		comp.setItalic(modif.isItalic());
		comp.setObfuscated(modif.isRandom());
		comp.setUnderlined(modif.isUnderlined());
		comp.setStrikethrough(modif.isStrikethrough());

		if (!component.getSiblings().isEmpty()) {
			List<Component> extra = new ArrayList<>();
			for (IChatBaseComponent base : component.getSiblings())
				extra.add(fromIChatBaseComponent(base));
			comp.setExtra(extra);
		}
		return comp;
	}

	@Override
	public BlockDataStorage toMaterial(Object blockOrIBlockData) {
		if (blockOrIBlockData instanceof Block) {
			IBlockData data = ((Block) blockOrIBlockData).getBlockData();
			return new BlockDataStorage(CraftMagicNumbers.getMaterial(data.getBlock()), (byte) 0, asString(data));
		}
		if (blockOrIBlockData instanceof IBlockData) {
			IBlockData data = (IBlockData) blockOrIBlockData;
			return new BlockDataStorage(CraftMagicNumbers.getMaterial(data.getBlock()), (byte) 0, asString(data));
		}
		return new BlockDataStorage(Material.AIR);
	}

	private static Function<Entry<IBlockState<?>, Comparable<?>>, String> STATE_TO_VALUE = new Function<Entry<IBlockState<?>, Comparable<?>>, String>() {
		@Override
		public String apply(Entry<IBlockState<?>, Comparable<?>> var0) {
			if (var0 == null)
				return "<NULL>";
			IBlockState<?> var1 = var0.getKey();
			return var1.a() + "=" + a(var1, var0.getValue());
		}

		@SuppressWarnings("unchecked")
		private <T extends Comparable<T>> String a(IBlockState<T> var0, Comparable<?> var1) {
			return var0.a((T) var1);
		}
	};

	private String asString(IBlockData data) {
		StringBuilder stateString = new StringBuilder();
		if (!data.getStateMap().isEmpty()) {
			stateString.append('[');
			stateString.append(data.getStateMap().entrySet().stream().map(STATE_TO_VALUE).collect(Collectors.joining(",")));
			stateString.append(']');
		}
		return stateString.toString();
	}

	@Override
	public Object toIBlockData(BlockDataStorage material) {
		if (material == null || material.getType() == null || material.getType() == Material.AIR)
			return Blocks.AIR.getBlockData();
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

	private IBlockData readArgument(Block block, BlockDataStorage material) {
		IBlockData ib = block.getBlockData();
		return writeData(ib, ib.getBlock().getStates(), material.getData());
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
						ib = ib.set(ibj, (Comparable) optional.get());
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
					ib = ib.set(ibj, (Comparable) optional.get());
			}
		}
		return ib;
	}

	@Override
	public ItemStack toItemStack(BlockDataStorage material) {
		Item item = CraftMagicNumbers.getItem(material.getType(), ParseUtils.getShort(material.getData()));
		return CraftItemStack.asBukkitCopy(new net.minecraft.server.v1_14_R1.ItemStack(item));
	}

	@Override
	public Object getChunk(World world, int x, int z) {
		return ((CraftChunk) world.getChunkAt(x, z)).getHandle();
	}

	static Field blockNbt = Ref.field(net.minecraft.server.v1_14_R1.Chunk.class, "e");

	@Override
	public void setBlock(Object objChunk, int x, int y, int z, Object IblockData, int data) {
		net.minecraft.server.v1_14_R1.Chunk chunk = (net.minecraft.server.v1_14_R1.Chunk) objChunk;
		if (y < 0)
			return;
		ChunkSection sc = chunk.getSections()[y >> 4];
		if (sc == null)
			return;
		BlockPosition pos = new BlockPosition(x, y, z);

		IBlockData iblock = IblockData == null ? Blocks.AIR.getBlockData() : (IBlockData) IblockData;

		boolean onlyModifyState = iblock.getBlock() instanceof ITileEntity;

		// REMOVE TILE ENTITY IF NOT SAME TYPE
		TileEntity ent = chunk.tileEntities.get(pos);
		if (ent != null) {
			boolean shouldSkip = true;
			if (!onlyModifyState)
				shouldSkip = false;
			else if (onlyModifyState && !ent.getBlock().getBlock().getClass().equals(iblock.getBlock().getClass())) {
				shouldSkip = false;
				onlyModifyState = false;
			}
			if (!shouldSkip)
				chunk.removeTileEntity(pos);
		}

		IBlockData old = sc.setType(x & 15, y & 15, z & 15, iblock, false);

		// ADD TILE ENTITY
		if (iblock.getBlock() instanceof ITileEntity && !onlyModifyState) {
			ent = ((ITileEntity) iblock.getBlock()).createTile(chunk);
			chunk.tileEntities.put(pos, ent);
			ent.setWorld(chunk.world);
			ent.setPosition(pos);
			Ref.set(ent, "c", iblock);
			Object packet = ent.getUpdatePacket();
			BukkitLoader.getPacketHandler().send(chunk.world.getWorld().getPlayers(), packet);
		}

		// MARK CHUNK TO SAVE
		chunk.setNeedsSaving(true);

		// POI
		chunk.world.a(pos, old, iblock);
	}

	@Override
	public void updatePhysics(Object objChunk, int x, int y, int z, Object iblockdata) {
		net.minecraft.server.v1_14_R1.Chunk chunk = (net.minecraft.server.v1_14_R1.Chunk) objChunk;

		BlockPosition blockPos = new BlockPosition(x, y, z);

		doPhysicsAround((WorldServer) chunk.world, blockPos, ((IBlockData) iblockdata).getBlock());
	}

	private void doPhysicsAround(WorldServer world, BlockPosition blockposition, Block block) {
		doPhysics(world, blockposition.west(), block, blockposition);
		doPhysics(world, blockposition.east(), block, blockposition);
		doPhysics(world, blockposition.down(), block, blockposition);
		doPhysics(world, blockposition.up(), block, blockposition);
		doPhysics(world, blockposition.north(), block, blockposition);
		doPhysics(world, blockposition.south(), block, blockposition);
	}

	private void doPhysics(WorldServer world, BlockPosition blockposition, Block block, BlockPosition blockposition1) {
		world.getType(blockposition).doPhysics(world, blockposition, block, blockposition1, false);
		IBlockData state = world.getType(blockposition);
		state.doPhysics(world, blockposition, block, blockposition1, false);
		if (state.getBlock() instanceof BlockFalling)
			((BlockFalling) state.getBlock()).onPlace(state, world, blockposition, block.getBlockData(), false);
	}

	@Override
	public void updateLightAt(Object chunk, int x, int y, int z) {
		net.minecraft.server.v1_14_R1.Chunk c = (net.minecraft.server.v1_14_R1.Chunk) chunk;
		c.world.getChunkProvider().getLightEngine().a(new BlockPosition(x, y, z));
	}

	@Override
	public Object getBlock(Object objChunk, int x, int y, int z) {
		net.minecraft.server.v1_14_R1.Chunk chunk = (net.minecraft.server.v1_14_R1.Chunk) objChunk;
		if (y < 0)
			return Blocks.AIR.getBlockData();
		ChunkSection sc = chunk.getSections()[y >> 4];
		if (sc == null)
			return Blocks.AIR.getBlockData();
		return sc.getBlocks().a(x & 15, y & 15, z & 15);
	}

	@Override
	public byte getData(Object chunk, int x, int y, int z) {
		return 0;
	}

	@Override
	public String getNBTOfTile(Object objChunk, int x, int y, int z) {
		net.minecraft.server.v1_14_R1.Chunk chunk = (net.minecraft.server.v1_14_R1.Chunk) objChunk;
		return chunk.a(new BlockPosition(x, y, z), EnumTileEntityState.IMMEDIATE).save(new NBTTagCompound()).toString();
	}

	@Override
	public void setNBTToTile(Object objChunk, int x, int y, int z, String nbt) {
		net.minecraft.server.v1_14_R1.Chunk chunk = (net.minecraft.server.v1_14_R1.Chunk) objChunk;
		TileEntity ent = chunk.a(new BlockPosition(x, y, z), EnumTileEntityState.IMMEDIATE);
		NBTTagCompound parsedNbt = (NBTTagCompound) parseNBT(nbt);
		parsedNbt.setInt("x", x);
		parsedNbt.setInt("y", y);
		parsedNbt.setInt("z", z);
		ent.load(parsedNbt);
		Object packet = ent.getUpdatePacket();
		BukkitLoader.getPacketHandler().send(chunk.world.getWorld().getPlayers(), packet);
	}

	@Override
	public boolean isTileEntity(Object objChunk, int x, int y, int z) {
		net.minecraft.server.v1_14_R1.Chunk chunk = (net.minecraft.server.v1_14_R1.Chunk) objChunk;
		return chunk.a(new BlockPosition(x, y, z), EnumTileEntityState.IMMEDIATE) != null;
	}

	@Override
	public int getCombinedId(Object IblockDataOrBlock) {
		return Block.getCombinedId((IBlockData) IblockDataOrBlock);
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

	@Override
	public Chunk toBukkitChunk(Object nmsChunk) {
		return ((net.minecraft.server.v1_14_R1.Chunk) nmsChunk).bukkitChunk;
	}

	@Override
	public int getPing(Player player) {
		return ((EntityPlayer) getPlayer(player)).ping;
	}

	@Override
	public Object getPlayerConnection(Player player) {
		return ((EntityPlayer) getPlayer(player)).playerConnection;
	}

	@Override
	public Object getConnectionNetwork(Object playercon) {
		return ((PlayerConnection) playercon).networkManager;
	}

	@Override
	public Object getNetworkChannel(Object network) {
		return ((NetworkManager) network).channel;
	}

	@Override
	public void closeGUI(Player player, Object container, boolean closePacket) {
		if (closePacket)
			BukkitLoader.getPacketHandler().send(player, new PacketPlayOutCloseWindow(BukkitLoader.getNmsProvider().getContainerId(container)));
		EntityPlayer nmsPlayer = (EntityPlayer) getPlayer(player);
		nmsPlayer.activeContainer = nmsPlayer.defaultContainer;
		((Container) container).transferTo(nmsPlayer.activeContainer, (CraftPlayer) player);
	}

	@Override
	public void setSlot(Object container, int slot, Object item) {
		((Container) container).setItem(slot, (net.minecraft.server.v1_14_R1.ItemStack) item);
	}

	@Override
	public void setGUITitle(Player player, Object container, String legacy, int size, Component title) {
		int id = ((Container) container).windowId;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, legacy, size, title));
		net.minecraft.server.v1_14_R1.ItemStack carried = ((CraftPlayer) player).getHandle().inventory.getCarried();
		if (!carried.isEmpty())
			BukkitLoader.getPacketHandler().send(player, new PacketPlayOutSetSlot(id, -1, carried));
		int slot = 0;
		for (net.minecraft.server.v1_14_R1.ItemStack item : ((Container) container).b()) {
			if (slot == size)
				break;
			if (!item.isEmpty())
				BukkitLoader.getPacketHandler().send(player, new PacketPlayOutSetSlot(id, slot, item));
			++slot;
		}
	}

	@Override
	public void openGUI(Player player, Object container, String legacy, int size, Component title) {
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		int id = ((Container) container).windowId;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, legacy, size, title));
		nmsPlayer.activeContainer.transferTo((Container) container, (CraftPlayer) player);
		nmsPlayer.activeContainer = (Container) container;
		((Container) container).addSlotListener(nmsPlayer);
		((Container) container).checkReachable = false;
	}

	@Override
	public void openAnvilGUI(Player player, Object con, Component title) {
		ContainerAnvil container = (ContainerAnvil) con;
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		int id = container.windowId;
		BukkitLoader.getPacketHandler().send(player, packetOpenWindow(id, "minecraft:anvil", 0, title));
		net.minecraft.server.v1_14_R1.ItemStack carried = nmsPlayer.inventory.getCarried();
		if (!carried.isEmpty())
			BukkitLoader.getPacketHandler().send(player, new PacketPlayOutSetSlot(id, -1, carried));
		int slot = 0;
		for (net.minecraft.server.v1_14_R1.ItemStack item : container.b()) {
			if (slot == 3)
				break;
			if (!item.isEmpty())
				BukkitLoader.getPacketHandler().send(player, new PacketPlayOutSetSlot(id, slot, item));
			++slot;
		}
		nmsPlayer.activeContainer.transferTo(container, (CraftPlayer) player);
		nmsPlayer.activeContainer = container;
		container.addSlotListener(nmsPlayer);
		container.checkReachable = false;
	}

	@Override
	public Object createContainer(Inventory inv, Player player) {
		return inv.getType() == InventoryType.ANVIL ? createAnvilContainer(inv, player)
				: new CraftContainer(inv, ((CraftPlayer) player).getHandle(), ((CraftPlayer) player).getHandle().nextContainerCounter());
	}

	@Override
	public Object getSlotItem(Object container, int slot) {
		return slot < 0 ? null : ((Container) container).getSlot(slot).getItem();
	}

	static BlockPosition zero = new BlockPosition(0, 0, 0);

	public Object createAnvilContainer(Inventory inv, Player player) {
		ContainerAnvil container = new ContainerAnvil(((CraftPlayer) player).getHandle().nextContainerCounter(), ((CraftPlayer) player).getHandle().inventory,
				ContainerAccess.at(((CraftPlayer) player).getHandle().world, v1_14_R1.zero));
		for (int i = 0; i < 2; ++i)
			container.setItem(i, (net.minecraft.server.v1_14_R1.ItemStack) asNMSItem(inv.getItem(i)));
		return container;
	}

	@Override
	public String getAnvilRenameText(Object anvil) {
		return ((ContainerAnvil) anvil).renameText;
	}

	public static int c(final int quickCraftData) {
		return quickCraftData >> 2 & 3;
	}

	public static int d(final int quickCraftData) {
		return quickCraftData & 3;
	}

	@Override
	public boolean processInvClickPacket(Player player, HolderGUI gui, Object provPacket) {
		PacketPlayInWindowClick packet = (PacketPlayInWindowClick) provPacket;
		int slot = packet.c();

		Object container = gui.getContainer(player);
		if (container == null)
			return false;

		int id = packet.b();
		int mouseClick = packet.d();
		InventoryClickType type = packet.g();

		if (packet.c() < -1 && packet.c() != -999)
			return true;

		Container c = (Container) container;
		EntityPlayer nPlayer = ((CraftPlayer) player).getHandle();

		ItemStack newItem;
		ItemStack oldItem;
		switch (type) {
		case PICKUP: // PICKUP
			oldItem = asBukkitItem(getSlotItem(container, slot));
			newItem = asBukkitItem(nPlayer.inventory.getCarried());
			if (slot > 0 && mouseClick != 0) {
				if (nPlayer.inventory.getCarried().isEmpty()) { // pickup half
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
			newItem = asBukkitItem(nPlayer.inventory.getCarried());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case SWAP:// SWAP
			newItem = asBukkitItem(nPlayer.inventory.getItem(mouseClick));
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case CLONE:// CLONE
			newItem = asBukkitItem(getSlotItem(container, slot));
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case THROW:// THROW
			if (nPlayer.inventory.getCarried().isEmpty() && slot >= 0) {
				Slot slot3 = c.getSlot(slot);
				newItem = asBukkitItem(slot3.getItem());
				if (mouseClick != 0 || newItem.getAmount() - 1 <= 0)
					newItem = new ItemStack(Material.AIR);
				else
					newItem.setAmount(newItem.getAmount() - 1);
			} else
				newItem = asBukkitItem(nPlayer.inventory.getCarried());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		case QUICK_CRAFT:// QUICK_CRAFT
			newItem = asBukkitItem(nPlayer.inventory.getCarried());
			oldItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(getSlotItem(container, slot));
			break;
		case PICKUP_ALL:// PICKUP_ALL
			newItem = asBukkitItem(nPlayer.inventory.getCarried());
			oldItem = asBukkitItem(getSlotItem(container, slot));
			break;
		default:
			newItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(packet.f());
			oldItem = slot <= -1 ? new ItemStack(Material.AIR) : asBukkitItem(packet.f());
			break;
		}

		if (oldItem.getType() == Material.AIR && newItem.getType() == Material.AIR)
			return true;

		boolean cancel = false;
		int gameSlot = slot > gui.size() - 1 ? InventoryUtils.convertToPlayerInvSlot(slot - gui.size()) : slot;

		ClickType clickType = InventoryUtils.buildClick(type == InventoryClickType.QUICK_CRAFT ? 1 : type == InventoryClickType.QUICK_MOVE ? 2 : 0, mouseClick);
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
		BukkitLoader.getPacketHandler().send(player, packetSetSlot(-1, -1, 0, nPlayer.inventory.getCarried()));
		switch (type) {
		case CLONE:
			break;
		case SWAP:
		case QUICK_MOVE:
		case PICKUP_ALL:
			nPlayer.updateInventory(c);
			break;
		default:
			BukkitLoader.getPacketHandler().send(player, packetSetSlot(id, slot, 0, c.getSlot(packet.c()).getItem()));
			break;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void processEvent(Container c, InventoryClickType type, HolderGUI gui, Player player, int slot, int gameSlot, ItemStack newItem, ItemStack oldItem, PacketPlayInWindowClick packet,
			int mouseClick, ClickType clickType, EntityPlayer nPlayer) {
		net.minecraft.server.v1_14_R1.ItemStack result;
		switch (type) {
		case QUICK_MOVE: {
			ItemStack[] contents = slot < gui.size() ? player.getInventory().getStorageContents() : gui.getInventory().getStorageContents();
			Collection<Integer> modified = slot < gui.size()
					? InventoryUtils.shift(slot, player, gui, clickType, gui instanceof AnvilGUI ? DestinationType.PLAYER_INV_ANVIL : DestinationType.PLAYER_INV_CUSTOM_INV, null, contents, oldItem)
							.keySet()
					: InventoryUtils.shift(slot, player, gui, clickType, DestinationType.CUSTOM_INV, gui.getNotInterableSlots(player), contents, oldItem).keySet();
			if (!modified.isEmpty())
				if (slot < gui.size()) {
					boolean canRemove = !modified.contains(-1);
					player.getInventory().setStorageContents(contents);
					if (canRemove)
						gui.remove(gameSlot);
					else
						gui.getInventory().setItem(gameSlot, newItem);
				} else {
					boolean canRemove = !modified.contains(-1);
					gui.getInventory().setStorageContents(contents);
					if (canRemove)
						player.getInventory().setItem(gameSlot, null);
					else
						player.getInventory().setItem(gameSlot, newItem);
				}
			result = c.getSlot(slot).getItem();
			break;
		}
		default:
			result = processClick(gui, gui.getNotInterableSlots(player), c, slot, mouseClick, type, nPlayer);
			break;
		}
		postToMainThread(() -> {
			if (net.minecraft.server.v1_14_R1.ItemStack.matches(packet.f(), result)) {
				nPlayer.playerConnection.sendPacket(new PacketPlayOutTransaction(packet.b(), packet.e(), true));
				nPlayer.e = true;
				c.c();
				nPlayer.broadcastCarriedItem();
				nPlayer.e = false;
			} else {
				((Map<Integer, Short>) Ref.get(nPlayer.playerConnection, "k")).put(c.windowId, packet.e());
				nPlayer.playerConnection.sendPacket(new PacketPlayOutTransaction(packet.b(), packet.e(), false));
				c.a(nPlayer, false);
				NonNullList<net.minecraft.server.v1_14_R1.ItemStack> nonnulllist1 = NonNullList.a();
				for (int j = 0; j < c.slots.size(); ++j) {
					net.minecraft.server.v1_14_R1.ItemStack cursor = c.slots.get(j).getItem();
					nonnulllist1.add(cursor.isEmpty() ? net.minecraft.server.v1_14_R1.ItemStack.a : cursor);
				}
				nPlayer.a(c, nonnulllist1);
			}
		});
	}

	private Method addAmount = Ref.method(Slot.class, "b", int.class);

	@SuppressWarnings("unchecked")
	private net.minecraft.server.v1_14_R1.ItemStack processClick(HolderGUI gui, List<Integer> ignoredSlots, Container container, int slotIndex, int button, InventoryClickType actionType,
			EntityPlayer player) {
		net.minecraft.server.v1_14_R1.ItemStack result = net.minecraft.server.v1_14_R1.ItemStack.a;

		if (actionType == InventoryClickType.QUICK_CRAFT)
			processDragMove(gui, container, player, slotIndex, button);
		else {
			int u = (int) Ref.get(container, containerU);
			Set<Slot> mod = (Set<Slot>) Ref.get(container, containerV);
			if (u != 0) {
				Ref.set(container, containerU, u = 0);
				mod.clear();
			} else if (actionType == InventoryClickType.PICKUP && (button == 0 || button == 1)) {
				if (slotIndex == -999) {
					if (!player.inventory.getCarried().isEmpty())
						if (button == 0) {
							net.minecraft.server.v1_14_R1.ItemStack carried = player.inventory.getCarried();
							player.inventory.setCarried(net.minecraft.server.v1_14_R1.ItemStack.a);
							postToMainThread(() -> player.drop(carried, true));
						} else
							postToMainThread(() -> player.drop(player.inventory.getCarried().cloneAndSubtract(1), true));
				} else {
					if (slotIndex < 0)
						return net.minecraft.server.v1_14_R1.ItemStack.a;

					PlayerInventory playerinventory = player.inventory;

					int k1;
					net.minecraft.server.v1_14_R1.ItemStack itemstack1;
					net.minecraft.server.v1_14_R1.ItemStack itemstack2;
					Slot slot2 = container.slots.get(slotIndex);
					if (slot2 != null) {
						itemstack2 = slot2.getItem();
						itemstack1 = playerinventory.getCarried();
						if (!itemstack2.isEmpty())
							result = itemstack2.cloneItemStack();
						if (itemstack2.isEmpty()) {
							if (!itemstack1.isEmpty() && slot2.isAllowed(itemstack1)) {
								k1 = button == 0 ? itemstack1.getCount() : 1;
								if (k1 > slot2.getMaxStackSize(itemstack1))
									k1 = slot2.getMaxStackSize(itemstack1);

								slot2.set(itemstack1.cloneAndSubtract(k1));
							}
						} else if (slot2.isAllowed(player))
							if (itemstack1.isEmpty()) {
								if (itemstack2.isEmpty()) {
									slot2.set(net.minecraft.server.v1_14_R1.ItemStack.a);
									playerinventory.setCarried(net.minecraft.server.v1_14_R1.ItemStack.a);
								} else {
									k1 = button == 0 ? itemstack2.getCount() : (itemstack2.getCount() + 1) / 2;
									playerinventory.setCarried(slot2.a(k1));
									if (itemstack2.isEmpty())
										slot2.set(net.minecraft.server.v1_14_R1.ItemStack.a);

									slot2.a(player, playerinventory.getCarried());
								}
							} else if (slot2.isAllowed(itemstack1)) {
								if (Container.a(itemstack2, itemstack1)) {
									k1 = button == 0 ? itemstack1.getCount() : 1;
									if (k1 > slot2.getMaxStackSize(itemstack1) - itemstack2.getCount())
										k1 = slot2.getMaxStackSize(itemstack1) - itemstack2.getCount();

									if (k1 > itemstack1.getMaxStackSize() - itemstack2.getCount())
										k1 = itemstack1.getMaxStackSize() - itemstack2.getCount();

									itemstack1.subtract(k1);
									itemstack2.add(k1);
								} else if (itemstack1.getCount() <= slot2.getMaxStackSize(itemstack1)) {
									slot2.set(itemstack1);
									playerinventory.setCarried(itemstack2);
								}
							} else if (itemstack1.getMaxStackSize() > 1 && Container.a(itemstack2, itemstack1) && !itemstack2.isEmpty()) {
								k1 = itemstack2.getCount();
								if (k1 + itemstack1.getCount() <= itemstack1.getMaxStackSize()) {
									itemstack1.add(k1);
									itemstack2 = slot2.a(k1);
									if (itemstack2.isEmpty())
										slot2.set(net.minecraft.server.v1_14_R1.ItemStack.a);

									slot2.a(player, playerinventory.getCarried());
								}
							}

						slot2.d();
						if (player instanceof EntityPlayer && slot2.getMaxStackSize() != 64) {
							BukkitLoader.getPacketHandler().send(player.getBukkitEntity(), BukkitLoader.getNmsProvider().packetSetSlot(container.windowId, slot2.rawSlotIndex, 0, slot2.getItem()));
							if (container.getBukkitView().getType() == InventoryType.WORKBENCH || container.getBukkitView().getType() == InventoryType.CRAFTING)
								BukkitLoader.getPacketHandler().send(player.getBukkitEntity(), BukkitLoader.getNmsProvider().packetSetSlot(container.windowId, 0, 0, container.getSlot(0).getItem()));
						}
					}
				}
			} else if (actionType == InventoryClickType.SWAP) {
				if (slotIndex < 0)
					return result;
				PlayerInventory playerinventory = player.inventory;
				Slot slot3 = container.getSlot(slotIndex);
				net.minecraft.server.v1_14_R1.ItemStack itemstack2 = playerinventory.getItem(button);
				net.minecraft.server.v1_14_R1.ItemStack itemstack = slot3.getItem();
				if (!itemstack2.isEmpty() || !itemstack.isEmpty())
					if (itemstack2.isEmpty()) {
						if (slot3.isAllowed(player)) {
							playerinventory.setItem(button, itemstack);
							Ref.invoke(slot3, addAmount, itemstack.getCount());
							slot3.set(net.minecraft.server.v1_14_R1.ItemStack.a);
							slot3.a(player, itemstack);
						}
					} else if (itemstack.isEmpty()) {
						if (slot3.isAllowed(itemstack2)) {
							int j2 = slot3.getMaxStackSize(itemstack2);
							if (itemstack2.getCount() > j2)
								slot3.set(itemstack2.cloneAndSubtract(j2));
							else {
								playerinventory.setItem(button, net.minecraft.server.v1_14_R1.ItemStack.a);
								slot3.set(itemstack2);
							}
						}
					} else if (slot3.isAllowed(player) && slot3.isAllowed(itemstack2)) {
						int j2 = slot3.getMaxStackSize(itemstack2);
						if (itemstack2.getCount() > j2) {
							slot3.set(itemstack2.cloneAndSubtract(j2));
							slot3.a(player, itemstack);
							if (!playerinventory.pickup(itemstack))
								postToMainThread(() -> player.drop(itemstack, true));
						} else {
							playerinventory.setItem(button, itemstack);
							slot3.set(itemstack2);
							slot3.a(player, itemstack);
						}
					}
			} else if (actionType == InventoryClickType.CLONE && player.abilities.canInstantlyBuild && player.inventory.getCarried().isEmpty() && slotIndex >= 0) {
				Slot slot3 = container.getSlot(slotIndex);
				if (slot3.hasItem()) {
					net.minecraft.server.v1_14_R1.ItemStack itemstack2 = slot3.getItem();

					net.minecraft.server.v1_14_R1.ItemStack stack = itemstack2.cloneItemStack();
					stack.setCount(itemstack2.getCount());
					player.inventory.setCarried(stack);
				}
			} else if (actionType == InventoryClickType.THROW && player.inventory.getCarried().isEmpty() && slotIndex >= 0) {
				Slot slot2 = container.getSlot(slotIndex);
				if (slot2 != null && slot2.hasItem() && slot2.isAllowed(player)) {
					net.minecraft.server.v1_14_R1.ItemStack itemstack2 = slot2.a(button == 0 ? 1 : slot2.getItem().getCount());
					slot2.a(player, itemstack2);
					postToMainThread(() -> player.drop(itemstack2, true));
				}
			} else if (actionType == InventoryClickType.PICKUP_ALL && slotIndex >= 0) {
				Slot slot2 = container.slots.get(slotIndex);
				net.minecraft.server.v1_14_R1.ItemStack itemstack1 = player.inventory.getCarried();
				if (!itemstack1.isEmpty() && (!slot2.hasItem() || !slot2.isAllowed(player))) {
					List<Integer> ignoreSlots = ignoredSlots == null ? Collections.emptyList() : ignoredSlots;
					List<Integer> corruptedSlots = ignoredSlots == null ? Collections.emptyList() : new ArrayList<>();
					Map<Integer, ItemStack> modifiedSlots = new HashMap<>();
					Map<Integer, ItemStack> modifiedSlotsPlayerInv = new HashMap<>();

					int l = button == 0 ? 0 : container.slots.size() - 1;
					int i2 = button == 0 ? 1 : -1;

					for (int l1 = 0; l1 < 2; ++l1)
						for (int j2 = l; j2 >= 0 && j2 < container.slots.size() && itemstack1.getCount() < itemstack1.getMaxStackSize(); j2 += i2) {
							Slot slot3 = container.slots.get(j2);
							if (slot3.hasItem() && Container.a(slot3, itemstack1, true) && slot3.isAllowed(player) && container.a(itemstack1, slot3)) {
								net.minecraft.server.v1_14_R1.ItemStack itemstack3 = slot3.getItem();
								if (l1 != 0 || itemstack3.getCount() != itemstack3.getMaxStackSize()) {
									if (j2 < gui.size() && ignoreSlots.contains(j2)) {
										corruptedSlots.add(j2);
										continue;
									}
									int count = Math.min(itemstack1.getMaxStackSize() - itemstack1.getCount(), itemstack3.getCount());
									net.minecraft.server.v1_14_R1.ItemStack itemstack6 = slot3.a(count);
									itemstack1.add(count);
									if (itemstack6.isEmpty())
										slot3.set(net.minecraft.server.v1_14_R1.ItemStack.a);
									slot3.a(player, itemstack6);
									int gameSlot = j2 > gui.size() - 1 ? InventoryUtils.convertToPlayerInvSlot(j2 - gui.size()) : j2;
									if (j2 < gui.size())
										modifiedSlots.put(gameSlot, asBukkitItem(slot3.getItem()));
									else
										modifiedSlotsPlayerInv.put(gameSlot, asBukkitItem(slot3.getItem()));
								}
							}
						}
					if (slotIndex < gui.size())
						modifiedSlots.put(slotIndex, new ItemStack(Material.AIR));
					else
						modifiedSlotsPlayerInv.put(InventoryUtils.convertToPlayerInvSlot(slotIndex - gui.size()), new ItemStack(Material.AIR));
					if (!modifiedSlots.isEmpty() || !modifiedSlotsPlayerInv.isEmpty())
						gui.onMultipleIteract(player.getBukkitEntity(), modifiedSlots, modifiedSlotsPlayerInv);
					for (int s : corruptedSlots)
						BukkitLoader.getPacketHandler().send(player.getBukkitEntity(), BukkitLoader.getNmsProvider().packetSetSlot(BukkitLoader.getNmsProvider().getContainerId(container), s,
								getContainerStateId(container), BukkitLoader.getNmsProvider().getSlotItem(container, s)));
				}
			}
		}
		return result;
	}

	private Field containerU = Ref.field(Container.class, "h"), containerV = Ref.field(Container.class, "i"), containerT = Ref.field(Container.class, "dragType");

	@SuppressWarnings("unchecked")
	private void processDragMove(HolderGUI gui, Container container, EntityPlayer player, int slot, int mouseClick) {
		int previous = (int) Ref.get(container, containerU);
		int u = d(mouseClick);
		Set<Slot> mod = (Set<Slot>) Ref.get(container, containerV);
		if ((previous != 1 || u != 2) && previous != u || player.inventory.getCarried().isEmpty()) {
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
				final Slot bslot = container.getSlot(slot);
				final net.minecraft.server.v1_14_R1.ItemStack itemstack = player.inventory.getCarried();
				if (Container.a(bslot, itemstack, true) && bslot.isAllowed(itemstack) && (t == 2 || itemstack.getCount() > mod.size()) && container.b(bslot))
					mod.add(bslot);
				break;
			}
			case 2:
				if (!mod.isEmpty()) {
					final net.minecraft.server.v1_14_R1.ItemStack itemstack2 = player.inventory.getCarried().cloneItemStack();
					if (itemstack2.isEmpty()) {
						mod.clear();
						Ref.set(container, containerU, 0);
						return;
					}
					int t = (int) Ref.get(container, containerT);
					int l = player.inventory.getCarried().getCount();
					final Iterator<Slot> iterator = mod.iterator();
					final Map<Integer, net.minecraft.server.v1_14_R1.ItemStack> draggedSlots = new HashMap<>();
					while (iterator.hasNext()) {
						final Slot slot2 = iterator.next();
						final net.minecraft.server.v1_14_R1.ItemStack itemstack3 = player.inventory.getCarried();
						if (slot2 != null && Container.a(slot2, itemstack3, true) && slot2.isAllowed(itemstack3) && (t == 2 || itemstack3.getCount() >= mod.size()) && container.b(slot2)) {

							final int j1 = slot2.hasItem() ? slot2.getItem().getCount() : 0;
							final int k1 = Math.min(itemstack2.getMaxStackSize(), slot2.getMaxStackSize(itemstack2));
							final int l2 = Math.min(a(mod, t, itemstack2) + j1, k1);
							l -= l2 - j1;
							net.minecraft.server.v1_14_R1.ItemStack stack = itemstack2.cloneItemStack();
							stack.setCount(l2);
							draggedSlots.put(slot2.rawSlotIndex, stack);
						}
					}
					final InventoryView view = container.getBukkitView();
					final org.bukkit.inventory.ItemStack newcursor = CraftItemStack.asCraftMirror(itemstack2);
					newcursor.setAmount(l);
					final Map<Integer, org.bukkit.inventory.ItemStack> guiSlots = new HashMap<>();
					final Map<Integer, org.bukkit.inventory.ItemStack> playerSlots = new HashMap<>();
					for (final Entry<Integer, net.minecraft.server.v1_14_R1.ItemStack> ditem : draggedSlots.entrySet())
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
					player.inventory.setCarried(CraftItemStack.asNMSCopy(newcursor));
					if (!guiSlots.isEmpty() || !playerSlots.isEmpty())
						gui.onMultipleIteract(player.getBukkitEntity(), guiSlots, playerSlots);
					for (final Entry<Integer, net.minecraft.server.v1_14_R1.ItemStack> dslot : draggedSlots.entrySet())
						view.setItem(dslot.getKey(), CraftItemStack.asBukkitCopy(dslot.getValue()));
					if (player.inventory.getCarried() != null)
						player.updateInventory(container);
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

	public static int a(Set<Slot> slots, int mode, net.minecraft.server.v1_14_R1.ItemStack stack) {
		int j;
		switch (mode) {
		case 0:
			j = MathHelper.d((float) stack.getCount() / (float) slots.size());
			break;
		case 1:
			j = 1;
			break;
		case 2:
			j = stack.getItem().getMaxStackSize();
			break;
		default:
			j = stack.getCount();
		}

		return j;
	}

	static Field field = Ref.field(PacketStatusOutServerInfo.class, "b");

	@Override
	public boolean processServerListPing(String player, Object channel, Object packet) {
		PacketStatusOutServerInfo status = (PacketStatusOutServerInfo) packet;
		ServerPing ping;
		try {
			ping = (ServerPing) v1_14_R1.field.get(status);
		} catch (Exception e) {
			return false;
		}

		List<GameProfileHandler> players = new ArrayList<>();
		if (ping.b().c() != null)
			for (GameProfile profile : ping.b().c())
				players.add(fromGameProfile(profile));

		ServerListPingEvent event = new ServerListPingEvent(getOnlinePlayers().size(), Bukkit.getMaxPlayers(), players, Bukkit.getMotd(), ping.d(),
				((InetSocketAddress) ((Channel) channel).remoteAddress()).getAddress(), ping.getServerData().a(), ping.getServerData().getProtocolVersion());
		EventManager.call(event);
		if (event.isCancelled())
			return true;
		ServerPingPlayerSample playerSample = new ServerPingPlayerSample(event.getMaxPlayers(), event.getOnlinePlayers());
		if (event.getPlayersText() != null) {
			GameProfile[] profiles = new GameProfile[event.getPlayersText().size()];
			int i = -1;
			for (GameProfileHandler s : event.getPlayersText())
				profiles[++i] = new GameProfile(s.getUUID(), s.getUsername());
			playerSample.a(profiles);
		} else
			playerSample.a(new GameProfile[0]);
		ping.setPlayerSample(playerSample);

		if (event.getMotd() != null)
			ping.setMOTD((IChatBaseComponent) this.toIChatBaseComponent(ComponentAPI.fromString(event.getMotd())));
		else
			ping.setMOTD((IChatBaseComponent) BukkitLoader.getNmsProvider().chatBase("{\"text\":\"\"}"));
		if (event.getVersion() != null)
			ping.setServerInfo(new ServerData(event.getVersion(), event.getProtocol()));
		if (event.getFalvicon() != null)
			ping.setFavicon(event.getFalvicon());
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processPlayerInfo(Player player, Object channel, Object packet, Tablist tablist) {
		for (Object data : (List<Object>) Ref.get(packet, playerInfo)) {
			UUID id = ((GameProfile) Ref.get(data, "d")).getId();
			if (id.equals(player.getUniqueId())) {
				if (tablist.isGameProfileModified())
					Ref.set(data, "d", toGameProfile(tablist.getGameProfile()));
				if (tablist.getLatency().isPresent())
					Ref.set(data, "b", tablist.getLatency().get());
				if (tablist.getGameMode().isPresent())
					Ref.set(data, "c", EnumGamemode.valueOf(tablist.getGameMode().get().name()));
				if (tablist.getPlayerListName().isPresent())
					Ref.set(data, "e", toIChatBaseComponent(tablist.getPlayerListName().get()));
			} else {
				TabEntry entry = tablist.getEntryById(id);
				if (entry == null)
					continue; // not registered yet / removed from entries, skip
				if (entry.isGameProfileModified())
					Ref.set(data, "d", toGameProfile(entry.getGameProfile()));
				if (entry.getLatency().isPresent())
					Ref.set(data, "b", entry.getLatency().get());
				if (entry.getGameMode().isPresent())
					Ref.set(data, "c", EnumGamemode.valueOf(entry.getGameMode().get().name()));
				if (entry.getPlayerListName().isPresent())
					Ref.set(data, "e", toIChatBaseComponent(entry.getPlayerListName().get()));
			}
		}
	}

	@Override
	public Object getNBT(Entity entity) {
		return ((CraftEntity) entity).getHandle().save(new NBTTagCompound());
	}

	@Override
	public Object setString(Object nbt, String path, String value) {
		((NBTTagCompound) nbt).setString(path, value);
		return nbt;
	}

	@Override
	public Object setInteger(Object nbt, String path, int value) {
		((NBTTagCompound) nbt).setInt(path, value);
		return nbt;
	}

	@Override
	public Object setDouble(Object nbt, String path, double value) {
		((NBTTagCompound) nbt).setDouble(path, value);
		return nbt;
	}

	@Override
	public Object setLong(Object nbt, String path, long value) {
		((NBTTagCompound) nbt).setLong(path, value);
		return nbt;
	}

	@Override
	public Object setShort(Object nbt, String path, short value) {
		((NBTTagCompound) nbt).setShort(path, value);
		return nbt;
	}

	@Override
	public Object setFloat(Object nbt, String path, float value) {
		((NBTTagCompound) nbt).setFloat(path, value);
		return nbt;
	}

	@Override
	public Object setBoolean(Object nbt, String path, boolean value) {
		((NBTTagCompound) nbt).setBoolean(path, value);
		return nbt;
	}

	@Override
	public Object setIntArray(Object nbt, String path, int[] value) {
		((NBTTagCompound) nbt).setIntArray(path, value);
		return nbt;
	}

	@Override
	public Object setByteArray(Object nbt, String path, byte[] value) {
		((NBTTagCompound) nbt).setByteArray(path, value);
		return nbt;
	}

	@Override
	public Object setNBTBase(Object nbt, String path, Object value) {
		((NBTTagCompound) nbt).set(path, (NBTBase) value);
		return nbt;
	}

	@Override
	public String getString(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getString(path);
	}

	@Override
	public int getInteger(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getInt(path);
	}

	@Override
	public double getDouble(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getDouble(path);
	}

	@Override
	public long getLong(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getLong(path);
	}

	@Override
	public short getShort(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getShort(path);
	}

	@Override
	public float getFloat(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getFloat(path);
	}

	@Override
	public boolean getBoolean(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getBoolean(path);
	}

	@Override
	public int[] getIntArray(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getIntArray(path);
	}

	@Override
	public byte[] getByteArray(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getByteArray(path);
	}

	@Override
	public Object getNBTBase(Object nbt, String path) {
		return ((NBTTagCompound) nbt).get(path);
	}

	@Override
	public Set<String> getKeys(Object nbt) {
		return ((NBTTagCompound) nbt).getKeys();
	}

	@Override
	public boolean hasKey(Object nbt, String path) {
		return ((NBTTagCompound) nbt).hasKey(path);
	}

	@Override
	public void removeKey(Object nbt, String path) {
		((NBTTagCompound) nbt).remove(path);
	}

	@Override
	public Object setByte(Object nbt, String path, byte value) {
		((NBTTagCompound) nbt).setByte(path, value);
		return nbt;
	}

	@Override
	public byte getByte(Object nbt, String path) {
		return ((NBTTagCompound) nbt).getByte(path);
	}

	@Override
	public Object getDataWatcher(Entity entity) {
		return ((CraftEntity) entity).getHandle().getDataWatcher();
	}

	@Override
	public Object getDataWatcher(Object entity) {
		return ((net.minecraft.server.v1_14_R1.Entity) entity).getDataWatcher();
	}

	@Override
	public int incrementStateId(Object container) {
		return 0;
	}

	@Override
	public Object packetEntityHeadRotation(Entity entity) {
		return new PacketPlayOutEntityHeadRotation((net.minecraft.server.v1_14_R1.Entity) getEntity(entity), (byte) (entity.getLocation().getYaw() * 256F / 360F));
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
		return new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.valueOf(type.name()), (EntityPlayer) getPlayer(player));
	}

	private static Field playerInfo = Ref.field(PacketPlayOutPlayerInfo.class, "b");

	private static Constructor<?> infoData = Ref.constructor(Ref.nms("", "PacketPlayOutPlayerInfo$PlayerInfoData"), PacketPlayOutPlayerInfo.class, GameProfile.class, int.class, EnumGamemode.class,
			IChatBaseComponent.class);

	@SuppressWarnings("unchecked")
	@Override
	public Object packetPlayerInfo(PlayerInfoType type, GameProfileHandler gameProfile, int latency, GameMode gameMode, Component playerName) {
		PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.valueOf(type.name()), Collections.emptyList());
		((List<Object>) Ref.get(packet, playerInfo))
				.add(Ref.newInstance(infoData, packet, toGameProfile(gameProfile), latency, gameMode == null ? EnumGamemode.SURVIVAL : EnumGamemode.valueOf(gameMode.name()),
						playerName == null ? toIChatBaseComponent(new Component(gameProfile.getUsername())) : toIChatBaseComponent(playerName)));
		return packet;
	}

	@Override
	public Object packetPosition(double x, double y, double z, float yaw, float pitch) {
		return new PacketPlayOutPosition(x, y, z, yaw, pitch, Collections.emptySet(), 0);
	}

	@Override
	public Object packetRespawn(Player player) {
		EntityPlayer entityPlayer = (EntityPlayer) getPlayer(player);
		WorldServer worldserver = entityPlayer.getWorldServer();
		return new PacketPlayOutRespawn(worldserver.worldProvider.getDimensionManager(), entityPlayer.world.getWorldData().getType(), entityPlayer.playerInteractManager.getGameMode());
	}

	@Override
	public String getProviderName() {
		return "1_14_R1 (1.14)";
	}

	@Override
	public int getContainerStateId(Object container) {
		return 0;
	}

	@Override
	public void loadParticles() {
		for (MinecraftKey s : IRegistry.PARTICLE_TYPE.keySet())
			me.devtec.theapi.bukkit.game.particles.Particle.identifier.put(s.getKey(), IRegistry.PARTICLE_TYPE.get(s));
	}

	@Override
	public Object toGameProfile(GameProfileHandler gameProfileHandler) {
		GameProfile profile = new GameProfile(gameProfileHandler.getUUID(), gameProfileHandler.getUsername());
		for (Entry<String, PropertyHandler> entry : gameProfileHandler.getProperties().entrySet())
			profile.getProperties().put(entry.getKey(), new Property(entry.getValue().getName(), entry.getValue().getValues(), entry.getValue().getSignature()));
		return profile;
	}

	@Override
	public GameProfileHandler fromGameProfile(Object gameProfile) {
		GameProfile profile = (GameProfile) gameProfile;
		GameProfileHandler handler = GameProfileHandler.of(profile.getName(), profile.getId());
		for (Entry<String, Property> entry : profile.getProperties().entries())
			handler.getProperties().put(entry.getKey(), PropertyHandler.of(entry.getValue().getName(), entry.getValue().getValue(), entry.getValue().getSignature()));
		return handler;
	}

	@Override
	public Object getGameProfile(Object nmsPlayer) {
		return ((EntityPlayer) nmsPlayer).getProfile();
	}
}
