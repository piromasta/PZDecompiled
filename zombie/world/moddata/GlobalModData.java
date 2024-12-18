package zombie.world.moddata;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.service.GlobalModDataPacket;
import zombie.network.packets.service.GlobalModDataRequestPacket;
import zombie.world.WorldDictionary;

public final class GlobalModData {
   public static final String SAVE_EXT = ".bin";
   public static final String SAVE_FILE = "global_mod_data";
   public static GlobalModData instance = new GlobalModData();
   private Map<String, KahluaTable> modData = new HashMap();
   private static final int BLOCK_SIZE = 524288;
   private static int LAST_BLOCK_SIZE = -1;

   private KahluaTable createModDataTable() {
      return LuaManager.platform.newTable();
   }

   public GlobalModData() {
      this.reset();
   }

   public void init() throws IOException {
      this.reset();
      this.load();
      LuaEventManager.triggerEvent("OnInitGlobalModData", WorldDictionary.isIsNewGame());
   }

   public void reset() {
      LAST_BLOCK_SIZE = -1;
      this.modData.clear();
   }

   public void collectTableNames(List<String> var1) {
      var1.clear();
      Iterator var2 = this.modData.entrySet().iterator();

      while(var2.hasNext()) {
         Map.Entry var3 = (Map.Entry)var2.next();
         var1.add((String)var3.getKey());
      }

   }

   public boolean exists(String var1) {
      return this.modData.containsKey(var1);
   }

   public KahluaTable getOrCreate(String var1) {
      KahluaTable var2 = this.get(var1);
      if (var2 == null) {
         var2 = this.create(var1);
      }

      return var2;
   }

   public KahluaTable get(String var1) {
      return (KahluaTable)this.modData.get(var1);
   }

   public String create() {
      String var1 = UUID.randomUUID().toString();
      this.create(var1);
      return var1;
   }

   public KahluaTable create(String var1) {
      if (this.exists(var1)) {
         DebugLog.log("GlobalModData -> Cannot create table '" + var1 + "', already exists. Returning null.");
         return null;
      } else {
         KahluaTable var2 = this.createModDataTable();
         this.modData.put(var1, var2);
         return var2;
      }
   }

   public KahluaTable remove(String var1) {
      return (KahluaTable)this.modData.remove(var1);
   }

   public void add(String var1, KahluaTable var2) {
      this.modData.put(var1, var2);
   }

   public void transmit(String var1) {
      KahluaTable var2 = this.get(var1);
      if (var2 != null) {
         GlobalModDataPacket var3;
         if (GameClient.bClient) {
            var3 = new GlobalModDataPacket();
            var3.set(var1, var2);
            ByteBufferWriter var4 = GameClient.connection.startPacket();
            PacketTypes.PacketType.GlobalModData.doPacket(var4);

            try {
               var3.write(var4);
               PacketTypes.PacketType.GlobalModData.send(GameClient.connection);
            } catch (RuntimeException var9) {
               GameClient.connection.cancelPacket();
            }
         } else if (GameServer.bServer) {
            try {
               var3 = new GlobalModDataPacket();
               var3.set(var1, var2);

               for(int var11 = 0; var11 < GameServer.udpEngine.connections.size(); ++var11) {
                  UdpConnection var5 = (UdpConnection)GameServer.udpEngine.connections.get(var11);
                  ByteBufferWriter var6 = var5.startPacket();
                  PacketTypes.PacketType.GlobalModData.doPacket(var6);

                  try {
                     var3.write(var6);
                     PacketTypes.PacketType.GlobalModData.send(var5);
                  } catch (RuntimeException var8) {
                     var5.cancelPacket();
                  }
               }
            } catch (Exception var10) {
               DebugLog.log(var10.getMessage());
            }
         }
      } else {
         DebugLog.log("GlobalModData -> cannot transmit moddata not found: " + var1);
      }

   }

   public void request(String var1) {
      if (GameClient.bClient) {
         GlobalModDataRequestPacket var2 = new GlobalModDataRequestPacket();
         var2.set(var1);
         ByteBufferWriter var3 = GameClient.connection.startPacket();
         PacketTypes.PacketType.GlobalModDataRequest.doPacket(var3);

         try {
            var2.write(var3);
            PacketTypes.PacketType.GlobalModDataRequest.send(GameClient.connection);
         } catch (RuntimeException var5) {
            GameClient.connection.cancelPacket();
         }
      } else {
         DebugLog.log("GlobalModData -> can only request from Client.");
      }

   }

