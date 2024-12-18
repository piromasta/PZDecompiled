package zombie.iso.objects;

import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoPhysicsObject;

public class IsoFallingClothing extends IsoPhysicsObject {
   private InventoryItem clothing = null;
   private int dropTimer = 0;
   public boolean addWorldItem = true;
   public float targetX = 0.0F;
   public float targetY = 0.0F;
   public float targetZ = 0.0F;

   public String getObjectName() {
      return "FallingClothing";
   }

   public IsoFallingClothing(IsoCell var1) {
      super(var1);
   }

   public IsoFallingClothing(IsoCell var1, float var2, float var3, float var4, float var5, float var6, InventoryItem var7) {
      super(var1);
      this.clothing = var7;
      this.dropTimer = 60;
      this.velX = var5;
      this.velY = var6;
      float var8 = (float)Rand.Next(4000) / 10000.0F;
      float var9 = (float)Rand.Next(4000) / 10000.0F;
      var8 -= 0.2F;
      var9 -= 0.2F;
      this.velX += var8;
      this.velY += var9;
      this.setX(var2);
      this.setY(var3);
      this.setZ(var4);
      this.setNextX(var2);
      this.setNextY(var3);
      this.offsetX = 0.0F;
      this.offsetY = 0.0F;
      this.terminalVelocity = -0.02F;
      Texture var10 = this.sprite.LoadFrameExplicit(var7.getTex().getName());
      if (var10 != null) {
         this.sprite.Animate = false;
         int var11 = Core.TileScale;
         this.sprite.def.scaleAspect((float)var10.getWidthOrig(), (float)var10.getHeightOrig(), (float)(16 * var11), (float)(16 * var11));
      }

      this.speedMod = 4.5F;
   }

   public void collideGround() {
      this.drop();
   }

   public void collideWall() {
      this.drop();
   }

   public void update() {
      super.update();
      if (this.targetX != 0.0F) {
         float var1 = 1.0F - Math.min(1.0F, (float)this.dropTimer / 60.0F);
         this.setNextX(PZMath.lerp(this.getNextX(), this.targetX, var1));
         this.setNextY(PZMath.lerp(this.getNextY(), this.targetY, var1));
         this.setZ(PZMath.lerp(this.getZ(), this.targetZ, var1));
      }

      --this.dropTimer;
      if (this.dropTimer <= 0) {
         this.drop();
      }

   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      float var8 = (float)(60 - this.dropTimer) / 60.0F * 360.0F;
      ItemModelRenderer.RenderStatus var9 = WorldItemModelDrawer.renderMain(this.clothing, this.getCurrentSquare(), this.getRenderSquare(), this.getX(), this.getY(), this.getZ(), var8);
      if (var9 != ItemModelRenderer.RenderStatus.Loading && var9 != ItemModelRenderer.RenderStatus.Ready) {
         super.render(var1, var2, var3, var4, var5, var6, var7);
      }
   }

   void drop() {
      if (this.targetX != 0.0F) {
         this.setX(this.targetX);
         this.setY(this.targetY);
         this.setZ(this.targetZ);
      }

      DebugLogStream var10000 = DebugLog.General;
      float var10001 = this.getX();
      var10000.println("IsoFallingClothing added x=" + var10001 + " y=" + this.getY());
      IsoGridSquare var1 = this.getCurrentSquare();
      if (var1 != null && this.clothing != null) {
         if (this.addWorldItem) {
            float var2 = var1.getApparentZ(this.getX() % 1.0F, this.getY() % 1.0F);
            var1.AddWorldInventoryItem(this.clothing, this.getX() % 1.0F, this.getY() % 1.0F, var2 - (float)var1.getZ(), false);
         }

         this.clothing = null;
         this.setDestroyed(true);
         var1.getMovingObjects().remove(this);
         this.getCell().Remove(this);
         LuaEventManager.triggerEvent("OnContainerUpdate", var1);
      }

   }
}
