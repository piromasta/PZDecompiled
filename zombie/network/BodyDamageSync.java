package zombie.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.Moodles.MoodleType;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.network.packets.BodyDamageUpdatePacket;

public class BodyDamageSync {
   public static final byte BD_Health = 1;
   public static final byte BD_bandaged = 2;
   public static final byte BD_bitten = 3;
   public static final byte BD_bleeding = 4;
   public static final byte BD_IsBleedingStemmed = 5;
   public static final byte BD_IsCauterized = 6;
   public static final byte BD_scratched = 7;
   public static final byte BD_stitched = 8;
   public static final byte BD_deepWounded = 9;
   public static final byte BD_IsInfected = 10;
   public static final byte BD_IsFakeInfected = 11;
   public static final byte BD_bandageLife = 12;
   public static final byte BD_scratchTime = 13;
   public static final byte BD_biteTime = 14;
   public static final byte BD_alcoholicBandage = 15;
   public static final byte BD_woundInfectionLevel = 16;
   public static final byte BD_infectedWound = 17;
   public static final byte BD_bleedingTime = 18;
   public static final byte BD_deepWoundTime = 19;
   public static final byte BD_haveGlass = 20;
   public static final byte BD_stitchTime = 21;
   public static final byte BD_alcoholLevel = 22;
   public static final byte BD_additionalPain = 23;
   public static final byte BD_bandageType = 24;
   public static final byte BD_getBandageXp = 25;
   public static final byte BD_getStitchXp = 26;
   public static final byte BD_getSplintXp = 27;
   public static final byte BD_fractureTime = 28;
   public static final byte BD_splint = 29;
   public static final byte BD_splintFactor = 30;
   public static final byte BD_haveBullet = 31;
   public static final byte BD_burnTime = 32;
   public static final byte BD_needBurnWash = 33;
   public static final byte BD_lastTimeBurnWash = 34;
   public static final byte BD_splintItem = 35;
   public static final byte BD_plantainFactor = 36;
   public static final byte BD_comfreyFactor = 37;
   public static final byte BD_garlicFactor = 38;
   public static final byte BD_cut = 39;
   public static final byte BD_cutTime = 40;
   public static final byte BD_stiffness = 41;
   public static final byte BD_MaxParam = 42;
   public static final byte BD_BodyDamage = 50;
   public static final byte BD_START = 64;
   public static final byte BD_END = 65;
   public static BodyDamageSync instance = new BodyDamageSync();
   private final ArrayList<Updater> updaters = new ArrayList();

   public BodyDamageSync() {
   }

   private static void noise(String var0) {
      if (Core.bDebug || GameServer.bServer && GameServer.bDebug) {
         DebugLog.log("BodyDamage: " + var0);
      }

   }

   public void startSendingUpdates(short var1, short var2) {
      if (GameClient.bClient) {
         noise("start sending updates to " + var2);

         Updater var4;
         for(int var3 = 0; var3 < this.updaters.size(); ++var3) {
            var4 = (Updater)this.updaters.get(var3);
            if (var4.localIndex == var1 && var4.remoteID == var2) {
               return;
            }
         }

         IsoPlayer var5 = IsoPlayer.players[var1];
         var4 = new Updater();
         var4.localIndex = var1;
         var4.remoteID = var2;
         var4.bdLocal = var5.getBodyDamage();
         var4.bdSent = new BodyDamage((IsoGameCharacter)null);
         this.updaters.add(var4);
      }
   }

   public void stopSendingUpdates(short var1, short var2) {
      if (GameClient.bClient) {
         noise("stop sending updates to " + var2);

         for(int var3 = 0; var3 < this.updaters.size(); ++var3) {
            Updater var4 = (Updater)this.updaters.get(var3);
            if (var4.localIndex == var1 && var4.remoteID == var2) {
               this.updaters.remove(var3);
               return;
            }
         }

      }
   }

   public void startReceivingUpdates(IsoPlayer var1) {
      if (GameClient.bClient) {
         BodyDamageUpdatePacket var2 = new BodyDamageUpdatePacket();
         var2.setStart(var1);
         ByteBufferWriter var3 = GameClient.connection.startPacket();
         PacketTypes.PacketType.BodyDamageUpdate.doPacket(var3);
         var2.write(var3);
         PacketTypes.PacketType.BodyDamageUpdate.send(GameClient.connection);
      }
   }

