package me.devtec.theapi.bukkit.game;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.collect.Multimap;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.devtec.shared.Ref;
import me.devtec.shared.Ref.ServerType;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StreamUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.GameProfileHandler;
import me.devtec.theapi.bukkit.nms.GameProfileHandler.PropertyHandler;
import me.devtec.theapi.bukkit.nms.NBTEdit;
import me.devtec.theapi.bukkit.xseries.XMaterial;
import net.md_5.bungee.api.chat.BaseComponent;

public class ItemMaker implements Cloneable {
	private static Material skull = XMaterial.PLAYER_HEAD.parseMaterial();
	static Object hdbApi;
	static int HDB_TYPE;
	static {
		if (Ref.getClass("me.arcaniax.hdb.api.HeadDatabaseAPI.HeadDatabaseAPI") != null) {
			hdbApi = new HeadDatabaseAPI();
			HDB_TYPE = 1; // paid
		}
	}
	static final Field properties = Ref.field(Ref.craft("profile.CraftPlayerProfile"), "properties");
	static final Field value = Ref.field(Ref.getClass("com.mojang.authlib.properties.Property"), "value");

	private Material material;
	private int amount = 1;
	private short damage;

	// additional
	private String displayName;
	private List<String> lore;
	private Map<Enchantment, Integer> enchants;
	private List<String> itemFlags;
	private int customModel;
	private boolean unbreakable;
	public byte data;
	private NBTEdit nbt;

	protected ItemMaker(Material material) {
		this.material = material;
	}

