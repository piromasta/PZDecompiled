package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

public class CutawayAttachedModifier implements Consumer<TextureDraw> {
   public static CutawayAttachedModifier instance = new CutawayAttachedModifier();
   private Texture depthTexture;
   private Texture cutawayTexture;
   private IsoSprite sprite;
   protected final int[] col = new int[4];
   protected int colTint = 0;
   private int cutawayX;
   private int cutawayY;
   private int cutawayW;
   private int cutawayH;
   private SpriteRenderer.WallShaderTexRender wallShaderTexRender = null;

   public CutawayAttachedModifier() {
   }

   public void accept(TextureDraw var1) {
      Texture var2 = var1.tex;
      Texture var3 = this.depthTexture;
      Texture var4 = this.cutawayTexture;
      float var5 = var1.x0 - var2.getOffsetX();
      float var6 = var1.y0 - var2.getOffsetY();
      short var7 = 226;
      short var8 = 128;
      int var10 = this.cutawayX % var8;
      int var11 = this.cutawayY % var7;
      int var12 = this.cutawayW;
      int var13 = this.cutawayH;
      float var20;
      float var14 = var20 = PZMath.max(var2.offsetX, var3.offsetX, (float)var10);
      float var18;
      float var16 = var18 = PZMath.min(var2.offsetX + (float)var2.getWidth(), var3.offsetX + (float)var3.getWidth(), (float)(var10 + var12));
      float var17;
      float var15 = var17 = PZMath.max(var2.offsetY, var3.offsetY, (float)var11);
      float var21;
      float var19 = var21 = PZMath.min(var2.offsetY + (float)var2.getHeight(), var3.offsetY + (float)var3.getHeight(), (float)(var11 + var13));
      if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.LeftOnly) {
         var16 = var18 = PZMath.min(var16, 63.0F);
      }

