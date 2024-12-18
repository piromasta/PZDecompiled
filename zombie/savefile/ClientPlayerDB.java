package zombie.savefile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.utils.UpdateLimit;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class ClientPlayerDB {
   private static ClientPlayerDB instance = null;
   private static boolean allow = false;
   public NetworkCharacterProfile networkProfile = null;
   private UpdateLimit saveToDBPeriod4Network = new UpdateLimit(30000L);
   private boolean forceSavePlayers;
   public boolean canSavePlayers = false;

   public ClientPlayerDB() {
   }

   public static void setAllow(boolean var0) {
      allow = var0;
   }

   public static boolean isAllow() {
      return allow;
   }

   public static synchronized ClientPlayerDB getInstance() {
      if (instance == null && allow) {
         instance = new ClientPlayerDB();
      }

      return instance;
   }

   public static boolean isAvailable() {
      return instance != null;
   }

   public void updateMain() {
      this.saveNetworkPlayersToDB();
   }

   public void close() {
      instance = null;
      allow = false;
   }

   private void saveNetworkPlayersToDB() {
      if (this.canSavePlayers && (this.forceSavePlayers || this.saveToDBPeriod4Network.Check())) {
         this.forceSavePlayers = false;

         for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
            IsoPlayer var2 = IsoPlayer.players[var1];
            if (var2 != null) {
               this.clientSendNetworkPlayerInt(var2);
            }
         }
      }

   }

   public ArrayList<IsoPlayer> getAllNetworkPlayers() {
      ArrayList var1 = new ArrayList();

      for(int var2 = 1; var2 < this.networkProfile.playerCount; ++var2) {
         byte[] var3 = this.getClientLoadNetworkPlayerData(var2 + 1);
         if (var3 != null) {
            ByteBuffer var4 = ByteBuffer.allocate(var3.length);
            var4.rewind();
            var4.put(var3);
            var4.rewind();

            try {
               IsoPlayer var5 = new IsoPlayer(IsoWorld.instance.CurrentCell);
               var5.serverPlayerIndex = var2 + 1;
               var5.load(var4, this.networkProfile.worldVersion[var2]);
               if (this.networkProfile.isDead[var2]) {
                  var5.getBodyDamage().setOverallBodyHealth(0.0F);
                  var5.setHealth(0.0F);
               }

               var1.add(var5);
            } catch (Exception var6) {
               ExceptionLogger.logException(var6);
            }
         }
      }

      return var1;
   }

   private boolean isClientLoadNetworkCharacterCompleted() {
      return this.networkProfile != null && this.networkProfile.isLoaded;
   }

   public void clientSendNetworkPlayerInt(IsoPlayer var1) {
      if (GameClient.connection != null) {
         INetworkPacket.send(PacketTypes.PacketType.SyncInventory, var1);
         INetworkPacket.send(PacketTypes.PacketType.PlayerDamage, var1);
      }
   }

   public boolean isAliveMainNetworkPlayer() {
      return !this.networkProfile.isDead[0];
   }

   public boolean clientLoadNetworkPlayer() {
      if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip)) {
         return this.networkProfile.playerCount > 0;
      } else if (GameClient.connection == null) {
         return false;
      } else {
         if (this.networkProfile != null) {
            this.networkProfile = null;
         }

         INetworkPacket.send(PacketTypes.PacketType.LoadPlayerProfile);
         int var1 = 200;

         while(var1-- > 0) {
            if (this.isClientLoadNetworkCharacterCompleted()) {
               return this.networkProfile.playerCount > 0;
            }

            try {
               Thread.sleep(50L);
            } catch (InterruptedException var3) {
               ExceptionLogger.logException(var3);
            }
         }

         return false;
      }
   }

   public byte[] getClientLoadNetworkPlayerData(int var1) {
      if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip)) {
         switch (var1) {
            case 1:
            case 2:
            case 3:
            case 4:
               return this.networkProfile.character[var1 - 1];
            default:
               return null;
         }
      } else if (!this.clientLoadNetworkPlayer()) {
         return null;
      } else {
         switch (var1) {
            case 1:
            case 2:
            case 3:
            case 4:
               return this.networkProfile.character[var1 - 1];
            default:
               return null;
         }
      }
   }

   public boolean loadNetworkPlayer() {
      try {
         byte[] var1 = this.getClientLoadNetworkPlayerData(1);
         if (var1 != null) {
            ByteBuffer var2 = ByteBuffer.allocate(var1.length);
            var2.rewind();
            var2.put(var1);
            var2.rewind();
            if (IsoPlayer.getInstance() == null) {
               IsoPlayer.setInstance(new IsoPlayer(IsoCell.getInstance()));
               IsoPlayer.players[0] = IsoPlayer.getInstance();
            }

            IsoPlayer.getInstance().serverPlayerIndex = 1;
            IsoPlayer.getInstance().load(var2, this.networkProfile.worldVersion[0]);
            return true;
         }
      } catch (Exception var3) {
         ExceptionLogger.logException(var3);
      }

      return false;
   }

   public boolean loadNetworkPlayerInfo(int var1) {
      if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip) && var1 >= 1 && var1 <= 4 && var1 <= this.networkProfile.playerCount) {
         int var2 = (int)(this.networkProfile.x[var1 - 1] / 8.0F) + IsoWorld.saveoffsetx * 30;
         int var3 = (int)(this.networkProfile.y[var1 - 1] / 8.0F) + IsoWorld.saveoffsety * 30;
         IsoChunkMap.WorldXA = (int)this.networkProfile.x[var1 - 1];
         IsoChunkMap.WorldYA = (int)this.networkProfile.y[var1 - 1];
         IsoChunkMap.WorldZA = (int)this.networkProfile.z[var1 - 1];
         IsoChunkMap.WorldXA += 300 * IsoWorld.saveoffsetx;
         IsoChunkMap.WorldYA += 300 * IsoWorld.saveoffsety;
         IsoChunkMap.SWorldX[0] = var2;
         IsoChunkMap.SWorldY[0] = var3;
         int[] var10000 = IsoChunkMap.SWorldX;
         var10000[0] += 30 * IsoWorld.saveoffsetx;
         var10000 = IsoChunkMap.SWorldY;
         var10000[0] += 30 * IsoWorld.saveoffsety;
         return true;
      } else {
         return false;
      }
   }

   public void forgetPlayer(int var1) {
      if (this.networkProfile != null && var1 >= 1 && var1 <= 4) {
         this.networkProfile.character[var1 - 1] = null;
         this.networkProfile.isDead[var1 - 1] = true;
      }

   }

   public int getNextServerPlayerIndex() {
      if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip)) {
         for(int var1 = 1; var1 < 4; ++var1) {
            if (this.networkProfile.character[var1] == null || this.networkProfile.isDead[var1]) {
               return var1 + 1;
            }
         }
      }

      return 2;
   }

   public static final class NetworkCharacterProfile {
      public boolean isLoaded = false;
      public final byte[][] character = new byte[4][];
      public String username;
      public String server;
      public int playerCount = 0;
      public final int[] worldVersion = new int[4];
      public final float[] x = new float[4];
      public final float[] y = new float[4];
      public final float[] z = new float[4];
      public final boolean[] isDead = new boolean[4];

      public NetworkCharacterProfile() {
      }
   }
}
