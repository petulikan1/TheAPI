package me.devtec.theapi.bukkit.packetlistener;

import me.devtec.shared.annotations.ScheduledForRemoval;

public abstract class PacketListener {
	protected Priority priority;

	public PacketListener() {
		this(Priority.NORMAL);
	}

	public PacketListener(Priority priority) {
		this.priority = priority;
	}

	public final PacketListener setPriority(Priority priority) {
		if (priority == null)
			return this;
		PacketManager.notify(this, priority, priority);
		this.priority = priority;
		return this;
	}

	public final void register() {
		PacketManager.register(this);
	}

	public final void unregister() {
		PacketManager.unregister(this);
	}

	public final Priority getPriority() {
		return priority;
	}

	/**
	 * 
	 * @param player  Player name
	 * @param packet  Packet
	 * @param channel Channel
	 * @return packet cancel status
	 * 
	 *         This is outdated method which will be soon removed, please use;
	 * @see PacketListener#playOut(String, PacketContainer, ChannelContainer)
	 */
	@ScheduledForRemoval(inVersion = "12.0")
	public abstract boolean playOut(String player, Object packet, Object channel);

	/**
	 * 
	 * @param player  Player name
	 * @param packet  PacketContainer
	 * @param channel ChannelContainer
	 */
	public void playOut(String player, PacketContainer container, ChannelContainer channel) {

	}

	/**
	 * 
	 * @param player  Player name
	 * @param packet  Packet
	 * @param channel Channel
	 * @return packet cancel status
	 * 
	 *         This is outdated method which will be soon removed, please use;
	 * @see PacketListener#playIn(String, PacketContainer, ChannelContainer)
	 */
	@ScheduledForRemoval(inVersion = "12.0")
	public abstract boolean playIn(String player, Object packet, Object channel);

	/**
	 * 
	 * @param player  Player name
	 * @param packet  PacketContainer
	 * @param channel ChannelContainer
	 */
	public void playIn(String player, PacketContainer container, ChannelContainer channel) {

	}
}
