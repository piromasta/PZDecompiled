package zombie.ai.states;

import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoDirections;
import zombie.iso.IsoObject;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ClimbThroughWindowPositioningParams extends PooledObject {
   public boolean canClimb;
   public IsoDirections climbDir;
   public IsoGameCharacter climbingCharacter;
   public IsoObject windowObject;
   public int z;
   public int startX;
   public int startY;
   public int endX;
   public int endY;
   public int oppositeX;
   public int oppositeY;
   public boolean scratch;
   public boolean isCounter;
   public boolean isFloor;
   public boolean isSheetRope;
   private static final Pool<ClimbThroughWindowPositioningParams> s_pool = new Pool(ClimbThroughWindowPositioningParams::new);

   protected ClimbThroughWindowPositioningParams() {
      this.reset();
   }

   private void reset() {
      this.canClimb = false;
      this.climbDir = IsoDirections.N;
      this.climbingCharacter = null;
      this.windowObject = null;
      this.z = 0;
      this.startX = 0;
      this.startY = 0;
      this.endX = 0;
      this.endY = 0;
      this.oppositeX = 0;
      this.oppositeY = 0;
      this.scratch = false;
      this.isCounter = false;
      this.isFloor = false;
      this.isSheetRope = false;
   }

   public void onReleased() {
      this.reset();
      super.onReleased();
   }

   public static ClimbThroughWindowPositioningParams alloc() {
      return (ClimbThroughWindowPositioningParams)s_pool.alloc();
   }

   public static void release(ClimbThroughWindowPositioningParams var0) {
      Pool.tryRelease((IPooledObject)var0);
   }
}
