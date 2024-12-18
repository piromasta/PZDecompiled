package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.CharacterModelCamera;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.interfaces.ITexture;
import zombie.iso.IsoCamera;
import zombie.popman.ObjectPool;

public final class ModelOutlines {
   public static final ModelOutlines instance = new ModelOutlines();
   public TextureFBO m_fboA;
   public TextureFBO m_fboB;
   public TextureFBO m_fboC;
   public boolean m_dirty = false;
   private int m_playerIndex;
   private final ColorInfo m_outlineColor = new ColorInfo();
   private boolean m_behindPlayer = false;
   private ModelSlotRenderData m_playerRenderData;
   private ShaderProgram m_thickenHShader;
   private ShaderProgram m_thickenVShader;
   private ShaderProgram m_blitShader;
   private final ObjectPool<Drawer> m_drawerPool = new ObjectPool(Drawer::new);

   public ModelOutlines() {
   }

   public void startFrameMain(int var1) {
      Drawer var2 = (Drawer)this.m_drawerPool.alloc();
      var2.m_startFrame = true;
      var2.m_playerIndex = var1;
      SpriteRenderer.instance.drawGeneric(var2);
   }

   public void endFrameMain(int var1) {
      Drawer var2 = (Drawer)this.m_drawerPool.alloc();
      var2.m_startFrame = false;
      var2.m_playerIndex = var1;
      SpriteRenderer.instance.drawGeneric(var2);
   }

   public void startFrame(int var1) {
      this.m_dirty = false;
      this.m_playerIndex = var1;
      this.m_playerRenderData = null;
   }

   public void checkFBOs() {
      if (this.m_fboA != null && (this.m_fboA.getWidth() != Core.width || this.m_fboB.getHeight() != Core.height)) {
         this.m_fboA.destroy();
         this.m_fboB.destroy();
         this.m_fboC.destroy();
         this.m_fboA = null;
         this.m_fboB = null;
         this.m_fboC = null;
      }

      if (this.m_fboA == null) {
         Texture var1 = new Texture(Core.width, Core.height, 16);
         this.m_fboA = new TextureFBO(var1, false);
         Texture var2 = new Texture(Core.width, Core.height, 16);
         this.m_fboB = new TextureFBO(var2, false);
         Texture var3 = new Texture(Core.width, Core.height, 16);
         this.m_fboC = new TextureFBO(var3, false);
      }

   }

   public void setPlayerRenderData(ModelSlotRenderData var1) {
      this.m_playerRenderData = var1;
   }

   public boolean beginRenderOutline(ColorInfo var1, boolean var2, boolean var3) {
      if (!var3 && this.m_dirty && (!this.m_outlineColor.equals(var1) || var2 != this.m_behindPlayer)) {
         this.endFrame(SpriteRenderer.instance.getRenderingPlayerIndex());
         this.m_dirty = false;
      }

      this.m_outlineColor.set(var1);
      this.m_behindPlayer = var2;
      if (this.m_dirty) {
         return false;
      } else {
         this.m_dirty = true;
         this.checkFBOs();
         return true;
      }
   }

