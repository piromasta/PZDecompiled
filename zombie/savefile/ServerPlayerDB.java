package zombie.savefile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;

public final class ServerPlayerDB {
   private static ServerPlayerDB instance = null;
   private static boolean allow = false;
   public Connection conn = null;
   private ConcurrentLinkedQueue<NetworkCharacterData> CharactersToSave;

   public static void setAllow(boolean var0) {
      allow = var0;
   }

   public static boolean isAllow() {
      return allow;
   }

   public static synchronized ServerPlayerDB getInstance() {
      if (instance == null && allow) {
         instance = new ServerPlayerDB();
      }

      return instance;
   }

   public static boolean isAvailable() {
      return instance != null;
   }

   public ServerPlayerDB() {
      if (!Core.getInstance().isNoSave()) {
         this.create();
      }
   }

   public void close() {
      instance = null;
      allow = false;
   }

   private void create() {
      this.conn = PlayerDBHelper.create();
      this.CharactersToSave = new ConcurrentLinkedQueue();
      DatabaseMetaData var1 = null;

      try {
         var1 = this.conn.getMetaData();
         Statement var2 = this.conn.createStatement();
         ResultSet var3 = var1.getColumns((String)null, (String)null, "networkPlayers", "steamid");
         if (!var3.next()) {
            var2.executeUpdate("ALTER TABLE 'networkPlayers' ADD 'steamid' STRING NULL");
         }

         var3.close();
         var2.close();
      } catch (SQLException var4) {
         var4.printStackTrace();
      }

   }

   public void process() {
      if (!this.CharactersToSave.isEmpty()) {
         for(NetworkCharacterData var1 = (NetworkCharacterData)this.CharactersToSave.poll(); var1 != null; var1 = (NetworkCharacterData)this.CharactersToSave.poll()) {
            this.serverUpdateNetworkCharacterInt(var1);
         }
      }

   }

   /** @deprecated */
   @Deprecated
   public void serverUpdateNetworkCharacter(ByteBuffer var1, UdpConnection var2) {
      this.CharactersToSave.add(new NetworkCharacterData(var1, var2));
   }

   public void save() {
      Iterator var1 = GameServer.udpEngine.connections.iterator();

      while(var1.hasNext()) {
         UdpConnection var2 = (UdpConnection)var1.next();
         IsoPlayer[] var3 = var2.players;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            IsoPlayer var6 = var3[var5];
            if (var6 != null) {
               this.serverUpdateNetworkCharacter(var6, var6.getIndex(), var2);
            }
         }
      }

      while(!this.CharactersToSave.isEmpty()) {
         try {
            Thread.sleep(100L);
         } catch (InterruptedException var7) {
            throw new RuntimeException(var7);
         }
      }