	@Override
	public ItemMaker clone() {
		try {
			return (ItemMaker) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Map<String, Object> serializeToMap() {
		Map<String, Object> map = new HashMap<>();
		try {
			XMaterial type = XMaterial.matchXMaterial(material);
			if (type == null)
				map.put("type", material.name()); // Modded item
			else
				map.put("type", type.name());
		} catch (IllegalArgumentException error) {
			map.put("type", material.name()); // Modded item
		}
		map.put("amount", amount);
		if (damage != 0)
			map.put("durability", damage);
		if (customModel != 0)
			map.put("customModel", customModel);
		if (data != 0)
			map.put("data", data);
		if (unbreakable)
			map.put("unbreakable", unbreakable);
		if (displayName != null && !displayName.isEmpty())
			map.put("displayName", displayName);
		if (lore != null && !lore.isEmpty())
			map.put("lore", lore);
		if (enchants != null) {
			Map<String, Integer> serialized = new HashMap<>(enchants.size());
			for (Entry<Enchantment, Integer> s : enchants.entrySet())
				serialized.put(s.getKey().getName(), s.getValue());
			map.put("enchants", serialized);
		}
		if (itemFlags != null)
			map.put("itemFlags", itemFlags);
		if (nbt != null && nbt.getNBT() != null)
			map.put("nbt", nbt.getNBT().toString());
		return map;
	}

	protected ItemMeta apply(ItemMeta meta) {
		if (displayName != null)
			meta.setDisplayName(displayName);
		if (lore != null)
			meta.setLore(lore);
		if (enchants != null)
			for (Entry<Enchantment, Integer> s : enchants.entrySet())
				meta.addEnchant(s.getKey(), s.getValue(), true);
		if (Ref.isNewerThan(7) && itemFlags != null)
			for (String flag : itemFlags)
				try {
					ItemFlag iFlag = ItemFlag.valueOf(flag.toUpperCase());
					if (iFlag != null)
						meta.addItemFlags(iFlag);
				} catch (NoSuchFieldError | Exception err) {

				}
		if (Ref.isNewerThan(13) && customModel != 0)
			meta.setCustomModelData(customModel);
		if (unbreakable)
			if (Ref.isNewerThan(10))
				meta.setUnbreakable(true);
			else
				try {
					Ref.invoke(Ref.invoke(meta, "spigot"), "setUnbreakable", true);
				} catch (NoSuchFieldError | Exception e2) {
					// unsupported
				}
		return meta;
	}

	public ItemMaker type(Material material) {
		this.material = material;
		return this;
	}

	public ItemMaker type(XMaterial material) {
		return type(material.parseMaterial());
	}

	public Material getMaterial() {
		return material;
	}

	public ItemMaker amount(int amount) {
		this.amount = amount;
		return this;
	}

	public int getAmount() {
		return amount;
	}

	public ItemMaker damage(int damage) {
		this.damage = (short) damage;
		return this;
	}

	public short getDamage() {
		return damage;
	}

	public ItemMaker data(int data) {
		this.data = (byte) data;
		return this;
	}

	public byte getData() {
		return data;
	}

	public ItemMaker displayName(String name) {
		displayName = ColorUtils.colorize(name);
		return this;
	}

	public ItemMaker rawDisplayName(String name) {
		displayName = name;
		return this;
	}

	@Nullable
	public String getDisplayName() {
		return displayName;
	}

	public ItemMaker lore(String... lore) {
		return this.lore(Arrays.asList(lore));
	}

	public ItemMaker lore(List<String> lore) {
		this.lore = ColorUtils.colorize(new ArrayList<>(lore));
		return this;
	}

	public ItemMaker rawLore(String... lore) {
		return this.lore(Arrays.asList(lore));
	}

	public ItemMaker rawLore(List<String> lore) {
		this.lore = new ArrayList<>(lore);
		return this;
	}

	@Nullable
	public List<String> getLore() {
		return lore;
	}

	public ItemMaker customModel(int customModel) {
		this.customModel = customModel;
		return this;
	}

	public int getCustomModel() {
		return customModel;
	}

	public ItemMaker unbreakable(boolean unbreakable) {
		this.unbreakable = unbreakable;
		return this;
	}

	public boolean isUnbreakable() {
		return unbreakable;
	}

	public ItemMaker itemFlags(String... flag) {
		return this.itemFlags(Arrays.asList(flag));
	}

	public ItemMaker itemFlags(List<String> flag) {
		itemFlags = flag;
		return this;
	}

	@Nullable
	public List<String> getItemFlags() {
		return itemFlags;
	}

	public ItemMaker enchant(Enchantment enchant, int level) {
		if (enchant == null)
			return this;
		if (enchants == null)
			enchants = new HashMap<>();
		enchants.put(enchant, level);
		return this;
	}

	@Nullable
	public Map<Enchantment, Integer> getEnchants() {
		return enchants;
	}

	public ItemMaker nbt(NBTEdit nbtEdit) {
		if (nbtEdit == null) {
			nbt = null;
			return this;
		}
		// remove unused tags
		nbtEdit.remove("id");
		nbtEdit.remove("Count");
		nbtEdit.remove("lvl");
		nbtEdit.remove("display");
		nbtEdit.remove("Name");
		nbtEdit.remove("Lore");
		nbtEdit.remove("Damage");
		nbtEdit.remove("color");
		nbtEdit.remove("Unbreakable");
		nbtEdit.remove("HideFlags");
		nbtEdit.remove("Enchantments");
		nbtEdit.remove("CustomModelData");
		nbtEdit.remove("ench");
		nbtEdit.remove("SkullOwner");
		nbtEdit.remove("BlockEntityTag");
		// book
		nbtEdit.remove("author");
		nbtEdit.remove("title");
		nbtEdit.remove("filtered_title");
		nbtEdit.remove("pages");
		nbtEdit.remove("resolved");
		nbtEdit.remove("generation");
		// banner
		nbtEdit.remove("base-color");
		nbtEdit.remove("patterns");
		nbtEdit.remove("pattern");
		// banner
		nbtEdit.remove("base-color");
		nbtEdit.remove("patterns");
		nbtEdit.remove("pattern");

		if (!nbtEdit.getKeys().isEmpty())
			nbt = nbtEdit;
		else
			nbt = null;
		return this;
	}

	@Nullable
	public NBTEdit getNbt() {
		if (nbt == null)
			return new NBTEdit(new ItemStack(material));
		return nbt;
	}

	public ItemMaker enchanted() {
		if (itemFlags != null) {
			itemFlags.add("HIDE_ENCHANTS");
			itemFlags.add("HIDE_ATTRIBUTES");
		} else
			itemFlags("HIDE_ENCHANTS", "HIDE_ATTRIBUTES");
		if (enchants == null || enchants != null && enchants.isEmpty())
			return enchant(Enchantment.DURABILITY, 1);
		return this;
	}

	public ItemMaker itemMeta(ItemMeta meta) {
		XMaterial xmaterial = XMaterial.matchXMaterial(material);
		ItemMaker maker = this;

		if (material.name().contains("BANNER")) {
			BannerMeta banner = (BannerMeta) meta;
			maker = ofBanner(BannerColor.valueOf(banner.getBaseColor() != null ? banner.getBaseColor().toString().toUpperCase() : "NONE"));
			List<Pattern> patternlist = new ArrayList<>(banner.getPatterns());
			if (!patternlist.isEmpty())
				((BannerItemMaker) maker).patterns = patternlist;
		}

		if (material.name().contains("LEATHER_")) {
			LeatherArmorMeta armor = (LeatherArmorMeta) meta;
			maker = ofLeatherArmor(material);
			((LeatherItemMaker) maker).color(Color.fromRGB(armor.getColor().asRGB()));
		}

		if (xmaterial == XMaterial.PLAYER_HEAD) {
			SkullMeta skull = (SkullMeta) meta;
			maker = ofHead();
			if (Ref.isNewerThan(16) && Ref.serverType() == ServerType.PAPER) {
				com.destroystokyo.paper.profile.PlayerProfile profile = skull.getPlayerProfile();
				for (ProfileProperty property : profile.getProperties())
					if (property.getName().equals("textures")) {
						((HeadItemMaker) maker).skinValues(property.getValue());
						break;
					}
			} else if (Ref.isNewerThan(17)) {
				PlayerProfile profile = skull.getOwnerProfile();
				@SuppressWarnings("unchecked")
				Multimap<String, Object> props = (Multimap<String, Object>) Ref.get(profile, ItemMaker.properties);
				Collection<Object> coll = props.get("textures");
				String value = coll.isEmpty() ? null : (String) Ref.get(coll.iterator().next(), ItemMaker.value);
				if (value != null)
					((HeadItemMaker) maker).skinValues(value);
			} else if (skull.getOwner() != null && !skull.getOwner().isEmpty())
				((HeadItemMaker) maker).skinName(skull.getOwner());
			else {
				Object profile = Ref.get(skull, HeadItemMaker.profileField);
				if (profile != null) {
					PropertyHandler properties = BukkitLoader.getNmsProvider().fromGameProfile(profile).getProperties().get("textures");
					String value = properties == null ? null : properties.getValues();
					if (value != null)
						((HeadItemMaker) maker).skinValues(value);
				}
			}
		}

		if (material.name().contains("POTION")) {
			PotionMeta potion = (PotionMeta) meta;

			maker = ofPotion(Potion.POTION);
			if (material.name().equalsIgnoreCase("LINGERING_POTIO"))
				maker = ofPotion(Potion.LINGERING);
			if (material.name().equalsIgnoreCase("SPLASH_POTION"))
				maker = ofPotion(Potion.SPLASH);

			List<PotionEffect> effects = new ArrayList<>(potion.getCustomEffects());

			if (!effects.isEmpty())
				((PotionItemMaker) maker).potionEffects(effects);
			if (Ref.isNewerThan(10)) // 1.11+
				if (potion.getColor() != null)
					((PotionItemMaker) maker).color(potion.getColor());
		}

		if (xmaterial == XMaterial.ENCHANTED_BOOK) {
			EnchantmentStorageMeta book = (EnchantmentStorageMeta) meta;
			maker = ofEnchantedBook();
			if (book.hasStoredEnchants() && book.getStoredEnchants() != null)
				for (Entry<Enchantment, Integer> enchant : book.getStoredEnchants().entrySet())
					enchant(enchant.getKey(), enchant.getValue());
		} else if (meta.getEnchants() != null)
			for (Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet())
				enchant(enchant.getKey(), enchant.getValue());

		if (xmaterial == XMaterial.WRITTEN_BOOK || xmaterial == XMaterial.WRITABLE_BOOK) {
			BookMeta book = (BookMeta) meta;
			maker = xmaterial == XMaterial.WRITTEN_BOOK ? ofBook() : ofWritableBook();
			if (book.getAuthor() != null)
				((BookItemMaker) maker).rawAuthor(book.getAuthor());
			if (Ref.isNewerThan(9)) // 1.10+
				((BookItemMaker) maker).generation(book.getGeneration() == null ? null : book.getGeneration().name());
			((BookItemMaker) maker).rawTitle(book.getTitle());
			if (!book.getPages().isEmpty())
				((BookItemMaker) maker).rawPages(book.getPages());
		}

		if (meta.getDisplayName() != null)
			maker.rawDisplayName(meta.getDisplayName());
		if (meta.getLore() != null && !meta.getLore().isEmpty())
			maker.rawLore(meta.getLore());
		// Unbreakable
		if (Ref.isNewerThan(10)) { // 1.11+
			if (meta.isUnbreakable())
				maker.unbreakable(true);
		} else if ((boolean) Ref.invoke(Ref.invoke(meta, "spigot"), "isUnbreakable"))
			try {
				maker.unbreakable(true);
			} catch (NoSuchFieldError | Exception e2) {
				// unsupported
			}
		// ItemFlags
		if (Ref.isNewerThan(7)) { // 1.8+
			List<String> flags = new ArrayList<>();
			for (ItemFlag flag : meta.getItemFlags())
				flags.add(flag.name());
			if (!flags.isEmpty())
				maker.itemFlags(flags);
		}
		// Modeldata
		if (Ref.isNewerThan(13)) { // 1.14+
			int modelData = meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
			if (modelData != 0)
				maker.customModel(modelData);
		}
		return maker;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		if (material != null)
			hash = hash * 33 + material.hashCode();
		hash = hash * 33 + amount;
		hash = hash * 33 + damage;
		if (displayName != null)
			hash = hash * 33 + displayName.hashCode();
		if (lore != null)
			hash = hash * 33 + lore.hashCode();
		if (enchants != null)
			hash = hash * 33 + enchants.hashCode();
		if (itemFlags != null)
			hash = hash * 33 + itemFlags.hashCode();
		hash = hash * 33 + customModel;
		hash = hash * 33 + (unbreakable ? 1 : 0);
		hash = hash * 33 + data;
		if (nbt != null && nbt.getNBT() != null)
			hash = hash * 33 + nbt.getNBT().hashCode();
		return hash;
	}

	public ItemStack build() {
		if (material == null)
			throw new IllegalArgumentException("Material cannot be null");
		ItemStack item = data != 0 && Ref.isOlderThan(13) ? new ItemStack(material, amount, (short) 0, data) : new ItemStack(material, amount);
		if (damage != 0)
			item.setDurability(damage);
		if (nbt != null)
			item = BukkitLoader.getNmsProvider().setNBT(item, nbt.getNBT());
		if (item.getItemMeta() == null)
			throw new IllegalArgumentException("Cannot create ItemMeta for material type " + material);
		item.setItemMeta(apply(item.getItemMeta()));
		return item;
	}

	public static class HeadItemMaker extends ItemMaker {
		static final String URL_FORMAT = "https://api.mineskin.org/generate/url?url=%s&%s";
		static final Field profileField = Ref.field(Ref.craft("inventory.CraftMetaSkull"), "profile");

		private String owner;
		/**
		 * 0 = offlinePlayer 1 = player.values 2 = url.png
		 */
		private int ownerType;

		protected HeadItemMaker() {
			super(ItemMaker.skull);
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (owner != null) {
				map.put("head.type", getFormattedOwnerType());
				map.put("head.owner", owner);
			}
			return map;
		}

		public HeadItemMaker skinName(String name) {
			owner = name;
			ownerType = 0;
			return this;
		}

		public HeadItemMaker skinValues(String name) {
			owner = name;
			ownerType = 1;
			return this;
		}

		public HeadItemMaker skinUrl(String name) {
			owner = name;
			ownerType = 2;
			return this;
		}

		public HeadItemMaker skinHDB(String id) {
			if (hdbApi != null) {
				owner = getBase64OfId(id);
				ownerType = 1;
			} else {
				owner = id;
				ownerType = 0;
			}
			return this;
		}

		@Nullable
		public String getHeadOwner() {
			return owner;
		}

		/**
		 * @apiNote Return's head owner type. 0 = Name 1 = Values 2 = Url
		 * @return int Head owner type
		 */
		public int getHeadOwnerType() {
			return ownerType;
		}

		public String getFormattedOwnerType() {
			switch (ownerType) {
			case 0:
				return "PLAYER_NAME";
			case 1:
				return "VALUES";
			case 2:
				return "URL";
			}
			return "PLAYER_NAME";
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof SkullMeta))
				return super.apply(meta);
			SkullMeta iMeta = (SkullMeta) meta;
			String finalValue = owner;
			if (finalValue != null)
				switch (ownerType) {
				case 0: // Player
					iMeta.setOwner(finalValue);
					break;
				case 2: // Url
					finalValue = ItemMaker.fromUrl(owner);
				case 1: { // Values
					if (Ref.isNewerThan(16) && Ref.serverType() == ServerType.PAPER) {
						com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
						profile.setProperty(new ProfileProperty("textures", finalValue));
						iMeta.setPlayerProfile(profile);
					} else if (Ref.isNewerThan(17)) {
						PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
						@SuppressWarnings("unchecked")
						Multimap<String, Object> props = (Multimap<String, Object>) Ref.get(profile, ItemMaker.properties);
						props.removeAll("textures");
						Object property = Ref.newInstance(Ref.constructor(Ref.getClass("com.mojang.authlib.properties.Property"), String.class, String.class, String.class), "textures", finalValue,
								null);
						props.put("textures", property);
						iMeta.setOwnerProfile(profile);
					} else
						Ref.set(iMeta, HeadItemMaker.profileField,
								BukkitLoader.getNmsProvider().toGameProfile(GameProfileHandler.of("", UUID.randomUUID(), PropertyHandler.of("textures", finalValue))));
					break;
				}
				default: // New dimension
					break;
				}
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (owner != null)
				hash = hash * 33 + owner.hashCode();
			return hash * 33 + ownerType;
		}
	}

