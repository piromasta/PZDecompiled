package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
   ordering = 0,
   priority = 1,
   reliability = 3,
   requiredCapability = Capability.LoginOnServer,
   handlingType = 7
)
public class ChecksumPacket implements INetworkPacket {
   private static final short PacketTotalChecksum = 1;
   private static final short PacketGroupChecksum = 2;
   private static final short PacketFileChecksums = 3;
   private static final short PacketError = 4;
   private static final byte FileDifferent = 1;
   private static final byte FileNotOnServer = 2;
   private static final byte FileNotOnClient = 3;
   short pkt;
   boolean okLua;
   boolean okScript;
   boolean match;
   short groupIndex;
   String relPath;
   byte reason;
   private final byte[] checksum = new byte[64];

   public ChecksumPacket() {
   }

   public void setPacketTotalChecksum() {
   }

   private void setPacketGroupChecksum(short var1, boolean var2) {
   }

   private void setPacketGroupChecksum(short var1) {
   }

   private void setPacketFileChecksums(short var1, String var2, byte var3) {
   }

   private void setPacketFileChecksums() {
   }

   private void setPacketError(String var1) {
   }

   public void write(ByteBufferWriter var1) {
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
   }

   public void parseServer(ByteBuffer var1, UdpConnection var2) {
   }

   private String getReason(byte var1) {
      return "";
   }

   private void sendFileMismatch(UdpConnection var1, short var2, String var3, byte var4) {
   }

   public static void sendTotalChecksum() {
   }

   public static void sendError(UdpConnection var0, String var1) {
   }

   private boolean checksumEquals(byte[] var1) {
      return true;
   }

   private void sendGroupChecksum() {
   }

   private void sendFileChecksums() {
   }
}
