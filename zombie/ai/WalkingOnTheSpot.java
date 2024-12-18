package zombie.ai;

import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.ui.SpeedControls;

public final class WalkingOnTheSpot {
   private float x;
   private float y;
   private float time;

   public WalkingOnTheSpot() {
   }

   public boolean check(IsoGameCharacter var1) {
      float var2 = 400.0F;
      if (var1.isAnimal()) {
         var2 = 30.0F;
         if (!GameServer.bServer && SpeedControls.instance.getCurrentGameSpeed() == 4) {
            var2 = 150.0F;
         }
      }

      if (IsoUtils.DistanceToSquared(this.x, this.y, var1.getX(), var1.getY()) < 0.010000001F) {
         this.time += GameTime.getInstance().getMultiplier();
      } else {
         this.x = var1.getX();
         this.y = var1.getY();
         this.time = 0.0F;
      }

      return this.time > var2;
   }

   public boolean check(float var1, float var2) {
      if (IsoUtils.DistanceToSquared(this.x, this.y, var1, var2) < 0.010000001F) {
         this.time += GameTime.getInstance().getMultiplier();
      } else {
         this.x = var1;
         this.y = var2;
         this.time = 0.0F;
      }

      return this.time > 100.0F;
   }

   public void reset(float var1, float var2) {
      this.x = var1;
      this.y = var2;
      this.time = 0.0F;
   }
}
