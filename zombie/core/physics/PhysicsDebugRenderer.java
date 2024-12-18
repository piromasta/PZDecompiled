package zombie.core.physics;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.math.PZMath;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.popman.ObjectPool;

public final class PhysicsDebugRenderer extends TextureDraw.GenericDrawer {
   private static final ObjectPool<PhysicsDebugRenderer> POOL = new ObjectPool(PhysicsDebugRenderer::new);
   private float camOffX;
   private float camOffY;
   private float deferredX;
   private float deferredY;
   private int drawOffsetX;
   private int drawOffsetY;
   private int playerIndex;
   private float playerX;
   private float playerY;
   private float playerZ;
   private float offscreenWidth;
   private float offscreenHeight;
   private final TFloatArrayList elements = new TFloatArrayList();
   private static ArrayList<RagdollController> renderedRagdollControllers = new ArrayList();
   private static ArrayList<BallisticsController> renderedBallisticsControllers = new ArrayList();
   private static ArrayList<BallisticsTarget> renderedBallisticsTargets = new ArrayList();

   public static PhysicsDebugRenderer alloc() {
      return (PhysicsDebugRenderer)POOL.alloc();
   }

   public void release() {
      POOL.release((Object)this);
   }

   public PhysicsDebugRenderer() {
   }

   public void init(IsoPlayer var1) {
      this.playerIndex = var1.getPlayerNum();
      this.camOffX = IsoCamera.getRightClickOffX() + (float)IsoCamera.PLAYER_OFFSET_X;
      this.camOffY = IsoCamera.getRightClickOffY() + (float)IsoCamera.PLAYER_OFFSET_Y;
      this.camOffX += this.XToScreenExact(var1.getX() - (float)PZMath.fastfloor(var1.getX()), var1.getY() - (float)PZMath.fastfloor(var1.getY()), 0.0F, 0);
      this.camOffY += this.YToScreenExact(var1.getX() - (float)PZMath.fastfloor(var1.getX()), var1.getY() - (float)PZMath.fastfloor(var1.getY()), 0.0F, 0);
      this.deferredX = IsoCamera.cameras[this.playerIndex].DeferedX;
      this.deferredY = IsoCamera.cameras[this.playerIndex].DeferedY;
      this.drawOffsetX = PZMath.fastfloor(var1.getX());
      this.drawOffsetY = PZMath.fastfloor(var1.getY());
      this.playerX = var1.getX();
      this.playerY = var1.getY();
      this.playerZ = IsoCamera.frameState.CamCharacterZ;
      this.offscreenWidth = (float)Core.getInstance().getOffscreenWidth(this.playerIndex);
      this.offscreenHeight = (float)Core.getInstance().getOffscreenHeight(this.playerIndex);
      this.elements.clear();
      int var2 = PZMath.fastfloor(WorldSimulation.instance.offsetX) - this.drawOffsetX;
      int var3 = PZMath.fastfloor(WorldSimulation.instance.offsetY) - this.drawOffsetY;
      int var4 = -32;
      int var5 = 31;
      if (DebugOptions.instance.PhysicsRenderPlayerLevelOnly.getValue()) {
         var4 = var5 = PZMath.fastfloor(this.playerZ);
      }

      Iterator var6;
      if (DebugOptions.instance.PhysicsRenderBallisticsControllers.getValue()) {
         var6 = renderedBallisticsControllers.iterator();

         while(var6.hasNext()) {
            BallisticsController var7 = (BallisticsController)var6.next();
            this.renderBallistics(var7.getID(), var2, var3);
         }
      }

      renderedBallisticsControllers.clear();
      if (DebugOptions.instance.PhysicsRenderBallisticsTargets.getValue()) {
         var6 = renderedBallisticsTargets.iterator();

         while(var6.hasNext()) {
            BallisticsTarget var8 = (BallisticsTarget)var6.next();
            if (var8.isValidIsoGameCharacter()) {
               this.renderBallisticsTarget(var8.getID(), var2, var3);
            }
         }
      }

      renderedBallisticsTargets.clear();
      if (DebugOptions.instance.PhysicsRender.getValue()) {
         this.n_debugDrawWorld(var2, var3, var4, var5);
      }

   }

   public static void addRagdollRender(RagdollController var0) {
      if (!renderedRagdollControllers.contains(var0)) {
         renderedRagdollControllers.add(var0);
      }

   }

   public static void addBallisticsRender(BallisticsController var0) {
      if (!renderedBallisticsControllers.contains(var0)) {
         renderedBallisticsControllers.add(var0);
      }

   }

   public static void addBallisticsRender(BallisticsTarget var0) {
      if (!renderedBallisticsTargets.contains(var0)) {
         renderedBallisticsTargets.add(var0);
      }

   }

   public static void removeRagdollRender(RagdollController var0) {
      renderedRagdollControllers.remove(var0);
   }

   public static void removeBallisticsRender(BallisticsTarget var0) {
      renderedBallisticsTargets.remove(var0);
   }

