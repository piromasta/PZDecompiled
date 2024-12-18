package zombie.worldMap.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.spnetwork.SinglePlayerServer;
import zombie.util.StringUtils;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

public final class WorldMapServer {
   public static final WorldMapServer instance = new WorldMapServer();
   public static final int SAVEFILE_VERSION = 1;
   private static final byte[] FILE_MAGIC = new byte[]{87, 77, 83, 89};
   public static final byte PACKET_AddMarker = 1;
   public static final byte PACKET_RemoveMarker = 2;
   public static final byte PACKET_AddSymbol = 3;
   public static final byte PACKET_RemoveSymbol = 4;
   public static final byte PACKET_ModifySymbol = 5;
   public static final byte PACKET_SetPrivateSymbol = 6;
   public static final byte PACKET_ModifySharing = 7;
   private final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(1048576);
   private final ByteBufferWriter BYTE_BUFFER_WRITER;
   private final WorldMapSymbols m_symbols;
   private int m_nextElementID;
   private final TIntObjectHashMap<WorldMapBaseSymbol> m_symbolByID;

   public WorldMapServer() {
      this.BYTE_BUFFER_WRITER = new ByteBufferWriter(this.BYTE_BUFFER);
      this.m_symbols = new WorldMapSymbols();
      this.m_nextElementID = 1;
      this.m_symbolByID = new TIntObjectHashMap();
   }

   public void receive(ByteBuffer var1, UdpConnection var2) throws IOException {
      byte var3 = var1.get();
      switch (var3) {
         case 1:
            this.receiveAddMarker(var1, var2);
            break;
         case 2:
            this.receiveRemoveMarker(var1, var2);
            break;
         case 3:
            this.receiveAddSymbol(var1, var2);
            break;
         case 4:
            this.receiveRemoveSymbol(var1, var2);
            break;
         case 5:
            this.receiveModifySymbol(var1, var2);
            break;
         case 6:
            this.receiveSetPrivateSymbol(var1, var2);
            break;
         case 7:
            this.receiveModifySharing(var1, var2);
      }

   }

   private void receiveAddMarker(ByteBuffer var1, UdpConnection var2) {
   }

   private void receiveRemoveMarker(ByteBuffer var1, UdpConnection var2) {
   }

   private void receiveAddSymbol(ByteBuffer var1, UdpConnection var2) throws IOException {
      WorldMapSymbolNetworkInfo var3 = new WorldMapSymbolNetworkInfo();
      var3.load(var1, 219, 1);
      if (this.canClientModify(var3, var2)) {
         byte var4 = var1.get();
         Object var5;
         if (var4 == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
            var5 = new WorldMapTextSymbol(this.m_symbols);
         } else {
            if (var4 != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
               throw new IOException("unknown map symbol type " + var4);
            }

            var5 = new WorldMapTextureSymbol(this.m_symbols);
         }

         ((WorldMapBaseSymbol)var5).load(var1, 219, 1);
         var3.setID(this.m_nextElementID++);
         ((WorldMapBaseSymbol)var5).setNetworkInfo(var3);
         this.m_symbols.addSymbol((WorldMapBaseSymbol)var5);
         this.m_symbolByID.put(((WorldMapBaseSymbol)var5).getNetworkInfo().getID(), var5);
         this.addSymbolOnClient((WorldMapBaseSymbol)var5);
      }
   }

   private void receiveRemoveSymbol(ByteBuffer var1, UdpConnection var2) {
      int var3 = var1.getInt();
      WorldMapBaseSymbol var4 = (WorldMapBaseSymbol)this.m_symbolByID.get(var3);
      if (var4 != null) {
         if (this.canClientModify(var4.getNetworkInfo(), var2)) {
            this.m_symbolByID.remove(var3);
            this.m_symbols.removeSymbol(var4);
            this.removeSymbolOnClient(var3);
            var4.release();
         }
      }
   }

   private void receiveModifySymbol(ByteBuffer var1, UdpConnection var2) throws IOException {
      int var3 = var1.getInt();
      WorldMapBaseSymbol var4 = (WorldMapBaseSymbol)this.m_symbolByID.get(var3);
      if (var4 != null) {
         if (this.canClientModify(var4.getNetworkInfo(), var2)) {
            var4.load(var1, 219, 1);
            this.modifySymbolOnClient(var4);
         }
      }
   }

