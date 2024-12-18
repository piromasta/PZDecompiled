package zombie.iso.fboRenderChunk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.GameTime;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.vehicles.BaseVehicle;

public final class FBORenderTracerEffects {
   private static FBORenderTracerEffects instance;
   private final ArrayList<Effect> m_effects = new ArrayList();
   private final ObjectPool<Effect> m_effectPool = new ObjectPool(Effect::new);
   private final ObjectPool<Drawer> m_drawerPool = new ObjectPool(Drawer::new);
   public final HashMap<IsoGameCharacter, Matrix4f> playerWeaponTransform = new HashMap();
   private static final int VERSION = 1;
   private final ArrayList<ConfigOption> options = new ArrayList();
   final DoubleConfigOption1 StartRadius = new DoubleConfigOption1("StartRadius", 0.0010000000474974513, 0.019999999552965164, 0.0024999999441206455);
   final DoubleConfigOption1 EndRadius = new DoubleConfigOption1("EndRadius", 0.0010000000474974513, 0.019999999552965164, 0.009999999776482582);
   final DoubleConfigOption1 Length = new DoubleConfigOption1("Length", 0.10000000149011612, 10.0, 1.0);
   final DoubleConfigOption1 Speed = new DoubleConfigOption1("Speed", 0.0010000000474974513, 0.5, 0.07500000298023224);
   final DoubleConfigOption1 Red = new DoubleConfigOption1("Red", 0.0, 1.0, 1.0);
   final DoubleConfigOption1 Green = new DoubleConfigOption1("Green", 0.0, 1.0, 1.0);
   final DoubleConfigOption1 Blue = new DoubleConfigOption1("Blue", 0.0, 1.0, 1.0);
   final DoubleConfigOption1 Alpha = new DoubleConfigOption1("Alpha", 0.0, 1.0, 1.0);

   public static FBORenderTracerEffects getInstance() {
      if (instance == null) {
         instance = new FBORenderTracerEffects();
      }

      return instance;
   }

   private FBORenderTracerEffects() {
      this.load();
   }

   public void releaseWeaponTransform(IsoGameCharacter var1) {
      Matrix4f var2 = (Matrix4f)this.playerWeaponTransform.remove(var1);
      if (var2 != null) {
         BaseVehicle.releaseMatrix4f(var2);
      }
   }

   public void storeWeaponTransform(IsoGameCharacter var1, Matrix4f var2) {
      if (DebugOptions.instance.FBORenderChunk.BulletTracers.getValue()) {
         Matrix4f var3 = (Matrix4f)this.playerWeaponTransform.remove(var1);
         if (var3 != null) {
            BaseVehicle.releaseMatrix4f(var3);
         }

         if (var1 != null && var1.primaryHandModel != null && var1.primaryHandModel.m_modelScript != null) {
            ModelAttachment var4 = var1.primaryHandModel.m_modelScript.getAttachmentById("muzzle");
            if (var4 != null) {
               var3 = BaseVehicle.allocMatrix4f();
               var3.set(var2);
               var3.transpose();
               Matrix4f var5 = BaseVehicle.allocMatrix4f();
               ModelInstanceRenderData.makeAttachmentTransform(var4, var5);
               var3.mul(var5);
               BaseVehicle.releaseMatrix4f(var5);
               this.playerWeaponTransform.put(var1, var3);
            }
         }
      }
   }

   public void addEffect(IsoGameCharacter var1, float var2) {
      if (var1 != null && var1.getAnimationPlayer().isReady()) {
         if (this.playerWeaponTransform.containsKey(var1)) {
            Effect var3 = (Effect)this.m_effectPool.alloc();
            var3.x0 = var1.getX();
            var3.y0 = var1.getY();
            var3.z0 = var1.getZ();
            var3.angle = var1.getAnimationPlayer().getRenderedAngle();
            var3.range = var2;
            var3.r1 = (float)this.Red.getValue();
            var3.g1 = (float)this.Green.getValue();
            var3.b1 = (float)this.Blue.getValue();
            var3.a1 = (float)this.Alpha.getValue();
            var3.thickness0 = (float)this.StartRadius.getValue();
            var3.thickness1 = (float)this.EndRadius.getValue();
            var3.length = (float)this.Length.getValue();
            var3.speed = (float)this.Speed.getValue();
            var3.t = 0.0F;
            var3.weaponXfrm.set((Matrix4fc)this.playerWeaponTransform.get(var1));
            this.m_effects.add(var3);
         }
      }
   }

