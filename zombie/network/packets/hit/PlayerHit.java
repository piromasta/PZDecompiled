package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoLivingCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.Player;
import zombie.network.fields.Weapon;

public abstract class PlayerHit implements HitCharacter {
   @JSONField
   public final Player wielder = new Player();
   @JSONField
   public final Weapon weapon = new Weapon();

   public PlayerHit() {
   }

   public boolean set(Object... var1) {
      return false;
   }

   public void parse(ByteBuffer var1, UdpConnection var2) {
      this.wielder.parse(var1, var2);
      this.wielder.parsePlayer(var2);
      this.weapon.parse(var1, var2, (IsoLivingCharacter)this.wielder.getCharacter());
   }

   public void write(ByteBufferWriter var1) {
      this.wielder.write(var1);
      this.weapon.write(var1);
   }

   public boolean isRelevant(UdpConnection var1) {
      return this.wielder.isRelevant(var1);
   }

   public boolean isConsistent(UdpConnection var1) {
      return this.weapon.isConsistent(var1) && this.wielder.isConsistent(var1);
   }

   public void preProcess() {
      this.wielder.process();
   }

   public void postProcess() {
      this.wielder.process();
   }

   public void attack() {
      this.wielder.attack(this.weapon.getWeapon(), false);
   }
}
