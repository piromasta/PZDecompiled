package zombie.popman;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import zombie.VirtualZombieManager;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.NetworkZombieVariables;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.ZombiePacket;

public class NetworkZombieSimulator {
   public static final int MAX_ZOMBIES_PER_UPDATE = 300;
   private static final NetworkZombieSimulator instance = new NetworkZombieSimulator();
   private static final ZombiePacket zombiePacket = new ZombiePacket();
   private final ByteBuffer bb = ByteBuffer.allocate(1000000);
   private final ArrayList<Short> unknownZombies = new ArrayList();
   private final HashSet<Short> authoriseZombies = new HashSet();
   private final ArrayDeque<IsoZombie> SendQueue = new ArrayDeque();
   private final ArrayDeque<IsoZombie> ExtraSendQueue = new ArrayDeque();
   private HashSet<Short> authoriseZombiesCurrent = new HashSet();
   private HashSet<Short> authoriseZombiesLast = new HashSet();
   UpdateLimit ZombieSimulationReliableLimit = new UpdateLimit(1000L);

   public NetworkZombieSimulator() {
   }

   public static NetworkZombieSimulator getInstance() {
      return instance;
   }

   public int getAuthorizedZombieCount() {
      return (int)IsoWorld.instance.CurrentCell.getZombieList().stream().filter((var0) -> {
         return var0.authOwner == GameClient.connection;
      }).count();
   }

   public int getUnauthorizedZombieCount() {
      return (int)IsoWorld.instance.CurrentCell.getZombieList().stream().filter((var0) -> {
         return var0.authOwner == null;
      }).count();
   }

   public void clear() {
      HashSet var1 = this.authoriseZombiesCurrent;
      this.authoriseZombiesCurrent = this.authoriseZombiesLast;
      this.authoriseZombiesLast = var1;
      this.authoriseZombiesLast.removeIf((var0) -> {
         return GameClient.getZombie(var0) == null;
      });
      this.authoriseZombiesCurrent.clear();
   }

   public void addExtraUpdate(IsoZombie var1) {
      if (var1.authOwner == GameClient.connection && !this.ExtraSendQueue.contains(var1)) {
         this.ExtraSendQueue.add(var1);
      }

   }

   public void add(short var1) {
      this.authoriseZombiesCurrent.add(var1);
   }

   public void added() {
      Sets.SetView var1 = Sets.difference(this.authoriseZombiesCurrent, this.authoriseZombiesLast);
      UnmodifiableIterator var2 = var1.iterator();

      while(true) {
         while(var2.hasNext()) {
            Short var3 = (Short)var2.next();
            IsoZombie var4 = GameClient.getZombie(var3);
            if (var4 != null && var4.OnlineID == var3) {
               this.becomeLocal(var4);
            } else if (!this.unknownZombies.contains(var3)) {
               this.unknownZombies.add(var3);
            }
         }

         Sets.SetView var8 = Sets.difference(this.authoriseZombiesLast, this.authoriseZombiesCurrent);
         UnmodifiableIterator var9 = var8.iterator();

         while(var9.hasNext()) {
            Short var10 = (Short)var9.next();
            IsoZombie var5 = GameClient.getZombie(var10);
            if (var5 != null) {
               this.becomeRemote(var5);
            }
         }

         synchronized(this.authoriseZombies) {
            this.authoriseZombies.clear();
            this.authoriseZombies.addAll(this.authoriseZombiesCurrent);
            return;
         }
      }
   }

   public void becomeLocal(IsoZombie var1) {
      var1.lastRemoteUpdate = 0;
      var1.authOwner = GameClient.connection;
      var1.authOwnerPlayer = IsoPlayer.getInstance();
      var1.networkAI.setUpdateTimer(0.0F);
      var1.AllowRepathDelay = 0.0F;
      var1.networkAI.mindSync.restorePFBTarget();
   }

   public void becomeRemote(IsoZombie var1) {
      if (var1.isDead() && var1.authOwner == GameClient.connection) {
         var1.getNetworkCharacterAI().setLocal(true);
      }

      var1.lastRemoteUpdate = 0;
      var1.authOwner = null;
      var1.authOwnerPlayer = null;
      if (var1.group != null) {
         var1.group.remove(var1);
      }

   }

   public boolean isZombieSimulated(Short var1) {
      synchronized(this.authoriseZombies) {
         return this.authoriseZombies.contains(var1);
      }
   }

   public void receivePacket(ByteBuffer var1, UdpConnection var2) {
      if (DebugOptions.instance.Network.Client.UpdateZombiesFromPacket.getValue()) {
         short var3 = var1.getShort();

         for(short var4 = 0; var4 < var3; ++var4) {
            this.parseZombie(var1, var2);
         }

      }
   }

