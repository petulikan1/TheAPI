package me.devtec.theapi.bukkit.commands.hooker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.shared.Ref;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.manager.CommandsRegister;
import me.devtec.theapi.bukkit.BukkitLoader;

@SuppressWarnings("unchecked")
public class BukkitCommandManager implements CommandsRegister {
	private static Method syncCommands = Ref.isNewerThan(12) ? Ref.method(Ref.craft("CraftServer"), "syncCommands") : null;
	protected static CommandMap cmdMap;
	protected static Map<String, Command> knownCommands;

	private static Constructor<?> constructor;

	static {
		if (Bukkit.getPluginManager().getClass().getSimpleName().equals("PaperPluginManagerImpl")) // For future updates from paper side
			cmdMap = (CommandMap) Ref.get(Ref.get(Bukkit.getPluginManager(), "instanceManager"), "commandMap");
		else
			cmdMap = (CommandMap) Ref.get(Bukkit.getPluginManager(), "commandMap");
		knownCommands = (Map<String, Command>) Ref.get(cmdMap, "knownCommands");
		constructor = Ref.constructor(PluginCommand.class, String.class, Plugin.class);
	}

	public static PluginCommand createCommand(String name, Plugin plugin) {
		return (PluginCommand) Ref.newInstance(constructor, name, plugin);
	}

	public static void registerCommand(PluginCommand command) {
		String label = command.getName().toLowerCase(Locale.ENGLISH).trim();
		String sd = command.getPlugin().getName().toLowerCase(Locale.ENGLISH).trim();
		command.setLabel(sd + ":" + label);
		command.register(cmdMap);
		if (command.getTabCompleter() == null)
			if (command.getExecutor() instanceof TabCompleter)
				command.setTabCompleter((TabCompleter) command.getExecutor());
			else
				command.setTabCompleter((arg0, arg1, arg2, arg3) -> null);
		if (command.getExecutor() == null)
			if (command.getTabCompleter() instanceof CommandExecutor)
				command.setExecutor((CommandExecutor) command.getTabCompleter());
			else
				return; // exectutor can't be null
		List<String> low = new ArrayList<>();
		for (String s : command.getAliases()) {
			s = s.toLowerCase(Locale.ENGLISH).trim();
			low.add(s);
		}
		command.setAliases(low);
		if (command.getPermission() == null)
			command.setPermission("");
		if (!low.contains(label))
			low.add(label);
		for (String s : low)
			knownCommands.put(s, command);
		if (syncCommands != null)
			Ref.invoke(Bukkit.getServer(), syncCommands);
	}

	@Override
	public void register(CommandHolder<?> commandHolder, String command, String[] aliases) {
		CustomPluginCommand cmd = new CustomPluginCommand(command, JavaPlugin.getPlugin(BukkitLoader.class), commandHolder);
		cmd.setAliases(Arrays.asList(aliases));
		cmd.setExecutor((s, arg1, arg2, args) -> {
			commandHolder.execute(s, args);
			return true;
		});
		cmd.setTabCompleter((s, arg1, arg2, args) -> {
			Collection<String> tablist = commandHolder.tablist(s, args);
			if (tablist.isEmpty())
				return Collections.emptyList();
			return tablist instanceof List ? (List<String>) tablist : new ArrayList<>(tablist);
		});
		cmd.setPermission(commandHolder.getStructure().getPermission());
		commandHolder.setRegisteredCommand(cmd, command, aliases);

		String label = cmd.getName().toLowerCase(Locale.ENGLISH).trim();
		String sd = cmd.getPlugin().getName().toLowerCase(Locale.ENGLISH).trim();
		cmd.setLabel(sd + ":" + label);

		List<String> low = new ArrayList<>();
		for (String s : cmd.getAliases()) {
			s = s.toLowerCase(Locale.ENGLISH).trim();
			low.add(s);
		}
		cmd.setAliases(low);
		if (cmd.getPermission() == null)
			cmd.setPermission("");
		if (!low.contains(label))
			low.add(label);
		for (String s : low)
			knownCommands.put(s, cmd);
		if (syncCommands != null)
			Ref.invoke(Bukkit.getServer(), syncCommands);
	}

	@Override
	public void unregister(CommandHolder<?> commandHolder) {
		knownCommands.remove(commandHolder.getCommandName().toLowerCase(Locale.ENGLISH).trim());
		for (String alias : commandHolder.getCommandAliases())
			knownCommands.remove(alias.toLowerCase(Locale.ENGLISH).trim());
		if (syncCommands != null)
			Ref.invoke(Bukkit.getServer(), syncCommands);
	}
}