   public void receiveRequest(ByteBuffer var1, UdpConnection var2) {
      String var3 = GameWindow.ReadString(var1);
      KahluaTable var4 = this.get(var3);
      if (var4 == null) {
         DebugLog.log("GlobalModData -> received request for non-existing table, table: " + var3);
      }

      if (GameServer.bServer) {
         try {
            for(int var5 = 0; var5 < GameServer.udpEngine.connections.size(); ++var5) {
               UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var5);
               if (var6 == var2) {
                  ByteBufferWriter var7 = var6.startPacket();
                  PacketTypes.PacketType.GlobalModData.doPacket(var7);
                  ByteBuffer var8 = var7.bb;

                  try {
                     GameWindow.WriteString(var8, var3);
                     var8.put((byte)(var4 != null ? 1 : 0));
                     if (var4 != null) {
                        var4.save(var8);
                     }
                  } catch (Exception var14) {
                     var14.printStackTrace();
                     var6.cancelPacket();
                  } finally {
                     PacketTypes.PacketType.GlobalModData.send(var6);
                  }
               }
            }
         } catch (Exception var16) {
            DebugLog.log(var16.getMessage());
         }
      }

   }

   private static ByteBuffer ensureCapacity(ByteBuffer var0) {
      if (var0 == null) {
         LAST_BLOCK_SIZE = 1048576;
         return ByteBuffer.allocate(LAST_BLOCK_SIZE);
      } else {
         LAST_BLOCK_SIZE = var0.capacity() + 524288;
         ByteBuffer var1 = ByteBuffer.allocate(LAST_BLOCK_SIZE);
         return var1.put(var0.array(), 0, var0.position());
      }
   }

   public void save() throws IOException {
      if (!Core.getInstance().isNoSave()) {
         try {
            DebugLog.log("Saving GlobalModData");
            ByteBuffer var1 = ByteBuffer.allocate(LAST_BLOCK_SIZE == -1 ? 1048576 : LAST_BLOCK_SIZE);
            var1.putInt(219);
            var1.putInt(this.modData.size());
            int var2 = 0;
            Iterator var3 = this.modData.entrySet().iterator();

            while(var3.hasNext()) {
               Map.Entry var4 = (Map.Entry)var3.next();
               if (var1.capacity() - var1.position() < 4) {
                  var2 = var1.position();
                  ensureCapacity(var1);
                  var1.position(var2);
               }

               int var5 = var1.position();
               var1.putInt(0);
               int var6 = var1.position();

               while(true) {
                  try {
                     var2 = var1.position();
                     GameWindow.WriteString(var1, (String)var4.getKey());
                     ((KahluaTable)var4.getValue()).save(var1);
                  } catch (BufferOverflowException var8) {
                     var1 = ensureCapacity(var1);
                     var1.position(var2);
                     continue;
                  }

                  int var7 = var1.position();
                  var1.position(var5);
                  var1.putInt(var7 - var6);
                  var1.position(var7);
                  break;
               }
            }

            var1.flip();
            File var10 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("global_mod_data.tmp"));
            FileOutputStream var11 = new FileOutputStream(var10);
            var11.getChannel().truncate(0L);
            var11.write(var1.array(), 0, var1.limit());
            var11.flush();
            var11.close();
            File var12 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("global_mod_data.bin"));
            Files.copy(var10, var12);
            var10.delete();
         } catch (Exception var9) {
            var9.printStackTrace();
            throw new IOException("Error saving GlobalModData.", var9);
         }
      }
   }

   public void load() throws IOException {
      if (!Core.getInstance().isNoSave()) {
         String var1 = ZomboidFileSystem.instance.getFileNameInCurrentSave("global_mod_data.bin");
         File var2 = new File(var1);
         if (!var2.exists()) {
            if (!WorldDictionary.isIsNewGame()) {
            }

         } else {
            try {
               FileInputStream var3 = new FileInputStream(var2);

               try {
                  DebugLog.DetailedInfo.trace("Loading GlobalModData:" + var1);
                  this.modData.clear();
                  ByteBuffer var4 = ByteBuffer.allocate((int)var2.length());
                  var4.clear();
                  int var5 = var3.read(var4.array());
                  var4.limit(var5);
                  int var6 = var4.getInt();
                  int var7 = var4.getInt();

                  for(int var8 = 0; var8 < var7; ++var8) {
                     int var9 = var4.getInt();
                     String var10 = GameWindow.ReadString(var4);
                     KahluaTable var11 = this.createModDataTable();
                     var11.load(var4, var6);
                     this.modData.put(var10, var11);
                  }
               } catch (Throwable var13) {
                  try {
                     var3.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               var3.close();
            } catch (Exception var14) {
               var14.printStackTrace();
               throw new IOException("Error loading GlobalModData.", var14);
            }
         }
      }
   }
}
