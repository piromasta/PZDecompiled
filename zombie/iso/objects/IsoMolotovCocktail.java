package zombie.iso.objects;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoPhysicsObject;
import zombie.network.GameClient;

public class IsoMolotovCocktail extends IsoPhysicsObject {
   private HandWeapon weapon = null;
   private IsoGameCharacter character = null;
   private int timer = 0;
   private int explodeTimer = 0;

   public String getObjectName() {
      return "MolotovCocktail";
   }

   public IsoMolotovCocktail(IsoCell var1) {
      super(var1);
   }

   public IsoMolotovCocktail(IsoCell var1, float var2, float var3, float var4, float var5, float var6, HandWeapon var7, IsoGameCharacter var8) {
      super(var1);
      this.weapon = var7;
      this.character = var8;
      this.explodeTimer = var7.getTriggerExplosionTimer();
      this.velX = var5;
      this.velY = var6;
      float var9 = (float)Rand.Next(4000) / 10000.0F;
      float var10 = (float)Rand.Next(4000) / 10000.0F;
      var9 -= 0.2F;
      var10 -= 0.2F;
      this.velX += var9;
      this.velY += var10;
      this.setX(var2);
      this.setY(var3);
      this.setZ(var4);
      this.setNextX(var2);
      this.setNextY(var3);
      this.offsetX = 0.0F;
      this.offsetY = 0.0F;
      this.terminalVelocity = -0.02F;
      Texture var11 = this.sprite.LoadSingleTexture(var7.getTex().getName());
      if (var11 != null) {
         this.sprite.Animate = false;
         int var12 = Core.TileScale;
         this.sprite.def.scaleAspect((float)var11.getWidthOrig(), (float)var11.getHeightOrig(), (float)(16 * var12), (float)(16 * var12));
      }

      this.speedMod = 0.6F;
   }

   public void collideCharacter() {
      if (this.explodeTimer == 0) {
         this.Explode();
      }

   }

   public void collideGround() {
      if (this.explodeTimer == 0) {
         this.Explode();
      }

   }

   public void collideWall() {
      if (this.explodeTimer == 0) {
         this.Explode();
      }

   }

   public void update() {
      super.update();
      if (!this.isDestroyed()) {
         if (this.isCollidedThisFrame() && this.explodeTimer == 0) {
            this.Explode();
         }

         if (this.explodeTimer > 0) {
            ++this.timer;
            if (this.timer >= this.explodeTimer) {
               this.Explode();
            }
         }

      }
   }

   public void render(float var1, float var2, float var3, ColorInfo var4, boolean var5, boolean var6, Shader var7) {
      if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(this.weapon)) {
         ItemModelRenderer.RenderStatus var8 = WorldItemModelDrawer.renderMain(this.weapon, this.getSquare(), this.getRenderSquare(), var1, var2, var3, 0.0F);
         if (var8 == ItemModelRenderer.RenderStatus.Loading || var8 == ItemModelRenderer.RenderStatus.Ready) {
            return;
         }
      }

      super.render(var1, var2, var3, var4, var5, var6, var7);
      if (Core.bDebug) {
      }

   }

   void Explode() {
      this.setDestroyed(true);
      this.getCurrentSquare().getMovingObjects().remove(this);
      this.getCell().Remove(this);
      if (GameClient.bClient) {
         if (!(this.character instanceof IsoPlayer) || !((IsoPlayer)this.character).isLocalPlayer()) {
            return;
         }

         this.square.syncIsoTrap(this.weapon);
      }

      IsoTrap var1 = new IsoTrap(this.weapon, this.getCurrentSquare().getCell(), this.getCurrentSquare());
      if (this.weapon.isInstantExplosion()) {
         var1.triggerExplosion(false);
      } else {
         var1.getSquare().AddTileObject(var1);
      }

   }
}
