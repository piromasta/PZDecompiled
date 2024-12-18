package zombie.worldMap.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.types.MapItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.spnetwork.SinglePlayerClient;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

public final class WorldMapClient {
   public static final WorldMapClient instance = new WorldMapClient();
   private final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(1048576);
   private final ByteBufferWriter BYTE_BUFFER_WRITER;
   private final TIntObjectHashMap<WorldMapBaseSymbol> m_symbolByID;
   private boolean m_bReceivedRequestData;
   private boolean m_bWorldMapLoaded;

   public WorldMapClient() {
      this.BYTE_BUFFER_WRITER = new ByteBufferWriter(this.BYTE_BUFFER);
      this.m_symbolByID = new TIntObjectHashMap();
      this.m_bReceivedRequestData = false;
      this.m_bWorldMapLoaded = false;
   }

   public static WorldMapClient getInstance() {
      return instance;
   }

   public void receive(ByteBuffer var1) throws IOException {
      byte var2 = var1.get();
      switch (var2) {
         case 1:
            this.receiveAddMarker(var1);
            break;
         case 2:
            this.receiveRemoveMarker(var1);
            break;
         case 3:
            this.receiveAddSymbol(var1);
            break;
         case 4:
            this.receiveRemoveSymbol(var1);
            break;
         case 5:
            this.receiveModifySymbol(var1);
            break;
         case 6:
            this.receiveSetPrivateSymbol(var1);
            break;
         case 7:
            this.receiveModifySharing(var1);
      }

   }

   private void receiveAddMarker(ByteBuffer var1) throws IOException {
   }

   private void receiveRemoveMarker(ByteBuffer var1) throws IOException {
   }

   private void receiveAddSymbol(ByteBuffer var1) throws IOException {
      if (this.m_bReceivedRequestData) {
         WorldMapSymbolNetworkInfo var2 = new WorldMapSymbolNetworkInfo();
         var2.load(var1, 219, 1);
         byte var3 = var1.get();
         Object var4;
         if (var3 == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
            var4 = new WorldMapTextSymbol(MapItem.getSingleton().getSymbols());
         } else {
            if (var3 != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
               throw new IOException("unknown map symbol type " + var3);
            }

            var4 = new WorldMapTextureSymbol(MapItem.getSingleton().getSymbols());
         }

         ((WorldMapBaseSymbol)var4).load(var1, 219, 1);
         ((WorldMapBaseSymbol)var4).setNetworkInfo(var2);
         this.m_symbolByID.put(((WorldMapBaseSymbol)var4).getNetworkInfo().getID(), var4);
         if (this.m_bWorldMapLoaded) {
            MapItem.getSingleton().getSymbols().addSymbol((WorldMapBaseSymbol)var4);
         }

      }
   }

   private void receiveRemoveSymbol(ByteBuffer var1) throws IOException {
      if (this.m_bReceivedRequestData) {
         int var2 = var1.getInt();
         WorldMapBaseSymbol var3 = (WorldMapBaseSymbol)this.m_symbolByID.remove(var2);
         if (var3 != null) {
            if (this.m_bWorldMapLoaded) {
               MapItem.getSingleton().getSymbols().removeSymbol(var3);
            }

         }
      }
   }

   private void receiveModifySymbol(ByteBuffer var1) throws IOException {
      if (this.m_bReceivedRequestData) {
         int var2 = var1.getInt();
         WorldMapBaseSymbol var3 = (WorldMapBaseSymbol)this.m_symbolByID.get(var2);
         if (var3 != null) {
            var3.getNetworkInfo().load(var1, 219, 1);
            var3.load(var1, 219, 1);
            if (this.m_bWorldMapLoaded) {
               MapItem.getSingleton().getSymbols().invalidateLayout();
            }

         }
      }
   }

   private void receiveSetPrivateSymbol(ByteBuffer var1) throws IOException {
      if (this.m_bReceivedRequestData) {
         int var2 = var1.getInt();
         WorldMapBaseSymbol var3 = (WorldMapBaseSymbol)this.m_symbolByID.get(var2);
         if (var3 != null) {
            this.m_symbolByID.remove(var2);
            boolean var4 = var3.isAuthorLocalPlayer();
            var3.setPrivate();
            if (this.m_bWorldMapLoaded && !var4) {
               MapItem.getSingleton().getSymbols().removeSymbol(var3);
            }

         }
      }
   }

   private void receiveModifySharing(ByteBuffer var1) throws IOException {
      if (this.m_bReceivedRequestData) {
         int var2 = var1.getInt();
         WorldMapBaseSymbol var3 = (WorldMapBaseSymbol)this.m_symbolByID.get(var2);
         if (var3 != null) {
            var3.getNetworkInfo().load(var1, 219, 1);
         }
      }
   }