	public static class LeatherItemMaker extends ItemMaker {
		private Color color;

		protected LeatherItemMaker(Material material) {
			super(material);
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (color != null) {
				String hex = Integer.toHexString(color.asRGB());
				map.put("leather.color", "#" + (hex.length() > 6 ? hex.substring(2) : hex));
			}
			return map;
		}

		public LeatherItemMaker color(Color color) {
			this.color = color;
			return this;
		}

		@Nullable
		public Color getColor() {
			return color;
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof LeatherArmorMeta))
				return super.apply(meta);
			LeatherArmorMeta iMeta = (LeatherArmorMeta) meta;
			if (color != null)
				iMeta.setColor(color);
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (color != null)
				hash = hash * 33 + color.hashCode();
			return hash;
		}
	}

	public static class BookItemMaker extends ItemMaker {
		private String author;
		private String title;
		private List<Component> pages;
		private String generation;

		protected BookItemMaker(boolean written) {
			super(written ? Material.WRITTEN_BOOK : XMaterial.WRITABLE_BOOK.parseMaterial());
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (author != null)
				map.put("book.author", author);
			if (title != null)
				map.put("book.title", title);
			if (pages != null) {
				List<String> jsonList = new ArrayList<>();
				for (Component page : pages)
					jsonList.add(Json.writer().simpleWrite(ComponentAPI.toJsonList(page)));
				map.put("book.pages", jsonList);
			}
			if (generation != null)
				map.put("book.generation", generation);
			return map;
		}

		public BookItemMaker author(String author) {
			this.author = ColorUtils.colorize(author);
			return this;
		}

		public BookItemMaker rawAuthor(String author) {
			this.author = author;
			return this;
		}

		@Nullable
		public String getAuthor() {
			return author;
		}

		public BookItemMaker title(String title) {
			this.title = ColorUtils.colorize(title);
			return this;
		}

		public BookItemMaker rawTitle(String title) {
			this.title = title;
			return this;
		}

		@Nullable
		public String getTitle() {
			return title;
		}

		public BookItemMaker generation(String generation) {
			this.generation = generation;
			return this;
		}

		public BookItemMaker pages(String... pages) {
			return this.pages(Arrays.asList(pages));
		}

		@Nullable
		public String getGeneration() {
			return generation;
		}

		public BookItemMaker pages(List<String> pages) {
			this.pages = new ArrayList<>();
			for (String string : pages)
				this.pages.add(ComponentAPI.fromString(ColorUtils.colorize(string)));
			return this;
		}

		public BookItemMaker rawPages(List<String> pages) {
			this.pages = new ArrayList<>();
			for (String string : pages)
				this.pages.add(ComponentAPI.fromString(string));
			return this;
		}

		public BookItemMaker pagesComp(Component... pages) {
			return this.pagesComp(Arrays.asList(pages));
		}

		public BookItemMaker pagesComp(List<Component> pages) {
			this.pages = pages;
			return this;
		}

		@Nullable
		public List<Component> getPages() {
			return pages;
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof BookMeta))
				return super.apply(meta);
			BookMeta iMeta = (BookMeta) meta;
			if (author != null)
				iMeta.setAuthor(author);
			if (pages != null)
				if (!Ref.isNewerThan(11) || Ref.serverType() == ServerType.BUKKIT) {
					List<String> page = new ArrayList<>(pages.size());
					for (Component comp : pages)
						page.add(comp.toString());
					iMeta.setPages(page);
				} else
					for (Component page : pages)
						iMeta.spigot().addPage((BaseComponent[]) ComponentAPI.bungee().fromComponents(page));
			if (Ref.isNewerThan(9) && generation != null)
				iMeta.setGeneration(Generation.valueOf(generation.toUpperCase()));
			if (title != null)
				iMeta.setTitle(title);
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (author != null)
				hash = hash * 33 + author.hashCode();
			if (title != null)
				hash = hash * 33 + title.hashCode();
			if (pages != null)
				hash = hash * 33 + pages.hashCode();
			if (generation != null)
				hash = hash * 33 + generation.hashCode();
			return hash;
		}
	}

	public static class EnchantedBookItemMaker extends ItemMaker {
		protected EnchantedBookItemMaker() {
			super(Material.ENCHANTED_BOOK);
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof EnchantmentStorageMeta))
				return super.apply(meta);
			EnchantmentStorageMeta iMeta = (EnchantmentStorageMeta) meta;
			if (super.displayName != null)
				iMeta.setDisplayName(super.displayName);
			if (super.lore != null)
				iMeta.setLore(super.lore);
			if (super.enchants != null)
				for (Entry<Enchantment, Integer> s : super.enchants.entrySet())
					iMeta.addStoredEnchant(s.getKey(), s.getValue(), true);
			if (Ref.isNewerThan(7) && super.itemFlags != null)
				for (String flag : super.itemFlags)
					try {
						ItemFlag iFlag = ItemFlag.valueOf(flag.toUpperCase());
						if (iFlag != null)
							iMeta.addItemFlags(iFlag);
					} catch (NoSuchFieldError | Exception err) {

					}
			if (Ref.isNewerThan(13) && super.customModel != 0)
				iMeta.setCustomModelData(super.customModel);
			if (super.unbreakable)
				if (Ref.isNewerThan(10))
					iMeta.setUnbreakable(true);
				else
					try {
						Ref.invoke(Ref.invoke(meta, "spigot"), "setUnbreakable", true);
					} catch (NoSuchFieldError | Exception e2) {
						// unsupported
					}
			return iMeta;
		}
	}

	public static class PotionItemMaker extends ItemMaker {
		private Color color;
		private List<PotionEffect> effects;

		protected PotionItemMaker(Material material) {
			super(material);
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (Ref.isNewerThan(10) && color != null) {
				String hex = Integer.toHexString(color.asRGB());
				map.put("potion.color", "#" + (hex.length() > 6 ? hex.substring(2) : hex));
			}
			if (effects != null) {
				List<String> serialized = new ArrayList<>(effects.size());
				for (PotionEffect effect : effects)
					serialized.add(effect.getType().getName() + ":" + effect.getDuration() + ":" + effect.getAmplifier() + ":" + effect.isAmbient() + ":" + effect.hasParticles());
				map.put("potion.effects", serialized);
			}
			return map;
		}

		public PotionItemMaker color(Color color) {
			this.color = color;
			return this;
		}

		@Nullable
		public Color getColor() {
			return color;
		}

		public PotionItemMaker potionEffects(PotionEffect... effects) {
			return this.potionEffects(Arrays.asList(effects));
		}

		public PotionItemMaker potionEffects(List<PotionEffect> effects) {
			this.effects = effects;
			return this;
		}

		@Nullable
		public List<PotionEffect> getPotionEffects() {
			return effects;
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof PotionMeta))
				return super.apply(meta);
			PotionMeta iMeta = (PotionMeta) meta;
			if (color != null && Ref.isNewerThan(10))
				iMeta.setColor(color);
			if (effects != null)
				for (PotionEffect effect : effects)
					iMeta.addCustomEffect(effect, true);
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (color != null)
				hash = hash * 33 + color.hashCode();
			if (effects != null)
				hash = hash * 33 + effects.hashCode();
			return hash;
		}
	}

	public static class ShulkerBoxItemMaker extends ItemMaker {
		private String name;
		private ItemStack[] contents;

		protected ShulkerBoxItemMaker(XMaterial xMaterial) {
			super(xMaterial.parseMaterial());
			super.data = xMaterial.getData();
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (name != null)
				map.put("shulker.name", name);
			if (contents != null) {
				List<Map<String, Object>> serialized = new ArrayList<>(contents.length);
				for (ItemStack content : contents)
					if (content == null || content.getType() == Material.AIR)
						serialized.add(null);
					else
						serialized.add(ItemMaker.of(content).serializeToMap());
				map.put("shulker.contents", serialized);
			}
			return map;
		}

		public ShulkerBoxItemMaker name(String name) {
			this.name = name;
			return this;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public ShulkerBoxItemMaker contents(ItemStack[] contents) {
			ItemStack[] copy = new ItemStack[27];
			for (int i = 0; i < 27 && i < contents.length; ++i)
				copy[i] = contents[i];
			this.contents = copy;
			return this;
		}

		@Nullable
		public ItemStack[] getContents() {
			return contents;
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof BlockStateMeta))
				return super.apply(meta);
			BlockStateMeta iMeta = (BlockStateMeta) meta;
			ShulkerBox shulker = (ShulkerBox) iMeta.getBlockState();
			if (name != null)
				shulker.setCustomName(name);
			if (contents != null)
				shulker.getInventory().setContents(contents);
			iMeta.setBlockState(shulker);
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (name != null)
				hash = hash * 33 + name.hashCode();
			if (contents != null)
				hash = hash * 33 + contents.hashCode();
			return hash;
		}
	}

	public static class BundleItemMaker extends ItemMaker {
		private List<ItemStack> contents;

		protected BundleItemMaker() {
			super(Material.getMaterial("BUNDLE"));
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (contents != null) {
				List<Map<String, Object>> serialized = new ArrayList<>(contents.size());
				for (ItemStack content : contents)
					serialized.add(ItemMaker.of(content).serializeToMap());
				map.put("bundle.contents", serialized);
			}
			return map;
		}

		public BundleItemMaker contents(ItemStack... contents) {
			return this.contents(Arrays.asList(contents));
		}

		public BundleItemMaker contents(List<ItemStack> contents) {
			List<ItemStack> items = new ArrayList<>(contents.size());
			for (ItemStack stack : contents)
				if (stack != null && stack.getType() != Material.AIR)
					items.add(stack);
			this.contents = items;
			return this;
		}

		@Nullable
		public List<ItemStack> getContents() {
			return contents;
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof BundleMeta))
				return super.apply(meta);
			BundleMeta iMeta = (BundleMeta) meta;
			if (contents != null)
				iMeta.setItems(contents);
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (contents != null)
				hash = hash * 33 + contents.hashCode();
			return hash;
		}
	}

	public static class BannerItemMaker extends ItemMaker {
		private List<Pattern> patterns;

		protected BannerItemMaker(XMaterial xMaterial) {
			super(xMaterial.parseMaterial());
			super.data = xMaterial.getData();
		}

		@Override
		public Map<String, Object> serializeToMap() {
			Map<String, Object> map = super.serializeToMap();
			if (patterns != null) {
				List<String> serialized = new ArrayList<>(patterns.size());
				for (Pattern pattern : patterns)
					serialized.add(pattern.getColor().name() + ":" + pattern.getPattern().name());
				map.put("banner.patterns", serialized);
			}
			return map;
		}

		public BannerItemMaker patterns(Pattern... contents) {
			return this.patterns(Arrays.asList(contents));
		}

		public BannerItemMaker patterns(List<Pattern> patterns) {
			this.patterns = patterns;
			return this;
		}

		@Nullable
		public List<Pattern> getPatterns() {
			return patterns;
		}

		@Override
		protected ItemMeta apply(ItemMeta meta) {
			if (!(meta instanceof BannerMeta))
				return super.apply(meta);
			BannerMeta iMeta = (BannerMeta) meta;
			if (patterns != null)
				iMeta.setPatterns(patterns);
			return super.apply(iMeta);
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (patterns != null)
				hash = hash * 33 + patterns.hashCode();
			return hash;
		}
	}

	public static ItemMaker of(XMaterial material) {
		switch (material) {
		case WRITABLE_BOOK:
			return ofWritableBook();
		case WRITTEN_BOOK:
			return ofBook();
		case LEATHER_HELMET:
		case LEATHER_CHESTPLATE:
		case LEATHER_LEGGINGS:
		case LEATHER_BOOTS:
			return ofLeatherArmor(material.parseMaterial());
		case ENCHANTED_BOOK:
			return ofEnchantedBook();
		case POTION:
		case LINGERING_POTION:
		case SPLASH_POTION:
			return ofPotion(Potion.fromType(material));
		case BLACK_SHULKER_BOX:
		case BLUE_SHULKER_BOX:
		case BROWN_SHULKER_BOX:
		case CYAN_SHULKER_BOX:
		case GRAY_SHULKER_BOX:
		case GREEN_SHULKER_BOX:
		case LIGHT_BLUE_SHULKER_BOX:
		case LIGHT_GRAY_SHULKER_BOX:
		case ORANGE_SHULKER_BOX:
		case LIME_SHULKER_BOX:
		case MAGENTA_SHULKER_BOX:
		case PINK_SHULKER_BOX:
		case PURPLE_SHULKER_BOX:
		case RED_SHULKER_BOX:
		case WHITE_SHULKER_BOX:
		case YELLOW_SHULKER_BOX:
		case SHULKER_BOX:
			return ofShulkerBox(ShulkerBoxColor.fromType(material));
		case BUNDLE:
			return ofBundle();
		case PLAYER_HEAD:
			return ofHead();
		default:
			break;
		}
		if (material.getId() == 425 || material.getId() == 177)
			return ofBanner(BannerColor.fromType(material));
		return new ItemMaker(material.parseMaterial()).data(material.getData());
	}

	public static ItemMaker of(Material bukkitMaterial) {
		try {
			XMaterial material = XMaterial.matchXMaterial(bukkitMaterial);
			if (material != null) {
				switch (material) {
				case WRITABLE_BOOK:
					return ofWritableBook();
				case WRITTEN_BOOK:
					return ofBook();
				case LEATHER_HELMET:
				case LEATHER_CHESTPLATE:
				case LEATHER_LEGGINGS:
				case LEATHER_BOOTS:
					return ofLeatherArmor(material.parseMaterial());
				case ENCHANTED_BOOK:
					return ofEnchantedBook();
				case POTION:
				case LINGERING_POTION:
				case SPLASH_POTION:
					return ofPotion(Potion.fromType(material));
				case BLACK_SHULKER_BOX:
				case BLUE_SHULKER_BOX:
				case BROWN_SHULKER_BOX:
				case CYAN_SHULKER_BOX:
				case GRAY_SHULKER_BOX:
				case GREEN_SHULKER_BOX:
				case LIGHT_BLUE_SHULKER_BOX:
				case LIGHT_GRAY_SHULKER_BOX:
				case ORANGE_SHULKER_BOX:
				case LIME_SHULKER_BOX:
				case MAGENTA_SHULKER_BOX:
				case PINK_SHULKER_BOX:
				case PURPLE_SHULKER_BOX:
				case RED_SHULKER_BOX:
				case WHITE_SHULKER_BOX:
				case YELLOW_SHULKER_BOX:
				case SHULKER_BOX:
					return ofShulkerBox(ShulkerBoxColor.fromType(material));
				case BUNDLE:
					return ofBundle();
				case PLAYER_HEAD:
					return ofHead();
				default:
					break;
				}
				if (material.getId() == 425 || material.getId() == 177)
					return ofBanner(BannerColor.fromType(material));
				return new ItemMaker(material.parseMaterial()).data(material.getData());
			}
		} catch (IllegalArgumentException error) { // Modded item or null
		}
		return new ItemMaker(bukkitMaterial);
	}

	public static HeadItemMaker ofHead() {
		return new HeadItemMaker();
	}

	public static LeatherItemMaker ofLeatherArmor(Material material) {
		return new LeatherItemMaker(material);
	}

	public static BookItemMaker ofBook() {
		return new BookItemMaker(true);
	}

	public static BookItemMaker ofWritableBook() {
		return new BookItemMaker(false);
	}

	public static EnchantedBookItemMaker ofEnchantedBook() {
		return new EnchantedBookItemMaker();
	}

	public static enum Potion {
		LINGERING(Material.getMaterial("LINGERING_POTION")), SPLASH(Material.getMaterial("SPLASH_POTION")), POTION(Material.POTION);

		private Material m;

		Potion(Material mat) {
			m = mat;
		}

		public static Potion fromType(XMaterial material) {
			switch (material) {
			case POTION:
				return POTION;
			case LINGERING_POTION:
				return LINGERING;
			case SPLASH_POTION:
				return SPLASH;
			default:
				break;
			}
			return POTION;
		}

		public Material toMaterial() {
			return m;
		}
	}

	public static PotionItemMaker ofPotion(Potion potionType) {
		return new PotionItemMaker(potionType.toMaterial());
	}

	public static enum ShulkerBoxColor {
		NONE(XMaterial.SHULKER_BOX), WHITE(XMaterial.WHITE_SHULKER_BOX), BLACK(XMaterial.BLACK_SHULKER_BOX), BLUE(XMaterial.BLUE_SHULKER_BOX), BROWN(XMaterial.BROWN_SHULKER_BOX),
		CYAN(XMaterial.CYAN_SHULKER_BOX), GRAY(XMaterial.GRAY_SHULKER_BOX), GREEN(XMaterial.GREEN_SHULKER_BOX), LIGHT_BLUE(XMaterial.LIGHT_BLUE_SHULKER_BOX),
		LIGHT_GRAY(XMaterial.LIGHT_GRAY_SHULKER_BOX), LIME(XMaterial.LIME_SHULKER_BOX), MAGENTA(XMaterial.MAGENTA_SHULKER_BOX), ORANGE(XMaterial.ORANGE_SHULKER_BOX),
		YELLOW(XMaterial.YELLOW_SHULKER_BOX), RED(XMaterial.RED_SHULKER_BOX), PURPLE(XMaterial.PURPLE_SHULKER_BOX), PINK(XMaterial.PINK_SHULKER_BOX);

		private XMaterial m;

		ShulkerBoxColor(XMaterial mat) {
			m = mat;
		}

		public static ShulkerBoxColor fromType(XMaterial material) {
			switch (material) {
			case BLACK_SHULKER_BOX:
				return BLACK;
			case BLUE_SHULKER_BOX:
				return BLUE;
			case BROWN_SHULKER_BOX:
				return BROWN;
			case CYAN_SHULKER_BOX:
				return CYAN;
			case GRAY_SHULKER_BOX:
				return GRAY;
			case GREEN_SHULKER_BOX:
				return GREEN;
			case LIGHT_BLUE_SHULKER_BOX:
				return LIGHT_BLUE;
			case LIGHT_GRAY_SHULKER_BOX:
				return LIGHT_GRAY;
			case ORANGE_SHULKER_BOX:
				return ORANGE;
			case LIME_SHULKER_BOX:
				return LIME;
			case MAGENTA_SHULKER_BOX:
				return MAGENTA;
			case PINK_SHULKER_BOX:
				return PINK;
			case PURPLE_SHULKER_BOX:
				return PURPLE;
			case RED_SHULKER_BOX:
				return RED;
			case WHITE_SHULKER_BOX:
				return WHITE;
			case YELLOW_SHULKER_BOX:
				return YELLOW;
			default:
				break;
			}
			return NONE;
		}

		public XMaterial toMaterial() {
			return m;
		}
	}

	public static ShulkerBoxItemMaker ofShulkerBox(ShulkerBoxColor color) {
		return new ShulkerBoxItemMaker(color.toMaterial());
	}

	public static BundleItemMaker ofBundle() {
		return new BundleItemMaker();
	}

	public static enum BannerColor {
		NONE(XMaterial.WHITE_BANNER), WHITE(XMaterial.WHITE_BANNER), BLACK(XMaterial.BLACK_BANNER), BLUE(XMaterial.BLUE_BANNER), BROWN(XMaterial.BROWN_BANNER), CYAN(XMaterial.CYAN_BANNER),
		GRAY(XMaterial.GRAY_BANNER), GREEN(XMaterial.GREEN_BANNER), LIGHT_BLUE(XMaterial.LIGHT_BLUE_BANNER), LIGHT_GRAY(XMaterial.LIGHT_GRAY_BANNER), LIME(XMaterial.LIME_BANNER),
		MAGENTA(XMaterial.MAGENTA_BANNER), ORANGE(XMaterial.ORANGE_BANNER), YELLOW(XMaterial.YELLOW_BANNER), RED(XMaterial.RED_BANNER), PURPLE(XMaterial.PURPLE_BANNER), PINK(XMaterial.PINK_BANNER);

		private XMaterial m;

		BannerColor(XMaterial mat) {
			m = mat;
		}

		public static BannerColor fromType(XMaterial material) {
			switch (material) {
			case BLACK_BANNER:
			case BLACK_WALL_BANNER:
				return BLACK;
			case BLUE_BANNER:
			case BLUE_WALL_BANNER:
				return BLUE;
			case BROWN_BANNER:
			case BROWN_WALL_BANNER:
				return BROWN;
			case CYAN_BANNER:
			case CYAN_WALL_BANNER:
				return CYAN;
			case GRAY_BANNER:
			case GRAY_WALL_BANNER:
				return GRAY;
			case GREEN_BANNER:
			case GREEN_WALL_BANNER:
				return GREEN;
			case LIGHT_BLUE_BANNER:
			case LIGHT_BLUE_WALL_BANNER:
				return LIGHT_BLUE;
			case LIGHT_GRAY_BANNER:
			case LIGHT_GRAY_WALL_BANNER:
				return LIGHT_GRAY;
			case ORANGE_BANNER:
			case ORANGE_WALL_BANNER:
				return ORANGE;
			case LIME_BANNER:
			case LIME_WALL_BANNER:
				return LIME;
			case MAGENTA_BANNER:
			case MAGENTA_WALL_BANNER:
				return MAGENTA;
			case PINK_BANNER:
			case PINK_WALL_BANNER:
				return PINK;
			case PURPLE_BANNER:
			case PURPLE_WALL_BANNER:
				return PURPLE;
			case RED_BANNER:
			case RED_WALL_BANNER:
				return RED;
			case WHITE_BANNER:
			case WHITE_WALL_BANNER:
				return WHITE;
			case YELLOW_BANNER:
			case YELLOW_WALL_BANNER:
				return YELLOW;
			default:
				break;
			}
			return NONE;
		}

		public XMaterial toMaterial() {
			return m;
		}
	}

	public static BannerItemMaker ofBanner(BannerColor color) {
		return new BannerItemMaker(color.toMaterial());
	}

	public static void saveToConfig(Config config, String path, ItemStack stack) {
		if (stack == null)
			return; // invalid item
		config.remove(path); // clear section
		if (!path.isEmpty() && path.charAt(path.length() - 1) != '.')
			path = path + '.';

		XMaterial type;
		try {
			type = XMaterial.matchXMaterial(stack);
			if (type == null) {
				type = XMaterial.STONE;
				config.set(path + "type", stack.getType().name()); // Modded item
			} else
				config.set(path + "type", type.name());
		} catch (IllegalArgumentException error) {
			type = XMaterial.STONE;
			config.set(path + "type", stack.getType().name()); // Modded item
		}
		config.set(path + "amount", stack.getAmount());
		if (stack.hasItemMeta()) {
			ItemMeta meta = stack.getItemMeta();
			if (meta instanceof Damageable && ((Damageable) meta).getDamage() > 0)
				config.set(path + "damage", ((Damageable) meta).getDamage());
			if (meta.getDisplayName() != null)
				config.set(path + "displayName", meta.getDisplayName());
			if (meta.getLore() != null && !meta.getLore().isEmpty())
				config.set(path + "lore", meta.getLore());

			if (Ref.isNewerThan(10)) { // 1.11+
				if (meta.isUnbreakable())
					config.set(path + "unbreakable", true);
			} else if ((boolean) Ref.invoke(Ref.invoke(meta, "spigot"), "isUnbreakable"))
				try {
					config.set(path + "unbreakable", true);
				} catch (NoSuchFieldError | Exception e2) {
					// unsupported
				}
			if (Ref.isNewerThan(7)) { // 1.8+
				List<String> flags = new ArrayList<>();
				for (ItemFlag flag : meta.getItemFlags())
					flags.add(flag.name());
				if (!flags.isEmpty())
					config.set(path + "itemFlags", flags);
			}
			if (Ref.isNewerThan(13)) { // 1.14+
				int modelData = meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
				if (modelData != 0)
					config.set(path + "modelData", modelData);
			}
			if (type == XMaterial.BUNDLE) {
				BundleMeta iMeta = (BundleMeta) meta;
				List<String> contents = new ArrayList<>();
				for (ItemStack itemStack : iMeta.getItems())
					if (itemStack != null && itemStack.getType() != Material.AIR)
						contents.add(Json.writer().simpleWrite(ItemMaker.of(itemStack).serializeToMap()));
				if (!contents.isEmpty())
					config.set(path + "bundle.contents", contents);
			} else if (type.name().contains("SHULKER_BOX")) {
				BlockStateMeta iMeta = (BlockStateMeta) meta;
				ShulkerBox shulker = (ShulkerBox) iMeta.getBlockState();
				if (shulker.getCustomName() != null)
					config.set(path + "shulker.name", shulker.getCustomName());
				List<String> contents = new ArrayList<>();
				for (ItemStack itemStack : shulker.getInventory().getContents())
					contents.add(itemStack == null || itemStack.getType() == Material.AIR ? null : Json.writer().simpleWrite(ItemMaker.of(itemStack).serializeToMap()));
				if (!contents.isEmpty())
					config.set(path + "shulker.contents", contents);
			} else if (type.name().endsWith("_BANNER") && meta instanceof BannerMeta) {
				BannerMeta banner = (BannerMeta) meta;
				List<String> patterns = new ArrayList<>();
				for (Pattern pattern : banner.getPatterns())
					patterns.add(pattern.getColor().name() + ":" + pattern.getPattern().name());
				if (!patterns.isEmpty())
					config.set(path + "banner.patterns", patterns);
			} else if (type.name().startsWith("LEATHER_") && meta instanceof LeatherArmorMeta) {
				LeatherArmorMeta armor = (LeatherArmorMeta) meta;
				String hex = Integer.toHexString(armor.getColor().asRGB());
				config.set(path + "leather.color", "#" + (hex.length() > 6 ? hex.substring(2) : hex));
			} else if (type == XMaterial.PLAYER_HEAD && meta instanceof SkullMeta) {
				SkullMeta skull = (SkullMeta) meta;
				if (Ref.isNewerThan(16) && Ref.serverType() == ServerType.PAPER) {
					com.destroystokyo.paper.profile.PlayerProfile profile = skull.getPlayerProfile();
					for (ProfileProperty property : profile.getProperties())
						if (property.getName().equals("textures")) {
							config.set(path + "head.owner", property.getValue());
							config.set(path + "head.type", "VALUES");
							break;
						}
				} else if (Ref.isNewerThan(17)) {
					PlayerProfile profile = skull.getOwnerProfile();
					@SuppressWarnings("unchecked")
					Multimap<String, Object> props = (Multimap<String, Object>) Ref.get(profile, ItemMaker.properties);
					Collection<Object> coll = props.get("textures");
					String value = coll.isEmpty() ? null : (String) Ref.get(coll.iterator().next(), ItemMaker.value);
					if (value != null) {
						config.set(path + "head.owner", value);
						config.set(path + "head.type", "VALUES");
					}
				} else if (skull.getOwner() != null && !skull.getOwner().isEmpty()) {
					config.set(path + "head.owner", skull.getOwner());
					config.set(path + "head.type", "PLAYER");
				} else {
					Object profile = Ref.get(skull, HeadItemMaker.profileField);
					if (profile != null) {
						PropertyHandler properties = BukkitLoader.getNmsProvider().fromGameProfile(profile).getProperties().get("textures");
						String value = properties == null ? null : properties.getValues();
						if (value != null) {
							config.set(path + "head.owner", value);
							config.set(path + "head.type", "VALUES");
						}
					}
				}
			} else if (type.name().contains("POTION") && meta instanceof PotionMeta) {
				PotionMeta potion = (PotionMeta) meta;
				if (Ref.isNewerThan(9))
					config.set(path + "potion.type", potion.getBasePotionData().getType().name());
				List<String> effects = new ArrayList<>();
				for (PotionEffect effect : potion.getCustomEffects())
					effects.add(effect.getType().getName() + ":" + effect.getDuration() + ":" + effect.getAmplifier() + ":" + effect.isAmbient() + ":" + effect.hasParticles());
				if (!effects.isEmpty())
					config.set(path + "potion.effects", effects);
				if (Ref.isNewerThan(10) && potion.getColor() != null) {
					String hex = Integer.toHexString(potion.getColor().asRGB());
					config.set(path + "potion.color", "#" + (hex.length() > 6 ? hex.substring(2) : hex));
				}
			}
			List<String> enchants = new ArrayList<>();
			if (type == XMaterial.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta book = (EnchantmentStorageMeta) meta;
				for (Entry<Enchantment, Integer> enchant : book.getStoredEnchants().entrySet())
					enchants.add(enchant.getKey().getName() + ":" + enchant.getValue().toString());
			} else
				for (Entry<Enchantment, Integer> enchant : meta.getEnchants().entrySet())
					enchants.add(enchant.getKey().getName() + ":" + enchant.getValue().toString());
			if (!enchants.isEmpty())
				config.set(path + "enchants", enchants);
			if ((type == XMaterial.WRITTEN_BOOK || type == XMaterial.WRITABLE_BOOK) && meta instanceof BookMeta) {
				BookMeta book = (BookMeta) meta;
				config.set(path + "book.author", book.getAuthor());
				if (Ref.isNewerThan(9)) // 1.10+
					config.set(path + "book.generation", book.getGeneration() == null ? null : book.getGeneration().name());
				config.set(path + "book.title", book.getTitle());
				if (!book.getPages().isEmpty())
					config.set(path + "book.pages", book.getPages());
			}

			NBTEdit nbtEdit = new NBTEdit(stack);
			// remove unused tags
			nbtEdit.remove("id");
			nbtEdit.remove("Count");
			nbtEdit.remove("lvl");
			nbtEdit.remove("display");
			nbtEdit.remove("Name");
			nbtEdit.remove("Lore");
			nbtEdit.remove("Damage");
			nbtEdit.remove("color");
			nbtEdit.remove("Unbreakable");
			nbtEdit.remove("HideFlags");
			nbtEdit.remove("Enchantments");
			nbtEdit.remove("CustomModelData");
			nbtEdit.remove("ench");
			nbtEdit.remove("SkullOwner");
			nbtEdit.remove("BlockEntityTag");
			// book
			nbtEdit.remove("author");
			nbtEdit.remove("title");
			nbtEdit.remove("filtered_title");
			nbtEdit.remove("pages");
			nbtEdit.remove("resolved");
			nbtEdit.remove("generation");
			// banner
			nbtEdit.remove("base-color");
			nbtEdit.remove("patterns");
			nbtEdit.remove("pattern");
			// banner
			nbtEdit.remove("base-color");
			nbtEdit.remove("patterns");
			nbtEdit.remove("pattern");
			if (!nbtEdit.getKeys().isEmpty())
				config.set(path + "nbt", nbtEdit.getNBT() + ""); // save clear nbt
		}
	}

	@Nullable // Nullable if section is empty / type is invalid
	public static ItemStack loadFromConfig(Config config, String path) {
		return loadFromConfig(config, path, true);
	}

	@Nullable // Nullable if section is empty / type is invalid
	public static ItemStack loadFromConfig(Config config, String path, boolean colorize) {
		ItemMaker maker = loadMakerFromConfig(config, path, colorize);
		return maker == null ? null : maker.build();
	}

	@Nullable // Nullable if map is empty / type is invalid
	public static ItemStack loadFromJson(Map<String, Object> serializedItem) {
		return loadFromJson(serializedItem, true);
	}

	@Nullable // Nullable if map is empty / type is invalid
	public static ItemStack loadFromJson(Map<String, Object> serializedItem, boolean colorize) {
		ItemMaker maker = loadMakerFromJson(serializedItem, colorize);
		return maker == null ? null : maker.build();
	}

	@Nullable // Nullable if section is empty / type is invalid
	public static ItemMaker loadMakerFromJson(Map<String, Object> serializedItem) {
		return loadMakerFromJson(serializedItem, true);
	}

	@SuppressWarnings("unchecked")
	@Nullable // Nullable if section is empty / type is invalid
	public static ItemMaker loadMakerFromJson(Map<String, Object> serializedItem, boolean colorize) {
		if (serializedItem.isEmpty() || !serializedItem.containsKey("type"))
			return null;
		String materialTypeName = serializedItem.get("type").toString();
		XMaterial type = XMaterial.matchXMaterial(materialTypeName.toUpperCase()).orElse(XMaterial.STONE);
		if (!type.isSupported())
			return null;
		ItemMaker maker;
		if (type == XMaterial.BUNDLE) {
			maker = ItemMaker.ofBundle();
			List<ItemStack> contents = new ArrayList<>();
			List<Map<String, Object>> serializedContents = (List<Map<String, Object>>) serializedItem.get("bundle.contents");
			for (Map<String, Object> pattern : serializedContents) {
				if (pattern == null)
					continue;
				ItemMaker itemMaker = ItemMaker.loadMakerFromJson(pattern, colorize);
				if (itemMaker != null && itemMaker.getMaterial() != Material.AIR)
					contents.add(itemMaker.build());
			}
			((BundleItemMaker) maker).contents(contents);
		} else if (type.name().contains("SHULKER_BOX")) {
			maker = ItemMaker.ofShulkerBox(ShulkerBoxColor.fromType(type));
			String name = (String) serializedItem.get("shulker.name");
			if (name != null)
				((ShulkerBoxItemMaker) maker).name(name);
			List<ItemStack> contents = new ArrayList<>(27);
			List<Map<String, Object>> serializedContents = (List<Map<String, Object>>) serializedItem.get("shulker.contents");
			for (Map<String, Object> pattern : serializedContents)
				if (pattern == null)
					contents.add(null);
				else {
					ItemMaker itemMaker = ItemMaker.loadMakerFromJson(pattern, colorize);
					contents.add(itemMaker == null ? null : itemMaker.build());
				}
			((ShulkerBoxItemMaker) maker).contents(contents.toArray(new ItemStack[27]));
		} else if (type.name().contains("BANNER")) {
			maker = ItemMaker.ofBanner(BannerColor.valueOf(type.name().substring(0, type.name().indexOf('_'))));
			// Example: RED:STRIPE_TOP
			List<Pattern> patterns = new ArrayList<>();
			if (serializedItem.containsKey("banner.patterns"))
				for (String pattern : (List<String>) serializedItem.get("banner.patterns")) {
					String[] split = pattern.split(":");
					patterns.add(new Pattern(DyeColor.valueOf(split[0].toUpperCase()), PatternType.valueOf(split[1].toUpperCase())));
				}
			((BannerItemMaker) maker).patterns(patterns);
		} else if (type.name().contains("LEATHER_") && serializedItem.containsKey("leather.color"))
			maker = ItemMaker.ofLeatherArmor(type.parseMaterial()).color(Color.fromRGB(Integer.decode(serializedItem.get("leather.color").toString())));
		else if (type == XMaterial.PLAYER_HEAD) {
			maker = ItemMaker.ofHead();
			String headOwner = (String) serializedItem.get("head.owner");
			if (headOwner != null) {
				/*
				 * PLAYER VALUES URL
				 */
				String headType = serializedItem.getOrDefault("head.type", "PLAYER").toString().toUpperCase();
				if (headType.equalsIgnoreCase("PLAYER") || headType.equalsIgnoreCase("PLAYER_NAME") || headType.equalsIgnoreCase("NAME"))
					((HeadItemMaker) maker).skinName(headOwner);
				else if (headType.equalsIgnoreCase("VALUES") || headType.equalsIgnoreCase("VALUE") || headType.equalsIgnoreCase("URL")) {
					if (headType.equalsIgnoreCase("URL"))
						headOwner = ItemMaker.fromUrl(headOwner);
					((HeadItemMaker) maker).skinValues(headOwner);
				} else if (headType.equalsIgnoreCase("HDB")) {
					if (hdbApi != null)
						headOwner = getBase64OfId(headOwner);
					((HeadItemMaker) maker).skinValues(headOwner);
				}
			}
		} else if (type == XMaterial.POTION || type == XMaterial.LINGERING_POTION || type == XMaterial.SPLASH_POTION) {
			maker = ItemMaker.ofPotion(type == XMaterial.POTION ? Potion.POTION : type == XMaterial.LINGERING_POTION ? Potion.LINGERING : Potion.SPLASH);
			List<PotionEffect> effects = new ArrayList<>();
			if (serializedItem.containsKey("potion.effects"))
				for (String pattern : (List<String>) serializedItem.get("potion.effects")) {
					String[] split = pattern.split(":");
					// PotionEffectType type, int duration, int amplifier, boolean ambient, boolean
					// particles
					effects.add(new PotionEffect(PotionEffectType.getByName(split[0].toUpperCase()), ParseUtils.getInt(split[1]), ParseUtils.getInt(split[2]),
							split.length >= 4 ? ParseUtils.getBoolean(split[3]) : true, split.length >= 5 ? ParseUtils.getBoolean(split[4]) : true));
				}
			((PotionItemMaker) maker).potionEffects(effects);
			if (serializedItem.containsKey("potion.color")) // 1.11+
				((PotionItemMaker) maker).color(Color.fromRGB(Integer.decode(serializedItem.get("potion.color").toString())));
		} else if (type == XMaterial.ENCHANTED_BOOK)
			maker = ItemMaker.ofEnchantedBook();
		else if (type == XMaterial.WRITTEN_BOOK || type == XMaterial.WRITABLE_BOOK) {
			maker = type == XMaterial.WRITTEN_BOOK ? ItemMaker.ofBook() : ItemMaker.ofWritableBook();
			if (serializedItem.containsKey("book.author"))
				if (colorize)
					((BookItemMaker) maker).author(serializedItem.get("book.author").toString());
				else
					((BookItemMaker) maker).rawAuthor(serializedItem.get("book.author").toString());
			if (serializedItem.containsKey("book.generation")) // 1.10+
				((BookItemMaker) maker).generation(serializedItem.get("book.generation").toString().toUpperCase());
			if (serializedItem.containsKey("book.title"))
				if (colorize)
					((BookItemMaker) maker).title(serializedItem.get("book.title").toString());
				else
					((BookItemMaker) maker).rawTitle(serializedItem.get("book.title").toString());

			if (serializedItem.containsKey("book.pages")) {
				List<String> inJson = (List<String>) serializedItem.get("book.pages");
				List<Component> components = new ArrayList<>(inJson.size());
				for (String json : inJson)
					components.add(ComponentAPI.fromJson(json));
				((BookItemMaker) maker).pagesComp(components);
			}
		} else {
			Material bukkitType; // Modded server support
			maker = type == XMaterial.STONE && !materialTypeName.equals("STONE") && (bukkitType = Material.getMaterial(materialTypeName)) != null ? ItemMaker.of(bukkitType) : ItemMaker.of(type);
		}

		String nbt = (String) serializedItem.get("nbt"); // additional nbt
		if (nbt != null)
			maker.nbt(new NBTEdit(nbt));

		if (serializedItem.containsKey("amount"))
			maker.amount(((Number) serializedItem.getOrDefault("amount", 1)).intValue());
		if (serializedItem.containsKey("durability")) {
			short damage = ((Number) serializedItem.get("durability")).shortValue();
			if (damage != 0)
				maker.damage(damage);
		}

		String displayName = (String) serializedItem.get("displayName");
		if (displayName != null)
			if (colorize)
				maker.displayName(displayName);
			else
				maker.rawDisplayName(displayName);
		if (serializedItem.containsKey("lore")) {
			List<String> lore = (List<String>) serializedItem.get("lore");
			if (!lore.isEmpty())
				if (colorize)
					maker.lore(lore);
				else
					maker.rawLore(lore);
		}
		if (serializedItem.containsKey("unbreakable") && (boolean) serializedItem.get("unbreakable"))
			maker.unbreakable(true);
		if (Ref.isNewerThan(7)) // 1.8+
			if (serializedItem.containsKey("itemFlags"))
				maker.itemFlags((List<String>) serializedItem.get("itemFlags"));
		if (serializedItem.containsKey("customModel")) {
			int modelData = ((Number) serializedItem.get("customModel")).intValue();
			maker.customModel(modelData);
		}

		if (serializedItem.containsKey("enchants"))
			for (Entry<String, Object> enchant : ((Map<String, Object>) serializedItem.get("enchants")).entrySet())
				maker.enchant(EnchantmentAPI.byName(enchant.getKey().toUpperCase()).getEnchantment(), ((Number) enchant.getValue()).intValue());
		return maker;
	}

	@Nullable // Nullable if section is empty / type is invalid
	public static ItemMaker loadMakerFromConfig(Config config, String path) {
		return loadMakerFromConfig(config, path, true);
	}

	@Nullable // Nullable if section is empty / type is invalid
	public static ItemMaker loadMakerFromConfig(Config config, String path, boolean colorize) {
		if (!path.isEmpty() && path.charAt(path.length() - 1) != '.')
			path = path + '.';
		if (config.getString(path + "type", config.getString(path + "icon")) == null)
			return null; // missing type

		String materialTypeName = config.getString(path + "type", config.getString(path + "icon"));
		XMaterial type = XMaterial.matchXMaterial(materialTypeName.toUpperCase()).orElse(XMaterial.STONE);
		if (!type.isSupported())
			return null;
		ItemMaker maker;
		if (type == XMaterial.BUNDLE) {
			maker = ItemMaker.ofBundle();
			List<ItemStack> contents = new ArrayList<>();
			List<Map<String, Object>> serializedContents = config.getMapList(path + "bundle.contents");
			for (Map<String, Object> pattern : serializedContents) {
				if (pattern == null)
					continue;
				ItemMaker itemMaker = ItemMaker.loadMakerFromJson(pattern, colorize);
				if (itemMaker != null && itemMaker.getMaterial() != Material.AIR)
					contents.add(itemMaker.build());
			}
			((BundleItemMaker) maker).contents(contents);
		} else if (type.name().contains("SHULKER_BOX")) {
			maker = ItemMaker.ofShulkerBox(ShulkerBoxColor.fromType(type));
			String name = config.getString(path + "shulker.name");
			if (name != null)
				((ShulkerBoxItemMaker) maker).name(name);
			List<ItemStack> contents = new ArrayList<>(27);
			List<Map<String, Object>> serializedContents = config.getMapList(path + "shulker.contents");
			for (Map<String, Object> pattern : serializedContents)
				if (pattern == null)
					contents.add(null);
				else {
					ItemMaker itemMaker = ItemMaker.loadMakerFromJson(pattern, colorize);
					contents.add(itemMaker == null ? null : itemMaker.build());
				}
			((ShulkerBoxItemMaker) maker).contents(contents.toArray(new ItemStack[27]));
		} else if (type.name().contains("BANNER")) {
			maker = ItemMaker.ofBanner(BannerColor.valueOf(type.name().substring(0, type.name().indexOf('_'))));
			// Example: RED:STRIPE_TOP
			List<Pattern> patterns = new ArrayList<>();
			for (String pattern : config.getStringList(path + "banner.patterns")) {
				String[] split = pattern.split(":");
				patterns.add(new Pattern(DyeColor.valueOf(split[0].toUpperCase()), PatternType.valueOf(split[1].toUpperCase())));
			}
			((BannerItemMaker) maker).patterns(patterns);
		} else if (type.name().contains("LEATHER_") && config.getString(path + "leather.color") != null)
			maker = ItemMaker.ofLeatherArmor(type.parseMaterial()).color(Color.fromRGB(Integer.decode(config.getString(path + "leather.color"))));
		else if (type == XMaterial.PLAYER_HEAD) {
			maker = ItemMaker.ofHead();
			String headOwner = config.getString(path + "head.owner");
			if (headOwner != null) {
				/*
				 * PLAYER VALUES URL
				 */
				String headType = config.getString(path + "head.type", "PLAYER").toUpperCase();
				if (headType.equalsIgnoreCase("PLAYER") || headType.equalsIgnoreCase("PLAYER_NAME") || headType.equalsIgnoreCase("NAME"))
					((HeadItemMaker) maker).skinName(headOwner);
				else if (headType.equalsIgnoreCase("VALUES") || headType.equalsIgnoreCase("VALUE") || headType.equalsIgnoreCase("URL")) {
					if (headType.equalsIgnoreCase("URL"))
						headOwner = ItemMaker.fromUrl(headOwner);
					((HeadItemMaker) maker).skinValues(headOwner);
				} else if (headType.equalsIgnoreCase("HDB")) {
					if (hdbApi != null)
						headOwner = getBase64OfId(headOwner);
					((HeadItemMaker) maker).skinValues(headOwner);
				}
			}
		} else if (type == XMaterial.POTION || type == XMaterial.LINGERING_POTION || type == XMaterial.SPLASH_POTION) {
			maker = ItemMaker.ofPotion(type == XMaterial.POTION ? Potion.POTION : type == XMaterial.LINGERING_POTION ? Potion.LINGERING : Potion.SPLASH);
			List<PotionEffect> effects = new ArrayList<>();
			for (String pattern : config.getStringList(path + "potion.effects")) {
				String[] split = pattern.split(":");
				// PotionEffectType type, int duration, int amplifier, boolean ambient, boolean
				// particles
				effects.add(new PotionEffect(PotionEffectType.getByName(split[0].toUpperCase()), ParseUtils.getInt(split[1]), ParseUtils.getInt(split[2]),
						split.length >= 4 ? ParseUtils.getBoolean(split[3]) : true, split.length >= 5 ? ParseUtils.getBoolean(split[4]) : true));
			}
			((PotionItemMaker) maker).potionEffects(effects);
			if (config.getString(path + "potion.color") != null) // 1.11+
				((PotionItemMaker) maker).color(Color.fromRGB(Integer.decode(config.getString(path + "potion.color"))));
		} else if (type == XMaterial.ENCHANTED_BOOK)
			maker = ItemMaker.ofEnchantedBook();
		else if (type == XMaterial.WRITTEN_BOOK || type == XMaterial.WRITABLE_BOOK) {
			maker = type == XMaterial.WRITTEN_BOOK ? ItemMaker.ofBook() : ItemMaker.ofWritableBook();
			if (config.getString(path + "book.author") != null)
				if (colorize)
					((BookItemMaker) maker).author(config.getString(path + "book.author"));
				else
					((BookItemMaker) maker).rawAuthor(config.getString(path + "book.author"));
			if (config.getString(path + "book.generation") != null) // 1.10+
				((BookItemMaker) maker).generation(config.getString(path + "book.generation").toUpperCase());
			if (config.getString(path + "book.title") != null)
				if (colorize)
					((BookItemMaker) maker).title(config.getString(path + "book.title"));
				else
					((BookItemMaker) maker).rawTitle(config.getString(path + "book.title"));
			if (!config.getStringList(path + "book.pages").isEmpty())
				if (colorize)
					((BookItemMaker) maker).pages(config.getStringList(path + "book.pages"));
				else
					((BookItemMaker) maker).rawPages(config.getStringList(path + "book.pages"));
		} else {
			Material bukkitType; // Modded server support
			maker = type == XMaterial.STONE && !materialTypeName.equals("STONE") && (bukkitType = Material.getMaterial(materialTypeName)) != null ? ItemMaker.of(bukkitType) : ItemMaker.of(type);
		}

		String nbt = config.getString(path + "nbt"); // additional nbt
		if (nbt != null)
			maker.nbt(new NBTEdit(nbt));

		maker.amount(config.getInt(path + "amount", 1));
		short damage = config.getShort(path + "damage", config.getShort(path + "durability"));
		if (damage != 0)
			maker.damage(damage);

		String displayName = config.getString(path + "displayName", config.getString(path + "display-name"));
		if (displayName != null)
			if (colorize)
				maker.displayName(displayName);
			else
				maker.rawDisplayName(displayName);
		List<String> lore = config.getStringList(path + "lore");
		if (!lore.isEmpty())
			if (colorize)
				maker.lore(lore);
			else
				maker.rawLore(lore);
		if (config.getBoolean(path + "unbreakable"))
			maker.unbreakable(true);
		if (Ref.isNewerThan(7)) // 1.8+
			maker.itemFlags(config.getStringList(path + "itemFlags"));
		int modelData = config.getInt(path + "modelData");
		maker.customModel(modelData);

		for (String enchant : config.getStringList(path + "enchants")) {
			String[] split = enchant.split(":");
			maker.enchant(EnchantmentAPI.byName(split[0].toUpperCase()).getEnchantment(), split.length >= 2 ? ParseUtils.getInt(split[1]) : 1);
		}
		return maker;
	}

	private static String getBase64OfId(String headOwner) {
		if (HDB_TYPE == 1)
			return ((HeadDatabaseAPI) hdbApi).getBase64(headOwner);
		return null;
	}

	public static ItemMaker of(ItemStack stack) {
		if (stack == null)
			return null; // invalid item
		ItemMaker maker = of(stack.getType());

		if (Ref.isOlderThan(13) && stack.getData() != null)
			maker.data(stack.getData().getData());

		ItemMeta meta = stack.getItemMeta();
		maker = maker.itemMeta(meta);

		if (stack.getDurability() != 0)
			maker.damage(stack.getDurability());
		maker.amount(stack.getAmount());

		maker.nbt(new NBTEdit(stack));
		return maker;
	}

	@SuppressWarnings("unchecked")
	public static String fromUrl(String url) {
		try {
			java.net.URLConnection connection = new URL(url).openConnection();
			connection.setRequestProperty("User-Agent", "DevTec-JavaClient");
			HttpURLConnection conn = (HttpURLConnection) new URL(String.format(HeadItemMaker.URL_FORMAT, url, "name=DevTec&model=steve&visibility=1")).openConnection();
			conn.setRequestProperty("User-Agent", "DevTec-JavaClient");
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setRequestMethod("POST");
			conn.connect();
			Map<String, Object> text = (Map<String, Object>) Json.reader().simpleRead(StreamUtils.fromStream(new GZIPInputStream(conn.getInputStream())));
			return (String) ((Map<String, Object>) ((Map<String, Object>) text.get("data")).get("texture")).get("value");
		} catch (Exception err) {
		}
		return null;
	}
}
