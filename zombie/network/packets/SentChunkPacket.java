package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.ClientChunkRequest;
import zombie.network.PacketSetting;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 2,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 0
)
public class SentChunkPacket implements INetworkPacket {
   static final int chunkSize = 1000;
   int requestNumber;
   int fileSize;
   int numChunks;
   int bytesSent;
   int chunkIndex;
   int bytesToSend;
   private static byte[] inMemoryZip = new byte[20480];
   private static final Deflater compressor = new Deflater();

   public SentChunkPacket() {
   }

   public void setChunk(ClientChunkRequest.Chunk var1) {
   }

   public boolean hasData() {
      return true;
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   private int compressChunk(ClientChunkRequest.Chunk var1) {
      return 0;
   }
}