   public void receiveRequestData(ByteBuffer var1) throws IOException {
      this.m_bReceivedRequestData = true;
      int var2 = var1.getInt();

      for(int var3 = 0; var3 < var2; ++var3) {
         WorldMapSymbolNetworkInfo var4 = new WorldMapSymbolNetworkInfo();
         var4.load(var1, 219, 1);
         byte var5 = var1.get();
         Object var6;
         if (var5 == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
            var6 = new WorldMapTextSymbol(MapItem.getSingleton().getSymbols());
         } else {
            if (var5 != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
               throw new IOException("unknown map symbol type " + var5);
            }

            var6 = new WorldMapTextureSymbol(MapItem.getSingleton().getSymbols());
         }

         ((WorldMapBaseSymbol)var6).load(var1, 219, 1);
         ((WorldMapBaseSymbol)var6).setNetworkInfo(var4);
         this.m_symbolByID.put(((WorldMapBaseSymbol)var6).getNetworkInfo().getID(), var6);
      }

   }

   public void worldMapLoaded() {
      this.m_bWorldMapLoaded = true;
      WorldMapSymbols var1 = MapItem.getSingleton().getSymbols();
      this.m_symbolByID.forEachValue((var1x) -> {
         var1.addSymbol(var1x);
         return true;
      });
   }

   private void sendPacket(ByteBuffer var1) {
      if (GameServer.bServer) {
         throw new IllegalStateException("can't call this method on the server");
      } else {
         ByteBufferWriter var2;
         if (GameClient.bClient) {
            var2 = GameClient.connection.startPacket();
            var1.flip();
            var2.bb.put(var1);
            PacketTypes.PacketType.WorldMap.send(GameClient.connection);
         } else {
            var2 = SinglePlayerClient.connection.startPacket();
            var1.flip();
            var2.bb.put(var1);
            SinglePlayerClient.connection.endPacketImmediate();
         }

      }
   }

   public void sendShareSymbol(WorldMapBaseSymbol var1, WorldMapSymbolNetworkInfo var2) {
      if (var1.isShared()) {
         if (!var1.getNetworkInfo().equals(var2)) {
            var2.setID(var1.getNetworkInfo().getID());
            this.BYTE_BUFFER.clear();
            ByteBufferWriter var3 = this.BYTE_BUFFER_WRITER;
            PacketTypes.PacketType.WorldMap.doPacket(var3);
            var3.putByte((byte)7);

            try {
               var3.putInt(var1.getNetworkInfo().getID());
               var2.save(var3.bb);
               this.sendPacket(this.BYTE_BUFFER);
            } catch (IOException var5) {
               ExceptionLogger.logException(var5);
            }

         }
      } else {
         this.sendAddSymbol(var1, var2);
      }
   }

   public void sendAddSymbol(WorldMapBaseSymbol var1, WorldMapSymbolNetworkInfo var2) {
      if (!var1.isShared()) {
         this.BYTE_BUFFER.clear();
         ByteBufferWriter var3 = this.BYTE_BUFFER_WRITER;
         PacketTypes.PacketType.WorldMap.doPacket(var3);
         var3.putByte((byte)3);

         try {
            var2.save(var3.bb);
            var3.putByte((byte)var1.getType().index());
            var1.save(var3.bb);
            this.sendPacket(this.BYTE_BUFFER);
            MapItem.getSingleton().getSymbols().removeSymbol(var1);
         } catch (IOException var5) {
            ExceptionLogger.logException(var5);
         }

      }
   }

   public void sendModifySymbol(WorldMapBaseSymbol var1) {
      if (!var1.isPrivate()) {
         this.BYTE_BUFFER.clear();
         ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
         PacketTypes.PacketType.WorldMap.doPacket(var2);
         var2.putByte((byte)5);

         try {
            var2.putInt(var1.getNetworkInfo().getID());
            var1.save(var2.bb);
            this.sendPacket(this.BYTE_BUFFER);
         } catch (IOException var4) {
            ExceptionLogger.logException(var4);
         }

      }
   }

   public void sendSetPrivateSymbol(WorldMapBaseSymbol var1) {
      if (!var1.isPrivate()) {
         this.BYTE_BUFFER.clear();
         ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
         PacketTypes.PacketType.WorldMap.doPacket(var2);
         var2.putByte((byte)6);
         var2.putInt(var1.getNetworkInfo().getID());
         this.sendPacket(this.BYTE_BUFFER);
      }
   }

   public void sendRemoveSymbol(WorldMapBaseSymbol var1) {
      if (!var1.isPrivate()) {
         this.BYTE_BUFFER.clear();
         ByteBufferWriter var2 = this.BYTE_BUFFER_WRITER;
         PacketTypes.PacketType.WorldMap.doPacket(var2);
         var2.putByte((byte)4);
         var2.putInt(var1.getNetworkInfo().getID());
         this.sendPacket(this.BYTE_BUFFER);
      }
   }

   public void Reset() {
      this.m_bReceivedRequestData = false;
      this.m_bWorldMapLoaded = false;
      this.m_symbolByID.forEachValue((var0) -> {
         var0.release();
         return true;
      });
      this.m_symbolByID.clear();
   }
}
