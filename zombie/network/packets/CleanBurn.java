package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.PacketValidator;
import zombie.network.packets.hit.Player;
import zombie.network.packets.hit.PlayerBodyPart;
import zombie.network.packets.hit.PlayerItem;

public class CleanBurn implements INetworkPacket {
   protected final Player wielder = new Player();
   protected final Player target = new Player();
   protected PlayerBodyPart bodyPart = new PlayerBodyPart();
   protected PlayerItem bandage = new PlayerItem();

   public CleanBurn() {
   }

   public void set(IsoGameCharacter var1, IsoGameCharacter var2, BodyPart var3, InventoryItem var4) {
      this.wielder.set(var1);
      this.target.set(var2);
      this.bodyPart.set(var3);
      this.bandage.set(var4);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.wielder.parse(var1, var2);
      this.wielder.parsePlayer(var2);
      this.target.parse(var1, var2);
      this.target.parsePlayer((UdpConnection)null);
      this.bodyPart.parse(var1, this.target.getCharacter());
      this.bandage.parse(var1, var2);
   }

   public void write(ByteBufferWriter var1) {
      this.wielder.write(var1);
      this.target.write(var1);
      this.bodyPart.write(var1);
      this.bandage.write(var1);
   }

   public void process() {
      int var1 = this.wielder.getCharacter().getPerkLevel(PerkFactory.Perks.Doctor);
      if (!this.wielder.getPlayer().isAccessLevel("None")) {
         var1 = 10;
      }

      if (this.wielder.getCharacter().HasTrait("Hemophobic")) {
         this.wielder.getCharacter().getStats().setPanic(this.wielder.getCharacter().getStats().getPanic() + 50.0F);
      }

      this.wielder.getCharacter().getXp().AddXP(PerkFactory.Perks.Doctor, 10.0F);
      int var2 = 60 - var1 * 1;
      this.bodyPart.getBodyPart().setAdditionalPain(this.bodyPart.getBodyPart().getAdditionalPain() + (float)var2);
      this.bodyPart.getBodyPart().setNeedBurnWash(false);
      this.bandage.getItem().Use();
   }

   public boolean isConsistent() {
      return this.wielder.getCharacter() != null && this.target.getCharacter() != null && this.bodyPart.getBodyPart() != null && this.bandage.getItem() != null;
   }

   public boolean validate(UdpConnection var1) {
      if (GameClient.bClient && !this.bodyPart.getBodyPart().isNeedBurnWash()) {
         DebugLogStream var10000 = DebugLog.General;
         String var10001 = this.getClass().getSimpleName();
         var10000.warn(var10001 + ": Validate error: " + this.getDescription());
         return false;
      } else {
         return PacketValidator.checkShortDistance(var1, this.wielder, this.target, this.getClass().getSimpleName());
      }
   }

   public String getDescription() {
      String var1 = "\n\t" + this.getClass().getSimpleName() + " [";
      var1 = var1 + "wielder=" + this.wielder.getDescription() + " | ";
      var1 = var1 + "target=" + this.target.getDescription() + " | ";
      var1 = var1 + "bodyPart=" + this.bodyPart.getDescription() + " | ";
      var1 = var1 + "bandage=" + this.bandage + "] ";
      return var1;
   }
}