   public void endFrame(int var1) {
      if (this.m_dirty) {
         this.m_playerIndex = var1;
         if (this.m_thickenHShader == null) {
            this.m_thickenHShader = ShaderProgram.createShaderProgram("aim_outline_h", false, false, true);
            this.m_thickenVShader = ShaderProgram.createShaderProgram("aim_outline_v", false, false, true);
            this.m_blitShader = ShaderProgram.createShaderProgram("aim_outline_blit", false, false, true);
         }

         int var2 = IsoCamera.getScreenLeft(this.m_playerIndex);
         int var3 = IsoCamera.getScreenTop(this.m_playerIndex);
         int var4 = IsoCamera.getScreenWidth(this.m_playerIndex);
         int var5 = IsoCamera.getScreenHeight(this.m_playerIndex);
         Matrix4f var10 = Core.getInstance().projectionMatrixStack.alloc();
         var10.setOrtho2D(0.0F, (float)var4, (float)var5, 0.0F);
         Core.getInstance().projectionMatrixStack.push(var10);
         Matrix4f var11 = Core.getInstance().modelViewMatrixStack.alloc();
         var11.identity();
         Core.getInstance().modelViewMatrixStack.push(var11);
         float var12 = (float)this.m_fboA.getWidth();
         float var13 = (float)this.m_fboA.getHeight();
         float var14 = SpriteRenderer.instance.getPlayerZoomLevel();
         float var15 = PZMath.lerp(0.5F, 0.2F, var14 / 2.5F);
         GL11.glBlendFunc(770, 771);
         this.m_fboB.startDrawing(true, true);
         GL11.glViewport(var2, var3, var4, var5);
         this.m_thickenHShader.Start();
         this.m_thickenHShader.setVector2("u_resolution", var12, var13);
         this.m_thickenHShader.setValue("u_radius", var15);
         this.m_thickenHShader.setVector4("u_color", this.m_outlineColor.r, this.m_outlineColor.g, this.m_outlineColor.b, this.m_outlineColor.a);
         this.renderTexture(this.m_fboA.getTexture(), var2, var3, var4, var5, this.m_thickenHShader);
         this.m_thickenHShader.End();
         this.m_fboB.endDrawing();
         this.m_fboC.startDrawing(true, true);
         GL11.glViewport(var2, var3, var4, var5);
         this.m_thickenVShader.Start();
         this.m_thickenVShader.setVector2("u_resolution", var12, var13);
         this.m_thickenVShader.setValue("u_radius", var15);
         this.m_thickenVShader.setVector4("u_color", this.m_outlineColor.r, this.m_outlineColor.g, this.m_outlineColor.b, this.m_outlineColor.a);
         this.renderTexture(this.m_fboB.getTexture(), var2, var3, var4, var5, this.m_thickenVShader);
         this.m_thickenVShader.End();
         this.m_fboC.endDrawing();
         if (this.m_playerRenderData != null && this.m_behindPlayer) {
            float var16 = CharacterModelCamera.instance.m_x;
            float var17 = CharacterModelCamera.instance.m_y;
            float var18 = CharacterModelCamera.instance.m_z;
            boolean var19 = CharacterModelCamera.instance.m_bInVehicle;
            float var20 = CharacterModelCamera.instance.m_useAngle;
            boolean var21 = CharacterModelCamera.instance.m_bUseWorldIso;
            boolean var22 = CharacterModelCamera.instance.bDepthMask;
            ModelCamera var23 = ModelCamera.instance;
            CharacterModelCamera.instance.m_x = this.m_playerRenderData.x;
            CharacterModelCamera.instance.m_y = this.m_playerRenderData.y;
            CharacterModelCamera.instance.m_z = this.m_playerRenderData.z;
            CharacterModelCamera.instance.m_bInVehicle = this.m_playerRenderData.bInVehicle;
            CharacterModelCamera.instance.m_useAngle = this.m_playerRenderData.animPlayerAngle;
            CharacterModelCamera.instance.m_bUseWorldIso = true;
            CharacterModelCamera.instance.bDepthMask = false;
            ModelCamera.instance = CharacterModelCamera.instance;
            GL11.glViewport(var2, var3, var4, var5);
            this.m_playerRenderData.performRenderCharacterOutline(true, this.m_outlineColor, this.m_behindPlayer);
            CharacterModelCamera.instance.m_x = var16;
            CharacterModelCamera.instance.m_y = var17;
            CharacterModelCamera.instance.m_z = var18;
            CharacterModelCamera.instance.m_bInVehicle = var19;
            CharacterModelCamera.instance.m_useAngle = var20;
            CharacterModelCamera.instance.m_bUseWorldIso = var21;
            CharacterModelCamera.instance.bDepthMask = var22;
            ModelCamera.instance = var23;
         }

         GL11.glBlendFunc(770, 771);
         GL11.glViewport(var2, var3, var4, var5);
         this.m_blitShader.Start();
         this.m_blitShader.setSamplerUnit("texture", 0);
         this.m_blitShader.setSamplerUnit("mask", 1);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, this.m_fboA.getTexture().getID());
         GL13.glActiveTexture(33984);
         this.renderTexture(this.m_fboC.getTexture(), var2, var3, var4, var5, this.m_blitShader);
         this.m_blitShader.End();
         Core.getInstance().projectionMatrixStack.pop();
         Core.getInstance().modelViewMatrixStack.pop();
         SpriteRenderer.ringBuffer.restoreBoundTextures = true;
      }
   }

   private void renderTexture(ITexture var1, int var2, int var3, int var4, int var5, ShaderProgram var6) {
      VBORenderer var7 = VBORenderer.getInstance();
      var7.startRun(var7.FORMAT_PositionColorUV);
      var7.setMode(7);
      var7.setTextureID(((Texture)var1).getTextureId());
      var7.setShaderProgram(var6);
      float var8 = (float)var2 / (float)var1.getWidthHW();
      float var9 = (float)(var3 + var5) / (float)var1.getHeightHW();
      float var10 = (float)(var2 + var4) / (float)var1.getWidthHW();
      float var11 = (float)var3 / (float)var1.getHeightHW();
      float var12 = 0.0F;
      byte var14 = 0;
      byte var13 = 0;
      var7.addQuad((float)var13, (float)var14, var8, var9, (float)(var13 + var4), (float)(var14 + var5), var10, var11, var12, 1.0F, 1.0F, 1.0F, 1.0F);
      var7.endRun();
      var7.flush();
   }

   public void renderDebug() {
   }

   public static final class Drawer extends TextureDraw.GenericDrawer {
      boolean m_startFrame;
      int m_playerIndex;

      public Drawer() {
      }

      public void render() {
         if (this.m_startFrame) {
            ModelOutlines.instance.startFrame(this.m_playerIndex);
         } else {
            ModelOutlines.instance.endFrame(this.m_playerIndex);
         }

      }

      public void postRender() {
         ModelOutlines.instance.m_drawerPool.release((Object)this);
      }
   }
}