   private void parseZombie(ByteBuffer var1, UdpConnection var2) {
      ZombiePacket var3 = zombiePacket;
      var3.parse(var1, var2);
      if (var3.id == -1) {
         DebugLog.General.error("NetworkZombieSimulator.parseZombie id=" + var3.id);
      } else {
         try {
            IsoZombie var4 = (IsoZombie)GameClient.IDToZombieMap.get(var3.id);
            if (var4 == null) {
               if (IsoDeadBody.isDead(var3.id)) {
                  DebugLog.Death.debugln("Skip dead zombie creation id=%d", var3.id);
                  return;
               }

               IsoGridSquare var5 = IsoWorld.instance.CurrentCell.getGridSquare((double)var3.realX, (double)var3.realY, (double)var3.realZ);
               if (var5 != null) {
                  VirtualZombieManager.instance.choices.clear();
                  VirtualZombieManager.instance.choices.add(var5);
                  var4 = VirtualZombieManager.instance.createRealZombieAlways(var3.descriptorID, IsoDirections.getRandom().index(), false);
                  DebugLog.log(DebugType.ActionSystem, "ParseZombie: CreateRealZombieAlways id=" + var3.id);
                  if (var4 != null) {
                     var4.setFakeDead(false);
                     var4.OnlineID = var3.id;
                     GameClient.IDToZombieMap.put(var3.id, var4);
                     var4.lx = var4.nx = var4.x = var3.realX;
                     var4.ly = var4.ny = var4.y = var3.realY;
                     var4.lz = var4.z = (float)var3.realZ;
                     var4.setForwardDirection(var4.dir.ToVector());
                     var4.setCurrent(var5);
                     var4.networkAI.targetX = var3.x;
                     var4.networkAI.targetY = var3.y;
                     var4.networkAI.targetZ = var3.z;
                     var4.networkAI.predictionType = var3.moveType;
                     var4.networkAI.reanimatedBodyID = var3.reanimatedBodyID;
                     NetworkZombieVariables.setInt(var4, (short)0, var3.realHealth);
                     NetworkZombieVariables.setInt(var4, (short)2, var3.speedMod);
                     NetworkZombieVariables.setInt(var4, (short)1, var3.target);
                     NetworkZombieVariables.setInt(var4, (short)3, var3.timeSinceSeenFlesh);
                     NetworkZombieVariables.setInt(var4, (short)4, var3.smParamTargetAngle);
                     NetworkZombieVariables.setBooleanVariables(var4, var3.booleanVariables);
                     if (var4.isKnockedDown()) {
                        var4.setOnFloor(true);
                        var4.changeState(ZombieOnGroundState.instance());
                     }

                     var4.setWalkType(var3.walkType.toString());
                     var4.realState = var3.realState;
                     if (var4.isReanimatedPlayer()) {
                        IsoDeadBody var6 = IsoDeadBody.getDeadBody(var4.networkAI.reanimatedBodyID);
                        if (var6 != null) {
                           var4.setDir(var6.getDir());
                           var4.setForwardDirection(var6.getDir().ToVector());
                           var4.setFallOnFront(var6.isFallOnFront());
                        }

                        var4.getStateMachine().changeState(ZombieOnGroundState.instance(), (Iterable)null);
                        var4.bNeverDoneAlpha = false;
                     }

                     for(int var9 = 0; var9 < IsoPlayer.numPlayers; ++var9) {
                        IsoPlayer var7 = IsoPlayer.players[var9];
                        if (var5.isCanSee(var9)) {
                           var4.setAlphaAndTarget(var9, 1.0F);
                        }

                        if (var7 != null && var7.ReanimatedCorpseID == var3.id && var3.id != -1) {
                           var7.ReanimatedCorpseID = -1;
                           var7.ReanimatedCorpse = var4;
                        }
                     }

                     var4.networkAI.mindSync.parse(var3);
                  } else {
                     DebugLog.log("Error: VirtualZombieManager can't create zombie");
                  }
               }

               if (var4 == null) {
                  return;
               }
            }

            if (getInstance().isZombieSimulated(var4.OnlineID)) {
               var4.authOwner = GameClient.connection;
               var4.authOwnerPlayer = IsoPlayer.getInstance();
               return;
            }

            var4.authOwner = null;
            var4.authOwnerPlayer = null;
            if (!var4.networkAI.isSetVehicleHit() || !var4.isCurrentState(ZombieHitReactionState.instance())) {
               var4.networkAI.parse(var3);
               var4.networkAI.mindSync.parse(var3);
            }

            var4.lastRemoteUpdate = 0;
            if (!IsoWorld.instance.CurrentCell.getZombieList().contains(var4)) {
               IsoWorld.instance.CurrentCell.getZombieList().add(var4);
            }

            if (!IsoWorld.instance.CurrentCell.getObjectList().contains(var4)) {
               IsoWorld.instance.CurrentCell.getObjectList().add(var4);
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }

      }
   }

   public boolean anyUnknownZombies() {
      return this.unknownZombies.size() > 0;
   }

   public void send() {
      if (this.authoriseZombies.size() != 0 || this.unknownZombies.size() != 0) {
         IsoZombie var4;
         if (this.SendQueue.isEmpty()) {
            synchronized(this.authoriseZombies) {
               Iterator var2 = this.authoriseZombies.iterator();

               while(var2.hasNext()) {
                  Short var3 = (Short)var2.next();
                  var4 = GameClient.getZombie(var3);
                  if (var4 != null && var4.OnlineID != -1) {
                     this.SendQueue.add(var4);
                  }
               }
            }
         }

         this.bb.clear();
         int var9;
         int var10;
         synchronized(ZombieCountOptimiser.zombiesForDelete) {
            var9 = ZombieCountOptimiser.zombiesForDelete.size();
            this.bb.putShort((short)var9);
            var10 = 0;

            while(true) {
               if (var10 >= var9) {
                  ZombieCountOptimiser.zombiesForDelete.clear();
                  break;
               }

               this.bb.putShort(((IsoZombie)ZombieCountOptimiser.zombiesForDelete.get(var10)).OnlineID);
               ++var10;
            }
         }

         int var1 = this.unknownZombies.size();
         this.bb.putShort((short)var1);

         for(var9 = 0; var9 < var1; ++var9) {
            this.bb.putShort((Short)this.unknownZombies.get(var9));
         }

         this.unknownZombies.clear();
         var9 = this.bb.position();
         this.bb.putShort((short)300);
         var10 = 0;

         while(!this.SendQueue.isEmpty()) {
            var4 = (IsoZombie)this.SendQueue.poll();
            this.ExtraSendQueue.remove(var4);
            var4.zombiePacket.set(var4);
            if (var4.OnlineID != -1) {
               var4.zombiePacket.write(this.bb);
               var4.networkAI.targetX = var4.realx = var4.x;
               var4.networkAI.targetY = var4.realy = var4.y;
               var4.networkAI.targetZ = var4.realz = (byte)((int)var4.z);
               var4.realdir = var4.getDir();
               ++var10;
               if (var10 >= 300) {
                  break;
               }
            }
         }

         int var11;
         if (var10 < 300) {
            var11 = this.bb.position();
            this.bb.position(var9);
            this.bb.putShort((short)var10);
            this.bb.position(var11);
         }

         if (var10 > 0 || var1 > 0) {
            ByteBufferWriter var12 = GameClient.connection.startPacket();
            PacketTypes.PacketType var5;
            if (var1 > 0 && this.ZombieSimulationReliableLimit.Check()) {
               var5 = PacketTypes.PacketType.ZombieSimulationReliable;
            } else {
               var5 = PacketTypes.PacketType.ZombieSimulation;
            }

            var5.doPacket(var12);
            var12.bb.put(this.bb.array(), 0, this.bb.position());
            var5.send(GameClient.connection);
         }

         if (!this.ExtraSendQueue.isEmpty()) {
            this.bb.clear();
            this.bb.putShort((short)0);
            this.bb.putShort((short)0);
            var9 = this.bb.position();
            this.bb.putShort((short)0);
            var11 = 0;

            while(!this.ExtraSendQueue.isEmpty()) {
               IsoZombie var13 = (IsoZombie)this.ExtraSendQueue.poll();
               var13.zombiePacket.set(var13);
               if (var13.OnlineID != -1) {
                  var13.zombiePacket.write(this.bb);
                  var13.networkAI.targetX = var13.realx = var13.x;
                  var13.networkAI.targetY = var13.realy = var13.y;
                  var13.networkAI.targetZ = var13.realz = (byte)((int)var13.z);
                  var13.realdir = var13.getDir();
                  ++var11;
               }
            }

            int var14 = this.bb.position();
            this.bb.position(var9);
            this.bb.putShort((short)var11);
            this.bb.position(var14);
            if (var11 > 0) {
               ByteBufferWriter var6 = GameClient.connection.startPacket();
               PacketTypes.PacketType.ZombieSimulation.doPacket(var6);
               var6.bb.put(this.bb.array(), 0, this.bb.position());
               PacketTypes.PacketType.ZombieSimulation.send(GameClient.connection);
            }
         }

      }
   }

   public void remove(IsoZombie var1) {
      if (var1 != null && var1.OnlineID != -1) {
         GameClient.IDToZombieMap.remove(var1.OnlineID);
      }
   }

   public void clearTargetAuth(IsoPlayer var1) {
      if (Core.bDebug) {
         DebugLog.log(DebugType.Multiplayer, "Clear zombies target and auth for player id=" + var1.getOnlineID());
      }

      if (GameClient.bClient) {
         Iterator var2 = GameClient.IDToZombieMap.valueCollection().iterator();

         while(var2.hasNext()) {
            IsoZombie var3 = (IsoZombie)var2.next();
            if (var3.target == var1) {
               var3.setTarget((IsoMovingObject)null);
            }
         }
      }

   }
}
