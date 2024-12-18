package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.HashSet;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

public final class FBORenderObjectOutline {
   private static FBORenderObjectOutline instance;
   private boolean m_bRendering = false;
   private final HashSet<IsoObject> m_objectSet = new HashSet();
   private final ArrayList<IsoObject> m_objectList = new ArrayList();
   private final ColorInfo m_colorInfo = new ColorInfo();
   private TextureFBO m_fbo;
   private Texture m_fboTexture;
   private final ObjectPool<Drawer> m_drawerPool = new ObjectPool(Drawer::new);
   private final ObjectPool<Drawer2> m_drawer2Pool = new ObjectPool(Drawer2::new);

   public FBORenderObjectOutline() {
   }

   public static FBORenderObjectOutline getInstance() {
      if (instance == null) {
         instance = new FBORenderObjectOutline();
      }

      return instance;
   }

   public boolean isRendering() {
      return this.m_bRendering;
   }

   public void registerObject(IsoObject var1) {
      if (!(var1 instanceof IsoMannequin)) {
         if (!this.m_objectSet.contains(var1)) {
            this.m_objectSet.add(var1);
         }
      }
   }

   public void unregisterObject(IsoObject var1) {
      this.m_objectSet.remove(var1);
   }

   public void render(int var1) {
      this.m_objectList.clear();
      this.m_objectList.addAll(this.m_objectSet);
      this.m_bRendering = true;

      for(int var2 = 0; var2 < this.m_objectList.size(); ++var2) {
         IsoObject var3 = (IsoObject)this.m_objectList.get(var2);
         if (var3.getObjectIndex() == -1) {
            this.m_objectSet.remove(var3);
         } else if (!var3.isOutlineHighlight(var1)) {
            this.m_objectSet.remove(var3);
         } else {
            this.renderObject(var1, var3);
         }
      }

      this.m_bRendering = false;
   }

   private void renderObject(int var1, IsoObject var2) {
      if (var2.getSpriteModel() == null) {
         if (var2.getSprite() != null) {
            Texture var3 = var2.getSprite().getTextureForCurrentFrame(var2.getDir());
            if (var3 != null) {
               ObjectRenderInfo var4 = var2.getRenderInfo(var1);
               if (var4.m_layer != ObjectRenderLayer.None && !(var4.m_targetAlpha <= 0.0F)) {
                  Drawer2 var5 = (Drawer2)this.m_drawer2Pool.alloc();
                  var5.init(var2);
                  SpriteRenderer.instance.drawGeneric(var5);
               }
            }
         }
      }
   }

   private static final class Drawer2 extends TextureDraw.GenericDrawer {
      final ArrayList<Texture> m_textures = new ArrayList();
      final ColorInfo m_outlineColor = new ColorInfo();
      boolean m_bOutlineBehindPlayer = true;
      float m_renderX;
      float m_renderY;
      static Shader outlineShader = null;

      private Drawer2() {
      }

      Drawer2 init(IsoObject var1) {
         this.m_textures.clear();
         int var2 = IsoCamera.frameState.playerIndex;
         this.m_outlineColor.setABGR(var1.getOutlineHighlightCol(var2));
         if (var1.isOutlineHlBlink(var2)) {
            ColorInfo var10000 = this.m_outlineColor;
            var10000.a *= Core.blinkAlpha;
         }

         this.m_bOutlineBehindPlayer = (float)var1.getSquare().getX() + 0.5F + (float)var1.getSquare().getY() + 0.5F < IsoCamera.frameState.CamCharacterX + IsoCamera.frameState.CamCharacterY;
         float var3 = IsoCamera.cameras[var2].fixJigglyModelsSquareX;
         float var4 = IsoCamera.cameras[var2].fixJigglyModelsSquareY;
         float var5 = IsoUtils.XToScreen(var1.getX() + var3, var1.getY() + var4, var1.getZ(), 0);
         float var6 = IsoUtils.YToScreen(var1.getX() + var3, var1.getY() + var4, var1.getZ(), 0);
         var5 -= var1.offsetX;
         var6 -= var1.offsetY + var1.getRenderYOffset() * (float)Core.TileScale;
         var5 += IsoSprite.globalOffsetX;
         var6 += IsoSprite.globalOffsetY;
         this.m_renderX = var5;
         this.m_renderY = var6;
         this.initTexture(var1, var1.getSprite());
         if (var1.isOutlineHlAttached(var2) && (var1.hasOverlaySprite() || var1.hasAttachedAnimSprites())) {
            this.initTexture(var1, var1.getOverlaySprite());
            if (var1.hasAttachedAnimSprites() && !var1.hasAnimatedAttachments()) {
               for(int var7 = 0; var7 < var1.getAttachedAnimSprite().size(); ++var7) {
                  this.initTexture(var1, ((IsoSpriteInstance)var1.getAttachedAnimSprite().get(var7)).getParentSprite());
               }
            }
         }

         return this;
      }

