package me.devtec.theapi.bukkit.game;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

import me.devtec.shared.Ref;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Position implements Cloneable {
	private String world;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	private Object cachedChunk;

	public Position() {
	}

	public Position(String world) {
		this.world = world;
	}

	public Position(World world) {
		this(world.getName());
	}

	public Position(World world, double x, double y, double z) {
		this(world, x, y, z, 0, 0);
	}

	public Position(World world, double x, double y, double z, float yaw, float pitch) {
		this(world.getName(), x, y, z, yaw, pitch);
	}

	public Position(String world, double x, double y, double z) {
		this(world, x, y, z, 0, 0);
	}

	public Position(String world, double x, double y, double z, float yaw, float pitch) {
		this(x, y, z, yaw, pitch);
		this.world = world;
	}

	public Position(double x, double y, double z) {
		this(x, y, z, 0, 0);
	}

	public Position(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public Position(Location location) {
		world = location.getWorld().getName();
		x = location.getX();
		y = location.getY();
		z = location.getZ();
		yaw = location.getYaw();
		pitch = location.getPitch();
	}

	public Position(Block b) {
		this(b.getLocation());
	}

	public Position(Entity b) {
		this(b.getLocation());
	}

	public Position(Position cloneable) {
		world = cloneable.getWorldName();
		x = cloneable.getX();
		y = cloneable.getY();
		z = cloneable.getZ();
		yaw = cloneable.getYaw();
		pitch = cloneable.getPitch();
	}

	public static Position fromBlock(Block block) {
		if (block != null)
			return new Position(block.getLocation());
		return null;
	}

	public static Position fromLocation(Location location) {
		if (location != null)
			return new Position(location);
		return null;
	}

	public Biome getBiome() {
		return getBlock().getBiome();
	}

	public int getData() {
		return Ref.isOlderThan(8) ? (byte) BukkitLoader.getNmsProvider().getData(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ()) : StringUtils.getByte(getType().getData());
	}

	public Material getBukkitType() {
		return getType().getType();
	}

	public Object getIBlockData() {
		return BukkitLoader.getNmsProvider().getBlock(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ());
	}

	public BlockDataStorage getType() {
		BlockDataStorage storage = BlockDataStorage.fromData(getIBlockData()).setItemData(BukkitLoader.getNmsProvider().getData(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ()));
		if (BukkitLoader.getNmsProvider().isTileEntity(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ()))
			storage.setNBT(BukkitLoader.getNmsProvider().getNBTOfTile(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ()));
		return storage;
	}

	public Position subtract(double x, double y, double z) {
		this.x -= x;
		this.y -= y;
		this.z -= z;
		cachedChunk = null;
		return this;
	}

	public Position subtract(Position position) {
		x -= position.getX();
		y -= position.getY();
		z -= position.getZ();
		cachedChunk = null;
		return this;
	}

	public Position subtract(Location location) {
		x -= location.getX();
		y -= location.getY();
		z -= location.getZ();
		cachedChunk = null;
		return this;
	}

	public String getWorldName() {
		return world;
	}

	public Position setWorld(World world) {
		this.world = world.getName();
		cachedChunk = null;
		return this;
	}

	public Position setX(double x) {
		this.x = x;
		cachedChunk = null;
		return this;
	}

	public Position setY(double y) {
		this.y = y;
		cachedChunk = null;
		return this;
	}

	public Position setZ(double z) {
		this.z = z;
		cachedChunk = null;
		return this;
	}

	public Position setYaw(float yaw) {
		this.yaw = yaw;
		cachedChunk = null;
		return this;
	}

	public Position setPitch(float pitch) {
		this.pitch = pitch;
		cachedChunk = null;
		return this;
	}

	public double distance(Location location) {
		return Math.sqrt(this.distanceSquared(location));
	}

	public double distance(Position position) {
		return Math.sqrt(this.distanceSquared(position));
	}

	public Position multiply(double m) {
		x *= m;
		y *= m;
		z *= m;
		cachedChunk = null;
		return this;
	}

	public Position zero() {
		x = 0;
		y = 0;
		z = 0;
		cachedChunk = null;
		return this;
	}

	public double length() {
		return Math.sqrt(lengthSquared());
	}

	public double lengthSquared() {
		return square(x) + square(y) + square(z);
	}

	public double distanceSquared(Location location) {
		return square(x - location.getX()) + square(y - location.getY()) + square(z - location.getZ());
	}

	public double distanceSquared(Position position) {
		return square(x - position.x) + square(y - position.y) + square(z - position.z);
	}

	private double square(double d) {
		return d * d;
	}

	public Chunk getChunk() {
		if (Ref.isNewerThan(12))
			return getWorld().getChunkAt(getBlockX() >> 4, getBlockZ() >> 4);
		return BukkitLoader.getNmsProvider().toBukkitChunk(getNMSChunk());
	}

	public Object getNMSChunk() {
		if (cachedChunk == null)
			cachedChunk = BukkitLoader.getNmsProvider().getChunk(getWorld(), getBlockX() >> 4, getBlockZ() >> 4);
		return cachedChunk;
	}

	public Object getBlockPosition() {
		return BukkitLoader.getNmsProvider().blockPosition(getBlockX(), getBlockY(), getBlockZ());
	}

	public ChunkSnapshot getChunkSnapshot() {
		return getChunk().getChunkSnapshot();
	}

	public Block getBlock() {
		return getWorld().getBlockAt(getBlockX(), getBlockY(), getBlockZ());
	}

	public World getWorld() {
		return Bukkit.getWorld(world);
	}

	public Position add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		cachedChunk = null;
		return this;
	}

	public Position add(Position position) {
		x += position.getX();
		y += position.getY();
		z += position.getZ();
		cachedChunk = null;
		return this;
	}

	public Position add(Location location) {
		x += location.getX();
		y += location.getY();
		z += location.getZ();
		cachedChunk = null;
		return this;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public int getBlockX() {
		int floor = (int) x;
		return floor == x ? floor : floor - (int) (Double.doubleToRawLongBits(x) >>> 63);
	}

	public int getBlockY() {
		int floor = (int) y;
		return floor == y ? floor : floor - (int) (Double.doubleToRawLongBits(y) >>> 63);
	}

	public int getBlockZ() {
		int floor = (int) z;
		return floor == z ? floor : floor - (int) (Double.doubleToRawLongBits(z) >>> 63);
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public Location toLocation() {
		return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
	}

	public long setType(Material with) {
		return this.setType(new BlockDataStorage(with));
	}

	public long setType(BlockDataStorage with) {
		return Position.set(this, with);
	}

	public void setTypeAndUpdate(Material with) {
		this.setTypeAndUpdate(new BlockDataStorage(with), true);
	}

	public void setTypeAndUpdate(Material with, boolean updatePhysics) {
		this.setTypeAndUpdate(new BlockDataStorage(with), updatePhysics);
	}

	public void setTypeAndUpdate(BlockDataStorage with) {
		setTypeAndUpdate(with, true);
	}

	public void setTypeAndUpdate(BlockDataStorage with, boolean updatePhysics) {
		Object prev = updatePhysics ? getIBlockData() : null;
		this.setType(with);
		Position.updateBlockAt(this);
		if (with.getNBT() != null && BukkitLoader.getNmsProvider().isTileEntity(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ()))
			BukkitLoader.getNmsProvider().setNBTToTile(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), with.getNBT());
		Position.updateLightAt(this);
		if (updatePhysics)
			BukkitLoader.getNmsProvider().updatePhysics(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), prev);
	}

	@Override
	public boolean equals(Object a) {
		if (a instanceof Position) {
			Position s = (Position) a;
			return world.equals(s.getWorld().getName()) && s.getX() == x && s.getY() == y && s.getZ() == z && s.getPitch() == pitch && s.getYaw() == yaw;
		}
		if (a instanceof Location) {
			Location s = (Location) a;
			return world.equals(s.getWorld().getName()) && s.getX() == x && s.getY() == y && s.getZ() == z && s.getPitch() == pitch && s.getYaw() == yaw;
		}
		return false;
	}

	public static void updateBlockAt(Position pos) {
		Object packet = BukkitLoader.getNmsProvider().packetBlockChange(pos.getWorld(), pos);
		BukkitLoader.getPacketHandler().send(pos.getWorld().getPlayers(), packet);
	}

	public static void updateLightAt(Position pos) {
		BukkitLoader.getNmsProvider().updateLightAt(pos.getNMSChunk(), pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
	}

	public static long set(Position pos, BlockDataStorage mat) {
		BukkitLoader.getNmsProvider().setBlock(pos.getNMSChunk(), pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), Ref.isOlderThan(8) ? mat.getBlock() : mat.getIBlockData(), mat.getItemData());
		return pos.getChunkKey();
	}

	public long getChunkKey() {
		long k = (getBlockX() >> 4 & 0xFFFF0000L) << 16L | getBlockX() >> 4 & 0xFFFFL;
		k |= (getBlockZ() >> 4 & 0xFFFF0000L) << 32L | (getBlockZ() >> 4 & 0xFFFFL) << 16L;
		return k;
	}

	public void setState(BlockState state) {
		Position.setState(this, state);
	}

	public void setBlockData(BlockData state) {
		Position.setBlockData(this, state);
	}

	public void setStateAndUpdate(BlockState state, boolean updatePhysics) {
		Object prev = updatePhysics ? getIBlockData() : null;
		Position.setState(this, state);
		Position.updateBlockAt(this);
		Position.updateLightAt(this);
		if (updatePhysics)
			BukkitLoader.getNmsProvider().updatePhysics(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), prev);
	}

	public void setBlockDataAndUpdate(BlockData state, boolean updatePhysics) {
		Object prev = updatePhysics ? getIBlockData() : null;
		Position.setBlockData(this, state);
		Position.updateBlockAt(this);
		Position.updateLightAt(this);
		if (updatePhysics)
			BukkitLoader.getNmsProvider().updatePhysics(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), prev);
	}

	public long setAir() {
		BukkitLoader.getNmsProvider().setBlock(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), null);
		return getChunkKey();
	}

	public void setAirAndUpdate(boolean updatePhysics) {
		Object prev = updatePhysics ? getIBlockData() : null;
		setAir();
		Position.updateBlockAt(this);
		Position.updateLightAt(this);
		if (updatePhysics)
			BukkitLoader.getNmsProvider().updatePhysics(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), prev);
	}

	public void updatePhysics() {
		BukkitLoader.getNmsProvider().updatePhysics(getNMSChunk(), getBlockX(), getBlockY(), getBlockZ(), getIBlockData());
	}

	public static void setBlockData(Position pos, BlockData data) {
		if (data == null || Ref.isOlderThan(13) || pos == null)
			return;
		BukkitLoader.getNmsProvider().setBlock(pos.getNMSChunk(), pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), BukkitLoader.getNmsProvider().toIBlockData(data));
	}

	public static void setState(Position pos, BlockState state) {
		if (state == null || pos == null)
			return;
		if (Ref.isNewerThan(7))
			BukkitLoader.getNmsProvider().setBlock(pos.getNMSChunk(), pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), BukkitLoader.getNmsProvider().toIBlockData(state));
		else
			BukkitLoader.getNmsProvider().setBlock(pos.getNMSChunk(), pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
					BukkitLoader.getNmsProvider().toBlock(BlockDataStorage.fromData(state.getType(), state.getRawData())));
	}

	@Override
	public Position clone() {
		return new Position(this);
	}

	@Override
	public String toString() {
		Map<String, Object> map = new HashMap<>();
		map.put("world", world);
		map.put("x", x);
		map.put("y", y);
		map.put("z", z);
		map.put("yaw", yaw);
		map.put("pitch", pitch);
		return Json.writer().simpleWrite(map);
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		hashCode = 31 * hashCode + world.hashCode();
		hashCode = (int) (31 * hashCode + x);
		hashCode = (int) (31 * hashCode + y);
		hashCode = (int) (31 * hashCode + z);
		hashCode = (int) (31 * hashCode + yaw);
		return (int) (31 * hashCode + pitch);
	}
}
