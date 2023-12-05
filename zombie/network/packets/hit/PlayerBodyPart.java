package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.packets.INetworkPacket;

public class PlayerBodyPart implements INetworkPacket {
   protected byte bodyPartIndex;
   protected BodyPart bodyPart;

   public PlayerBodyPart() {
   }

   public void set(BodyPart var1) {
      if (var1 == null) {
         this.bodyPartIndex = -1;
      } else {
         this.bodyPartIndex = (byte)var1.getIndex();
      }

      this.bodyPart = var1;
   }

   public void parse(ByteBuffer var1, IsoGameCharacter var2) {
      boolean var3 = var1.get() == 1;
      if (var3) {
         this.bodyPartIndex = var1.get();
         if (var2 == null) {
            this.bodyPart = null;
         } else {
            this.bodyPart = var2.getBodyDamage().getBodyPart(BodyPartType.FromIndex(this.bodyPartIndex));
         }
      } else {
         this.bodyPart = null;
      }

   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      DebugLog.Multiplayer.error("PlayerBodyPart.parse is not implemented");
   }

   public void write(ByteBufferWriter var1) {
      if (this.bodyPart == null) {
         var1.putByte((byte)0);
      } else {
         var1.putByte((byte)1);
         var1.putByte((byte)this.bodyPart.getIndex());
      }

   }

   public String getDescription() {
      String var10000 = this.bodyPart == null ? "?" : "\"" + this.bodyPart.getType().name() + "\"";
      return "\n\tPlayerBodyPart [ Item=" + var10000 + " ]";
   }

   public BodyPart getBodyPart() {
      return this.bodyPart;
   }
}
