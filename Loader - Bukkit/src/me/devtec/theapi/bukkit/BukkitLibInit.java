package me.devtec.theapi.bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.shared.API;
import me.devtec.shared.Ref;
import me.devtec.shared.Ref.ServerType;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.components.ComponentTransformer;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.json.JReader;
import me.devtec.shared.json.JWriter;
import me.devtec.shared.json.Json;
import me.devtec.shared.json.Json.DataReader;
import me.devtec.shared.json.Json.DataWriter;
import me.devtec.shared.json.modern.ModernJsonReader;
import me.devtec.shared.json.modern.ModernJsonWriter;
import me.devtec.shared.mcmetrics.GatheringInfoManager;
import me.devtec.shared.mcmetrics.Metrics;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.ColorUtils.ColormaticFactory;
import me.devtec.shared.utility.LibraryLoader;
import me.devtec.shared.utility.MathUtils;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.theapi.bukkit.commands.hooker.BukkitCommandManager;
import me.devtec.theapi.bukkit.commands.selectors.BukkitSelectorUtils;
import me.devtec.theapi.bukkit.game.BlockDataStorage;
import me.devtec.theapi.bukkit.game.EnchantmentAPI;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.game.Position;
import me.devtec.theapi.bukkit.nms.NBTEdit;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class BukkitLibInit {
	private static Method addUrl;

	private static class SimpleClassLoader extends URLClassLoader {

		public SimpleClassLoader(URL[] urls) {
			super(urls);
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}
	}

	private static class ImplementableJar extends JarFile {
		public List<JarFile> file = new ArrayList<>();

		public ImplementableJar(File file) throws IOException {
			super(file);
		}

		@Override
		public Enumeration<JarEntry> entries() {
			List<Enumeration<JarEntry>> totalEntries = new ArrayList<>();
			totalEntries.add(super.entries());
			for (JarFile search : file)
				totalEntries.add(search.entries());

			return new Enumeration<JarEntry>() {

				int posInList = 0;
				Enumeration<JarEntry> current = totalEntries.get(posInList);

				@Override
				public JarEntry nextElement() {
					if (current.hasMoreElements())
						return current.nextElement();
					if (++posInList < totalEntries.size())
						return (current = totalEntries.get(posInList)).nextElement();
					return null;
				}

				@Override
				public boolean hasMoreElements() {
					if (current.hasMoreElements())
						return true;
					if (posInList + 1 < totalEntries.size())
						return totalEntries.get(posInList + 1).hasMoreElements();
					return false;
				}
			};
		}

		@Override
		public ZipEntry getEntry(String name) {
			ZipEntry find = super.getEntry(name);
			if (find == null)
				for (JarFile search : file) {
					find = search.getEntry(name);
					if (find != null)
						return find;
				}
			return null;
		}

		@Override
		public JarEntry getJarEntry(String name) {
			JarEntry find = super.getJarEntry(name);
			if (find == null)
				for (JarFile search : file) {
					find = search.getJarEntry(name);
					if (find != null)
						return find;
				}
			return null;
		}

		@Override
		public InputStream getInputStream(ZipEntry name) throws IOException {
			InputStream find = super.getInputStream(name);
			if (find == null)
				for (JarFile search : file) {
					find = search.getInputStream(name);
					if (find != null)
						return find;
				}
			return null;
		}

		@Override
		public void close() throws IOException {
			super.close();
			for (JarFile f : file)
				f.close();
			file.clear();
		}

	}

	private static long seed = MathUtils.random.nextLong();

	private static int getJavaVersion() {
		String version = System.getProperty("java.version");
		if (version.startsWith("1."))
			version = version.substring(2, 3);
		else {
			int dot = version.indexOf(".");
			if (dot != -1)
				version = version.substring(0, dot);
		}
		return ParseUtils.getInt(version);
	}

	public static void initTheAPI() {
		Ref.init(Ref.getClass("net.md_5.bungee.api.ChatColor") != null ? Ref.getClass("net.kyori.adventure.Adventure") != null ? ServerType.PAPER : ServerType.SPIGOT : ServerType.BUKKIT,
				Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);

		Metrics.gatheringInfoManager = new GatheringInfoManager() {

			@Override
			public String getServerVersionVendor() {
				return null;
			}

			@Override
			public int getManagedServers() {
				return 0;
			}

			@Override
			public String getServerVersion() {
				return Bukkit.getVersion();
			}

			@Override
			public String getServerName() {
				return Bukkit.getName();
			}

			@Override
			public int getPlayers() {
				return BukkitLoader.getOnlinePlayers().size();
			}

			@Override
			public int getOnlineMode() {
				return Bukkit.getOnlineMode() ? 1 : 0;
			}

			@Override
			public Consumer<String> getInfoLogger() {
				return msg -> JavaPlugin.getPlugin(BukkitLoader.class).getLogger().log(Level.INFO, msg);
			}

			@Override
			public BiConsumer<String, Throwable> getErrorLogger() {
				return (msg, error) -> JavaPlugin.getPlugin(BukkitLoader.class).getLogger().log(Level.WARNING, msg, error);
			}
		};

		// Init json parsers
		registerWriterAndReaders();

		// version
		if (Ref.serverType() != ServerType.BUKKIT) {
			ComponentAPI.registerTransformer("BUNGEECORD", (ComponentTransformer<?>) Ref.newInstanceByClass(Ref.getClass("me.devtec.shared.components.BungeeComponentAPI")));
			if (Ref.serverType() == ServerType.PAPER)
				ComponentAPI.registerTransformer("ADVENTURE", (ComponentTransformer<?>) Ref.newInstanceByClass(Ref.getClass("me.devtec.shared.components.AdventureComponentAPI")));
		}
		Config config = new Config("plugins/TheAPI/config.yml");
		if (!config.getString("default-json-handler", "Guava").equalsIgnoreCase("TheAPI"))
			if (Ref.isNewerThan(7))
				Json.init(new ModernJsonReader(), new ModernJsonWriter()); // Modern version of Guava
			else
				Json.init((JReader) Ref.newInstanceByClass(Ref.getClass("me.devtec.shared.json.legacy.LegacyJsonReader")),
						(JWriter) Ref.newInstanceByClass(Ref.getClass("me.devtec.shared.json.legacy.LegacyJsonWriter"))); // 1.7.10

		// Commands api
		API.commandsRegister = new BukkitCommandManager();
		API.selectorUtils = new BukkitSelectorUtils();

		// OfflineCache support!
		API.initOfflineCache(Bukkit.getOnlineMode(), new Config("plugins/TheAPI/Cache.dat"));

		API.library = new LibraryLoader() {
			List<File> loaded = new ArrayList<>();
			ImplementableJar jar;
			Field libField;
			SimpleClassLoader lloader;

			@Override
			public void load(File file) {
				if (isLoaded(file) || !file.exists())
					return;
				loaded.add(file);
				ClassLoader loader = BukkitLoader.class.getClassLoader();
				if (getJavaVersion() <= 15) {
					if (addUrl == null)
						addUrl = Ref.method(URLClassLoader.class, "addURL", URL.class);
					try {
						Ref.invoke(loader, addUrl, file.toURI().toURL()); // Simple!
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				} else if (Ref.isNewerThan(16) || Ref.isNewerThan(15) && Ref.serverType() == ServerType.PAPER) {
					if (lloader == null) {
						try {
							lloader = new SimpleClassLoader(new URL[] { file.toURI().toURL() });
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						if (libField == null) {
							libField = Ref.field(loader.getClass(), "library");
							if (libField == null)
								libField = Ref.field(loader.getClass(), "libraryLoader");
						}
						Ref.set(loader, libField, lloader);
					} else
						try {
							lloader.addURL(file.toURI().toURL());
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
				} else
					try { // Just small hack for modern Java.. - Does not working for files inside jar
						if (jar == null) {
							jar = new ImplementableJar((File) Ref.get(loader, "file"));
							Ref.set(loader, "manifest", jar);
							Ref.set(loader, "jar", jar);
						}
						jar.file.add(new JarFile(file));
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			@Override
			public boolean isLoaded(File file) {
				return loaded.contains(file);
			}
		};
		API.basics().load();
		if (Ref.isOlderThan(16))
			ColorUtils.color = new ColormaticFactory() {
				String rainbow = "c6ea9b5";
				char[] chars = rainbow.toCharArray();
				AtomicInteger position = new AtomicInteger(0);

				int[][] EMPTY_ARRAY = {};
				char[] EMPTY_CHAR_ARRAY = {};
				char[] RESET_CHAR_ARRAY = { '§', 'r' };

				@Override
				public StringContainer gradient(StringContainer container, int start, int end, @Nullable String firstHex, @Nullable String secondHex, @Nullable List<String> protectedStrings) {
					boolean inRainbow = false;
					char[] formats = EMPTY_CHAR_ARRAY;

					// Skip regions
					int[][] skipRegions = EMPTY_ARRAY;
					byte allocated = 0;
					int currentSkipAt = -1;
					byte skipId = 0;

					if (protectedStrings != null) {
						for (String protect : protectedStrings) {
							int size = protect.length();

							int num = 0;
							while (true) {
								int position = container.indexOf(protect, num);
								if (position == -1)
									break;
								num = position + size;
								if (allocated == 0 || allocated >= skipRegions.length - 1) {
									int[][] copy = new int[(allocated << 1) + 1][];
									if (allocated > 0)
										System.arraycopy(skipRegions, 0, copy, 0, skipRegions.length);
									skipRegions = copy;
								}
								skipRegions[allocated++] = new int[] { position, size };
							}
						}
						if (allocated > 0)
							currentSkipAt = skipRegions[0][0];
					}

					int i = start - 1;
					for (int step = 0; step < end - start; ++step) {
						char c = container.charAt(++i);

						if (currentSkipAt == i) {
							int skipForChars = skipRegions[skipId++][1] - 1;
							currentSkipAt = skipId == allocated ? -1 : skipRegions[skipId][0];
							i += skipForChars;
							step += skipForChars;
							continue;
						}

						if (c == '&' && i + 1 < container.length() && container.charAt(i + 1) == 'u') {
							container.delete(i, i + 2);
							--i;
							inRainbow = true;
							continue;
						}

						if (inRainbow)
							switch (c) {
							case ' ':
								if (formats.length == 2 && formats[1] == 'r') {
									container.insertMultipleChars(i, formats);
									formats = EMPTY_CHAR_ARRAY;
									i += 2;
									container.insert(i, generateColor());
									i += 2;
								}
								continue;
							case '§':
								if (i + 1 < container.length()) {
									c = container.charAt(++i);
									++step;
									if (isFormat(c)) {
										if (c == 'r')
											formats = RESET_CHAR_ARRAY;
										else if (formats.length == 0)
											formats = new char[] { '§', c };
										else {
											char[] copy = new char[formats.length + 2];
											System.arraycopy(formats, 0, copy, 0, formats.length);
											formats = copy;
											formats[formats.length - 2] = '§';
											formats[formats.length - 1] = c;
										}
										break;
									}
									if (isColor(c) || c == 'x')
										inRainbow = false;
									break;
								}
							default:
								if (formats.length == 2 && formats[1] == 'r') {
									container.insertMultipleChars(i, formats);
									formats = EMPTY_CHAR_ARRAY;
									i += 2;
									container.insert(i, generateColor());
									i += 2;
								} else {
									container.insert(i, generateColor());
									i += 2;
									if (formats.length != 0) {
										container.insertMultipleChars(i, formats);
										i += formats.length;
									}
								}
								break;
							}
					}
					return container;
				}

				private boolean isColor(int charAt) {
					return charAt >= 97 && charAt <= 102 || charAt >= 65 && charAt <= 70 || charAt >= 48 && charAt <= 57;
				}

				private boolean isFormat(int charAt) {
					return charAt >= 107 && charAt <= 111 || charAt == 114;
				}

				@Override
				public String generateColor() {
					if (position.get() == chars.length)
						position.set(0);
					return new String(new char[] { '§', chars[position.getAndIncrement()] });
				}

				@Override
				public StringContainer replaceHex(StringContainer text) {
					return text;
				}

				@Override
				public StringContainer rainbow(StringContainer container, int start, int end, @Nullable String firstHex, @Nullable String secondHex, @Nullable List<String> protectedStrings) {
					gradient(container, start, end, null, null, protectedStrings);
					return container;
				}
			};
	}

	private static void registerWriterAndReaders() {
		// world
		Json.registerDataWriter(new DataWriter() {

			@Override
			public Map<String, Object> write(Object object) {
				Map<String, Object> map = new HashMap<>();
				World pos = (World) object;
				map.put("classType", "org.bukkit.World");
				map.put("name", pos.getName());
				map.put("uuid", pos.getUID());
				return map;
			}

			@Override
			public boolean isAllowed(Object object) {
				return object instanceof World;
			}
		});
		Json.registerDataReader(new DataReader() {

			@Override
			public boolean isAllowed(Map<String, Object> map) {
				return "org.bukkit.World".equals(map.get("classType"));
			}

			@Override
			public Object read(Map<String, Object> map) {
				return Bukkit.getWorld(UUID.fromString(map.get("uuid").toString()));
			}
		});
		// location
		Json.registerDataWriter(new DataWriter() {

			@Override
			public Map<String, Object> write(Object object) {
				Map<String, Object> map = new HashMap<>();
				Location pos = (Location) object;
				map.put("classType", "org.bukkit.Location");
				map.put("world", pos.getWorld().getName());
				map.put("x", pos.getX());
				map.put("y", pos.getY());
				map.put("z", pos.getZ());
				map.put("yaw", pos.getYaw());
				map.put("pitch", pos.getPitch());
				return map;
			}

			@Override
			public boolean isAllowed(Object object) {
				return object instanceof Location;
			}
		});
		Json.registerDataReader(new DataReader() {

			@Override
			public boolean isAllowed(Map<String, Object> map) {
				return "org.bukkit.Location".equals(map.get("classType"));
			}

			@Override
			public Object read(Map<String, Object> map) {
				Object result = map.get("yaw");
				return new Location(Bukkit.getWorld(map.get("world").toString()), ((Number) map.get("x")).doubleValue(), ((Number) map.get("y")).doubleValue(), ((Number) map.get("z")).doubleValue(),
						((Number) (result == null ? 0f : result)).floatValue(), ((Number) ((result = map.get("pitch")) == null ? 0f : result)).floatValue());
			}
		});
		// position
		Json.registerDataWriter(new DataWriter() {

			@Override
			public Map<String, Object> write(Object object) {
				Map<String, Object> map = new HashMap<>();
				Position pos = (Position) object;
				map.put("classType", "Position");
				map.put("world", pos.getWorldName());
				map.put("x", pos.getX());
				map.put("y", pos.getY());
				map.put("z", pos.getZ());
				map.put("yaw", pos.getYaw());
				map.put("pitch", pos.getPitch());
				return map;
			}

			@Override
			public boolean isAllowed(Object object) {
				return object instanceof Position;
			}
		});
		Json.registerDataReader(new DataReader() {

			@Override
			public boolean isAllowed(Map<String, Object> map) {
				return "Position".equals(map.get("classType"));
			}

			@Override
			public Object read(Map<String, Object> map) {
				Object result = map.get("yaw");
				return new Position(map.get("world").toString(), ((Number) map.get("x")).doubleValue(), ((Number) map.get("y")).doubleValue(), ((Number) map.get("z")).doubleValue(),
						((Number) (result == null ? 0f : result)).floatValue(), ((Number) ((result = map.get("pitch")) == null ? 0f : result)).floatValue());
			}
		});
		// itemstack
		Json.registerDataWriter(new DataWriter() {

			@Override
			public Map<String, Object> write(Object object) {
				Map<String, Object> map = new HashMap<>();
				ItemStack item = (ItemStack) object;
				map.put("classType", "ItemStack");
				try {
					map.put("type", XMaterial.matchXMaterial(item.getType()).name());
				} catch (IllegalArgumentException err) {
					map.put("type", item.getType().name()); // Modded item
				}
				map.put("amount", item.getAmount());
				if (item.hasItemMeta()) {
					ItemMeta meta = item.getItemMeta();
					if (meta instanceof Damageable)
						if (((Damageable) meta).getDamage() != 0)
							map.put("durability", ((Damageable) meta).getDamage());
					if (!meta.getEnchants().isEmpty())
						map.put("enchants", writeEnchants(meta.getEnchants()));
					if (meta.hasDisplayName())
						map.put("meta.displayName", meta.getDisplayName());
					if (meta.hasLore())
						map.put("meta.lore", meta.getLore());
					if (Ref.isNewerThan(7))
						if (!meta.getItemFlags().isEmpty())
							map.put("meta.itemFlags", writeItemFlags(meta.getItemFlags()));
					if (Ref.isNewerThan(13))
						if (meta.hasCustomModelData())
							map.put("meta.customModelData", meta.getCustomModelData());

					System.out.println(item.getItemMeta());
					NBTEdit nbt = new NBTEdit(item);
					System.out.println(nbt.getNBT());
					// remove unused tags
					nbt.remove("id");
					nbt.remove("Count");
					nbt.remove("lvl");
					nbt.remove("display");
					nbt.remove("Name");
					nbt.remove("Lore");
					nbt.remove("Damage");
					nbt.remove("HideFlags");
					nbt.remove("Enchantments");
					nbt.remove("CustomModelData");
					nbt.remove("ench");
					if (!nbt.getKeys().isEmpty())
						map.put("meta.nbt", String.valueOf(nbt.getNBT())); // save clear nbt
				}
				return map;
			}

			private Set<String> writeItemFlags(Set<?> itemFlags) {
				Set<String> set = new HashSet<>();
				for (Object flag : itemFlags)
					set.add(((ItemFlag) flag).name());
				return set;
			}

			private Map<String, Integer> writeEnchants(Map<Enchantment, Integer> enchantments) {
				Map<String, Integer> saved = new HashMap<>();
				for (Entry<Enchantment, Integer> enchant : enchantments.entrySet())
					saved.put(enchant.getKey().getName(), enchant.getValue());
				return saved;
			}

			@Override
			public boolean isAllowed(Object object) {
				return object instanceof ItemStack;
			}
		});
		Json.registerDataReader(new DataReader() {

			@Override
			public boolean isAllowed(Map<String, Object> map) {
				return "ItemStack".equals(map.get("classType"));
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object read(Map<String, Object> map) {
				ItemMaker maker;
				XMaterial material = XMaterial.matchXMaterial(map.get("type").toString().toUpperCase()).orElse(XMaterial.STONE);
				if (material == XMaterial.STONE && !map.get("type").toString().toUpperCase().equals("STONE")) {
					Material bukkitMaterial = Material.getMaterial(map.get("type").toString());
					if (bukkitMaterial != null)
						maker = ItemMaker.of(bukkitMaterial);
					else
						maker = ItemMaker.of(material);
				} else
					maker = ItemMaker.of(material);

				if (map.containsKey("amount"))
					maker.amount(((Number) map.get("amount")).intValue());
				if (map.containsKey("durability"))
					maker.damage(((Number) map.get("durability")).intValue());
				if (map.containsKey("meta.customModelData"))
					maker.customModel(((Number) map.get("meta.customModelData")).intValue());
				if (map.containsKey("meta.displayName"))
					maker.displayName(map.get("meta.displayName").toString());
				if (map.containsKey("meta.itemFlags") && map.get("meta.itemFlags") instanceof List)
					maker.itemFlags((List<String>) map.get("meta.itemFlags"));
				if (map.containsKey("meta.lore") && map.get("meta.lore") instanceof List)
					maker.lore((List<String>) map.get("meta.lore"));
				if (map.containsKey("enchants") && map.get("enchants") instanceof Map)
					enchants(maker, (Map<Object, Object>) map.get("enchants"));
				if (map.containsKey("meta.nbt"))
					maker.nbt(new NBTEdit(map.get("meta.nbt").toString()));
				return maker.build();
			}

			private ItemMaker enchants(ItemMaker nbt, Map<Object, Object> map) {
				for (Entry<Object, Object> enchant : map.entrySet())
					if (enchant.getValue() instanceof Number)
						nbt.enchant(EnchantmentAPI.byName(enchant.getKey().toString().toUpperCase()).getEnchantment(), ((Number) enchant.getValue()).intValue());
				return nbt;
			}
		});

		// blockdatastorage
		Json.registerDataReader(new DataReader() {

			@Override
			public Object read(Map<String, Object> json) {
				Object nbt = json.get("nbt");
				return new BlockDataStorage(Material.getMaterial(json.get("material").toString()), ((Number) json.get("itemData")).byteValue(), json.get("data").toString(),
						nbt == null ? null : nbt.toString());
			}

			@Override
			public boolean isAllowed(Map<String, Object> json) {
				return "BlockDataStorage".equals(json.get("classType"));
			}
		});
		Json.registerDataWriter(new DataWriter() {

			@Override
			public boolean isAllowed(Object obj) {
				return obj instanceof BlockDataStorage;
			}

			@Override
			public Map<String, Object> write(Object obj) {
				BlockDataStorage data = (BlockDataStorage) obj;
				Map<String, Object> map = new HashMap<>();
				map.put("classType", "BlockDataStorage");
				map.put("material", data.getType().name());
				map.put("itemData", data.getItemData());
				map.put("data", data.getData() == null ? "" : data.getData());
				if (data.getNBT() != null)
					map.put("nbt", data.getNBT());
				return map;
			}
		});
	}
}
