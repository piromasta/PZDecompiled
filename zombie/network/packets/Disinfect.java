package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.network.GameClient;
import zombie.network.PacketValidator;
import zombie.network.packets.hit.Player;
import zombie.network.packets.hit.PlayerBodyPart;
import zombie.network.packets.hit.PlayerItem;

public class Disinfect implements INetworkPacket {
   protected final Player wielder = new Player();
   protected final Player target = new Player();
   protected PlayerBodyPart bodyPart = new PlayerBodyPart();
   protected PlayerItem alcohol = new PlayerItem();

   public Disinfect() {
   }

   public void set(IsoGameCharacter var1, IsoGameCharacter var2, BodyPart var3, InventoryItem var4) {
      this.wielder.set(var1);
      this.target.set(var2);
      this.bodyPart.set(var3);
      this.alcohol.set(var4);
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.wielder.parse(var1, var2);
      this.wielder.parsePlayer(var2);
      this.target.parse(var1, var2);
      this.target.parsePlayer((UdpConnection)null);
      this.bodyPart.parse(var1, this.target.getCharacter());
      this.alcohol.parse(var1, var2);
   }

   public void write(ByteBufferWriter var1) {
      this.wielder.write(var1);
      this.target.write(var1);
      this.bodyPart.write(var1);
      this.alcohol.write(var1);
   }

   public void process() {
      int var1 = this.wielder.getCharacter().getPerkLevel(PerkFactory.Perks.Doctor);
      if (!this.wielder.getPlayer().isAccessLevel("None")) {
         var1 = 10;
      }

      this.bodyPart.getBodyPart().setAlcoholLevel(this.bodyPart.getBodyPart().getAlcoholLevel() + this.alcohol.getItem().getAlcoholPower());
      float var2 = this.alcohol.getItem().getAlcoholPower() * 13.0F - (float)(var1 / 2);
      this.bodyPart.getBodyPart().setAdditionalPain(this.bodyPart.getBodyPart().getAdditionalPain() + var2);
      if (this.alcohol.getItem() instanceof Food) {
         Food var3 = (Food)this.alcohol.getItem();
         var3.setThirstChange(var3.getThirstChange() + 0.1F);
         if (var3.getBaseHunger() < 0.0F) {
            var3.setHungChange(var3.getHungChange() + 0.1F);
         }
      }

      if (!((double)this.alcohol.getItem().getScriptItem().getThirstChange() > -0.01) && !((double)this.alcohol.getItem().getScriptItem().getHungerChange() > -0.01)) {
         if (this.alcohol.getItem() instanceof DrainableComboItem) {
            this.alcohol.getItem().Use();
         }
      } else {
         this.alcohol.getItem().Use();
      }

   }

   public boolean isConsistent() {
      return this.wielder.getCharacter() != null && this.wielder.getCharacter() instanceof IsoPlayer && this.target.getCharacter() != null && this.target.getCharacter() instanceof IsoPlayer && this.bodyPart.getBodyPart() != null && this.alcohol.getItem() != null;
   }

   public boolean validate(UdpConnection var1) {
      if (GameClient.bClient && this.alcohol.getItem().getAlcoholPower() <= 0.0F) {
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
      var1 = var1 + "alcohol=" + this.alcohol.getDescription() + "] ";
      return var1;
   }
}
