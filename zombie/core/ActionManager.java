package zombie.core;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.BuildActionPacket;
import zombie.network.packets.FishingActionPacket;
import zombie.network.packets.GeneralActionPacket;
import zombie.network.packets.NetTimedActionPacket;
import zombie.network.server.AnimEventEmulator;

public class ActionManager {
   private static ActionManager instance = null;
   private static final ConcurrentLinkedQueue<Action> actions = new ConcurrentLinkedQueue();

   public ActionManager() {
   }

   public static ActionManager getInstance() {
      if (instance == null) {
         instance = new ActionManager();
      }

      return instance;
   }

   public static void start(Action var0) {
      DebugLog.Action.debugln("ActionManager start action %s", var0.getDescription());
      var0.start();
      add(var0);
   }

   public static void add(Action var0) {
      DebugLog.Action.debugln("ActionManager add action %s", var0.getDescription());
      actions.add(var0);
   }

   public static void stop(Action var0) {
      DebugLog.Action.debugln("ActionManager stop action %s", var0.getDescription());
      remove(var0.id, true);
   }

   public static void update() {
      List var4;
      Iterator var5;
      Action var6;
      if (GameServer.bServer) {
         Iterator var0 = actions.iterator();

         while(true) {
            while(var0.hasNext()) {
               Action var1 = (Action)var0.next();
               if (var1.state == Transaction.TransactionState.Accept && var1.endTime <= GameTime.getServerTimeMills()) {
                  DebugLog.Action.debugln("ActionManager complete %s", var1.getDescription());
                  UdpConnection var2;
                  ByteBufferWriter var3;
                  if (var1.perform()) {
                     var1.state = Transaction.TransactionState.Done;
                     var2 = GameServer.getConnectionFromPlayer(var1.playerID.getPlayer());
                     if (var2 != null && var2.isFullyConnected()) {
                        if (var1 instanceof BuildAction) {
                           var3 = var2.startPacket();
                           PacketTypes.PacketType.BuildAction.doPacket(var3);
                           var1.write(var3);
                           PacketTypes.PacketType.BuildAction.send(var2);
                        }

                        if (var1 instanceof NetTimedAction) {
                           var3 = var2.startPacket();
                           PacketTypes.PacketType.NetTimedAction.doPacket(var3);
                           var1.write(var3);
                           PacketTypes.PacketType.NetTimedAction.send(var2);
                        }
                     }
                  } else {
                     var1.state = Transaction.TransactionState.Reject;
                     DebugLog.Action.noise("ActionManager reject %s", var1.getDescription());
                     var2 = GameServer.getConnectionFromPlayer(var1.playerID.getPlayer());
                     if (var2 != null && var2.isFullyConnected() && var1 instanceof NetTimedAction) {
                        var3 = var2.startPacket();
                        PacketTypes.PacketType.NetTimedAction.doPacket(var3);
                        var1.write(var3);
                        PacketTypes.PacketType.NetTimedAction.send(var2);
                     }
                  }
               } else {
                  var1.update();
               }
            }

            var4 = (List)actions.stream().filter((var0x) -> {
               return var0x.state == Transaction.TransactionState.Done || var0x.state == Transaction.TransactionState.Reject;
            }).collect(Collectors.toList());
            actions.removeAll(var4);
            var5 = var4.iterator();

            while(var5.hasNext()) {
               var6 = (Action)var5.next();
               DebugLog.Action.debugln("ActionManager clear action %s", var6.getDescription());
               if (var6 instanceof NetTimedAction) {
                  AnimEventEmulator.getInstance().remove((NetTimedAction)var6);
               }
            }
            break;
         }
      } else if (GameClient.bClient) {
         actions.forEach(Action::update);
         var4 = (List)actions.stream().filter((var0x) -> {
            return var0x.isUsingTimeout() && GameTime.getServerTimeMills() > var0x.startTime + AnimEventEmulator.getInstance().getDurationMax();
         }).collect(Collectors.toList());
         actions.removeAll(var4);
         var5 = var4.iterator();

         while(var5.hasNext()) {
            var6 = (Action)var5.next();
            DebugLog.Action.debugln("ActionManager clear action %s", var6.getDescription());
         }
      }

   }

