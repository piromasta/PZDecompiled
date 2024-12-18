package zombie.iso.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector3f;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.SoundManager;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.debug.LineDrawer;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector3;
import zombie.popman.ObjectPool;

public final class IsoBulletTracerEffects {
   private static IsoBulletTracerEffects instance;
   private final ArrayList<Effect> m_effects = new ArrayList();
   private final ObjectPool<Effect> m_effectPool = new ObjectPool(Effect::new);
   private final HashMap<String, IsoBulletTracerEffectsConfigOptions> isoBulletTracerEffectsConfigOptionsHashMap = new HashMap();

   public IsoBulletTracerEffects() {
   }

   public static IsoBulletTracerEffects getInstance() {
      if (instance == null) {
         instance = new IsoBulletTracerEffects();
      }

      return instance;
   }

   public HashMap<String, IsoBulletTracerEffectsConfigOptions> getIsoBulletTracerEffectsConfigOptionsHashMap() {
      return this.isoBulletTracerEffectsConfigOptionsHashMap;
   }

   public Effect addEffect(IsoGameCharacter var1, float var2) {
      Effect var3 = this.createEffect(var1);
      if (var3 == null) {
         return null;
      } else {
         var3.playSound = true;
         float var4 = var1.getLookAngleRadians();
         var3.directionVector.x = (float)Math.cos((double)var4);
         var3.directionVector.y = (float)Math.sin((double)var4);
         var3.directionVector.z = 0.0F;
         var3.directionVector.normalize(1.0F);
         var3.start.set(var3.x0, var3.y0, var3.z0);
         var3.end.set(var3.x0 + var3.directionVector.x * var2, var3.y0 + var3.directionVector.y * var2, var3.z0 + var3.directionVector.z * var2);
         var3.range = var2;
         var3.currentRange = var3.start.distance(var3.end);
         return var3;
      }
   }

   public Effect addEffect(IsoGameCharacter var1, float var2, float var3, float var4, float var5) {
      return this.addEffect(var1, var2, var3, var4, var5, (IsoGridSquare)null);
   }

   public Effect addEffect(IsoGameCharacter var1, float var2, float var3, float var4, float var5, IsoGridSquare var6) {
      Effect var7 = this.createEffect(var1);
      if (var7 == null) {
         return null;
      } else {
         var7.isoGridSquare = var6;
         var7.playSound = true;
         var7.start.set(var7.x0, var7.y0, var7.z0);
         var7.end.set(var3, var4, var5);
         directionVector(var7.directionVector, var7.start, var7.end);
         var7.directionVector.normalize(1.0F);
         var7.range = var2;
         var7.currentRange = var7.start.distance(var7.end);
         return var7;
      }
   }

   public static void directionVector(Vector3f var0, Vector3f var1, Vector3f var2) {
      var0.set(var2.x - var1.x, var2.y - var1.y, var2.z - var1.z);
   }

