package zombie.characters;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import zombie.characterTextures.ItemSmartTexture;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.popman.ObjectPool;

public final class EquippedTextureCreator extends TextureDraw.GenericDrawer {
   private boolean bRendered;
   private ModelInstance modelInstance;
   private float bloodLevel;
   private ImmutableColor tint;
   private String tintMask;
   private final ArrayList<Texture> texturesNotReady;
   private static final ObjectPool<EquippedTextureCreator> pool = new ObjectPool(EquippedTextureCreator::new);

   public EquippedTextureCreator() {
      this.tint = ImmutableColor.white;
      this.texturesNotReady = new ArrayList();
   }

   public void init(ModelInstance var1, InventoryItem var2) {
      float var3 = 0.0F;
      if (var2 instanceof HandWeapon var4) {
         var3 = var4.getBloodLevel();
      }

      ImmutableColor var5 = ImmutableColor.white;
      if (var2.getColorRed() * var2.getColorGreen() * var2.getColorBlue() != 1.0F) {
         var5 = new ImmutableColor(var2.getColorRed(), var2.getColorGreen(), var2.getColorBlue());
      }

      this.init(var1, var3, var5);
   }

   public void init(ModelInstance var1, float var2, ImmutableColor var3) {
      this.bRendered = false;
      this.texturesNotReady.clear();
      this.modelInstance = var1;
      this.bloodLevel = var2;
      this.tint = var3;
      this.tintMask = null;
      if (this.modelInstance != null) {
         ++this.modelInstance.renderRefCount;
         Texture var4 = this.modelInstance.tex;
         if (var4 instanceof ItemSmartTexture) {
            ItemSmartTexture var5 = (ItemSmartTexture)var4;

            assert var5.getTexName() != null;

            var4 = this.getTextureWithFlags(var5.getTexName());
         }

         if (var4 != null && !var4.isReady()) {
            this.texturesNotReady.add(var4);
         }

         String var6 = var4 == null ? null : var4.getName();
         if (var6 != null) {
            this.tintMask = this.initTextureName(var6, "TINT");
            var4 = this.getTextureWithFlags(this.tintMask);
            if (var4 == null) {
               this.tintMask = null;
            } else if (!var4.isReady()) {
               this.texturesNotReady.add(var4);
            }
         }

         var4 = this.getTextureWithFlags("media/textures/BloodTextures/BloodOverlayWeapon.png");
         if (var4 != null && !var4.isReady()) {
            this.texturesNotReady.add(var4);
         }

         var4 = this.getTextureWithFlags("media/textures/BloodTextures/BloodOverlayWeaponMask.png");
         if (var4 != null && !var4.isReady()) {
            this.texturesNotReady.add(var4);
         }
      }

   }

   public void render() {
      for(int var1 = 0; var1 < this.texturesNotReady.size(); ++var1) {
         Texture var2 = (Texture)this.texturesNotReady.get(var1);
         if (!var2.isReady()) {
            return;
         }
      }

      GL11.glPushAttrib(2048);

      try {
         this.updateTexture(this.modelInstance, this.bloodLevel);
      } finally {
         GL11.glPopAttrib();
      }

      this.bRendered = true;
   }

   private Texture getTextureWithFlags(String var1) {
      return Texture.getSharedTexture(var1, ModelManager.instance.getTextureFlags());
   }

   private void updateTexture(ModelInstance var1, float var2) {
      if (var1 != null) {
         ItemSmartTexture var3 = null;
         ItemSmartTexture var4;
         Texture var5;
         if (this.tint.equals(ImmutableColor.white) && !(var2 > 0.0F)) {
            var5 = var1.tex;
            if (var5 instanceof ItemSmartTexture) {
               var4 = (ItemSmartTexture)var5;
               var3 = var4;
            }
         } else {
            var5 = var1.tex;
            if (var5 instanceof ItemSmartTexture) {
               var4 = (ItemSmartTexture)var5;
               var3 = var4;
            } else if (var1.tex != null) {
               var3 = new ItemSmartTexture(var1.tex.getName());
            }
         }

         if (var3 != null) {
            String var6 = var3.getTexName();

            assert var6 != null;

            var3.clear();
            var3.add(var6);
            if (!ImmutableColor.white.equals(this.tint)) {
               if (this.tintMask != null) {
                  var3.setTintMask(this.tintMask, "media/textures/FullAlpha.png", 300, this.tint.toMutableColor());
               } else {
                  var3.addTint(var6, 300, this.tint.getRedFloat(), this.tint.getGreenFloat(), this.tint.getBlueFloat());
               }
            }

            var3.setBlood("media/textures/BloodTextures/BloodOverlayWeapon.png", "media/textures/BloodTextures/BloodOverlayWeaponMask.png", var2, 301);
            var3.calculate();
            var1.tex = var3;
         }
      }
   }

   public void postRender() {
      ModelManager.instance.derefModelInstance(this.modelInstance);
      this.texturesNotReady.clear();
      if (!this.bRendered) {
      }

      this.modelInstance = null;
      pool.release((Object)this);
   }

   private String initTextureName(String var1, String var2) {
      if (var1.endsWith(".png")) {
         var1 = var1.substring(0, var1.length() - 4);
      }

      return var1.contains("media/") ? var1 + var2 + ".png" : "media/textures/" + var1 + var2 + ".png";
   }

   public boolean isRendered() {
      return this.bRendered;
   }

   public static EquippedTextureCreator alloc() {
      return (EquippedTextureCreator)pool.alloc();
   }
}
