package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class IsoSprite extends OptionGroup {
   public final BooleanDebugOption RenderSprites = this.newDebugOnlyOption("Render.Sprites", true);
   public final BooleanDebugOption RenderModels = this.newDebugOnlyOption("Render.Models", true);
   public final BooleanDebugOption MovingObjectEdges = this.newDebugOnlyOption("Render.MovingObjectEdges", false);
   public final BooleanDebugOption DropShadowEdges = this.newDebugOnlyOption("Render.DropShadowEdges", false);
   public final BooleanDebugOption NearestMagFilterAtMinZoom = this.newDebugOnlyOption("Render.NearestMagFilterAtMinZoom", true);
   public final BooleanDebugOption ItemHeight = this.newDebugOnlyOption("Render.ItemHeight", false);
   public final BooleanDebugOption Surface = this.newDebugOnlyOption("Render.Surface", false);
   public final BooleanDebugOption TextureWrapClampToEdge = this.newDebugOnlyOption("Render.TextureWrap.ClampToEdge", false);
   public final BooleanDebugOption TextureWrapRepeat = this.newDebugOnlyOption("Render.TextureWrap.Repeat", false);
   public final BooleanDebugOption ForceLinearMagFilter = this.newDebugOnlyOption("Render.ForceLinearMagFilter", false);
   public final BooleanDebugOption ForceNearestMagFilter = this.newDebugOnlyOption("Render.ForceNearestMagFilter", false);
   public final BooleanDebugOption ForceNearestMipMapping = this.newDebugOnlyOption("Render.ForceNearestMipMapping", false);
   public final BooleanDebugOption CharacterMipmapColors = this.newDebugOnlyOption("Render.CharacterMipmapColors", false);
   public final BooleanDebugOption WorldMipmapColors = this.newDebugOnlyOption("Render.WorldMipmapColors", false);

   public IsoSprite() {
   }
}