      void initTexture(IsoObject var1, IsoSprite var2) {
         if (var2 != null && var2.getTextureForCurrentFrame(var1.getDir()) != null) {
            Texture var3 = var2.getTextureForCurrentFrame(var1.getDir());
            this.m_textures.add(var3);
         }

      }

      public void render() {
         boolean var1 = ModelOutlines.instance.beginRenderOutline(this.m_outlineColor, this.m_bOutlineBehindPlayer, false);
         GL11.glDepthMask(true);
         ModelOutlines.instance.m_fboA.startDrawing(var1, true);
         if (outlineShader == null) {
            outlineShader = new Shader("vboRenderer_SpriteOutline", true, false);
         }

         VBORenderer var2 = VBORenderer.getInstance();

         for(int var3 = 0; var3 < this.m_textures.size(); ++var3) {
            Texture var4 = (Texture)this.m_textures.get(var3);
            float var5 = this.m_renderX + var4.getOffsetX();
            float var6 = this.m_renderY + var4.getOffsetY();
            var2.startRun(VBORenderer.getInstance().FORMAT_PositionColorUV);
            var2.setMode(7);
            var2.setTextureID(var4.getTextureId());
            var2.setShaderProgram(outlineShader.getShaderProgram());
            var2.addQuad(var5, var6, var4.getXStart(), var4.getYStart(), var5 + (float)var4.getWidth(), var6 + (float)var4.getHeight(), var4.getXEnd(), var4.getYEnd(), 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            var2.endRun();
         }

         var2.flush();
         ModelOutlines.instance.m_fboA.endDrawing();
      }

      public void postRender() {
         this.m_textures.clear();
         FBORenderObjectOutline.instance.m_drawer2Pool.release((Object)this);
      }
   }

   private static final class Drawer extends TextureDraw.GenericDrawer {
      final ArrayList<Texture> m_textures = new ArrayList();
      float m_offsetY;

      private Drawer() {
      }

      Drawer init(IsoObject var1) {
         this.m_textures.clear();
         this.m_offsetY = 0.0F;
         this.initTexture(var1, var1.getSprite());
         this.initTexture(var1, var1.getOverlaySprite());
         if (var1.getAttachedAnimSprite() != null && !var1.hasAnimatedAttachments()) {
            for(int var2 = 0; var2 < var1.getAttachedAnimSprite().size(); ++var2) {
               this.initTexture(var1, ((IsoSpriteInstance)var1.getAttachedAnimSprite().get(var2)).getParentSprite());
            }
         }

         return this;
      }

      void initTexture(IsoObject var1, IsoSprite var2) {
         if (var2 != null && var2.getTextureForCurrentFrame(var1.getDir()) != null) {
            Texture var3 = var2.getTextureForCurrentFrame(var1.getDir());
            this.m_textures.add(var3);
         }

      }

      public void render() {
         if (FBORenderObjectOutline.instance.m_fbo == null) {
            FBORenderObjectOutline.instance.m_fboTexture = new Texture(64 * Core.TileScale, 128 * Core.TileScale, 16);
            FBORenderObjectOutline.instance.m_fbo = new TextureFBO(FBORenderObjectOutline.instance.m_fboTexture);
         }

         TextureFBO var1 = FBORenderObjectOutline.instance.m_fbo;
         GL11.glPushAttrib(2048);
         GL11.glViewport(0, 0, var1.getWidth(), var1.getHeight());
         Matrix4f var2 = BaseVehicle.allocMatrix4f();
         var2.setOrtho2D(0.0F, (float)var1.getWidth(), 0.0F, (float)var1.getHeight());
         Matrix4f var3 = BaseVehicle.allocMatrix4f();
         var3.identity();
         PZGLUtil.pushAndLoadMatrix(5889, var2);
         PZGLUtil.pushAndLoadMatrix(5888, var3);
         BaseVehicle.releaseMatrix4f(var2);
         BaseVehicle.releaseMatrix4f(var3);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(3089);
         var1.startDrawing(true, true);
         VBORenderer var4 = VBORenderer.getInstance();

         for(int var5 = 0; var5 < this.m_textures.size(); ++var5) {
            Texture var6 = (Texture)this.m_textures.get(var5);
            float var7 = var6.getOffsetX();
            float var8 = var6.getOffsetY() + this.m_offsetY;
            var4.startRun(VBORenderer.getInstance().FORMAT_PositionColorUV);
            var4.setMode(7);
            var4.setTextureID(var6.getTextureId());
            var4.addQuad(var7, var8, var6.getXStart(), var6.getYStart(), var7 + (float)var6.getWidth(), var8 + (float)var6.getHeight(), var6.getXEnd(), var6.getYEnd(), 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            var4.endRun();
         }

         var4.flush();
         var1.endDrawing();
         PZGLUtil.popMatrix(5889);
         PZGLUtil.popMatrix(5888);
         GL11.glPopAttrib();
         GLStateRenderThread.restore();
         GL11.glEnable(3089);
      }

      public void postRender() {
         this.m_textures.clear();
         FBORenderObjectOutline.instance.m_drawerPool.release((Object)this);
      }
   }
}
