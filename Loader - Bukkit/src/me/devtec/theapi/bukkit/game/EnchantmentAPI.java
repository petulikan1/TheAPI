package me.devtec.theapi.bukkit.game;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import me.devtec.shared.Ref;

public enum EnchantmentAPI {
	SHARPNESS("DAMAGE_ALL"), DAMAGEALL("DAMAGE_ALL"), ALLDAMAGE("DAMAGE_ALL"), DAMAGE_ALL("DAMAGE_ALL"),

	DAMAGE_ARTHROPODS("DAMAGE_ARTHROPODS"), ARTHROPODS("DAMAGE_ARTHROPODS"), BANEOFARTHROPODS("DAMAGE_ARTHROPODS"),

	EFFICIENCY("DIG_SPEED"), DIG_SPEED("DIG_SPEED"), DIGSPEED("DIG_SPEED"), SPEEDDIG("DIG_SPEED"),

	CHANNELING("CHANNELING", 13), DEPTH_STRIDER("DEPTH_STRIDER", 8),

	FROST_WALKER("FROST_WALKER", 9),

	KNOCKBACK("KNOCKBACK"),

	MULTISHOT("MULTISHOT", 14),

	PIERCING("PIERCING", 13),

	LOYALTY("LOYALTY", 13),

	RIPTIDE("RIPTIDE", 13),

	QUICK_CHARGE("QUICK_CHARGE", 14),

	THORNS("THORNS"),

	SILK_TOUCH("SILK_TOUCH"),

	SWEEPING_EDGE("SWEEPING_EDGE", 11),

	MENDING("MENDING", 9), REPEAIRING("MENDING", 9),

	ARROW_FIRE("ARROW_FIRE"), FIRE("ARROW_FIRE"),

	FIREASPECT("FIRE_ASPECT"), FIRE_ASPECT("FIRE_ASPECT"),

	INFINITY("ARROW_INFINITE", 11), ARROW_INFINITE("ARROW_INFINITE", 11),

	IMPALING("IMPALING", 13),

	LURE("LURE", 7),

	LUCK("LUCK", 7), LUCK_OF_SEA("LUCK", 7), LUCKOFSEA("LUCK", 7),

	FORTUNE("LOOT_BONUS_BLOCKS"), LOOTBLOCKS("LOOT_BONUS_BLOCKS"), LOOT_BONUS_BLOCKS("LOOT_BONUS_BLOCKS"),

	RESPIRATION("OXYGEN"), OXYGEN("OXYGEN"),

	UNBREAKING("DURABILITY"), DURABILITY("DURABILITY"),

	AQUA_AFFINITY("WATER_WORKER"), WATER_WORKER("WATER_WORKER"),

	PROTECTION("PROTECTION_ENVIRONMENTAL"), PROTECTION_ENVIRONMENTAL("PROTECTION_ENVIRONMENTAL"),

	BLAST_PROTECTION("PROTECTION_EXPLOSIONS"), PROTECTION_EXPLOSIONS("PROTECTION_EXPLOSIONS"),

	FEATHER_FALLING("PROTECTION_FALL"), PROTECTION_FALL("PROTECTION_FALL"),

	FIRE_PROTECTION("PROTECTION_FIRE"), PROTECTION_FIRE("PROTECTION_FIRE"),

	PROJECTILE_PROTECTION("PROTECTION_PROJECTILE"), PROTECTION_PROJECTILE("PROTECTION_PROJECTILE"),

	CURSE_OF_VANISHING("VANISHING_CURSE", 11), VANISHING_CURSE("VANISHING_CURSE", 11),

	CURSE_OF_BINDING("BINDING_CURSE", 11), BINDING_CURSE("BINDING_CURSE", 11),

	DAMAGE_UNDEAD("DAMAGE_UNDEAD"), SMITE("DAMAGE_UNDEAD"),

	POWER("ARROW_DAMAGE"), ARROW_DAMAGE("ARROW_DAMAGE"),

	PUNCH("ARROW_KNOCKBACK"), ARROW_KNOCKBACK("ARROW_KNOCKBACK"),

