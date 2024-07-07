package me.entity303.antipluginlookup.utils;

import io.netty.channel.Channel;
import me.entity303.antipluginlookup.main.AntiPluginLookUp;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public class ChannelUtils {
    private static Method _getHandleMethod;
    private static MethodOrField _aMethod;
    private static Field _playerConnectionField;
    private static Field _channelField;
    private static AntiPluginLookUp _antiPluginLookUp;

    public static void SetAntiPluginLookUp(AntiPluginLookUp antiPluginLookUp) {
        _antiPluginLookUp = antiPluginLookUp;
    }

    public static Object GetEntityPlayer(Player player) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        if (_getHandleMethod == null) FetchGetHandleMethod(player);

        return _getHandleMethod.invoke(player);
    }

    public static void FetchGetHandleMethod(Player player) throws ClassNotFoundException, NoSuchMethodException {
        var craftPlayerClass = player.getClass();

        _getHandleMethod = craftPlayerClass.getMethod("getHandle");

        _getHandleMethod.setAccessible(true);
    }

    public static Channel GetChannel(Player player) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        var playerConnection = GetPlayerConnection(player);

        if (playerConnection == null) return null;

        var networkManager = GetNetworkManager(playerConnection);

        if (_channelField == null) {
            _channelField = Arrays.stream(networkManager.getClass().getDeclaredFields())
                                  .filter(field -> field.getType().getName().toLowerCase(Locale.ROOT).contains("channel"))
                                  .findFirst()
                                  .orElse(null);
            _channelField.setAccessible(true);
        }

        return (Channel) _channelField.get(networkManager);
    }

    public static Object GetPlayerConnection(Player player) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        var entityPlayer = GetEntityPlayer(player);

        if (entityPlayer == null) return null;

        if (_playerConnectionField == null) {
            _playerConnectionField = Arrays.stream(entityPlayer.getClass().getDeclaredFields())
                                           .filter(field -> field.getType().getName().toLowerCase(Locale.ROOT).contains("playerconnection") ||
                                                            field.getType().getName().toLowerCase().contains("servergamepacketlistenerimpl"))
                                           .findFirst()
                                           .orElse(null);

            if (_playerConnectionField == null) {
                _antiPluginLookUp.Error("Couldn't find PlayerConnection field inside '" + entityPlayer.getClass().getName() + "'! (Modded environment?)");
                Arrays.stream(entityPlayer.getClass().getDeclaredFields())
                      .forEach(field -> _antiPluginLookUp.Log(field.getType() + " -> " + field.getName()));
                _antiPluginLookUp.Warn("Please forward this to the developer of AntiPluginLookUp!");
                return null;
            }

            _playerConnectionField.setAccessible(true);
        }

        return _playerConnectionField.get(entityPlayer);
    }

    public static Object GetNetworkManager(Object playerConnection) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException {
        if (playerConnection instanceof Player player) playerConnection = GetPlayerConnection(player);

        if (_aMethod == null) {
            var aMethod = Arrays.stream(playerConnection.getClass().getDeclaredMethods())
                                .filter(field -> field.getReturnType().getName().toLowerCase(Locale.ROOT).contains("networkmanager"))
                                .findFirst()
                                .orElse(null);

            if (aMethod == null) {
                var field = Arrays.stream(playerConnection.getClass().getDeclaredFields())
                                  .filter(field1 -> field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkdispatcher") ||
                                                    field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkmanager"))
                                  .findFirst()
                                  .orElse(null);

                if (field == null) //Since 1.20.2 the field is located in SuperClass
                    field = Arrays.stream(playerConnection.getClass().getSuperclass().getDeclaredFields())
                                  .filter(field1 -> field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkdispatcher") ||
                                                    field1.getType().getName().toLowerCase(Locale.ROOT).contains("networkmanager") ||
                                                    field1.getType().getName().equalsIgnoreCase("net.minecraft.network.Connection"))
                                  .findFirst()
                                  .orElse(null);

                if (field == null) {
                    _antiPluginLookUp.Error("Couldn't find NetworkManager field!");

                    _antiPluginLookUp.Error(playerConnection.getClass().getName());

                    for (var declaredField : playerConnection.getClass().getDeclaredFields())
                        _antiPluginLookUp.Log(declaredField.getType().getName() + " -> " + declaredField.getName());

                    _antiPluginLookUp.Error(playerConnection.getClass().getSuperclass().getName());

                    for (var declaredField : playerConnection.getClass().getSuperclass().getDeclaredFields())
                        _antiPluginLookUp.Log(declaredField.getType().getName() + " -> " + declaredField.getName());

                    _antiPluginLookUp.Warn("Please forward this to the developer of ServerSystem!");
                    return null;
                }

                _aMethod = new MethodOrField(field);
            } else _aMethod = new MethodOrField(aMethod);
        }

        return _aMethod.Invoke(playerConnection);
    }

    static class MethodOrField {
        private Method method = null;
        private Field field = null;

        public MethodOrField(Method method) {
            this.method = method;
            method.setAccessible(true);
        }

        public MethodOrField(Field field) {
            this.field = field;
            field.setAccessible(true);
        }

        public Object Invoke(Object object) {
            if (this.method != null) try {
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
