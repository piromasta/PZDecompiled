package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;

public final class WorldItemModelDrawer extends TextureDraw.GenericDrawer {
   private static final ObjectPool<WorldItemModelDrawer> s_modelDrawerPool = new ObjectPool(WorldItemModelDrawer::new);
   private static final ColorInfo tempColorInfo = new ColorInfo();
   private static final Matrix4f s_attachmentXfrm = new Matrix4f();
   public static final ImmutableColor ROTTEN_FOOD_COLOR = new ImmutableColor(0.5F, 0.5F, 0.5F);
   public static final ImmutableColor HIGHLIGHT_COLOR = new ImmutableColor(0.5F, 1.0F, 1.0F);
   public static boolean NEW_WAY = true;
   private final ItemModelRenderer m_renderer = new ItemModelRenderer();

   public WorldItemModelDrawer() {
   }

   public static ItemModelRenderer.RenderStatus renderMain(InventoryItem var0, IsoGridSquare var1, IsoGridSquare var2, float var3, float var4, float var5, float var6) {
      return renderMain(var0, var1, var2, var3, var4, var5, var6, -1.0F, false);
   }

   public static ItemModelRenderer.RenderStatus renderMain(InventoryItem var0, IsoGridSquare var1, IsoGridSquare var2, float var3, float var4, float var5, float var6, float var7, boolean var8) {
      if (var0 != null && var1 != null) {
         if (!Core.getInstance().isOption3DGroundItem()) {
            return ItemModelRenderer.RenderStatus.NoModel;
         } else if (renderAtlasTexture(var0, var1, var3, var4, var5, var6, var7, var8)) {
            return ItemModelRenderer.RenderStatus.Ready;
         } else if (!ItemModelRenderer.itemHasModel(var0)) {
            return ItemModelRenderer.RenderStatus.NoModel;
         } else {
            WorldItemModelDrawer var9 = (WorldItemModelDrawer)s_modelDrawerPool.alloc();
            boolean var10 = PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching();
            if (!var8 && !DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue()) {
               var10 = false;
            }

            ItemModelRenderer.RenderStatus var11 = var9.m_renderer.renderMain(var0, var1, var2, var3, var4, var5, var6, var7, var10);
            if (var11 == ItemModelRenderer.RenderStatus.Ready) {
               SpriteRenderer.instance.drawGeneric(var9);
               return var11;
            } else {
               var9.m_renderer.reset();
               s_modelDrawerPool.release((Object)var9);
               return var11;
            }
         }
      } else {
         return ItemModelRenderer.RenderStatus.Failed;
      }
   }

