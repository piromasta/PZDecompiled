package zombie.iso.fboRenderChunk;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.debug.options.OptionGroup;

public final class FBORenderDebugOptions extends OptionGroup {
   public final BooleanDebugOption BulletTracers = this.newDebugOnlyOption("BulletTracers", false);
   public final BooleanDebugOption CombinedFBO = this.newDebugOnlyOption("CombinedFBO", false);
   public final BooleanDebugOption CorpsesInChunkTexture = this.newDebugOnlyOption("CorpsesInChunkTexture", false);
   public final BooleanDebugOption DepthTestAll = this.newDebugOnlyOption("DepthTestAll", true);
   public final BooleanDebugOption FixJigglyModels = this.newDebugOnlyOption("FixJigglyModels", true);
   public final BooleanDebugOption HighResChunkTextures = this.newDebugOnlyOption("HighResChunkTextures", false);
   public final BooleanDebugOption ForceAlphaAndTargetOne = this.newDebugOnlyOption("ForceAlphaAndTargetOne", false);
   public final BooleanDebugOption ForceAlphaToTarget = this.newDebugOnlyOption("ForceAlphaToTarget", false);
   public final BooleanDebugOption ForceSkyLightLevel = this.newDebugOnlyOption("ForceSkyLightLevel", false);
   public final BooleanDebugOption ItemsInChunkTexture = this.newDebugOnlyOption("ItemsInChunkTexture", false);
   public final BooleanDebugOption MipMaps = this.newDebugOnlyOption("MipMaps", true);
   public final BooleanDebugOption NoLighting = this.newDebugOnlyOption("NoLighting", false);
   public final BooleanDebugOption RenderChunkTextures = this.newDebugOnlyOption("RenderChunkTextures", true);
   public final BooleanDebugOption RenderMustSeeSquares = this.newDebugOnlyOption("RenderMustSeeSquares", false);
   public final BooleanDebugOption RenderTranslucentFloor = this.newDebugOnlyOption("RenderTranslucentFloor", true);
   public final BooleanDebugOption RenderTranslucentNonFloor = this.newDebugOnlyOption("RenderTranslucentNonFloor", true);
   public final BooleanDebugOption RenderVisionPolygon = this.newDebugOnlyOption("RenderVisionPolygon", true);
   public final BooleanDebugOption RenderWallLines = this.newDebugOnlyOption("RenderWallLines", false);
   public final BooleanDebugOption SeamFix1 = this.newDebugOnlyOption("SeamFix1", false);
   public final BooleanDebugOption SeamFix2 = this.newDebugOnlyOption("SeamFix2", true);
   public final BooleanDebugOption UpdateSquareLightInfo = this.newDebugOnlyOption("UpdateSquareLightInfo", true);
   public final BooleanDebugOption UseWeatherShader = this.newDebugOnlyOption("UseWeatherShader", true);

   public FBORenderDebugOptions() {
      super((IDebugOptionGroup)null, "FBORenderChunk");
   }
}
