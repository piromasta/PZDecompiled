package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class Perk implements INetworkPacket {
   protected PerkFactory.Perk perk;
   protected byte perkIndex;

   public Perk() {
   }

   public void set(PerkFactory.Perk var1) {
      this.perk = var1;
      if (this.perk == null) {
         this.perkIndex = -1;
      } else {
         this.perkIndex = (byte)this.perk.index();
      }
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.perkIndex = var1.get();
      if (this.perkIndex >= 0 && this.perkIndex <= PerkFactory.Perks.getMaxIndex()) {
         this.perk = PerkFactory.Perks.fromIndex(this.perkIndex);
      }

   }

   public void write(ByteBufferWriter var1) {
      var1.putByte(this.perkIndex);
   }

   public String getDescription() {
      String var10000 = this.getClass().getSimpleName();
      return "\n\t" + var10000 + " [ perk=( " + this.perkIndex + " )" + (this.perk == null ? "null" : this.perk.name) + " ]";
   }

   public boolean isConsistent() {
      return this.perk != null;
   }

   public PerkFactory.Perk getPerk() {
      return this.perk;
   }
}
