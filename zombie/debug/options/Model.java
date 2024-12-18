package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Model extends OptionGroup {
   public final RenderOG Render = (RenderOG)this.newOptionGroup(new RenderOG());
   public final BooleanDebugOption ForceSkeleton = this.newOption("Force.Skeleton", false);

   public Model() {
   }

   public static final class RenderOG extends OptionGroup {
      public final BooleanDebugOption LimitTextureSize = this.newOption("LimitTextureSize", true);
      public final BooleanDebugOption Attachments = this.newOption("Attachments", false);
      public final BooleanDebugOption Axis = this.newOption("Axis", false);
      public final BooleanDebugOption Bones = this.newOption("Bones", false);
      public final BooleanDebugOption Bounds = this.newOption("Bounds", false);
      public final BooleanDebugOption ForceAlphaOne = this.newDebugOnlyOption("ForceAlphaOne", false);
      public final BooleanDebugOption Lights = this.newOption("Lights", false);
      public final BooleanDebugOption Muzzleflash = this.newOption("Muzzleflash", false);
      public final BooleanDebugOption SkipVehicles = this.newOption("SkipVehicles", false);
      public final BooleanDebugOption WeaponHitPoint = this.newOption("WeaponHitPoint", false);
      public final BooleanDebugOption Wireframe = this.newOption("Wireframe", false);

      public RenderOG() {
      }
   }
}
