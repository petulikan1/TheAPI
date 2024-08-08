package me.devtec.theapi.bukkit.game.resourcepack;

import me.devtec.shared.Ref;
import me.devtec.theapi.bukkit.BukkitLoader;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

public class ResourcePackAPI {
    public static void sendOffer(Player player, ResourcePackOffer offer, @Nullable ResourcePackHandler result) {
        if (Ref.isOlderThan(8))
            return;
        if (result != null)
            JavaPlugin.getPlugin(BukkitLoader.class).resourcePackHandler.put(player.getUniqueId(), result);
        BukkitLoader.getPacketHandler().send(player, BukkitLoader.getNmsProvider().packetResourcePackSend(offer.getUrl(), offer.getHash(), offer.isShouldForce(), offer.getPrompt().orElse(null)));
    }
}
