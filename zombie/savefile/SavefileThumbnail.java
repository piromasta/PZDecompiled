package zombie.savefile;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.MultiTextureFBO2;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.iso.IsoCamera;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoSprite;
import zombie.ui.UIManager;

public final class SavefileThumbnail {
   private static final int WIDTH = 256;
   private static final int HEIGHT = 256;
   private static VertexBufferObject.VertexFormat FORMAT_PositionColorUV = null;

   public SavefileThumbnail() {
   }

   public static void create() {
      int var0 = -1;

      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         if (IsoPlayer.players[var1] != null) {
            var0 = var1;
            break;
         }
      }

      if (var0 != -1) {
         create(var0);
      }
   }

   public static void create(int var0) {
      Core var1 = Core.getInstance();
      MultiTextureFBO2 var2 = var1.OffscreenBuffer;
      float var3 = var2.getZoom(var0);
      float var4 = var2.getTargetZoom(var0);
      setZoom(var0, 1.0F, 1.0F);
      IsoCamera.cameras[var0].center();
      renderWorld(var0, true, true);
      SpriteRenderer.instance.drawGeneric(new TakeScreenShotDrawer(var0));
      setZoom(var0, var3, var4);
      IsoCamera.cameras[var0].center();

      for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
         IsoPlayer var6 = IsoPlayer.players[var5];
         if (var6 != null) {
            renderWorld(var5, false, var5 == var0);
         }
      }

      var1.RenderOffScreenBuffer();
      if (var1.StartFrameUI()) {
         UIManager.render();
      }

      var1.EndFrameUI();
   }

   private static void renderWorld(int var0, boolean var1, boolean var2) {
      IsoPlayer.setInstance(IsoPlayer.players[var0]);
      IsoCamera.setCameraCharacter(IsoPlayer.players[var0]);
      IsoSprite.globalOffsetX = -1.0F;
      Core.getInstance().StartFrame(var0, var1);
      if (var2) {
         SpriteRenderer.instance.drawGeneric(new FixCameraDrawer(var0));
      }

      IsoCamera.frameState.set(var0);
      IndieGL.disableDepthTest();
      IsoWorld.instance.render();
      RenderSettings.getInstance().legacyPostRender(var0);
      Core.getInstance().EndFrame(var0);
   }

   private static void setZoom(int var0, float var1, float var2) {
      Core.getInstance().OffscreenBuffer.setZoom(var0, var1);
      Core.getInstance().OffscreenBuffer.setTargetZoom(var0, var2);
      IsoCamera.cameras[var0].zoom = var1;
      IsoCamera.cameras[var0].OffscreenWidth = IsoCamera.getOffscreenWidth(var0);
      IsoCamera.cameras[var0].OffscreenHeight = IsoCamera.getOffscreenHeight(var0);
   }

   private static void createWithRenderShader(int var0) {
      short var1 = 256;
      short var2 = 256;
      Texture var3 = new Texture(var1, var2, 16);
      TextureFBO var4 = new TextureFBO(var3, false);
      GL11.glPushAttrib(1048575);

      try {
         var4.startDrawing(true, false);
         GL11.glViewport(0, 0, var1, var2);
         Core var5 = Core.getInstance();
         Matrix4f var6 = Core.getInstance().projectionMatrixStack.alloc();
         var6.setOrtho2D(0.0F, (float)var1, (float)var2, 0.0F);
         var5.projectionMatrixStack.push(var6);
         Matrix4f var7 = Core.getInstance().modelViewMatrixStack.alloc();
         var7.identity();
         var5.modelViewMatrixStack.push(var7);
         GL11.glDisable(3089);
         GL11.glDisable(2960);
         GL11.glDisable(3042);
         GL11.glDisable(3008);
         GL11.glDisable(2929);
         GL11.glDisable(2884);
         int var8 = IsoCamera.getScreenLeft(var0) + IsoCamera.getScreenWidth(var0) / 2 - var1 / 2;
         int var9 = IsoCamera.getScreenTop(var0) + IsoCamera.getScreenHeight(var0) / 2 - var2 / 2;
         int var10 = var5.getOffscreenBuffer().getTexture().getWidthHW();
         int var11 = var5.getOffscreenBuffer().getTexture().getHeightHW();
         float var12 = (float)var8 / (float)var10;
         float var13 = (float)(var8 + var1) / (float)var10;
         float var14 = (float)var9 / (float)var11;
         float var15 = (float)(var9 + var2) / (float)var11;
         if (FORMAT_PositionColorUV == null) {
            FORMAT_PositionColorUV = new VertexBufferObject.VertexFormat(3);
            FORMAT_PositionColorUV.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
            FORMAT_PositionColorUV.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
            FORMAT_PositionColorUV.setElement(2, VertexBufferObject.VertexType.ColorArray, 16);
            FORMAT_PositionColorUV.calculate();
         }

         VBORenderer var16 = VBORenderer.getInstance();
         var16.startRun(FORMAT_PositionColorUV);
         var16.setShaderProgram(SceneShaderStore.WeatherShader.getProgram());
         var16.setTextureID(((Texture)var5.getOffscreenBuffer().getTexture()).getTextureId());
         var16.setMode(7);
         var16.addQuad(0.0F, 0.0F, var12, var15, 0.0F, (float)var2, var12, var14, (float)var1, (float)var2, var13, var14, (float)var1, 0.0F, var13, var15, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
         var16.endRun();
         var16.flush();
         var5.TakeScreenshot(0, 0, var1, var2, TextureFBO.getFuncs().GL_COLOR_ATTACHMENT0());
         var4.endDrawing();
         Core.getInstance().projectionMatrixStack.pop();
         Core.getInstance().modelViewMatrixStack.pop();
      } finally {
         var4.destroy();
         GL11.glPopAttrib();
      }

   }

   private static final class TakeScreenShotDrawer extends TextureDraw.GenericDrawer {
      int m_playerIndex;

      TakeScreenShotDrawer(int var1) {
         this.m_playerIndex = var1;
      }

      public void render() {
         Core var1 = Core.getInstance();
         MultiTextureFBO2 var2 = var1.OffscreenBuffer;
         if (var2.Current == null) {
            Core.getInstance().TakeScreenshot(256, 256, 1029);
         } else if (SceneShaderStore.WeatherShader == null) {
            Core.getInstance().getOffscreenBuffer().startDrawing(false, false);
            Core.getInstance().TakeScreenshot(256, 256, TextureFBO.getFuncs().GL_COLOR_ATTACHMENT0());
            Core.getInstance().getOffscreenBuffer().endDrawing();
         } else {
            SavefileThumbnail.createWithRenderShader(this.m_playerIndex);
         }
      }
   }

   private static final class FixCameraDrawer extends TextureDraw.GenericDrawer {
      int m_playerIndex;
      float m_zoom;
      int m_offscreenWidth;
      int m_offscreenHeight;

      FixCameraDrawer(int var1) {
         PlayerCamera var2 = IsoCamera.cameras[var1];
         this.m_playerIndex = var1;
         this.m_zoom = var2.zoom;
         this.m_offscreenWidth = var2.OffscreenWidth;
         this.m_offscreenHeight = var2.OffscreenHeight;
      }

      public void render() {
         SpriteRenderState var1 = SpriteRenderer.instance.getRenderingState();
         var1.playerCamera[this.m_playerIndex].zoom = this.m_zoom;
         var1.playerCamera[this.m_playerIndex].OffscreenWidth = this.m_offscreenWidth;
         var1.playerCamera[this.m_playerIndex].OffscreenHeight = this.m_offscreenHeight;
         var1.zoomLevel[this.m_playerIndex] = this.m_zoom;
      }
   }
}