   private Effect createEffect(IsoGameCharacter var1) {
      if (var1 != null && var1.getAnimationPlayer().isReady()) {
         BallisticsController var2 = var1.getBallisticsController();
         if (var2 == null) {
            return null;
         } else {
            Effect var3 = (Effect)this.m_effectPool.alloc();
            var3.ammoType = var1.getAttackingWeapon().getAmmoType();
            IsoBulletTracerEffectsConfigOptions var4 = (IsoBulletTracerEffectsConfigOptions)this.isoBulletTracerEffectsConfigOptionsHashMap.get(var3.ammoType);
            var3.x0 = var1.getX();
            var3.y0 = var1.getY();
            var3.z0 = var1.getZ();
            var3.projectileRed = (float)var4.projectileRed.getValue();
            var3.projectileGreen = (float)var4.projectileGreen.getValue();
            var3.projectileBlue = (float)var4.projectileBlue.getValue();
            var3.projectileAlpha = (float)var4.projectileAlpha.getValue();
            var3.projectileLength = (float)var4.projectileLength.getValue();
            var3.projectileStartThickness = (float)var4.projectileStartThickeness.getValue();
            var3.projectileEndThickness = (float)var4.projectileEndThickness.getValue();
            var3.projectileFadeRate = (float)var4.projectileFadeRate.getValue();
            var3.projectileTrailRed = (float)var4.projectileTrailRed.getValue();
            var3.projectileTrailGreen = (float)var4.projectileTrailGreen.getValue();
            var3.projectileTrailBlue = (float)var4.projectileTrailBlue.getValue();
            var3.projectileTrailAlpha = (float)var4.projectileTrailAlpha.getValue();
            var3.projectileTrailLength = (float)var4.projectileTrailLength.getValue();
            var3.projectileTrailStartThickness = (float)var4.projectileTrailStartThickness.getValue();
            var3.projectileTrailEndThickness = (float)var4.projectileTrailEndThickness.getValue();
            var3.projectilePathRed = (float)var4.projectilePathRed.getValue();
            var3.projectilePathGreen = (float)var4.projectilePathGreen.getValue();
            var3.projectilePathBlue = (float)var4.projectilePathBlue.getValue();
            var3.projectilePathAlpha = (float)var4.projectilePathAlpha.getValue();
            var3.projectilePathStartThickness = (float)var4.projectilePathStartThickness.getValue();
            var3.projectilePathEndThickness = (float)var4.projectilePathEndThickness.getValue();
            var3.projectilePathFadeRate = (float)var4.projectilePathFadeRate.getValue();
            var3.projectilePathTime = (float)var4.projectilePathTime.getValue();
            var3.projectileSpeed = (float)var4.projectileSpeed.getValue();
            var2.update();
            Vector3 var5 = var2.getMuzzlePosition();
            var3.x0 = var5.x;
            var3.y0 = var5.y;
            var3.z0 = var5.z;
            var3.timer = 0.0F;
            var3.projectilePathTimer = 0.0F;
            var3.currentProjectileAlpha = var3.projectileAlpha;
            this.m_effects.add(var3);
            return var3;
         }
      } else {
         return null;
      }
   }

   private void updateSettings(String var1, Effect var2) {
      IsoBulletTracerEffectsConfigOptions var3 = (IsoBulletTracerEffectsConfigOptions)this.isoBulletTracerEffectsConfigOptionsHashMap.get(var1);
      var2.projectileRed = (float)var3.projectileRed.getValue();
      var2.projectileGreen = (float)var3.projectileGreen.getValue();
      var2.projectileBlue = (float)var3.projectileBlue.getValue();
      var2.projectileAlpha = (float)var3.projectileAlpha.getValue();
      var2.projectileLength = (float)var3.projectileLength.getValue();
      var2.projectileStartThickness = (float)var3.projectileStartThickeness.getValue();
      var2.projectileEndThickness = (float)var3.projectileEndThickness.getValue();
      var2.projectileFadeRate = (float)var3.projectileFadeRate.getValue();
      var2.projectileTrailRed = (float)var3.projectileTrailRed.getValue();
      var2.projectileTrailGreen = (float)var3.projectileTrailGreen.getValue();
      var2.projectileTrailBlue = (float)var3.projectileTrailBlue.getValue();
      var2.projectileTrailAlpha = (float)var3.projectileTrailAlpha.getValue();
      var2.projectileTrailLength = (float)var3.projectileTrailLength.getValue();
      var2.projectileTrailStartThickness = (float)var3.projectileTrailStartThickness.getValue();
      var2.projectileTrailEndThickness = (float)var3.projectileTrailEndThickness.getValue();
      var2.projectilePathRed = (float)var3.projectilePathRed.getValue();
      var2.projectilePathGreen = (float)var3.projectilePathGreen.getValue();
      var2.projectilePathBlue = (float)var3.projectilePathBlue.getValue();
      var2.projectilePathAlpha = (float)var3.projectilePathAlpha.getValue();
      var2.projectilePathStartThickness = (float)var3.projectilePathStartThickness.getValue();
      var2.projectilePathEndThickness = (float)var3.projectilePathEndThickness.getValue();
      var2.projectilePathFadeRate = (float)var3.projectilePathFadeRate.getValue();
      var2.projectilePathTime = (float)var3.projectilePathTime.getValue();
      var2.projectileSpeed = (float)var3.projectileSpeed.getValue();
   }