   public void render() {
      Drawer var1 = (Drawer)this.m_drawerPool.alloc();

      for(int var2 = 0; var2 < this.m_effects.size(); ++var2) {
         Effect var3 = (Effect)this.m_effects.get(var2);
         Effect var4 = ((Effect)this.m_effectPool.alloc()).set(var3);
         var1.m_effects.add(var4);
         if (!GameTime.isGamePaused()) {
            var3.t += GameTime.getInstance().getMultiplier() * var3.speed;
         }

         if (var3.t >= 1.0F) {
            this.m_effects.remove(var2--);
            this.m_effectPool.release((Object)var3);
         }
      }

      SpriteRenderer.instance.drawGeneric(var1);
   }

   private void registerOption(ConfigOption var1) {
      this.options.add(var1);
   }

   public int getOptionCount() {
      return this.options.size();
   }

   public ConfigOption getOptionByIndex(int var1) {
      return (ConfigOption)this.options.get(var1);
   }

   public ConfigOption getOptionByName(String var1) {
      for(int var2 = 0; var2 < this.options.size(); ++var2) {
         ConfigOption var3 = (ConfigOption)this.options.get(var2);
         if (var3.getName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public void save() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "bulletTracerEffect-options.ini";
      ConfigFile var2 = new ConfigFile();
      var2.write(var1, 1, this.options);
   }

   public void load() {
      String var10000 = ZomboidFileSystem.instance.getCacheDir();
      String var1 = var10000 + File.separator + "bulletTracerEffect-options.ini";
      ConfigFile var2 = new ConfigFile();
      if (var2.read(var1)) {
         for(int var3 = 0; var3 < var2.getOptions().size(); ++var3) {
            ConfigOption var4 = (ConfigOption)var2.getOptions().get(var3);
            ConfigOption var5 = this.getOptionByName(var4.getName());
            if (var5 != null) {
               var5.parse(var4.getValueAsString());
            }
         }
      }

   }

   public class DoubleConfigOption1 extends DoubleConfigOption {
      public DoubleConfigOption1(String var2, double var3, double var5, double var7) {
         super(var2, var3, var5, var7);
         FBORenderTracerEffects.this.registerOption(this);
      }
   }

   private static final class Effect {
      float x0;
      float y0;
      float z0;
      float angle;
      float range;
      float r0;
      float g0;
      float b0;
      float a0;
      float r1;
      float g1;
      float b1;
      float a1;
      float thickness0;
      float thickness1;
      float length;
      float speed;
      float t = -1.0F;
      final Matrix4f PROJECTION = new Matrix4f();
      final Matrix4f VIEW = new Matrix4f();
      final Matrix4f MODEL = new Matrix4f();
      final Matrix4f weaponXfrm = new Matrix4f();

      private Effect() {
      }

      Effect set(Effect var1) {
         this.x0 = var1.x0;
         this.y0 = var1.y0;
         this.z0 = var1.z0;
         this.angle = var1.angle;
         this.range = var1.range;
         this.r1 = var1.r1;
         this.g1 = var1.g1;
         this.b1 = var1.b1;
         this.a1 = var1.a1;
         this.thickness0 = var1.thickness0;
         this.thickness1 = var1.thickness1;
         this.length = var1.length;
         this.speed = var1.speed;
         this.t = var1.t;
         this.PROJECTION.set(var1.PROJECTION);
         this.VIEW.set(var1.VIEW);
         this.MODEL.set(var1.MODEL);
         this.weaponXfrm.set(var1.weaponXfrm);
         return this;
      }

      void update() {
      }

      void render() {
         VBORenderer var1 = VBORenderer.getInstance();
         var1.addCylinder_Fill(this.thickness0, this.thickness1, this.length, 4, 1, this.r1, this.g1, this.b1, this.a1);
      }
   }

   private static final class Drawer extends TextureDraw.GenericDrawer {
      static final Matrix4f tempMatrix4f_1 = new Matrix4f();
      static final Vector3f tempVector3f_1 = new Vector3f();
      final ArrayList<Effect> m_effects = new ArrayList();

      private Drawer() {
      }

      public void render() {
         GL11.glDepthFunc(515);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         VBORenderer.getInstance().setDepthTestForAllRuns(Boolean.TRUE);
         float var1 = (Float)Core.getInstance().FloatParamMap.get(0);
         float var2 = (Float)Core.getInstance().FloatParamMap.get(1);

         for(int var3 = 0; var3 < this.m_effects.size(); ++var3) {
            Effect var4 = (Effect)this.m_effects.get(var3);
            calculateProjectViewXfrm(var4.PROJECTION, var4.VIEW, true);
            PZGLUtil.pushAndLoadMatrix(5889, var4.PROJECTION);
            calculateModelXfrm(var4.x0, var4.y0, var4.z0, var4.angle, false, var4.MODEL, true);
            Matrix4f var5 = tempMatrix4f_1.set(var4.VIEW);
            var5.mul(var4.MODEL);
            var5.mul(var4.weaponXfrm);
            var5.translate(0.0F, 0.0F, var4.t * var4.range);
            PZGLUtil.pushAndLoadMatrix(5888, var5);
            var5.set(var4.MODEL);
            var5.mul(var4.weaponXfrm);
            var5.setTranslation(0.0F, 0.0F, 0.0F);
            var5.translate(0.0F, 0.0F, var4.t * var4.range);
            Vector3f var6 = var5.transformPosition(tempVector3f_1.set(0.0F));
            float var7 = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
            IsoDepthHelper.Results var8 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(var1), PZMath.fastfloor(var2), var4.x0 - var6.x, var4.y0 - var6.z, var4.z0 * 0.0F + var6.y);
            float var9 = var8.depthStart - (var7 + 1.0F) / 2.0F;
            VBORenderer.getInstance().setUserDepthForAllRuns(var9);
            var4.render();
            VBORenderer.getInstance().flush();
            PZGLUtil.popMatrix(5888);
            PZGLUtil.popMatrix(5889);
         }

         VBORenderer.getInstance().setDepthTestForAllRuns((Boolean)null);
         VBORenderer.getInstance().setUserDepthForAllRuns((Float)null);
         GLStateRenderThread.restore();
      }

      public void postRender() {
         FBORenderTracerEffects.getInstance().m_effectPool.releaseAll(this.m_effects);
         this.m_effects.clear();
         FBORenderTracerEffects.getInstance().m_drawerPool.release((Object)this);
      }

      static Matrix4f calculateProjectViewXfrm(Matrix4f var0, Matrix4f var1, boolean var2) {
         int var3 = var2 ? SpriteRenderer.instance.getRenderingPlayerIndex() : IsoCamera.frameState.playerIndex;
         PlayerCamera var4 = var2 ? SpriteRenderer.instance.getRenderingPlayerCamera(var3) : IsoCamera.cameras[var3];
         float var5 = var2 ? (float)var4.OffscreenWidth : (float)IsoCamera.getOffscreenWidth(var3);
         float var6 = var2 ? (float)var4.OffscreenHeight : (float)IsoCamera.getOffscreenHeight(var3);
         double var7 = (double)(var5 / 1920.0F);
         double var9 = (double)(var6 / 1920.0F);
         var0.setOrtho(-((float)var7) / 2.0F, (float)var7 / 2.0F, -((float)var9) / 2.0F, (float)var9 / 2.0F, -10.0F, 10.0F);
         var1.scaling(Core.scale);
         var1.scale((float)Core.TileScale / 2.0F);
         var1.rotate(0.5235988F, 1.0F, 0.0F, 0.0F);
         var1.rotate(2.3561945F, 0.0F, 1.0F, 0.0F);
         return var1;
      }

      static void calculateModelXfrm(float var0, float var1, float var2, float var3, boolean var4, Matrix4f var5, boolean var6) {
         int var7 = var6 ? SpriteRenderer.instance.getRenderingPlayerIndex() : IsoCamera.frameState.playerIndex;
         float var8 = var6 ? (Float)Core.getInstance().FloatParamMap.get(0) : IsoCamera.frameState.CamCharacterX;
         float var9 = var6 ? (Float)Core.getInstance().FloatParamMap.get(1) : IsoCamera.frameState.CamCharacterY;
         float var10 = var6 ? (Float)Core.getInstance().FloatParamMap.get(2) : IsoCamera.frameState.CamCharacterZ;
         double var11 = (double)var8;
         double var13 = (double)var9;
         double var15 = (double)var10;
         PlayerCamera var17 = var6 ? SpriteRenderer.instance.getRenderingPlayerCamera(var7) : IsoCamera.cameras[var7];
         float var18 = var17.RightClickX;
         float var19 = var17.RightClickY;
         float var20 = var17.getTOffX();
         float var21 = var17.getTOffY();
         float var22 = var17.DeferedX;
         float var23 = var17.DeferedY;
         var11 -= (double)var17.XToIso(-var20 - var18, -var21 - var19, 0.0F);
         var13 -= (double)var17.YToIso(-var20 - var18, -var21 - var19, 0.0F);
         var11 += (double)var22;
         var13 += (double)var23;
         double var24 = (double)var0 - var11;
         double var26 = (double)var1 - var13;
         var5.identity();
         var5.translate(-((float)var24), (float)((double)var2 - var15) * 2.44949F, -((float)var26));
         if (var4) {
            var5.scale(-1.0F, 1.0F, 1.0F);
         } else {
            var5.scale(-1.5F, 1.5F, 1.5F);
         }

         var5.rotate(var3 + 3.1415927F, 0.0F, 1.0F, 0.0F);
         if (!var4) {
            var5.translate(0.0F, -0.48F, 0.0F);
         }

      }
   }
}
