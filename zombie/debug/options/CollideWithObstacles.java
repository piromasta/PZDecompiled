package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class CollideWithObstacles extends OptionGroup {
   public final RenderOG Render = (RenderOG)this.newOptionGroup(new RenderOG());

   public CollideWithObstacles() {
   }

   public static final class RenderOG extends OptionGroup {
      public final BooleanDebugOption Radius = this.newDebugOnlyOption("Radius", false);
      public final BooleanDebugOption Obstacles = this.newDebugOnlyOption("Obstacles", false);
      public final BooleanDebugOption Normals = this.newDebugOnlyOption("Normals", false);

      public RenderOG() {
      }
   }
}