      DebugLog.log("Saving players");
   }

   public void serverUpdateNetworkCharacter(IsoPlayer var1, int var2, UdpConnection var3) {
      this.CharactersToSave.add(new NetworkCharacterData(var1, var2, var3));
   }

   private void serverUpdateNetworkCharacterInt(NetworkCharacterData var1) {
      if (var1.playerIndex >= 0 && var1.playerIndex < 4) {
         if (this.conn != null) {
            String var2;
            if (GameServer.bCoop && SteamUtils.isSteamModeEnabled()) {
               var2 = "SELECT id FROM networkPlayers WHERE steamid=? AND world=? AND playerIndex=?";
            } else {
               var2 = "SELECT id FROM networkPlayers WHERE username=? AND world=? AND playerIndex=?";
            }

            String var3 = "INSERT INTO networkPlayers(world,username,steamid, playerIndex,name,x,y,z,worldversion,isDead,data) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            String var4 = "UPDATE networkPlayers SET x=?, y=?, z=?, worldversion = ?, isDead = ?, data = ?, name = ? WHERE id=?";

            try {
               PreparedStatement var5 = this.conn.prepareStatement(var2);

               label117: {
                  try {
                     if (GameServer.bCoop && SteamUtils.isSteamModeEnabled()) {
                        var5.setString(1, var1.steamid);
                     } else {
                        var5.setString(1, var1.username);
                     }

                     var5.setString(2, Core.GameSaveWorld);
                     var5.setInt(3, var1.playerIndex);
                     ResultSet var6 = var5.executeQuery();
                     if (!var6.next()) {
                        break label117;
                     }

                     int var7 = var6.getInt(1);
                     PreparedStatement var8 = this.conn.prepareStatement(var4);

                     try {
                        var8.setFloat(1, var1.x);
                        var8.setFloat(2, var1.y);
                        var8.setFloat(3, var1.z);
                        var8.setInt(4, var1.worldVersion);
                        var8.setBoolean(5, var1.isDead);
                        var8.setBytes(6, var1.buffer);
                        var8.setString(7, var1.playerName);
                        var8.setInt(8, var7);
                        int var9 = var8.executeUpdate();
                        this.conn.commit();
                     } catch (Throwable var15) {
                        if (var8 != null) {
                           try {
                              var8.close();
                           } catch (Throwable var13) {
                              var15.addSuppressed(var13);
                           }
                        }

                        throw var15;
                     }

                     if (var8 != null) {
                        var8.close();
                     }
                  } catch (Throwable var16) {
                     if (var5 != null) {
                        try {
                           var5.close();
                        } catch (Throwable var12) {
                           var16.addSuppressed(var12);
                        }
                     }

                     throw var16;
                  }

                  if (var5 != null) {
                     var5.close();
                  }

                  return;
               }

               if (var5 != null) {
                  var5.close();
               }

               var5 = this.conn.prepareStatement(var3);

               try {
                  var5.setString(1, Core.GameSaveWorld);
                  var5.setString(2, var1.username);
                  var5.setString(3, var1.steamid);
                  var5.setInt(4, var1.playerIndex);
                  var5.setString(5, var1.playerName);
                  var5.setFloat(6, var1.x);
                  var5.setFloat(7, var1.y);
                  var5.setFloat(8, var1.z);
                  var5.setInt(9, var1.worldVersion);
                  var5.setBoolean(10, var1.isDead);
                  var5.setBytes(11, var1.buffer);
                  int var18 = var5.executeUpdate();
                  this.conn.commit();
               } catch (Throwable var14) {
                  if (var5 != null) {
                     try {
                        var5.close();
                     } catch (Throwable var11) {
                        var14.addSuppressed(var11);
                     }
                  }

                  throw var14;
               }

               if (var5 != null) {
                  var5.close();
               }
            } catch (Exception var17) {
               ExceptionLogger.logException(var17);
               PlayerDBHelper.rollback(this.conn);
            }

         }
      }
   }

   public void serverConvertNetworkCharacter(String var1, String var2) {
      try {
         String var3 = "UPDATE networkPlayers SET steamid=? WHERE username=? AND world=? AND (steamid is null or steamid = '')";
         PreparedStatement var4 = this.conn.prepareStatement(var3);

         try {
            var4.setString(1, var2);
            var4.setString(2, var1);
            var4.setString(3, Core.GameSaveWorld);
            int var5 = var4.executeUpdate();
            if (var5 > 0) {
               DebugLog.DetailedInfo.warn("serverConvertNetworkCharacter: The steamid was set for the '" + var1 + "' for " + var5 + " players. ");
            }

            this.conn.commit();
         } catch (Throwable var8) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (SQLException var9) {
         ExceptionLogger.logException(var9);
      }

   }

   public IsoPlayer serverLoadNetworkCharacter(int var1, String var2) {
      if (var1 >= 0 && var1 < 4) {
         if (this.conn == null) {
            return null;
         } else {
            String var3;
            if (GameServer.bCoop && SteamUtils.isSteamModeEnabled()) {
               var3 = "SELECT id, x, y, z, data, worldversion, isDead FROM networkPlayers WHERE steamid=? AND world=? AND playerIndex=?";
            } else {
               var3 = "SELECT id, x, y, z, data, worldversion, isDead FROM networkPlayers WHERE username=? AND world=? AND playerIndex=?";
            }

            try {
               PreparedStatement var4 = this.conn.prepareStatement(var3);

               IsoPlayer var15;
               label86: {
                  Object var20;
                  label94: {
                     try {
                        var4.setString(1, var2);
                        var4.setString(2, Core.GameSaveWorld);
                        var4.setInt(3, var1);
                        ResultSet var5 = var4.executeQuery();
                        if (!var5.next()) {
                           var20 = null;
                           break label94;
                        }

                        int var6 = var5.getInt(1);
                        float var7 = var5.getFloat(2);
                        float var8 = var5.getFloat(3);
                        float var9 = var5.getFloat(4);
                        byte[] var10 = var5.getBytes(5);
                        int var11 = var5.getInt(6);
                        boolean var12 = var5.getBoolean(7);

                        try {
                           ByteBuffer var13 = ByteBuffer.allocate(var10.length);
                           var13.rewind();
                           var13.put(var10);
                           var13.rewind();
                           IsoPlayer var14 = new IsoPlayer(IsoWorld.instance.CurrentCell);
                           var14.serverPlayerIndex = var1;
                           var14.load(var13, var11);
                           if (var12) {
                              var14.getBodyDamage().setOverallBodyHealth(0.0F);
                              var14.setHealth(0.0F);
                           }

                           var14.bRemote = true;
                           var15 = var14;
                           break label86;
                        } catch (Exception var17) {
                           ExceptionLogger.logException(var17);
                        }
                     } catch (Throwable var18) {
                        if (var4 != null) {
                           try {
                              var4.close();
                           } catch (Throwable var16) {
                              var18.addSuppressed(var16);
                           }
                        }

                        throw var18;
                     }

                     if (var4 != null) {
                        var4.close();
                     }

                     return null;
                  }

                  if (var4 != null) {
                     var4.close();
                  }

                  return (IsoPlayer)var20;
               }

               if (var4 != null) {
                  var4.close();
               }

               return var15;
            } catch (SQLException var19) {
               ExceptionLogger.logException(var19);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static final class NetworkCharacterData {
      byte[] buffer;
      String username;
      String steamid;
      int playerIndex;
      String playerName;
      float x;
      float y;
      float z;
      boolean isDead;
      int worldVersion;

      public NetworkCharacterData(IsoPlayer var1, int var2, UdpConnection var3) {
         this.playerIndex = var2;
         String var10001 = var1.getDescriptor().getForename();
         this.playerName = var10001 + " " + var1.getDescriptor().getSurname();
         this.x = var1.getX();
         this.y = var1.getY();
         this.z = var1.getZ();
         this.isDead = var1.isDead();
         this.worldVersion = IsoWorld.getWorldVersion();

         try {
            ByteBuffer var4 = ByteBuffer.allocate(65536);
            var4.clear();
            var1.save(var4);
            this.buffer = new byte[var4.position()];
            var4.rewind();
            var4.get(this.buffer);
         } catch (IOException var5) {
            var5.printStackTrace();
         }

         if (GameServer.bCoop && SteamUtils.isSteamModeEnabled()) {
            this.steamid = var3.idStr;
         } else {
            this.steamid = "";
         }

         this.username = var3.username;
      }

      /** @deprecated */
      @Deprecated
      public NetworkCharacterData(ByteBuffer var1, UdpConnection var2) {
         this.playerIndex = var1.get();
         this.playerName = GameWindow.ReadStringUTF(var1);
         this.x = var1.getFloat();
         this.y = var1.getFloat();
         this.z = var1.getFloat();
         this.isDead = var1.get() == 1;
         this.worldVersion = var1.getInt();
         int var3 = var1.getInt();
         this.buffer = new byte[var3];
         var1.get(this.buffer);
         if (GameServer.bCoop && SteamUtils.isSteamModeEnabled()) {
            this.steamid = var2.idStr;
         } else {
            this.steamid = "";
         }

         this.username = var2.username;
      }
   }
}
