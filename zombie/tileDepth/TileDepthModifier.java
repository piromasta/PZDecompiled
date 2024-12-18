package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

public final class TileDepthModifier implements Consumer<TextureDraw> {
   public static final TileDepthModifier instance = new TileDepthModifier();
   private Texture depthTexture;
   private IsoSprite sprite;

   public TileDepthModifier() {
   }

   public void accept(TextureDraw var1) {
      Texture var2 = var1.tex;
      Texture var3 = this.depthTexture;
      if (var2 != null && var3 != null) {
         float var10;
         float var4 = var10 = PZMath.max((float)this.sprite.soffX + var2.offsetX, var3.offsetX);
         float var8;
         float var6 = var8 = PZMath.min((float)this.sprite.soffX + var2.offsetX + (float)var2.getWidth(), var3.offsetX + (float)var3.getWidth());
         float var7;
         float var5 = var7 = PZMath.max((float)this.sprite.soffY + var2.offsetY, var3.offsetY);
         float var11;
         float var9 = var11 = PZMath.min((float)this.sprite.soffY + var2.offsetY + (float)var2.getHeight(), var3.offsetY + (float)var3.getHeight());
         float var12 = var3.getXStart();
         float var13 = var3.getYStart();
         float var14 = (float)var3.getWidthHW();
         float var15 = (float)var3.getHeightHW();
         var1.tex1 = this.depthTexture;
         var1.tex1_u0 = var12 + (var4 - var3.offsetX) / var14;
         var1.tex1_u1 = var12 + (var6 - var3.offsetX) / var14;
         var1.tex1_u2 = var12 + (var8 - var3.offsetX) / var14;
         var1.tex1_u3 = var12 + (var10 - var3.offsetX) / var14;
         var1.tex1_v0 = var13 + (var5 - var3.offsetY) / var15;
         var1.tex1_v1 = var13 + (var7 - var3.offsetY) / var15;
         var1.tex1_v2 = var13 + (var9 - var3.offsetY) / var15;
         var1.tex1_v3 = var13 + (var11 - var3.offsetY) / var15;
      }
   }

   public void setupFloorDepth(IsoSprite var1) {
      this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.Floor);
      this.sprite = var1;
   }

   public void setupWallDepth(IsoSprite var1, IsoDirections var2) {
      if (var1.depthTexture != null) {
         this.depthTexture = var1.depthTexture.getTexture();
         this.sprite = var1;
      } else {
         TileDepthMapManager.TileDepthPreset var3 = TileDepthMapManager.TileDepthPreset.Floor;
         switch (var2) {
            case N:
               var3 = TileDepthMapManager.TileDepthPreset.NWall;
               if (var1.getProperties().Is(IsoFlagType.DoorWallN) && !var1.getProperties().Is(IsoFlagType.doorN)) {
                  var3 = TileDepthMapManager.TileDepthPreset.NDoorFrame;
               }
               break;
            case NW:
               var3 = TileDepthMapManager.TileDepthPreset.NWWall;
               break;
            case W:
               var3 = TileDepthMapManager.TileDepthPreset.WWall;
               if (var1.getProperties().Is(IsoFlagType.DoorWallW) && !var1.getProperties().Is(IsoFlagType.doorW)) {
                  var3 = TileDepthMapManager.TileDepthPreset.WDoorFrame;
               }

               if (var1.getProperties().Is(IsoFlagType.WallSE)) {
                  var3 = TileDepthMapManager.TileDepthPreset.SEWall;
               }
               break;
            case SE:
               var3 = TileDepthMapManager.TileDepthPreset.SEWall;
         }

         this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(var3);
         this.sprite = var1;
      }
   }

   public void setupTileDepthTexture(IsoSprite var1, TileDepthTexture var2) {
      this.depthTexture = var2.getTexture();
      this.sprite = var1;
   }
}
