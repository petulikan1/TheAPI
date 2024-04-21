package me.devtec.theapi.bukkit.events;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import me.devtec.shared.events.Cancellable;
import me.devtec.shared.events.Event;
import me.devtec.shared.events.ListenerHolder;
import me.devtec.theapi.bukkit.nms.GameProfileHandler;

public class ServerListPingEvent extends Event implements Cancellable {
	static List<ListenerHolder> handlers = new ArrayList<>();

	private boolean cancel;
	private int online;
	private int max;
	private int protocol;
	private List<GameProfileHandler> slots;
	private String motd;
	private String favicon;
	private String version;
	private final InetAddress address;

	public ServerListPingEvent(int online, int max, List<GameProfileHandler> slots, String motd, String favicon, InetAddress inetAddress, String ver, int protocol) {
		this.online = online;
		this.max = max;
		this.slots = slots;
		this.motd = motd;
		this.favicon = favicon;
		this.protocol = protocol;
		address = inetAddress;
		version = ver;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String ver) {
		version = ver;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getOnlinePlayers() {
		return online;
	}

	public int getMaxPlayers() {
		return max;
	}

	public void setOnlinePlayers(int online) {
		this.online = online;
	}

	public void setMaxPlayers(int max) {
		this.max = max;
	}

	public List<GameProfileHandler> getSlots() {
		return slots;
	}

	public void setPlayersText(List<GameProfileHandler> slots) {
		this.slots = slots;
	}

	public String getMotd() {
		return motd;
	}

	public void setMotd(String motd) {
		this.motd = motd;
	}

	public String getFavicon() {
		return favicon;
	}

	public void setFavicon(String falvicon) {
		favicon = falvicon;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}

	@Override
	public List<ListenerHolder> getHandlers() {
		return handlers;
	}

	public static List<ListenerHolder> getHandlerList() {
		return handlers;
	}
}
