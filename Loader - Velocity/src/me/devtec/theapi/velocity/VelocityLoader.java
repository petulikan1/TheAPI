package me.devtec.theapi.velocity;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.tools.ToolProvider;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.plugin.PluginClassLoader;

import me.devtec.shared.API;
import me.devtec.shared.Ref;
import me.devtec.shared.Ref.ServerType;
import me.devtec.shared.components.AdventureComponentAPI;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.json.modern.ModernJsonReader;
import me.devtec.shared.json.modern.ModernJsonWriter;
import me.devtec.shared.mcmetrics.GatheringInfoManager;
import me.devtec.shared.mcmetrics.Metrics;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.LibraryLoader;
import me.devtec.theapi.velocity.commands.hooker.VelocityCommandManager;
import me.devtec.theapi.velocity.commands.selectors.VelocitySelectorUtils;
import net.kyori.adventure.text.Component;

@Plugin(id = "theapi", name = "TheAPI", version = "12.8.9", authors = { "DevTec", "Straikerinos" }, url = "https://www.spigotmc.org/resources/72679/")
public class VelocityLoader {

	// Init static APIs
	static {
		VelocityLoader.initTheAPI();
	}

	private final ProxyServer server;
	private static VelocityLoader plugin;
	private final Logger logger;

	public static ProxyServer getServer() {
		return VelocityLoader.plugin.server;
	}

	@Inject
	public VelocityLoader(ProxyServer server, Logger logger) {
		VelocityLoader.plugin = this;
		this.server = server;
		this.logger = logger;
		broadcastSystemInfo();
	}

	private void broadcastSystemInfo() {
		ConsoleCommandSource console = server.getConsoleCommandSource();
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7>")));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7> &5TheAPI &dv" + VelocityLoader.class.getAnnotation(Plugin.class).version())));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7>")));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7> &5System info&7:")));
		console.sendMessage((Component) ComponentAPI.adventure()
				.fromString(ColorUtils.colorize("&7> &dJava&7: &e" + System.getProperty("java.version") + " &7(" + (ToolProvider.getSystemJavaCompiler() != null ? "&aJDK" : "&aJRE") + "&7)")));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7> &dServer type&7: &e" + Ref.serverType())));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7>")));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7> &dSupport&7: &ehttps://discord.gg/APwYKQRxby")));
		console.sendMessage((Component) ComponentAPI.adventure().fromString(ColorUtils.colorize("&7>")));
	}

	@Subscribe
	public void onProxyInitialization(ProxyShutdownEvent event) {
		API.setEnabled(false);

		// OfflineCache support!
		API.offlineCache().saveToConfig().setFile(new File("plugins/TheAPI/Cache.dat")).save("properties");
	}

	@Subscribe
	public void onPreLoginEvent(PreLoginEvent e) {
		if (e.getResult().isAllowed())
			API.offlineCache().setLookup(API.offlineCache().lookupId(e.getUsername()), e.getUsername());
	}

	@Subscribe
	public void onLoginEvent(LoginEvent e) { // fix uuid - premium login?
		if (e.getResult().isAllowed())
			API.offlineCache().setLookup(e.getPlayer().getUniqueId(), e.getPlayer().getUsername());
	}

	@Subscribe
	public void onDisconnect(DisconnectEvent e) {
		API.removeCache(e.getPlayer().getUniqueId());
	}

	public static void initTheAPI() {
		Ref.init(ServerType.VELOCITY, VelocityServer.class.getPackage().getImplementationVersion()); // Server version

		Metrics.gatheringInfoManager = new GatheringInfoManager() {

			@Override
			public Consumer<String> getInfoLogger() {
				return plugin.logger::info;
			}

			@Override
			public BiConsumer<String, Throwable> getErrorLogger() {
				return plugin.logger::warn;
			}

			@Override
			public String getServerVersionVendor() {
				return getServer().getVersion().getVendor();
			}

			@Override
			public int getManagedServers() {
				return getServer().getAllServers().size();
			}

			@Override
			public String getServerVersion() {
				return getServer().getVersion().getVersion();
			}

			@Override
			public String getServerName() {
				return getServer().getVersion().getName();
			}

			@Override
			public int getPlayers() {
				return getServer().getPlayerCount();
			}

			@Override
			public int getOnlineMode() {
				return getServer().getConfiguration().isOnlineMode() ? 1 : 0;
			}
		};

		ComponentAPI.registerTransformer("ADVENTURE", new AdventureComponentAPI<>());
		Config config = new Config("plugins/TheAPI/config.yml");
		if (!config.getString("default-json-handler", "TheAPI").equalsIgnoreCase("TheAPI"))
			Json.init(new ModernJsonReader(), new ModernJsonWriter()); // Modern version of Guava
		// Commands api
		API.commandsRegister = new VelocityCommandManager();
		API.selectorUtils = new VelocitySelectorUtils();

		// OfflineCache support!
		Path configPath = new File("velocity.toml").toPath();
		try {
			VelocityConfiguration configuration = VelocityConfiguration.read(configPath);
			API.initOfflineCache(configuration.isOnlineMode(), new Config("plugins/TheAPI/Cache.dat"));
		} catch (IOException e1) {
			API.initOfflineCache(false, new Config("plugins/TheAPI/Cache.dat"));
		}

		API.library = new LibraryLoader() {
			List<File> loaded = new ArrayList<>();

			@SuppressWarnings("resource")
			@Override
			public void load(File file) {
				if (isLoaded(file) || !file.exists())
					return;
				loaded.add(file);
				try {
					new PluginClassLoader(new URL[] { file.toURI().toURL() }).addToClassloaders();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}

			@Override
			public boolean isLoaded(File file) {
				return loaded.contains(file);
			}
		};
		API.basics().load();
	}
}