      if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.RightOnly) {
         var14 = var20 = PZMath.max(var14, 63.0F);
      }

      var1.x0 = var5 + var14;
      var1.x1 = var5 + var16;
      var1.x2 = var5 + var18;
      var1.x3 = var5 + var20;
      var1.y0 = var6 + var15;
      var1.y1 = var6 + var17;
      var1.y2 = var6 + var19;
      var1.y3 = var6 + var21;
      var1.u0 = (var2.getXStart() * (float)var2.getWidthHW() + (var14 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.u1 = (var2.getXStart() * (float)var2.getWidthHW() + (var16 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.u2 = (var2.getXStart() * (float)var2.getWidthHW() + (var18 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.u3 = (var2.getXStart() * (float)var2.getWidthHW() + (var20 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.v0 = (var2.getYStart() * (float)var2.getHeightHW() + (var15 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.v1 = (var2.getYStart() * (float)var2.getHeightHW() + (var17 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.v2 = (var2.getYStart() * (float)var2.getHeightHW() + (var19 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.v3 = (var2.getYStart() * (float)var2.getHeightHW() + (var21 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.tex1 = this.depthTexture;
      var1.tex1_u0 = (var3.getXStart() * (float)var3.getWidthHW() + (var14 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_u1 = (var3.getXStart() * (float)var3.getWidthHW() + (var16 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_u2 = (var3.getXStart() * (float)var3.getWidthHW() + (var18 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_u3 = (var3.getXStart() * (float)var3.getWidthHW() + (var20 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_v0 = (var3.getYStart() * (float)var3.getHeightHW() + (var15 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex1_v1 = (var3.getYStart() * (float)var3.getHeightHW() + (var17 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex1_v2 = (var3.getYStart() * (float)var3.getHeightHW() + (var19 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex1_v3 = (var3.getYStart() * (float)var3.getHeightHW() + (var21 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex2 = this.cutawayTexture;
      var1.tex2_u0 = ((float)this.cutawayX + (var14 - (float)var10)) / (float)var4.getWidthHW();
      var1.tex2_u1 = ((float)this.cutawayX + (var16 - (float)var10)) / (float)var4.getWidthHW();
      var1.tex2_u2 = ((float)this.cutawayX + (var18 - (float)var10)) / (float)var4.getWidthHW();
      var1.tex2_u3 = ((float)this.cutawayX + (var20 - (float)var10)) / (float)var4.getWidthHW();
      var1.tex2_v0 = ((float)this.cutawayY + (var15 - (float)var11)) / (float)var4.getHeightHW();
      var1.tex2_v1 = ((float)this.cutawayY + (var17 - (float)var11)) / (float)var4.getHeightHW();
      var1.tex2_v2 = ((float)this.cutawayY + (var19 - (float)var11)) / (float)var4.getHeightHW();
      var1.tex2_v3 = ((float)this.cutawayY + (var21 - (float)var11)) / (float)var4.getHeightHW();
      if (this.sprite == null || !this.sprite.getProperties().Is(IsoFlagType.NoWallLighting)) {
         this.applyShading_Wall(var1);
      }

   }

   public void setSprite(IsoSprite var1) {
      this.sprite = var1;
   }

   public void setVertColors(int var1, int var2, int var3, int var4) {
      this.col[0] = var1;
      this.col[1] = var2;
      this.col[2] = var3;
      this.col[3] = var4;
   }

   public void setAlpha4(float var1) {
      int var2 = (int)(var1 * 255.0F) & 255;
      this.col[0] = this.col[0] & 16777215 | var2 << 24;
      this.col[1] = this.col[1] & 16777215 | var2 << 24;
      this.col[2] = this.col[2] & 16777215 | var2 << 24;
      this.col[3] = this.col[3] & 16777215 | var2 << 24;
   }

   public void setTintColor(int var1) {
      this.colTint = var1;
   }

   public void setupWallDepth(IsoSprite var1, IsoDirections var2, Texture var3, int var4, int var5, int var6, int var7, SpriteRenderer.WallShaderTexRender var8) {
      TileDepthMapManager.TileDepthPreset var9;
      switch (var2) {
         case N:
            var9 = TileDepthMapManager.TileDepthPreset.NWall;
            if (var1.getProperties().Is(IsoFlagType.DoorWallN) && !var1.getProperties().Is(IsoFlagType.doorN)) {
               var9 = TileDepthMapManager.TileDepthPreset.NDoorFrame;
            }
            break;
         case NW:
            var9 = TileDepthMapManager.TileDepthPreset.NWWall;
            break;
         case W:
            var9 = TileDepthMapManager.TileDepthPreset.WWall;
            if (var1.getProperties().Is(IsoFlagType.DoorWallW) && !var1.getProperties().Is(IsoFlagType.doorW)) {
               var9 = TileDepthMapManager.TileDepthPreset.WDoorFrame;
            }
            break;
         case SE:
            var9 = TileDepthMapManager.TileDepthPreset.SEWall;
            break;
         default:
            var9 = TileDepthMapManager.TileDepthPreset.Floor;
      }

      this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(var9);
      if (var1.depthTexture != null) {
         this.depthTexture = var1.depthTexture.getTexture();
      }

      this.cutawayTexture = var3;
      this.cutawayX = var4;
      this.cutawayY = var5;
      this.cutawayW = var6;
      this.cutawayH = var7;
      this.wallShaderTexRender = var8;
   }

   private void applyShading_Wall(TextureDraw var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Walls.Lighting.getValue()) {
         var1.col0 = Color.blendBGR(var1.col0, this.col[0]);
         var1.col1 = Color.blendBGR(var1.col1, this.col[1]);
         var1.col2 = Color.blendBGR(var1.col2, this.col[2]);
         var1.col3 = Color.blendBGR(var1.col3, this.col[3]);
      }

      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         var1.col0 = -1;
         var1.col1 = -1;
         var1.col2 = -1;
         var1.col3 = -1;
      }

      if (this.colTint != 0) {
         var1.col0 = Color.tintABGR(var1.col0, this.colTint);
         var1.col1 = Color.tintABGR(var1.col1, this.colTint);
         var1.col2 = Color.tintABGR(var1.col2, this.colTint);
         var1.col3 = Color.tintABGR(var1.col3, this.colTint);
      }

   }
}