   private static boolean renderAtlasTexture(InventoryItem var0, IsoGridSquare var1, float var2, float var3, float var4, float var5, float var6, boolean var7) {
      if (var5 > 0.0F) {
         return false;
      } else if (var6 >= 0.0F) {
         return false;
      } else {
         boolean var8 = DebugOptions.instance.WorldItemAtlas.Enable.getValue();
         boolean var9 = PerformanceSettings.FBORenderChunk && FBORenderChunkManager.instance.isCaching();
         if (!var7 && !DebugOptions.instance.FBORenderChunk.ItemsInChunkTexture.getValue()) {
            var9 = false;
         }

         if (var9) {
            var8 = false;
         }

         if (!var8) {
            return false;
         } else {
            int var10 = IsoCamera.frameState.playerIndex;
            float var11 = Core.getInstance().getZoom(var10);
            boolean var10000;
            if (!PerformanceSettings.FBORenderChunk || !FBORenderChunkManager.instance.isCaching() && FBORenderLevels.getTextureScale(var11) != 1) {
               var10000 = false;
            } else {
               var10000 = true;
            }

            boolean var12 = !Core.getInstance().getOptionHighResPlacedItems() || var11 >= 0.75F;
            if (var0.atlasTexture != null && !var0.atlasTexture.isStillValid(var0, var12)) {
               var0.atlasTexture = null;
            }

            if (var0.atlasTexture == null) {
               var0.atlasTexture = WorldItemAtlas.instance.getItemTexture(var0, var12);
            }

            if (var0.atlasTexture == null) {
               return false;
            } else if (var0.atlasTexture.isTooBig()) {
               return false;
            } else {
               float var13 = IsoWorldInventoryObject.getSurfaceAlpha(var1, var4 - (float)((int)var4));
               if (var13 <= 0.0F) {
                  return true;
               } else {
                  if (IsoSprite.globalOffsetX == -1.0F) {
                     IsoSprite.globalOffsetX = -IsoCamera.frameState.OffX;
                     IsoSprite.globalOffsetY = -IsoCamera.frameState.OffY;
                  }

                  float var14 = IsoUtils.XToScreen(var2, var3, var4, 0);
                  float var15 = IsoUtils.YToScreen(var2, var3, var4, 0);
                  if (FBORenderChunkManager.instance.isCaching()) {
                     var14 = IsoUtils.XToScreen(PZMath.coordmodulof(var2, 8), PZMath.coordmodulof(var3, 8), var4, 0);
                     var15 = IsoUtils.YToScreen(PZMath.coordmodulof(var2, 8), PZMath.coordmodulof(var3, 8), var4, 0);
                     var14 += FBORenderChunkManager.instance.getXOffset();
                     var15 += FBORenderChunkManager.instance.getYOffset();
                     TextureDraw.nextZ = IsoDepthHelper.calculateDepth(var2 + 0.25F, var3 + 0.25F, var4) * 2.0F - 1.0F;
                  } else {
                     var14 += IsoSprite.globalOffsetX;
                     var15 += IsoSprite.globalOffsetY;
                  }

                  if (PerformanceSettings.FBORenderChunk && !FBORenderChunkManager.instance.isCaching()) {
                     var14 += IsoCamera.cameras[var10].fixJigglyModelsX * var11;
                     var15 += IsoCamera.cameras[var10].fixJigglyModelsY * var11;
                  }

                  var1.interpolateLight(tempColorInfo, var2 % 1.0F, var3 % 1.0F);
                  if (DebugOptions.instance.FBORenderChunk.NoLighting.getValue()) {
                     tempColorInfo.set(1.0F, 1.0F, 1.0F, tempColorInfo.a);
                  }

                  if (var0.getWorldItem() != null && var0.getWorldItem().isHighlighted()) {
                     tempColorInfo.set(HIGHLIGHT_COLOR.r, HIGHLIGHT_COLOR.g, HIGHLIGHT_COLOR.b, 1.0F);
                  }

                  if (PerformanceSettings.FBORenderChunk && !FBORenderChunkManager.instance.isCaching()) {
                     TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(IsoCamera.frameState.CamCharacterX), PZMath.fastfloor(IsoCamera.frameState.CamCharacterY), var2 + 0.25F, var3 + 0.25F, var4).depthStart * 2.0F - 1.0F;
                  }

                  var0.atlasTexture.render(var2, var3, var4, var14, var15, tempColorInfo.r, tempColorInfo.g, tempColorInfo.b, var13);
                  WorldItemAtlas.instance.render();
                  return var0.atlasTexture.isRenderMainOK();
               }
            }
         }
      }
   }

   public void render() {
      FBORenderChunk var1 = FBORenderChunkManager.instance.renderThreadCurrent;
      if (PerformanceSettings.FBORenderChunk && var1 != null) {
         FBORenderItems.getInstance().setCamera(var1, this.m_renderer.m_x, this.m_renderer.m_y, this.m_renderer.m_z, this.m_renderer.m_angle);
         this.m_renderer.DoRender(FBORenderItems.getInstance().getCamera(), true, var1.bHighRes);
      } else {
         this.m_renderer.DoRenderToWorld(this.m_renderer.m_x, this.m_renderer.m_y, this.m_renderer.m_z, this.m_renderer.m_angle);
      }
   }

   public void postRender() {
      this.m_renderer.reset();
      s_modelDrawerPool.release((Object)this);
   }
}