   public void render() {
      float var1 = GameTime.getInstance().getMultiplier() / 1.6F;
      IndieGL.glEnable(2848);
      IndieGL.glBlendFunc(770, 1);
      if (PerformanceSettings.FBORenderChunk) {
         IndieGL.disableDepthTest();
         IndieGL.StartShader(0);
      }

      for(int var2 = 0; var2 < this.m_effects.size(); ++var2) {
         Effect var3 = (Effect)this.m_effects.get(var2);
         if (Core.bDebug) {
            this.updateSettings(var3.ammoType, var3);
         }

         var3.render();
         if (!GameTime.isGamePaused()) {
            var3.timer += var1;
            var3.projectilePathTimer += var1;
         }

         float var4 = var3.start.distance(var3.current);
         if (var4 >= var3.currentRange - var3.projectileLength) {
            this.m_effects.remove(var2--);
            this.m_effectPool.release((Object)var3);
         }
      }

   }

   public void save(String var1) {
      IsoBulletTracerEffectsConfigOptions var2 = this.getOrCreate(var1);
      String var10000 = ZomboidFileSystem.instance.getMediaRootPath();
      String var3 = var10000 + File.separator + "effects" + File.separator + "bulletTracerEffect-" + var1 + ".txt";
      ConfigFile var4 = new ConfigFile();
      var4.write(var3, 1, var2.options);
   }

   public void load(String var1) {
      if (!this.isoBulletTracerEffectsConfigOptionsHashMap.containsKey(var1)) {
         IsoBulletTracerEffectsConfigOptions var2 = this.getOrCreate(var1);
         String var10000 = ZomboidFileSystem.instance.getMediaRootPath();
         String var3 = var10000 + File.separator + "effects" + File.separator + "bulletTracerEffect-" + var1 + ".txt";
         ConfigFile var4 = new ConfigFile();
         if (var4.read(var3)) {
            for(int var5 = 0; var5 < var4.getOptions().size(); ++var5) {
               ConfigOption var6 = (ConfigOption)var4.getOptions().get(var5);
               ConfigOption var7 = var2.getOptionByName(var6.getName());
               if (var7 != null) {
                  var7.parse(var6.getValueAsString());
               }
            }
         }

      }
   }

   public void reset(String var1) {
      IsoBulletTracerEffectsConfigOptions var2 = (IsoBulletTracerEffectsConfigOptions)this.isoBulletTracerEffectsConfigOptionsHashMap.get(var1);
      int var3 = var2.getOptionCount();

      for(int var4 = 0; var4 < var3; ++var4) {
         IsoBulletTracerEffectsConfigOption var5 = (IsoBulletTracerEffectsConfigOption)var2.getOptionByIndex(var4);
         var5.setValue(var5.getDefaultValue());
      }

   }

   private IsoBulletTracerEffectsConfigOptions getOrCreate(String var1) {
      IsoBulletTracerEffectsConfigOptions var2 = (IsoBulletTracerEffectsConfigOptions)this.isoBulletTracerEffectsConfigOptionsHashMap.get(var1);
      if (var2 == null) {
         var2 = new IsoBulletTracerEffectsConfigOptions();
         this.isoBulletTracerEffectsConfigOptionsHashMap.put(var1, var2);
      }

      return var2;
   }

   public static final class Effect {
      private String ammoType;
      private float x0;
      private float y0;
      private float z0;
      private float projectileRed;
      private float projectileGreen;
      private float projectileBlue;
      private float projectileAlpha;
      private float projectileLength;
      private float projectileStartThickness;
      private float projectileEndThickness;
      private float projectileFadeRate;
      private float projectileTrailRed;
      private float projectileTrailGreen;
      private float projectileTrailBlue;
      private float projectileTrailAlpha;
      private float projectileTrailLength;
      private float projectileTrailStartThickness;
      private float projectileTrailEndThickness;
      private float projectilePathRed;
      private float projectilePathGreen;
      private float projectilePathBlue;
      private float projectilePathAlpha;
      private float projectilePathStartThickness;
      private float projectilePathEndThickness;
      private float projectilePathFadeRate = 0.0F;
      private float projectilePathTime = 0.0F;
      private float projectilePathTimer = 0.0F;
      private float projectileSpeed = 0.0F;
      private float range = 0.0F;
      private float currentRange = 0.0F;
      private float currentProjectileAlpha = 0.0F;
      private float timer = 0.0F;
      private IsoGridSquare isoGridSquare;
      private boolean playSound;
      private final Vector3f start = new Vector3f();
      private final Vector3f end = new Vector3f();
      private final Vector3f current = new Vector3f();
      private final Vector3f directionVector = new Vector3f();
      private static final Vector3f gcTempVector3f = new Vector3f();
      private static final Vector3f gcTempVector3f2 = new Vector3f();

      public Effect() {
      }

