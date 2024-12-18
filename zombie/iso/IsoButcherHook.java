package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.sprite.IsoSpriteManager;

public class IsoButcherHook extends IsoObject {
   private IsoAnimal animal;
   private boolean removingBlood = false;
   private float removingBloodProgress = 0.0F;
   private float removingBloodTick = 0.0F;
   private float bloodAtStart = 0.0F;
   private KahluaTableImpl luaHook;

   public IsoButcherHook(IsoGridSquare var1) {
      this.sprite = IsoSpriteManager.instance.getSprite("crafted_04_120");
      this.square = var1;
      var1.getCell().addToProcessIsoObject(this);
   }

   public IsoButcherHook(IsoCell var1) {
      super(var1);
      var1.addToProcessIsoObject(this);
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
   }

   public void stopRemovingBlood() {
      this.removingBlood = false;
      this.removingBloodTick = 0.0F;
      this.removingBloodProgress = 0.0F;
      Object var1 = LuaManager.getFunctionObject("ISButcherHookUI.onStopBleedingAnimal");
      if (var1 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var1, this.luaHook);
      }

   }

   public void startRemovingBlood(KahluaTableImpl var1) {
      this.removingBlood = true;
      this.bloodAtStart = ((KahluaTableImpl)this.getAnimal().getModData()).rawgetFloat("BloodQty");
      this.luaHook = var1;
   }

   public void update() {
      if (this.removingBlood) {
         this.updateRemovingBlood();
      }

   }

   private void updateRemovingBlood() {
      if (this.getAnimal() == null) {
         this.removingBlood = false;
         this.removingBloodTick = 0.0F;
         this.removingBloodProgress = 0.0F;
      } else {
         this.removingBloodTick += GameTime.getInstance().getMultiplier();
         float var1 = ((KahluaTableImpl)this.getAnimal().getModData()).rawgetFloat("BloodQty");
         float var2 = 1.0F - var1 / this.bloodAtStart;
         float var3 = 1.0F - (var1 - 0.5F) / this.bloodAtStart;
         float var4 = var3 - var2;
         float var5 = this.removingBloodTick / 30.0F;
         this.removingBloodProgress = 1.0F - var1 / this.bloodAtStart + var4 * var5;
         if (this.removingBloodTick >= 30.0F) {
            this.removingBloodTick = 0.0F;
            var1 -= 0.5F;
            if (var1 <= 0.0F) {
               var1 = 0.0F;
            }

            this.getAnimal().getModData().rawset("BloodQty", (double)var1);
            int var6 = Rand.Next(5, 10);

            for(int var7 = 0; var7 < var6; ++var7) {
               this.getSquare().getChunk().addBloodSplat(this.getAnimal().getX() + Rand.Next(-0.2F, 0.2F), this.getAnimal().getY() + Rand.Next(-0.2F, 0.2F), this.getZ(), Rand.Next(20));
            }

            if (var1 <= 0.0F) {
               this.stopRemovingBlood();
            }
         }

      }
   }

   public boolean isRemovingBlood() {
      return this.removingBlood;
   }

   public float getRemovingBloodProgress() {
      return this.removingBloodProgress;
   }

   public String getObjectName() {
      return "ButcherHook";
   }

   public void setAnimal(IsoAnimal var1) {
      this.animal = var1;
   }

   public IsoAnimal getAnimal() {
      return this.animal;
   }

   public void removeHook() {
      if (this.getAnimal() != null) {
         IsoDeadBody var1 = new IsoDeadBody(this.getAnimal(), false);
         var1.setZ(this.getZ());
         var1.setModData(this.getAnimal().getModData());
         var1.invalidateCorpse();
         this.getAnimal().remove();
      }

      this.removeFromWorld();
      this.getSquare().transmitRemoveItemFromSquare(this);
   }

   public void removeFromWorld() {
      if (this.getAnimal() != null) {
         this.getAnimal().attachBackToHookX = (int)this.getX();
         this.getAnimal().attachBackToHookY = (int)this.getY();
         this.getAnimal().attachBackToHookZ = (int)this.getZ();
      }

      super.removeFromWorld();
   }

   public void reattachAnimal(IsoAnimal var1) {
      this.setAnimal(var1);
      var1.setHook(this);
      var1.setOnHook(true);
      var1.setVariable("onhook", true);
      Object var2 = LuaManager.getFunctionObject("ISButcherHookUI.onReattachAnimal");
      if (var2 != null) {
         LuaManager.caller.protectedCallVoid(LuaManager.thread, var2, this, var1);
      }

   }
}
