package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Character extends OptionGroup {
   public final BooleanDebugOption CreateAllOutfits = this.newOption("Create.AllOutfits", false);
   public final DebugOG Debug = (DebugOG)this.newOptionGroup(new DebugOG());

   public Character() {
   }

   public static final class DebugOG extends OptionGroup {
      public final RenderOG Render = (RenderOG)this.newOptionGroup(new RenderOG());
      public final AnimateOG Animate = (AnimateOG)this.newOptionGroup(new AnimateOG());
      public final BooleanDebugOption RegisterDebugVariables = this.newDebugOnlyOption("DebugVariables", false);
      public final BooleanDebugOption AlwaysTripOverFence = this.newDebugOnlyOption("AlwaysTripOverFence", false);
      public final BooleanDebugOption PlaySoundWhenInvisible = this.newDebugOnlyOption("PlaySoundWhenInvisible", false);
      public final BooleanDebugOption UpdateAlpha = this.newDebugOnlyOption("UpdateAlpha", true);
      public final BooleanDebugOption UpdateAlphaEighthSpeed = this.newDebugOnlyOption("UpdateAlphaEighthSpeed", false);
      public final BooleanDebugOption AlwaysHitTarget = this.newDebugOnlyOption("AlwaysHitTarget", false);

      public DebugOG() {
      }

      public static final class RenderOG extends OptionGroup {
         public final BooleanDebugOption AimCone = this.newDebugOnlyOption("AimCone", false);
         public final BooleanDebugOption Angle = this.newDebugOnlyOption("Angle", false);
         public final BooleanDebugOption TestDotSide = this.newDebugOnlyOption("TestDotSide", false);
         public final BooleanDebugOption DeferredMovement = this.newDebugOnlyOption("DeferredMovement", false);
         public final BooleanDebugOption DeferredAngles = this.newDebugOnlyOption("DeferredRotation", false);
         public final BooleanDebugOption TranslationData = this.newDebugOnlyOption("Translation_Data", false);
         public final BooleanDebugOption Bip01 = this.newDebugOnlyOption("Bip01", false);
         public final BooleanDebugOption PrimaryHandBone = this.newDebugOnlyOption("HandBones.Primary", false);
         public final BooleanDebugOption SecondaryHandBone = this.newDebugOnlyOption("HandBones.Secondary", false);
         public final BooleanDebugOption SkipCharacters = this.newDebugOnlyOption("SkipCharacters", false);
         public final BooleanDebugOption Vision = this.newDebugOnlyOption("Vision", false);
         public final BooleanDebugOption DisplayRoomAndZombiesZone = this.newDebugOnlyOption("DisplayRoomAndZombiesZone", false);
         public final BooleanDebugOption FMODRoomType = this.newDebugOnlyOption("FMODRoomType", false);
         public final BooleanDebugOption CarStopDebug = this.newDebugOnlyOption("CarStopDebug", false);
         public final BooleanDebugOption MeleeOutline = this.newDebugOnlyOption("MeleeOutline", false);
         public final BooleanDebugOption AimVector = this.newDebugOnlyOption("AimVector", false);

         public RenderOG() {
         }
      }

      public static final class AnimateOG extends OptionGroup {
         public final BooleanDebugOption DeferredRotationOnly = this.newDebugOnlyOption("DeferredRotationsOnly", false);
         public final BooleanDebugOption NoBoneMasks = this.newDebugOnlyOption("NoBoneMasks", false);
         public final BooleanDebugOption NoBoneTwists = this.newDebugOnlyOption("NoBoneTwists", false);
         public final BooleanDebugOption AlwaysAimTwist = this.newDebugOnlyOption("AlwaysAimTwist", false);
         public final BooleanDebugOption ZeroCounterRotationBone = this.newDebugOnlyOption("ZeroCounterRotation", false);
         public final BooleanDebugOption KeepAtOrigin = this.newDebugOnlyOption("KeepAtOrigin", true);

         public AnimateOG() {
         }
      }
   }
}