   public void stopReceivingUpdates(IsoPlayer var1) {
      if (GameClient.bClient) {
         BodyDamageUpdatePacket var2 = new BodyDamageUpdatePacket();
         var2.setStop(var1);
         ByteBufferWriter var3 = GameClient.connection.startPacket();
         PacketTypes.PacketType.BodyDamageUpdate.doPacket(var3);
         var2.write(var3);
         PacketTypes.PacketType.BodyDamageUpdate.send(GameClient.connection);
      }
   }

   public void update() {
      if (GameClient.bClient) {
         for(int var1 = 0; var1 < this.updaters.size(); ++var1) {
            Updater var2 = (Updater)this.updaters.get(var1);
            var2.update();
         }

      }
   }

   public static final class Updater {
      static ByteBuffer bb = ByteBuffer.allocate(1024);
      short localIndex;
      short remoteID;
      BodyDamage bdLocal;
      BodyDamage bdSent;
      boolean partStarted;
      byte partIndex;
      long sendTime;

      public Updater() {
      }

      void update() {
         long var1 = System.currentTimeMillis();
         if (var1 - this.sendTime >= 500L) {
            this.sendTime = var1;
            bb.clear();
            int var3 = this.bdLocal.getParentChar().getMoodles().getMoodleLevel(MoodleType.Pain);
            if (this.compareFloats(this.bdLocal.getOverallBodyHealth(), (float)((int)this.bdSent.getOverallBodyHealth())) || var3 != this.bdSent.getRemotePainLevel() || this.bdLocal.IsFakeInfected != this.bdSent.IsFakeInfected || this.compareFloats(this.bdLocal.InfectionLevel, this.bdSent.InfectionLevel)) {
               bb.put((byte)50);
               bb.putFloat(this.bdLocal.getOverallBodyHealth());
               bb.put((byte)var3);
               bb.put((byte)(this.bdLocal.IsFakeInfected ? 1 : 0));
               bb.putFloat(this.bdLocal.InfectionLevel);
               this.bdSent.setOverallBodyHealth(this.bdLocal.getOverallBodyHealth());
               this.bdSent.setRemotePainLevel(var3);
               this.bdSent.IsFakeInfected = this.bdLocal.IsFakeInfected;
               this.bdSent.InfectionLevel = this.bdLocal.InfectionLevel;
            }

            for(int var4 = 0; var4 < this.bdLocal.BodyParts.size(); ++var4) {
               this.updatePart(var4);
            }

            if (bb.position() > 0) {
               bb.put((byte)65);
               if (IsoPlayer.players[this.localIndex] != null && GameClient.IDToPlayerMap.get(this.remoteID) != null) {
                  BodyDamageUpdatePacket var6 = new BodyDamageUpdatePacket();
                  var6.setUpdate(IsoPlayer.players[this.localIndex], (IsoPlayer)GameClient.IDToPlayerMap.get(this.remoteID), bb);
                  ByteBufferWriter var5 = GameClient.connection.startPacket();
                  PacketTypes.PacketType.BodyDamageUpdate.doPacket(var5);
                  var6.write(var5);
                  PacketTypes.PacketType.BodyDamageUpdate.send(GameClient.connection);
               }
            }

         }
      }

      void updatePart(int var1) {
         BodyPart var2 = (BodyPart)this.bdLocal.BodyParts.get(var1);
         BodyPart var3 = (BodyPart)this.bdSent.BodyParts.get(var1);
         this.partStarted = false;
         this.partIndex = (byte)var1;
         var2.sync(var3, this);
         if (this.partStarted) {
            bb.put((byte)65);
         }

      }

      public void updateField(byte var1, boolean var2) {
         if (!this.partStarted) {
            bb.put((byte)64);
            bb.put(this.partIndex);
            this.partStarted = true;
         }

         bb.put(var1);
         bb.put((byte)(var2 ? 1 : 0));
      }

      private boolean compareFloats(float var1, float var2) {
         if (Float.compare(var1, 0.0F) != Float.compare(var2, 0.0F)) {
            return true;
         } else {
            return (int)var1 != (int)var2;
         }
      }

      public boolean updateField(byte var1, float var2, float var3) {
         if (!this.compareFloats(var2, var3)) {
            return false;
         } else {
            if (!this.partStarted) {
               bb.put((byte)64);
               bb.put(this.partIndex);
               this.partStarted = true;
            }

            bb.put(var1);
            bb.putFloat(var2);
            return true;
         }
      }

      public void updateField(byte var1, String var2) {
         if (!this.partStarted) {
            bb.put((byte)64);
            bb.put(this.partIndex);
            this.partStarted = true;
         }

         bb.put(var1);
         GameWindow.WriteStringUTF(bb, var2);
      }
   }
}
