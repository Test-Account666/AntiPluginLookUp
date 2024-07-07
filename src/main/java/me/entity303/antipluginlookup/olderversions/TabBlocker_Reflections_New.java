package me.entity303.antipluginlookup.olderversions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import me.entity303.antipluginlookup.utils.ChannelUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TabBlocker_Reflections_New extends TabBlocker {
    private Method cMethod;

    public TabBlocker_Reflections_New(AntiPluginLookUp plugin) {
        super(plugin);
    }


    @Override
    public void Inject(final Player player) {
        try {
            var channel = (Channel) ChannelUtils.GetChannel(player);

            if (channel == null) {
                Logger.getLogger(TabBlocker_Reflections_New.class.getName()).log(Level.SEVERE, "Couldn't get channel?!");
                return;
            }

            this.RemoveExistingTabBlocker(channel);
            this.AddTabBlockerHandler(channel, player);
        } catch (Exception e) {
            Logger.getLogger(TabBlocker_Reflections_New.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void RemoveExistingTabBlocker(Channel channel) {
        if (channel.pipeline().get("tabBlocker") == null)
            return;

        channel.pipeline().remove("tabBlocker");
    }

    private void AddTabBlockerHandler(Channel channel, Player player) {
        channel.pipeline().addAfter("decoder", "tabBlocker", new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
                if (!o.getClass()
                      .toString()
                      .replaceAll("(@[A-Z0-9]*)", "")
                      .equalsIgnoreCase("class net.minecraft.network.protocol.game" + ".PacketPlayInTabComplete")) {
                    super.channelRead(channelHandlerContext, o);
                    return;
                }


                if (TabBlocker_Reflections_New.this.cMethod == null) {
                    TabBlocker_Reflections_New.this.cMethod = Arrays.stream(o.getClass().getDeclaredMethods())
                                                                    .filter(method -> method.getParameterCount() == 0)
                                                                    .filter(method -> method.getReturnType() == String.class)
                                                                    .findFirst()
                                                                    .orElse(null);

                    if (TabBlocker_Reflections_New.this.cMethod == null) {
                        Bukkit.getLogger().severe("Couldn't find cMethod!");
                        player.sendMessage("§cAn error has occurred, please contact an admin!");
                        return;
                    }

                    TabBlocker_Reflections_New.this.cMethod.setAccessible(true);
                }

                var fullCommand = (String) TabBlocker_Reflections_New.this.cMethod.invoke(o);

                var command = fullCommand.split(" ")[0].toLowerCase().replaceFirst("/", "");

                var isBlocked = TabBlocker_Reflections_New.this.plugin.getBlockedCommandManager().IsBlockedCommand(command);

                var blockedCommand = TabBlocker_Reflections_New.this.plugin.getBlockedCommandManager().GetBlockedCommand(command);
                if (blockedCommand == null && isBlocked) {
                    Bukkit.getLogger().severe("Command '" + command + "' is blocked, but no BlockedCommand is present?!");
                    player.sendMessage("§cAn error has occurred, please contact an admin!");
                    return;
                }

                if (blockedCommand != null) {
                    var whitelistedCommands =
                            TabBlocker_Reflections_New.this.plugin.GetWhitelistedCommandManager().GetWhitelistedCommands(blockedCommand.command());
                    if (whitelistedCommands.stream()
                                           .anyMatch(whitelistedCommand -> whitelistedCommand.command().equalsIgnoreCase(blockedCommand.command())))
                        isBlocked = false;

                    if (isBlocked && !player.hasPermission(blockedCommand.permission()))
                        return;
                }

                super.channelRead(channelHandlerContext, o);
            }
        });
    }
}
