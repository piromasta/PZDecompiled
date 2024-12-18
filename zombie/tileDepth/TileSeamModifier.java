package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

public class TileSeamModifier implements Consumer<TextureDraw> {
   public static TileSeamModifier instance = new TileSeamModifier();
   private Texture depthTexture;
   private Texture maskTexture;
   private IsoSprite sprite;
   private Type seamType = null;
   protected final int[] col = new int[4];
   protected int colTint = 0;
   protected boolean isShore = false;
   protected final float[] waterDepth = new float[4];
   private float[] vertices = null;
   private final int[] col_floor = new int[4];

   public TileSeamModifier() {
   }

   public void accept(TextureDraw var1) {
      Texture var2 = var1.tex;
      Texture var3 = this.depthTexture;
      Texture var4 = this.maskTexture;
      float var5 = var1.x0 - var2.getOffsetX();
      float var6 = var1.y0 - var2.getOffsetY();
      float var7;
      float var8;
      float var9;
      float var10;
      float var11;
      float var12;
      float var13;
      float var14;
      if (this.vertices == null) {
         var7 = var13 = PZMath.max(var2.offsetX, var3.offsetX, var4.offsetX);
         var9 = var11 = PZMath.min(var2.offsetX + (float)var2.getWidth(), var3.offsetX + (float)var3.getWidth(), var4.offsetX + (float)var4.getWidth());
         var8 = var10 = PZMath.max(var2.offsetY, var3.offsetY, var4.offsetY);
         var12 = var14 = PZMath.min(var2.offsetY + (float)var2.getHeight(), var3.offsetY + (float)var3.getHeight(), var4.offsetY + (float)var4.getHeight());
      } else {
         var7 = this.vertices[0];
         var8 = this.vertices[1];
         var9 = this.vertices[2];
         var10 = this.vertices[3];
         var11 = this.vertices[4];
         var12 = this.vertices[5];
         var13 = this.vertices[6];
         var14 = this.vertices[7];
      }

      var1.x0 = var5 + var7;
      var1.x1 = var5 + var9;
      var1.x2 = var5 + var11;
      var1.x3 = var5 + var13;
      var1.y0 = var6 + var8;
      var1.y1 = var6 + var10;
      var1.y2 = var6 + var12;
      var1.y3 = var6 + var14;
      var1.u0 = (var2.getXStart() * (float)var2.getWidthHW() + (var7 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.u1 = (var2.getXStart() * (float)var2.getWidthHW() + (var9 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.u2 = (var2.getXStart() * (float)var2.getWidthHW() + (var11 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.u3 = (var2.getXStart() * (float)var2.getWidthHW() + (var13 - var2.offsetX)) / (float)var2.getWidthHW();
      var1.v0 = (var2.getYStart() * (float)var2.getHeightHW() + (var8 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.v1 = (var2.getYStart() * (float)var2.getHeightHW() + (var10 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.v2 = (var2.getYStart() * (float)var2.getHeightHW() + (var12 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.v3 = (var2.getYStart() * (float)var2.getHeightHW() + (var14 - var2.offsetY)) / (float)var2.getHeightHW();
      var1.tex1 = this.depthTexture;
      var1.tex1_u0 = (var3.getXStart() * (float)var3.getWidthHW() + (var7 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_u1 = (var3.getXStart() * (float)var3.getWidthHW() + (var9 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_u2 = (var3.getXStart() * (float)var3.getWidthHW() + (var11 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_u3 = (var3.getXStart() * (float)var3.getWidthHW() + (var13 - var3.offsetX)) / (float)var3.getWidthHW();
      var1.tex1_v0 = (var3.getYStart() * (float)var3.getHeightHW() + (var8 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex1_v1 = (var3.getYStart() * (float)var3.getHeightHW() + (var10 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex1_v2 = (var3.getYStart() * (float)var3.getHeightHW() + (var12 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex1_v3 = (var3.getYStart() * (float)var3.getHeightHW() + (var14 - var3.offsetY)) / (float)var3.getHeightHW();
      var1.tex2 = this.maskTexture;
      var1.tex2_u0 = (var4.getXStart() * (float)var4.getWidthHW() + (var7 - var4.offsetX)) / (float)var4.getWidthHW();
      var1.tex2_u1 = (var4.getXStart() * (float)var4.getWidthHW() + (var9 - var4.offsetX)) / (float)var4.getWidthHW();
      var1.tex2_u2 = (var4.getXStart() * (float)var4.getWidthHW() + (var11 - var4.offsetX)) / (float)var4.getWidthHW();
      var1.tex2_u3 = (var4.getXStart() * (float)var4.getWidthHW() + (var13 - var4.offsetX)) / (float)var4.getWidthHW();
      var1.tex2_v0 = (var4.getYStart() * (float)var4.getHeightHW() + (var8 - var4.offsetY)) / (float)var4.getHeightHW();
      var1.tex2_v1 = (var4.getYStart() * (float)var4.getHeightHW() + (var10 - var4.offsetY)) / (float)var4.getHeightHW();
      var1.tex2_v2 = (var4.getYStart() * (float)var4.getHeightHW() + (var12 - var4.offsetY)) / (float)var4.getHeightHW();
      var1.tex2_v3 = (var4.getYStart() * (float)var4.getHeightHW() + (var14 - var4.offsetY)) / (float)var4.getHeightHW();
      if (this.seamType == TileSeamModifier.Type.Floor) {
         this.applyShading_Floor(var1);
      }

      if (this.seamType == TileSeamModifier.Type.Wall) {
         this.applyShading_Wall(var1);
      }

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

   public void setShore(boolean var1) {
      this.isShore = var1;
   }

   public void setWaterDepth(float var1, float var2, float var3, float var4) {
      this.waterDepth[0] = var1;
      this.waterDepth[1] = var2;
      this.waterDepth[2] = var3;
      this.waterDepth[3] = var4;
   }

   public void setTintColor(int var1) {
      this.colTint = var1;
   }

   public void setupFloorDepth(IsoSprite var1, TileSeamManager.Tiles var2) {
      this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.Floor);
      this.maskTexture = TileSeamManager.instance.getTexture(var2);
      this.sprite = var1;
      this.seamType = TileSeamModifier.Type.Floor;
      this.vertices = TileSeamManager.instance.getVertices(var2);

      for(int var3 = 0; var3 < 4; ++var3) {
         this.col_floor[var3] = this.col[var3];
      }

      if (var2 == TileSeamManager.Tiles.FloorSouth) {
         this.col_floor[0] = this.col[3];
         this.col_floor[1] = this.col[2];
      }

      if (var2 == TileSeamManager.Tiles.FloorEast) {
         this.col_floor[0] = this.col_floor[3] = this.col[2];
         this.col_floor[2] = this.col[1];
      }

   }

   public void setupFloorDepth(IsoSprite var1, TileSeamManager.Tiles var2, TileDepthTexture var3) {
      this.depthTexture = var3.getTexture();
      this.maskTexture = TileSeamManager.instance.getTexture(var2);
      this.sprite = var1;
      this.seamType = TileSeamModifier.Type.Floor;
      this.vertices = TileSeamManager.instance.getVertices(var2);

      for(int var4 = 0; var4 < 4; ++var4) {
         this.col_floor[var4] = this.col[var4];
      }

      if (var2 == TileSeamManager.Tiles.FloorSouthOneThird || var2 == TileSeamManager.Tiles.FloorSouthTwoThirds) {
         this.col_floor[0] = this.col[3];
         this.col_floor[1] = this.col[2];
      }

      if (var2 == TileSeamManager.Tiles.FloorEastOneThird || var2 == TileSeamManager.Tiles.FloorEastTwoThirds) {
         this.col_floor[0] = this.col_floor[3] = this.col[2];
         this.col_floor[2] = this.col[1];
      }

   }

   public void setupWallDepth(IsoSprite var1, IsoDirections var2) {
      TileDepthMapManager.TileDepthPreset var3;
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
            break;
         case SE:
            var3 = TileDepthMapManager.TileDepthPreset.SEWall;
            break;
         default:
            var3 = TileDepthMapManager.TileDepthPreset.Floor;
      }

      this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(var3);
      if (var1.depthTexture != null) {
         this.depthTexture = var1.depthTexture.getTexture();
      }

      this.maskTexture = TileSeamManager.instance.getTexture(IsoSprite.SEAM_FIX2);
      this.sprite = var1;
      this.seamType = TileSeamModifier.Type.Wall;
      this.vertices = null;
   }

   private void applyShading_Floor(TextureDraw var1) {
      if (DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.Floor.Lighting.getValue()) {
         var1.col0 = Color.blendBGR(var1.col0, this.col_floor[0]);
         var1.col1 = Color.blendBGR(var1.col1, this.col_floor[1]);
         var1.col2 = Color.blendBGR(var1.col2, this.col_floor[2]);
         var1.col3 = Color.blendBGR(var1.col3, this.col_floor[3]);
      }

      if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
         var1.col0 = -1;
         var1.col1 = -1;
         var1.col2 = -1;
         var1.col3 = -1;
      }

      if (this.isShore && DebugOptions.instance.Terrain.RenderTiles.IsoGridSquare.ShoreFade.getValue()) {
         var1.col0 = Color.setAlphaChannelToABGR(var1.col0, 1.0F - this.waterDepth[0]);
         var1.col1 = Color.setAlphaChannelToABGR(var1.col1, 1.0F - this.waterDepth[1]);
         var1.col2 = Color.setAlphaChannelToABGR(var1.col2, 1.0F - this.waterDepth[2]);
         var1.col3 = Color.setAlphaChannelToABGR(var1.col3, 1.0F - this.waterDepth[3]);
      }

      if (this.colTint != 0) {
         var1.col0 = Color.tintABGR(var1.col0, this.colTint);
         var1.col1 = Color.tintABGR(var1.col1, this.colTint);
         var1.col2 = Color.tintABGR(var1.col2, this.colTint);
         var1.col3 = Color.tintABGR(var1.col3, this.colTint);
      }

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

   static enum Type {
      Floor,
      Wall;

      private Type() {
      }
   }
}
