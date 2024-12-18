package zombie;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.network.GameServer;

public final class AmbientSoundManager extends BaseAmbientStreamManager {
   public final ArrayList<Ambient> ambient = new ArrayList();
   private final Vector2 tempo = new Vector2();
   private int electricityShutOffState = -1;
   private long electricityShutOffTime = 0L;
   public boolean initialized = false;

   public AmbientSoundManager() {
   }

   public void update() {
      if (this.initialized) {
         this.updatePowerSupply();
         this.doOneShotAmbients();
      }
   }

   public void addAmbient(String var1, int var2, int var3, int var4, float var5) {
   }

   public void addAmbientEmitter(float var1, float var2, int var3, String var4) {
   }

   public void addDaytimeAmbientEmitter(float var1, float var2, int var3, String var4) {
   }

   public void doOneShotAmbients() {
      for(int var1 = 0; var1 < this.ambient.size(); ++var1) {
         Ambient var2 = (Ambient)this.ambient.get(var1);
         if (var2.finished()) {
            DebugLog.log(DebugType.Sound, "ambient: removing ambient sound " + var2.name);
            this.ambient.remove(var1--);
         } else {
            var2.update();
         }
      }

   }

   public void init() {
      if (!this.initialized) {
         this.initialized = true;
      }
   }

   public void addBlend(String var1, float var2, boolean var3, boolean var4, boolean var5, boolean var6) {
   }

   protected void addRandomAmbient() {
      // $FF: Couldn't be decompiled
   }

   public void doGunEvent() {
      // $FF: Couldn't be decompiled
   }

   public void doAlarm(RoomDef var1) {
      if (var1 != null && var1.building != null) {
         if ((float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) <= (float)(SandboxOptions.getInstance().getElecShutModifier() + var1.building.bAlarmDecay)) {
            float var2 = 1.0F;
            Ambient var3 = new Ambient("burglar2", (float)(var1.x + var1.getW() / 2), (float)(var1.y + var1.getH() / 2), 700.0F, var2);
            var3.duration = 49;
            var3.worldSoundDelay = 3;
            this.ambient.add(var3);
            GameServer.sendAlarm(var1.x + var1.getW() / 2, var1.y + var1.getH() / 2);
         }

         var1.building.bAlarmed = false;
         var1.building.setAllExplored(true);
      }

   }

   private int GetDistance(int var1, int var2, int var3, int var4) {
      return (int)Math.sqrt(Math.pow((double)(var1 - var3), 2.0) + Math.pow((double)(var2 - var4), 2.0));
   }

   public void handleThunderEvent(int var1, int var2) {
      int var3 = 9999999;
      ArrayList var4 = GameServer.getPlayers();
      if (!var4.isEmpty()) {
         for(int var5 = 0; var5 < var4.size(); ++var5) {
            IsoPlayer var6 = (IsoPlayer)var4.get(var5);
            if (var6 != null && var6.isAlive()) {
               int var7 = this.GetDistance((int)var6.getX(), (int)var6.getY(), var1, var2);
               if (var7 < var3) {
                  var3 = var7;
               }
            }
         }

         if (var3 <= 5000) {
            WorldSoundManager.instance.addSound((Object)null, PZMath.fastfloor((float)var1), PZMath.fastfloor((float)var2), 0, 5000, 5000);
            Ambient var8 = new Ambient("", (float)var1, (float)var2, 5100.0F, 1.0F);
            this.ambient.add(var8);
            GameServer.sendAmbient("", PZMath.fastfloor((float)var1), PZMath.fastfloor((float)var2), (int)Math.ceil((double)var8.radius), var8.volume);
         }

      }
   }

   public void stop() {
      this.ambient.clear();
      this.initialized = false;
   }

   public void save(ByteBuffer var1) {
   }

   public void load(ByteBuffer var1, int var2) {
   }

   private void updatePowerSupply() {
      boolean var1 = (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) < (float)SandboxOptions.getInstance().getElecShutModifier();
      if (this.electricityShutOffState == -1) {
         IsoWorld.instance.setHydroPowerOn(var1);
      }

      if (this.electricityShutOffState == 0) {
         if (var1) {
            IsoWorld.instance.setHydroPowerOn(true);
            this.checkHaveElectricity();
            this.electricityShutOffTime = 0L;
         } else if (this.electricityShutOffTime != 0L && System.currentTimeMillis() >= this.electricityShutOffTime) {
            this.electricityShutOffTime = 0L;
            IsoWorld.instance.setHydroPowerOn(false);
            this.checkHaveElectricity();
         }
      }

      if (this.electricityShutOffState == 1 && !var1) {
         this.electricityShutOffTime = System.currentTimeMillis() + 2650L;
      }

      this.electricityShutOffState = var1 ? 1 : 0;
   }

   public void checkHaveElectricity() {
   }

   public boolean isParameterInsideTrue() {
      return false;
   }

   public class Ambient {
      public float x;
      public float y;
      public String name;
      public float radius;
      public float volume;
      long startTime;
      public int duration;
      public int worldSoundDelay = 0;

      public Ambient(String var2, float var3, float var4, float var5, float var6) {
         this.name = var2;
         this.x = var3;
         this.y = var4;
         this.radius = var5;
         this.volume = var6;
         this.startTime = System.currentTimeMillis() / 1000L;
         this.duration = 2;
         this.update();
         LuaEventManager.triggerEvent("OnAmbientSound", var2, var3, var4);
      }

      public boolean finished() {
         long var1 = System.currentTimeMillis() / 1000L;
         return var1 - this.startTime >= (long)this.duration;
      }

      public void update() {
         long var1 = System.currentTimeMillis() / 1000L;
         if (var1 - this.startTime >= (long)this.worldSoundDelay) {
            WorldSoundManager.instance.addSound((Object)null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, 600, 600);
         }

      }
   }
}