   private void receiveSetPrivateSymbol(ByteBuffer var1, UdpConnection var2) throws IOException {
      int var3 = var1.getInt();
      WorldMapBaseSymbol var4 = (WorldMapBaseSymbol)this.m_symbolByID.get(var3);
      if (var4 != null) {
         if (this.canClientModify(var4.getNetworkInfo(), var2)) {
            this.setPrivateSymbolOnClient(var4);
            this.m_symbolByID.remove(var3);
            this.m_symbols.removeSymbol(var4);
            var4.release();
         }
      }
   }

   private void receiveModifySharing(ByteBuffer var1, UdpConnection var2) throws IOException {
      int var3 = var1.getInt();
      WorldMapBaseSymbol var4 = (WorldMapBaseSymbol)this.m_symbolByID.get(var3);
      if (var4 != null) {
         if (this.canClientModify(var4.getNetworkInfo(), var2)) {
            var4.getNetworkInfo().load(var1, 219, 1);
            this.modifySymbolOnClient(var4);
         }
      }
   }

   private boolean canClientModify(WorldMapSymbolNetworkInfo var1, UdpConnection var2) {
      return StringUtils.equals(var1.getAuthor(), var2.username);
   }

   private void sendPacket(ByteBuffer var1) {
      int var2;
      ByteBufferWriter var4;
      if (GameServer.bServer) {
         for(var2 = 0; var2 < GameServer.udpEngine.connections.size(); ++var2) {
            UdpConnection var3 = (UdpConnection)GameServer.udpEngine.connections.get(var2);
            var4 = var3.startPacket();
            var1.flip();
            var4.bb.put(var1);
            var3.endPacketImmediate();
         }
      } else {
         if (GameClient.bClient) {
            throw new IllegalStateException("can't call this method on the client");
         }

         for(var2 = 0; var2 < SinglePlayerServer.udpEngine.connections.size(); ++var2) {
            zombie.spnetwork.UdpConnection var5 = (zombie.spnetwork.UdpConnection)SinglePlayerServer.udpEngine.connections.get(var2);
            var4 = var5.startPacket();
            var1.flip();
            var4.bb.put(var1);
            var5.endPacketImmediate();
         }
      }

   }

   public void addMarkerOnClient(WorldMapBaseSymbol var1) throws IOException {
      this.BYTE_BUFFER.clear();
      ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
      PacketTypes.PacketType.WorldMap.doPacket(var2);
      var2.putByte((byte)1);
      this.sendPacket(this.BYTE_BUFFER);
   }

   public void removeMarkerOnClient(int var1) {
      this.BYTE_BUFFER.clear();
      ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
      PacketTypes.PacketType.WorldMap.doPacket(var2);
      var2.putByte((byte)2);
      this.sendPacket(this.BYTE_BUFFER);
   }

   public void addSymbolOnClient(WorldMapBaseSymbol var1) throws IOException {
      this.BYTE_BUFFER.clear();
      ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
      PacketTypes.PacketType.WorldMap.doPacket(var2);
      var2.putByte((byte)3);
      var1.getNetworkInfo().save(var2.bb);
      var2.putByte((byte)var1.getType().index());
      var1.save(var2.bb);
      this.sendPacket(this.BYTE_BUFFER);
   }

   public void removeSymbolOnClient(int var1) {
      this.BYTE_BUFFER.clear();
      ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
      PacketTypes.PacketType.WorldMap.doPacket(var2);
      var2.putByte((byte)4);
      var2.putInt(var1);
      this.sendPacket(this.BYTE_BUFFER);
   }

   public void modifySymbolOnClient(WorldMapBaseSymbol var1) throws IOException {
      this.BYTE_BUFFER.clear();
      ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
      PacketTypes.PacketType.WorldMap.doPacket(var2);
      var2.putByte((byte)5);
      var2.putInt(var1.getNetworkInfo().getID());
      var1.getNetworkInfo().save(var2.bb);
      var1.save(var2.bb);
      this.sendPacket(this.BYTE_BUFFER);
   }