   public void render() {
      DebugLog.Physics.debugOnceln("");
      GL11.glPushAttrib(1048575);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      boolean var1 = DefaultShader.isActive;
      int var2 = GL11.glGetInteger(35725);
      GL20.glUseProgram(0);
      Matrix4f var3 = Core.getInstance().projectionMatrixStack.alloc();
      var3.setOrtho(0.0F, this.offscreenWidth, this.offscreenHeight, 0.0F, 10000.0F, -10000.0F);
      Core.getInstance().projectionMatrixStack.push(var3);
      Matrix4f var4 = Core.getInstance().modelViewMatrixStack.alloc();
      var4.identity();
      int var5 = -this.drawOffsetX;
      int var6 = -this.drawOffsetY;
      float var7 = this.deferredX;
      float var8 = this.deferredY;
      var4.translate(this.offscreenWidth / 2.0F, this.offscreenHeight / 2.0F, 0.0F);
      float var9 = this.XToScreenExact(var7, var8, this.playerZ, 0);
      float var10 = this.YToScreenExact(var7, var8, this.playerZ, 0);
      var9 += this.camOffX;
      var10 += this.camOffY;
      var4.translate(-var9, -var10, 0.0F);
      var5 = (int)((float)var5 + WorldSimulation.instance.offsetX);
      var6 = (int)((float)var6 + WorldSimulation.instance.offsetY);
      int var11 = 32 * Core.TileScale;
      float var12 = (float)Math.sqrt((double)(var11 * var11 + var11 * var11));
      var4.scale(var12, var12, var12);
      var4.rotate(3.6651914F, 1.0F, 0.0F, 0.0F);
      var4.rotate(-0.7853982F, 0.0F, 1.0F, 0.0F);
      Core.getInstance().modelViewMatrixStack.push(var4);
      VBORenderer var13 = VBORenderer.getInstance();
      var13.startRun(var13.FORMAT_PositionColor);
      var13.setMode(1);
      var13.setLineWidth(1.0F);
      var13.setDepthTest(false);
      int var14 = 0;

      while(var14 < this.elements.size()) {
         float var15 = this.elements.getQuick(var14++);
         float var16 = this.elements.getQuick(var14++);
         float var17 = this.elements.getQuick(var14++);
         float var18 = this.elements.getQuick(var14++);
         float var19 = this.elements.getQuick(var14++);
         float var20 = this.elements.getQuick(var14++);
         float var21 = this.elements.getQuick(var14++);
         float var22 = this.elements.getQuick(var14++);
         float var23 = this.elements.getQuick(var14++);
         float var24 = this.elements.getQuick(var14++);
         float var25 = this.elements.getQuick(var14++);
         float var26 = this.elements.getQuick(var14++);
         var13.addLine(var15, var16, var17, var18, var19, var20, var21, var22, var23, 0.5F, var24, var25, var26, 0.5F);
      }

      var13.endRun();
      var13.flush();
      GL11.glLineWidth(1.0F);
      Core.getInstance().projectionMatrixStack.pop();
      Core.getInstance().modelViewMatrixStack.pop();
      GL11.glEnable(3042);
      GL11.glEnable(3553);
      GL11.glPopAttrib();
      GL20.glUseProgram(var2);
      DefaultShader.isActive = var1;
      ShaderHelper.forgetCurrentlyBound();
      Texture.lastTextureID = -1;
   }

   public void postRender() {
      this.release();
   }

   public float YToScreenExact(float var1, float var2, float var3, int var4) {
      return IsoUtils.YToScreen(var1, var2, var3, var4);
   }

   public float XToScreenExact(float var1, float var2, float var3, int var4) {
      return IsoUtils.XToScreen(var1, var2, var3, var4);
   }

   public void drawLine(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12) {
      DebugLog.Physics.debugOnceln("Called from JNI");
      if (!(var1 < -1000.0F) && !(var1 > 1000.0F) && !(var2 < -1000.0F) && !(var2 > 1000.0F)) {
         this.elements.add(var1);
         this.elements.add(var2);
         this.elements.add(var3);
         this.elements.add(var4);
         this.elements.add(var5);
         this.elements.add(var6);
         this.elements.add(var7);
         this.elements.add(var8);
         this.elements.add(var9);
         this.elements.add(var10);
         this.elements.add(var11);
         this.elements.add(var12);
      }
   }

   public void drawSphere(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
   }

   public void drawTriangle(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13) {
      DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
   }

   public void drawContactPoint(float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8, float var9, float var10, float var11) {
      DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
   }

   public void drawCapsule(float var1, float var2, int var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14) {
      DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
   }

   public native void n_debugDrawWorld(int var1, int var2, int var3, int var4);

   public native void renderRagdoll(int var1, int var2, int var3);

   public native void renderBallistics(int var1, int var2, int var3);

   public native void renderBallisticsTarget(int var1, int var2, int var3);
}
