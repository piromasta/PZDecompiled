package zombie.characters;

import java.nio.ByteBuffer;
import zombie.core.math.PZMath;
import zombie.iso.areas.NonPvpZone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

public class Safety {
   protected boolean enabled;
   protected boolean last;
   protected float cooldown;
   protected float toggle;
   protected IsoGameCharacter character;

   public Safety() {
   }

   public Safety(IsoGameCharacter var1) {
      this.character = var1;
      this.enabled = true;
      this.last = true;
      this.cooldown = 0.0F;
      this.toggle = 0.0F;
   }

   public void copyFrom(Safety var1) {
      this.enabled = var1.enabled;
      this.last = var1.last;
      this.cooldown = var1.cooldown;
      this.toggle = var1.toggle;
   }

   public Object getCharacter() {
      return this.character;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean var1) {
      this.enabled = var1;
   }

   public boolean isLast() {
      return this.last;
   }

   public void setLast(boolean var1) {
      this.last = var1;
   }

   public float getCooldown() {
      return this.cooldown;
   }

   public void setCooldown(float var1) {
      this.cooldown = var1;
   }

   public float getToggle() {
      return this.toggle;
   }

   public void setToggle(float var1) {
      this.toggle = var1;
   }

   public boolean isToggleAllowed() {
      return ServerOptions.getInstance().PVP.getValue() && NonPvpZone.getNonPvpZone(PZMath.fastfloor(this.character.getX()), PZMath.fastfloor(this.character.getY())) == null && (!ServerOptions.getInstance().SafetySystem.getValue() || this.getCooldown() == 0.0F && this.getToggle() == 0.0F);
   }

   public void toggleSafety() {
      if (this.isToggleAllowed()) {
         if (GameClient.bClient) {
            GameClient.sendChangeSafety(this);
         } else {
            this.setToggle((float)ServerOptions.getInstance().SafetyToggleTimer.getValue());
            this.setLast(this.isEnabled());
            if (this.isEnabled()) {
               this.setEnabled(!this.isEnabled());
            }

            if (GameServer.bServer) {
               GameServer.sendChangeSafety(this);
            }
         }
      }

   }

   public void load(ByteBuffer var1, int var2) {
      this.enabled = var1.get() != 0;
      this.last = var1.get() != 0;
      this.cooldown = var1.getFloat();
      this.toggle = var1.getFloat();
   }

   public void save(ByteBuffer var1) {
      var1.put((byte)(this.enabled ? 1 : 0));
      var1.put((byte)(this.last ? 1 : 0));
      var1.putFloat(this.cooldown);
      var1.putFloat(this.toggle);
   }

   public String getDescription() {
      return "enabled=" + this.enabled + " last=" + this.last + " cooldown=" + this.cooldown + " toggle=" + this.toggle;
   }
}