   public void setPrivateSymbolOnClient(WorldMapBaseSymbol var1) throws IOException {
      this.BYTE_BUFFER.clear();
      ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
      PacketTypes.PacketType.WorldMap.doPacket(var2);
      var2.putByte((byte)6);
      var2.putInt(var1.getNetworkInfo().getID());
      this.sendPacket(this.BYTE_BUFFER);
   }

   public void sendRequestData(ByteBuffer var1) throws IOException {
      var1.putInt(this.m_symbols.getSymbolCount());

      for(int var2 = 0; var2 < this.m_symbols.getSymbolCount(); ++var2) {
         WorldMapBaseSymbol var3 = this.m_symbols.getSymbolByIndex(var2);
         var3.getNetworkInfo().save(var1);
         var1.put((byte)var3.getType().index());
         var3.save(var1);
      }

   }

   public void writeSavefile() {
      try {
         ByteBuffer var1 = SliceY.SliceBuffer;
         var1.clear();
         var1.put(FILE_MAGIC);
         var1.putInt(219);
         var1.putInt(1);
         this.writeSavefile(var1);
         File var2 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("servermap_symbols.bin"));
         FileOutputStream var3 = new FileOutputStream(var2);

         try {
            BufferedOutputStream var4 = new BufferedOutputStream(var3);

            try {
               var4.write(var1.array(), 0, var1.position());
            } catch (Throwable var9) {
               try {
                  var4.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            var4.close();
         } catch (Throwable var10) {
            try {
               var3.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }

            throw var10;
         }

         var3.close();
      } catch (Exception var11) {
         ExceptionLogger.logException(var11);
      }

   }

   private void writeSavefile(ByteBuffer var1) throws IOException {
      var1.putInt(this.m_symbols.getSymbolCount());

      for(int var2 = 0; var2 < this.m_symbols.getSymbolCount(); ++var2) {
         WorldMapBaseSymbol var3 = this.m_symbols.getSymbolByIndex(var2);
         var3.getNetworkInfo().save(var1);
         var1.put((byte)var3.getType().index());
         var3.save(var1);
      }

   }

   public void readSavefile() {
      File var1 = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("servermap_symbols.bin"));

      try {
         FileInputStream var2 = new FileInputStream(var1);

         try {
            BufferedInputStream var3 = new BufferedInputStream(var2);

            try {
               ByteBuffer var4 = SliceY.SliceBuffer;
               var4.clear();
               int var5 = var3.read(var4.array());
               var4.limit(var5);
               byte[] var6 = new byte[4];
               var4.get(var6);
               if (!Arrays.equals(var6, FILE_MAGIC)) {
                  throw new IOException(var1.getAbsolutePath() + " does not appear to be servermap_symbols.bin");
               }

               int var7 = var4.getInt();
               int var8 = var4.getInt();
               this.readSavefile(var4, var7, var8);
            } catch (Throwable var11) {
               try {
                  var3.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            var3.close();
         } catch (Throwable var12) {
            try {
               var2.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }

            throw var12;
         }

         var2.close();
      } catch (FileNotFoundException var13) {
      } catch (Exception var14) {
         ExceptionLogger.logException(var14);
      }

   }

   private void readSavefile(ByteBuffer var1, int var2, int var3) throws IOException {
      int var4 = var1.getInt();

      for(int var5 = 0; var5 < var4; ++var5) {
         WorldMapSymbolNetworkInfo var6 = new WorldMapSymbolNetworkInfo();
         var6.load(var1, var2, var3);
         byte var7 = var1.get();
         Object var8;
         if (var7 == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
            var8 = new WorldMapTextSymbol(this.m_symbols);
         } else {
            if (var7 != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
               throw new IOException("unknown map symbol type " + var7);
            }

            var8 = new WorldMapTextureSymbol(this.m_symbols);
         }

         ((WorldMapBaseSymbol)var8).load(var1, var2, var3);
         var6.setID(this.m_nextElementID++);
         ((WorldMapBaseSymbol)var8).setNetworkInfo(var6);
         this.m_symbols.addSymbol((WorldMapBaseSymbol)var8);
         this.m_symbolByID.put(((WorldMapBaseSymbol)var8).getNetworkInfo().getID(), var8);
      }

   }
}