	LOOTMOBS("LOOT_BONUS_MOBS"), LOOTBONUSMOBS("LOOT_BONUS_MOBS"), LOOT_BONUS_MOBS("LOOT_BONUS_MOBS"), LOOTING("LOOT_BONUS_MOBS"), UKNOWN("UKNOWN", 0);

	private final String bukkitName;
	private final int version;
	private static Field acceptingNew = Ref.field(Enchantment.class, "acceptingNew");
	private static Field byName = Ref.field(Enchantment.class, "byName"); // All
	private static Field byKey = Ref.field(Enchantment.class, "byKey"); // 1.13+
	private static Field key = Ref.field(Enchantment.class, "key"); // 1.13+
	private static Field byId = Ref.field(Enchantment.class, "byId"); // 1.12-
	private static Field id = Ref.field(Enchantment.class, "id"); // 1.12-

	EnchantmentAPI(String bukkitName) {
		this(bukkitName, 0);
	}

	EnchantmentAPI(String bukkitName, int version) {
		this.bukkitName = bukkitName;
		this.version = version;
	}

	/**
	 * @apiNote Enchant ItemStack with this Enchantment if supported
	 */
	public void enchant(ItemStack item, int level) {
		if (isSupported())
			if (item.getType() == Material.ENCHANTED_BOOK) {
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
				meta.addStoredEnchant(getEnchantment(), level, true);
				item.setItemMeta(meta);
			} else
				item.addUnsafeEnchantment(getEnchantment(), level);
	}

	/**
	 * @apiNote Get Enchantment by bukkitName if supported
	 * @return boolean
	 */
	public Enchantment getEnchantment() {
		if (isSupported())
			return Enchantment.getByName(bukkitName);
		return null;
	}

	/**
	 * @apiNote Does server have this enchant
	 * @return boolean
	 */
	public boolean isSupported() {
		return version == 0 ? true : Ref.isNewerThan(version - 1);
	}

	/**
	 * @apiNote Return enchantment bukkit name
	 * @return String
	 */
	public String getName() {
		return bukkitName;
	}

	/**
	 * @apiNote Get EnchantmentAPI from bukkit or vanilla name
	 * @return EnchantmentAPI
	 */
	public static EnchantmentAPI byName(String name) {
		try {
			return valueOf(name.toUpperCase());
		} catch (Exception e) {
			if (Enchantment.getByName(name.toUpperCase()) != null)
				return UKNOWN;
			return null;
		}
	}

	/**
	 * @apiNote Conventor Enchantment to EnchantmentAPI
	 * @return EnchantmentAPI
	 */
	public static EnchantmentAPI fromEnchant(Enchantment enchantment) {
		return byName(enchantment.getName());
	}

	/**
	 * @apiNote Register enchantment to the bukkit
	 * @return boolean state of success
	 */
	public static boolean registerEnchantment(Enchantment enchantment) {
		boolean registered = false;
		if (Ref.isNewerThan(20) || Ref.serverVersionInt() == 20 && Ref.serverVersionRelease() >= 3) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) Ref.get(Ref.getStatic(Ref.getClass("org.bukkit.Registry"), "ENCHANTMENT"), "map");
			map.put(Ref.invoke(enchantment, "getKey"), enchantment);
			registered = true;
		} else {
			Ref.set(null, acceptingNew, true);
			try {
				Ref.invokeStatic(Ref.method(Enchantment.class, "registerEnchantment", Enchantment.class), enchantment);
				registered = true;
			} catch (Exception ea) {
			}
		}
		return registered;
	}

	/**
	 * @apiNote Unregister enchantment
	 * @return boolean state of success
	 */
	@SuppressWarnings("unchecked")
	public static boolean unregisterEnchantment(Enchantment enchantment) {
		boolean unregistered = false;
		try {
			((Map<String, Enchantment>) Ref.get(null, byName)).remove(enchantment.getName());
			if (Ref.isNewerThan(12))
				unregistered = ((Map<?, ?>) Ref.get(null, byKey)).remove(Ref.get(enchantment, key)) != null;
			else
				unregistered = ((Map<Integer, Enchantment>) Ref.get(null, byId)).remove(Ref.get(enchantment, id)) != null;
		} catch (Exception ea) {
		}
		return unregistered;
	}
}