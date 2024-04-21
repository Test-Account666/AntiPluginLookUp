package me.entity303.antipluginlookup.olderversions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TabBlocker_Reflections_New extends TabBlocker {
    private Method getHandleMethod;
    private Method cMethod;
    private Field channelField;
    private Field playerConnectionField;
    private MethodOrField networkManagerMethodOrField;

    public TabBlocker_Reflections_New(AntiPluginLookUp plugin) {
        super(plugin);
    }


    @Override
    public void Inject(final Player player) {
        try {
            if (this.getHandleMethod == null)
                this.FetchGetHandleMethod(player);

            var entityPlayer = this.getHandleMethod.invoke(player);

            if (this.playerConnectionField == null)
                this.FetchPlayerConnectionField(entityPlayer);

            var playerConnection = this.playerConnectionField.get(entityPlayer);

            if (this.networkManagerMethodOrField == null)
                this.FetchNetworkManagerMethodOrField(playerConnection);

            var networkManager = this.networkManagerMethodOrField.Invoke(playerConnection);

            if (this.channelField == null)
                this.FetchChannelField(networkManager);

            var channel = (Channel) this.channelField.get(networkManager);

            this.RemoveExistingTabBlocker(channel);
            this.AddTabBlockerHandler(channel, player);
        } catch (Exception e) {
            Logger.getLogger(TabBlocker_Reflections_New.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void FetchGetHandleMethod(Player player) throws NoSuchMethodException {
        this.getHandleMethod = player.getClass().getMethod("getHandle");
        this.getHandleMethod.setAccessible(true);
    }

    private void FetchPlayerConnectionField(Object entityPlayer) {
        this.playerConnectionField = Arrays.stream(entityPlayer.getClass().getDeclaredFields())
                                           .filter(field -> field.getType().getName().toLowerCase(Locale.ROOT).contains("playerconnection"))
                                           .findFirst()
                                           .orElseThrow(() -> new RuntimeException(
                                                   "Couldn't find PlayerConnection field! (Modded environment?)"));
        this.playerConnectionField.setAccessible(true);
    }

    private void FetchNetworkManagerMethodOrField(Object playerConnection) {
        if (this.networkManagerMethodOrField == null) {
            var aMethod = Arrays.stream(playerConnection.getClass().getDeclaredMethods())
                                .filter(field -> field.getReturnType().getName().toLowerCase(Locale.ROOT).contains("networkmanager"))
                                .findFirst()
                                .orElse(null);

            if (aMethod != null) {
                this.networkManagerMethodOrField = new MethodOrField(aMethod);
                return;
            }

            var field = Arrays.stream(playerConnection.getClass().getDeclaredFields())
                              .filter(field1 -> field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkdispatcher") ||
                                                field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkmanager"))
                              .findFirst()
                              .orElse(null);

            if (field == null) //Since 1.20.2 the field is located in SuperClass
                field = Arrays.stream(playerConnection.getClass().getSuperclass().getDeclaredFields())
                              .filter(field1 -> field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkdispatcher") ||
                                                field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkmanager"))
                              .findFirst()
                              .orElse(null);

            if (field == null) {
                this.GetPlugin().Error("Couldn't find NetworkManager field!");
                return;
            }

            this.networkManagerMethodOrField = new MethodOrField(field);
        }
    }

    private void FetchChannelField(Object networkManager) {
        this.channelField = Arrays.stream(networkManager.getClass().getDeclaredFields())
                                  .filter(field -> field.getType().getName().toLowerCase(Locale.ROOT).contains("channel"))
                                  .findFirst()
                                  .orElseThrow(() -> new RuntimeException("Couldn't find Channel field!"));
        this.channelField.setAccessible(true);
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
                    var whitelistedCommands = TabBlocker_Reflections_New.this.plugin.GetWhitelistedCommandManager()
                                                                                    .GetWhitelistedCommands(blockedCommand.command());
                    if (whitelistedCommands.stream()
                                           .anyMatch(whitelistedCommand -> whitelistedCommand.command()
                                                                                             .equalsIgnoreCase(blockedCommand.command())))
                        isBlocked = false;

                    if (isBlocked && !player.hasPermission(blockedCommand.permission()))
                        return;
                }

                super.channelRead(channelHandlerContext, o);
            }
        });
    }

    static class MethodOrField {
        private Method method = null;
        private Field field = null;

        public MethodOrField(Method method) {
            this.method = method;
            this.method.setAccessible(true);
        }

        public MethodOrField(Field field) {
            this.field = field;
            this.field.setAccessible(true);
        }

        public Object Invoke(Object object) {
            if (this.method != null)
                try {
                    return this.method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }

            try {
                return this.field.get(object);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
