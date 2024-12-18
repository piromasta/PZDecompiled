package zombie.characters;

import zombie.core.math.PZMath;

public final class MoveDeltaModifiers {
   public float turnDelta = -1.0F;
   public float moveDelta = -1.0F;
   public float twistDelta = -1.0F;

   public MoveDeltaModifiers() {
   }

   public float getTurnDelta() {
      return this.turnDelta;
   }

   public float getMoveDelta() {
      return this.moveDelta;
   }

   public float getTwistDelta() {
      return this.twistDelta;
   }

   public void setTurnDelta(float var1) {
      this.turnDelta = var1;
   }

   public void setMoveDelta(float var1) {
      this.moveDelta = var1;
   }

   public void setTwistDelta(float var1) {
      this.twistDelta = var1;
   }

   public void setMaxTurnDelta(float var1) {
      this.turnDelta = PZMath.max(this.turnDelta, var1);
   }

   public void setMaxMoveDelta(float var1) {
      this.moveDelta = PZMath.max(this.moveDelta, var1);
   }

   public void setMaxTwistDelta(float var1) {
      this.twistDelta = PZMath.max(this.twistDelta, var1);
   }
}