      public void setRange(float var1) {
         if (var1 < this.currentRange) {
            this.currentRange = var1;
         }

      }

      public void render() {
         float var1 = this.projectileSpeed / 200.0F;
         float var2 = this.timer * var1;
         this.current.set(this.x0 + this.directionVector.x * var2, this.y0 + this.directionVector.y * var2, this.z0 + this.directionVector.z * var2);
         float var3 = 1.0F;
         if (var2 < this.currentRange) {
            var3 = PZMath.clamp(var2 / this.currentRange, 0.0F, 1.0F);
         }

         float var4 = this.projectilePathAlpha * (1.0F - var3 * this.projectilePathFadeRate);
         float var5 = this.projectileTrailAlpha * (1.0F - var3);
         boolean var6 = this.start.distance(this.current) < this.start.distance(this.end);
         if (var6) {
            LineDrawer.DrawIsoLine(this.start.x, this.start.y, this.start.z, this.current.x, this.current.y, this.current.z, this.projectilePathRed, this.projectilePathGreen, this.projectilePathBlue, var4, this.projectilePathStartThickness, this.projectilePathEndThickness);
         } else {
            LineDrawer.DrawIsoLine(this.start.x, this.start.y, this.start.z, this.end.x, this.end.y, this.end.z, this.projectilePathRed, this.projectilePathGreen, this.projectilePathBlue, var4, this.projectilePathStartThickness, this.projectilePathEndThickness);
         }

         if (this.start.distance(this.current) > this.projectileTrailLength && var6) {
            gcTempVector3f.set(this.x0 + this.directionVector.x * (var2 - this.projectileTrailLength), this.y0 + this.directionVector.y * (var2 - this.projectileTrailLength), this.z0 + this.directionVector.z * (var2 - this.projectileTrailLength));
            gcTempVector3f2.set(gcTempVector3f.x + this.directionVector.x * this.projectileTrailLength, gcTempVector3f.y + this.directionVector.y * this.projectileTrailLength, gcTempVector3f.z + this.directionVector.z * this.projectileTrailLength);
            LineDrawer.DrawIsoLine(gcTempVector3f.x, gcTempVector3f.y, gcTempVector3f.z, gcTempVector3f2.x, gcTempVector3f2.y, gcTempVector3f2.z, this.projectileTrailRed, this.projectileTrailGreen, this.projectileTrailBlue, var5, this.projectileTrailStartThickness, this.projectileTrailEndThickness);
         }

         if (var6) {
            if (this.start.distance(this.current) > this.start.distance(this.end) - this.projectileLength * (1.0F / this.projectileFadeRate)) {
               this.currentProjectileAlpha -= this.projectileAlpha * this.projectileFadeRate;
               if (this.isoGridSquare != null && this.playSound) {
                  this.playSound = false;
                  if (!CombatManager.getInstance().hitIsoGridSquare(this.isoGridSquare, this.current)) {
                     SoundManager.instance.playImpactSound(this.isoGridSquare);
                  }
               }
            }

            gcTempVector3f.set(this.current.x + this.directionVector.x * this.projectileLength, this.current.y + this.directionVector.y * this.projectileLength, this.current.z + this.directionVector.z * this.projectileLength);
            LineDrawer.DrawIsoLine(this.current.x, this.current.y, this.current.z, gcTempVector3f.x, gcTempVector3f.y, gcTempVector3f.z, this.projectileRed, this.projectileGreen, this.projectileBlue, this.currentProjectileAlpha, this.projectileStartThickness, this.projectileEndThickness);
         }

      }
   }