   public static boolean isRejected(byte var0) {
      return !actions.isEmpty() && (actions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).allMatch((var0x) -> {
         return var0x.state == Transaction.TransactionState.Reject;
      }) || actions.stream().noneMatch((var1x) -> {
         return var0 == var1x.id;
      }));
   }

   public static boolean isDone(byte var0) {
      return !actions.isEmpty() && actions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).allMatch((var0x) -> {
         return var0x.state == Transaction.TransactionState.Done;
      });
   }

   public static boolean isLooped(byte var0) {
      Optional var1 = actions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).findFirst();
      if (var1.isEmpty()) {
         return false;
      } else {
         Action var2 = (Action)var1.get();
         return var2.duration == -1L;
      }
   }

   public static int getDuration(byte var0) {
      Optional var1 = actions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).findFirst();
      if (var1.isEmpty()) {
         return -1;
      } else {
         Action var2 = (Action)var1.get();
         return (int)(var2.endTime - var2.startTime);
      }
   }

   public static IsoPlayer getPlayer(byte var0) {
      Optional var1 = actions.stream().filter((var1x) -> {
         return var0 == var1x.id;
      }).findFirst();
      if (var1.isEmpty()) {
         return null;
      } else {
         Action var2 = (Action)var1.get();
         return var2.playerID.getPlayer();
      }
   }

   public static void remove(byte var0, boolean var1) {
      Action var4;
      List var5;
      Iterator var6;
      if (GameClient.bClient) {
         if (var0 != 0) {
            if (var1) {
               GeneralActionPacket var2 = new GeneralActionPacket();
               var2.setReject(var0);
               ByteBufferWriter var3 = GameClient.connection.startPacket();
               PacketTypes.PacketType.GeneralAction.doPacket(var3);
               var2.write(var3);
               PacketTypes.PacketType.GeneralAction.send(GameClient.connection);
            }

            var5 = (List)actions.stream().filter((var1x) -> {
               return var1x.id == var0;
            }).collect(Collectors.toList());
            actions.removeAll(var5);
            var6 = var5.iterator();

            while(var6.hasNext()) {
               var4 = (Action)var6.next();
               DebugLog.Action.debugln("ActionManager remove action %s", var4.getDescription());
            }
         }
      } else if (GameServer.bServer) {
         var5 = (List)actions.stream().filter((var1x) -> {
            return var1x.id == var0;
         }).collect(Collectors.toList());
         actions.removeAll(var5);
         var6 = var5.iterator();

         while(var6.hasNext()) {
            var4 = (Action)var6.next();
            DebugLog.Action.debugln("ActionManager remove action %s", var4.getDescription());
            var4.stop();
            if (var4 instanceof NetTimedAction) {
               AnimEventEmulator.getInstance().remove((NetTimedAction)var4);
            }
         }
      }

   }

   private byte sendAction(Action var1, PacketTypes.PacketType var2) {
      if (var1.isConsistent(GameClient.connection)) {
         add(var1);

         try {
            ByteBufferWriter var3 = GameClient.connection.startPacket();
            var2.doPacket(var3);
            var1.write(var3);
            var2.send(GameClient.connection);
            DebugLog.Action.noise("ActionManager send %s", var1.getDescription());
            return var1.id;
         } catch (Exception var4) {
            GameClient.connection.cancelPacket();
            DebugLog.Multiplayer.printException(var4, "SendAction: failed", LogSeverity.Error);
            return 0;
         }
      } else {
         DebugLog.Action.error("ActionManager send FAIL %s", var1.getDescription());
         return 0;
      }
   }

   public byte createNetTimedAction(IsoPlayer var1, KahluaTable var2) {
      NetTimedActionPacket var3 = new NetTimedActionPacket();
      var3.set(var1, var2);
      return this.sendAction(var3, PacketTypes.PacketType.NetTimedAction);
   }

   public byte createBuildAction(IsoPlayer var1, float var2, float var3, float var4, boolean var5, String var6, KahluaTable var7) {
      BuildActionPacket var8 = new BuildActionPacket();
      var8.set(var1, var2, var3, var4, var5, var6, var7);
      return this.sendAction(var8, PacketTypes.PacketType.BuildAction);
   }

   public byte createFishingAction(IsoPlayer var1, InventoryItem var2, IsoGridSquare var3, KahluaTable var4) {
      FishingActionPacket var5 = new FishingActionPacket();
      var5.setStartFishing(var1, var2, var3, var4);
      return this.sendAction(var5, PacketTypes.PacketType.FishingAction);
   }

   public void setStateFromPacket(Action var1) {
      Iterator var2 = actions.iterator();

      while(var2.hasNext()) {
         Action var3 = (Action)var2.next();
         if (var1.id == var3.id) {
            var3.setState(var1.state);
            if (var1.state == Transaction.TransactionState.Accept) {
               var3.setDuration(var1.duration);
            }
            break;
         }
      }

   }

   public void disconnectPlayer(UdpConnection var1) {
      for(int var2 = 0; var2 < 4; ++var2) {
         IsoPlayer var3 = var1.players[var2];
         if (var3 != null) {
            Iterator var4 = actions.iterator();

            while(var4.hasNext()) {
               Action var5 = (Action)var4.next();
               if (var5.playerID.getID() == var3.getOnlineID()) {
                  var5.state = Transaction.TransactionState.Reject;
               }
            }
         }
      }

   }

   public void replaceObjectInQueuedActions(IsoPlayer var1, Object var2, Object var3) {
      KahluaTable var4 = (KahluaTable)LuaManager.env.rawget("ISTimedActionQueue");
      KahluaTable var5 = (KahluaTable)var4.rawget("queues");
      KahluaTable var6 = (KahluaTable)var5.rawget(var1);
      if (var6 != null) {
         KahluaTableIterator var7 = var6.iterator();

         while(true) {
            do {
               if (!var7.advance()) {
                  return;
               }
            } while(!var7.getKey().equals("queue"));

            KahluaTable var8 = (KahluaTable)var7.getValue();
            KahluaTableIterator var9 = var8.iterator();

            while(var9.advance()) {
               KahluaTable var10 = (KahluaTable)var9.getValue();
               KahluaTableIterator var11 = var10.iterator();

               while(var11.advance()) {
                  if (var11.getValue() == var2) {
                     var10.rawset(var11.getKey(), var3);
                  }
               }
            }
         }
      }
   }
}
