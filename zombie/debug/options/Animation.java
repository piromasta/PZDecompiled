package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Animation extends OptionGroup {
   public final AnimLayerOG AnimLayer = (AnimLayerOG)this.newOptionGroup(new AnimLayerOG());
   public final SharedSkelesOG SharedSkeles = (SharedSkelesOG)this.newOptionGroup(new SharedSkelesOG());
   public final BooleanDebugOption DancingDoors = this.newDebugOnlyOption("DancingDoors", false);
   public final BooleanDebugOption Debug = this.newDebugOnlyOption("Debug", false);
   public final BooleanDebugOption AllowEarlyTransitionOut = this.newDebugOnlyOption("AllowEarlyTransitionOut", true);
   public final BooleanDebugOption AnimRenderPicker = this.newDebugOnlyOption("Render.Picker", false);
   public final BooleanDebugOption BlendUseFbx = this.newDebugOnlyOption("BlendUseFbx", false);
   public final BooleanDebugOption DisableAnimationBlends = this.newDebugOnlyOption("DisableAnimationBlends", false);

   public Animation() {
   }

   public static final class AnimLayerOG extends OptionGroup {
      public final BooleanDebugOption LogStateChanges = this.newDebugOnlyOption("Debug.LogStateChanges", false);
      public final BooleanDebugOption AllowAnimNodeOverride = this.newDebugOnlyOption("Debug.AllowAnimNodeOverride", false);
      public final BooleanDebugOption LogNodeConditions = this.newDebugOnlyOption("Debug.LogNodeConditions", false);

      public AnimLayerOG() {
      }
   }

   public static final class SharedSkelesOG extends OptionGroup {
      public final BooleanDebugOption Enabled = this.newDebugOnlyOption("Enabled", true);
      public final BooleanDebugOption AllowLerping = this.newDebugOnlyOption("AllowLerping", true);

      public SharedSkelesOG() {
      }
   }
}