   public class IsoBulletTracerEffectsConfigOptions {
      private static final int VERSION = 1;
      private final ArrayList<ConfigOption> options = new ArrayList();
      private final IsoBulletTracerEffectsConfigOption projectileRed;
      private final IsoBulletTracerEffectsConfigOption projectileGreen;
      private final IsoBulletTracerEffectsConfigOption projectileBlue;
      private final IsoBulletTracerEffectsConfigOption projectileAlpha;
      private final IsoBulletTracerEffectsConfigOption projectileLength;
      private final IsoBulletTracerEffectsConfigOption projectileStartThickeness;
      private final IsoBulletTracerEffectsConfigOption projectileEndThickness;
      private final IsoBulletTracerEffectsConfigOption projectileFadeRate;
      private final IsoBulletTracerEffectsConfigOption projectileTrailRed;
      private final IsoBulletTracerEffectsConfigOption projectileTrailGreen;
      private final IsoBulletTracerEffectsConfigOption projectileTrailBlue;
      private final IsoBulletTracerEffectsConfigOption projectileTrailAlpha;
      private final IsoBulletTracerEffectsConfigOption projectileTrailLength;
      private final IsoBulletTracerEffectsConfigOption projectileTrailStartThickness;
      private final IsoBulletTracerEffectsConfigOption projectileTrailEndThickness;
      private final IsoBulletTracerEffectsConfigOption projectilePathRed;
      private final IsoBulletTracerEffectsConfigOption projectilePathGreen;
      private final IsoBulletTracerEffectsConfigOption projectilePathBlue;
      private final IsoBulletTracerEffectsConfigOption projectilePathAlpha;
      private final IsoBulletTracerEffectsConfigOption projectilePathStartThickness;
      private final IsoBulletTracerEffectsConfigOption projectilePathEndThickness;
      private final IsoBulletTracerEffectsConfigOption projectilePathFadeRate;
      private final IsoBulletTracerEffectsConfigOption projectilePathTime;
      private final IsoBulletTracerEffectsConfigOption projectileSpeed;

      public IsoBulletTracerEffectsConfigOptions() {
         this.projectileRed = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileRed", 0.0, 1.0, 1.0, this.options);
         this.projectileGreen = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileGreen", 0.0, 1.0, 0.8399999737739563, this.options);
         this.projectileBlue = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileBlue", 0.0, 1.0, 0.5299999713897705, this.options);
         this.projectileAlpha = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileAlpha", 0.0, 1.0, 0.8100000023841858, this.options);
         this.projectileLength = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileLength", 0.009999999776482582, 1.0, 0.2475000023841858, this.options);
         this.projectileStartThickeness = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileStartThickeness", 0.009999999776482582, 5.0, 0.8981999754905701, this.options);
         this.projectileEndThickness = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileEndThickness", 0.009999999776482582, 5.0, 0.9480999708175659, this.options);
         this.projectileFadeRate = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileFadeRate", 0.0, 1.0, 0.05999999865889549, this.options);
         this.projectileTrailRed = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailRed", 0.0, 1.0, 1.0, this.options);
         this.projectileTrailGreen = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailGreen", 0.0, 1.0, 0.8399999737739563, this.options);
         this.projectileTrailBlue = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailBlue", 0.0, 1.0, 0.6100000143051147, this.options);
         this.projectileTrailAlpha = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailAlpha", 0.0, 1.0, 0.36000001430511475, this.options);
         this.projectileTrailLength = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailLength", 0.009999999776482582, 5.0, 0.8981999754905701, this.options);
         this.projectileTrailStartThickness = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailStartThickeness", 0.009999999776482582, 5.0, 0.04989999905228615, this.options);
         this.projectileTrailEndThickness = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileTrailEndThickness", 0.009999999776482582, 5.0, 1.1476999521255493, this.options);
         this.projectilePathRed = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathRed", 0.0, 1.0, 1.0, this.options);
         this.projectilePathGreen = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathGreen", 0.0, 1.0, 1.0, this.options);
         this.projectilePathBlue = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathBlue", 0.0, 1.0, 1.0, this.options);
         this.projectilePathAlpha = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathAlpha", 0.0, 1.0, 0.4399999976158142, this.options);
         this.projectilePathStartThickness = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathStartThickness", 0.009999999776482582, 5.0, 0.009999999776482582, this.options);
         this.projectilePathEndThickness = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathEndThickness", 0.009999999776482582, 5.0, 0.550000011920929, this.options);
         this.projectilePathFadeRate = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathFadeRate", 0.0, 2.0, 0.8600000143051147, this.options);
         this.projectilePathTime = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectilePathTime", 0.0, 10.0, 5.0, this.options);
         this.projectileSpeed = IsoBulletTracerEffects.this.new IsoBulletTracerEffectsConfigOption("ProjectileSpeed", 1.0, 4000.0, 1679.5799560546875, this.options);
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
   }

   public class IsoBulletTracerEffectsConfigOption extends DoubleConfigOption {
      public IsoBulletTracerEffectsConfigOption(String var2, double var3, double var5, double var7, ArrayList<ConfigOption> var9) {
         super(var2, var3, var5, var7);
         var9.add(this);
      }
   }
}
