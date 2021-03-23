package me.devtec.theapi.apis;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.block.Sign;

import me.devtec.theapi.TheAPI;
import me.devtec.theapi.blocksapi.schematic.construct.SerializedBlock;
import me.devtec.theapi.utils.Position;
import me.devtec.theapi.utils.nms.NMSAPI;
import me.devtec.theapi.utils.reflections.Ref;
import me.devtec.theapi.utils.theapiutils.LoaderClass;

public class SignAPI {
	public static enum SignAction {
		CONSOLE_COMMANDS, PLAYER_COMMANDS, BROADCAST, MESSAGES
	}

	public static void removeSign(Position loc) {
		LoaderClass.data.remove("Sign." + loc.toString());
		LoaderClass.data.save();
	}

	public static List<Position> getRegistredSigns() {
		List<Position> l = new ArrayList<>();
		boolean save = false;
		if (LoaderClass.data.exists("Sign"))
			for (String s : LoaderClass.data.getKeys("Sign")) {
				Position d = Position.fromString(s);
				if (d!=null && d.getBukkitType().name().contains("SIGN"))
					l.add(d);
				else {
					LoaderClass.data.remove("Sign." + s);
					save=true;
				}
			}
		if(save)LoaderClass.data.save();
		return l;
	}
	
	private static Class<?> sign = Ref.nms("TileEntitySign");
	
	public static void setLines(Position loc, String... lines) {
		Object state = SerializedBlock.getState(loc);
		if(state.getClass() != sign)return;
		if(TheAPI.isOlderThan(8)) {
			String[] shorted = new String[4];
			for(int i = 0; i < 4; ++i)
				if(lines.length>=i)
			shorted[i]=lines[i].length()>16?lines[i].substring(0, 15):lines[i];
			Ref.set(state, "lines", shorted);
		}else {
			Object[] parsed = (Object[]) Array.newInstance(Ref.nms("IChatBaseComponent"), 4);
			for(int i = 0; i < 4; ++i)
				if(lines.length>=i)
					parsed[i]=NMSAPI.getFixedIChatBaseComponent(lines[i]);
			Ref.set(state, "lines", parsed);
		}
		Ref.sendPacket(TheAPI.getOnlinePlayers(), Ref.invoke(state, "getUpdatePacket"));
	}
	
	public static String[] getLines(Position loc) {
		Object state = SerializedBlock.getState(loc);
		if(state.getClass() != sign)return new String[]{"","","",""};
		Object[] get = (Object[]) Ref.get(state, "lines");
		if(TheAPI.isOlderThan(8))return (String[])get;
		else {
			String[] parsed = new String[]{"","","",""};
			for(int i = 0; i < 4; ++i)
				parsed[i]=NMSAPI.fromComponent(get[i]);
			return parsed;
		}
	}

	public static void setActions(Position loc, Map<SignAction, List<String>> options) {
		String l = loc.toString();
		for (SignAction s : options.keySet()) {
			switch (s) {
			case CONSOLE_COMMANDS:
				if (options.get(s) instanceof List)
					LoaderClass.data.set("Sign." + l + ".CONSOLE_COMMANDS", options.get(s));
				break;
			case PLAYER_COMMANDS:
				if (options.get(s) instanceof List)
					LoaderClass.data.set("Sign." + l + ".PLAYER_COMMANDS", options.get(s));
				break;
			case MESSAGES:
				if (options.get(s) instanceof List)
					LoaderClass.data.set("Sign." + l + ".MESSAGES", options.get(s));
				break;
			case BROADCAST:
				if (options.get(s) instanceof List)
					LoaderClass.data.set("Sign." + l + ".BROADCAST", options.get(s));
				break;
			}
		}
		LoaderClass.data.save();
	}

	public static Map<SignAction, List<String>> getSignActions(Position loc) {
		HashMap<SignAction, List<String>> a = new HashMap<SignAction, List<String>>();
		String ff = loc.toString();
		for (String s : LoaderClass.data.getKeys("Sign." + ff))
			a.put(SignAction.valueOf(s), LoaderClass.data.getStringList("Sign." + ff + "." + s));
		return a;
	}

	//SYNC WITH SERVER ONLY
	public static Sign getSignState(Position loc) {
		Sign s = null;
		if (getRegistredSigns().contains(loc))
			s = (Sign) loc.getBlock().getState();
		return s;
	}
}
