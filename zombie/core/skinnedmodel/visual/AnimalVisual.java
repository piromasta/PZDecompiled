package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameWindow;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.Model;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;
import zombie.util.Type;

public class AnimalVisual extends BaseVisual {
   private final IAnimalVisual owner;
   private String skinTextureName = null;

   public AnimalVisual(IAnimalVisual var1) {
      this.owner = var1;
   }

   public void save(ByteBuffer var1) throws IOException {
      GameWindow.WriteStringUTF(var1, this.skinTextureName);
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      this.skinTextureName = GameWindow.ReadStringUTF(var1);
   }

   public Model getModel() {
      IsoAnimal var1 = this.getIsoAnimal();
      if (var1 != null) {
         return this.getModelTest(var1);
      } else {
         if (this.isSkeleton()) {
            AnimalDefinitions var2 = AnimalDefinitions.getDef(this.owner.getAnimalType());
            if (var2.bodyModelSkel != null) {
               this.skinTextureName = var2.textureSkeleton;
               return var2.bodyModelSkel;
            }
         }

         IAnimalVisual var3 = this.owner;
         if (var3 instanceof IsoDeadBody) {
            IsoDeadBody var4 = (IsoDeadBody)var3;
            AnimalDefinitions var5 = AnimalDefinitions.getDef(this.owner.getAnimalType());
            if (var5.bodyModelFleece != null && ((KahluaTableImpl)var4.getModData()).rawgetBool("shouldBeBodyFleece")) {
               return var5.bodyModelFleece;
            }

            if (!StringUtils.isNullOrEmpty(var5.textureSkinned) && ((KahluaTableImpl)var4.getModData()).rawgetBool("skinned")) {
               this.skinTextureName = var5.textureSkinned;
            }

            if (var5.bodyModelHeadless != null && var4.getModData() != null && ((KahluaTableImpl)var4.getModData()).rawgetBool("headless")) {
               return var5.bodyModelHeadless;
            }
         }

         return AnimalDefinitions.getDef(this.owner.getAnimalType()).bodyModel;
      }
   }

   public Model getModelTest(IsoAnimal var1) {
      if (var1.shouldBeSkeleton()) {
         this.skinTextureName = AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkeleton;
         return AnimalDefinitions.getDef(this.owner.getAnimalType()).bodyModelSkel;
      } else {
         if (!StringUtils.isNullOrEmpty(AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkinned) && ((KahluaTableImpl)var1.getModData()).rawgetBool("skinned")) {
            this.skinTextureName = AnimalDefinitions.getDef(this.owner.getAnimalType()).textureSkinned;
         }

         if (var1.adef.bodyModelHeadless != null && var1.getModData() != null && ((KahluaTableImpl)var1.getModData()).rawgetBool("headless")) {
            return var1.adef.bodyModelHeadless;
         } else if (StringUtils.isNullOrEmpty(var1.getBreed().woolType)) {
            return var1.adef.bodyModel;
         } else if (var1.getData().getWoolQuantity() >= var1.getData().getMaxWool() / 2.0F && var1.adef.bodyModelFleece != null) {
            return var1.adef.bodyModelFleece;
         } else {
            return ((KahluaTableImpl)var1.getModData()).rawgetBool("shouldBeBodyFleece") && var1.adef.bodyModelFleece != null ? var1.adef.bodyModelFleece : var1.adef.bodyModel;
         }
      }
   }

   public ModelScript getModelScript() {
      IsoAnimal var1 = this.getIsoAnimal();
      if (var1 == null) {
         AnimalDefinitions var2 = AnimalDefinitions.getDef(this.owner.getAnimalType());
         if (this.isSkeleton() && var2.bodyModelSkel != null) {
            this.skinTextureName = var2.textureSkeleton;
            return ScriptManager.instance.getModelScript(var2.bodyModelSkelStr);
         } else {
            IAnimalVisual var4 = this.owner;
            if (var4 instanceof IsoDeadBody) {
               IsoDeadBody var3 = (IsoDeadBody)var4;
               if (!StringUtils.isNullOrEmpty(var2.textureSkinned) && ((KahluaTableImpl)var3.getModData()).rawgetBool("skinned")) {
                  this.skinTextureName = var2.textureSkinned;
               }

               if (!StringUtils.isNullOrEmpty(var2.bodyModelHeadlessStr) && ((KahluaTableImpl)var3.getModData()).rawgetBool("headless")) {
                  return ScriptManager.instance.getModelScript(var2.bodyModelHeadlessStr);
               }

               if (!StringUtils.isNullOrEmpty(var2.bodyModelFleeceStr) && ((KahluaTableImpl)var3.getModData()).rawgetBool("shouldBeBodyFleece")) {
                  return ScriptManager.instance.getModelScript(var2.bodyModelFleeceStr);
               }
            }

            return ScriptManager.instance.getModelScript(var2.bodyModelStr);
         }
      } else {
         if (!StringUtils.isNullOrEmpty(var1.adef.textureSkinned) && ((KahluaTableImpl)var1.getModData()).rawgetBool("skinned")) {
            this.skinTextureName = var1.adef.textureSkinned;
         }

         if (!StringUtils.isNullOrEmpty(var1.adef.bodyModelHeadlessStr) && var1.getModData() != null && ((KahluaTableImpl)var1.getModData()).rawgetBool("headless")) {
            return ScriptManager.instance.getModelScript(var1.adef.bodyModelHeadlessStr);
         } else if (StringUtils.isNullOrEmpty(var1.getBreed().woolType)) {
            return ScriptManager.instance.getModelScript(var1.adef.bodyModelStr);
         } else {
            return var1.getData().getWoolQuantity() >= var1.getData().getMaxWool() / 2.0F && var1.adef.bodyModelFleeceStr != null ? ScriptManager.instance.getModelScript(var1.adef.bodyModelFleeceStr) : ScriptManager.instance.getModelScript(var1.adef.bodyModelStr);
         }
      }
   }

   public void dressInNamedOutfit(String var1, ItemVisuals var2) {
      var2.clear();
   }

   public String getAnimalType() {
      return this.owner.getAnimalType();
   }

   public float getAnimalSize() {
      return this.owner.getAnimalSize();
   }

   public IsoAnimal getIsoAnimal() {
      IsoAnimal var1 = (IsoAnimal)Type.tryCastTo(this.owner, IsoAnimal.class);
      if (var1 != null) {
         return var1;
      } else {
         AnimatedModel var2 = (AnimatedModel)Type.tryCastTo(this.owner, AnimatedModel.class);
         return var2 != null && var2.getCharacter() instanceof IsoAnimal ? (IsoAnimal)var2.getCharacter() : null;
      }
   }

   public String getSkinTexture() {
      return this.skinTextureName;
   }

   public void setSkinTextureName(String var1) {
      this.skinTextureName = var1;
   }

   public boolean isSkeleton() {
      return this.owner != null && this.owner.isSkeleton();
   }

   public void clear() {
      this.skinTextureName = null;
   }

   public void copyFrom(BaseVisual var1) {
      if (var1 == null) {
         this.clear();
      } else {
         AnimalVisual var2 = (AnimalVisual)Type.tryCastTo(var1, AnimalVisual.class);
         if (var2 == null) {
            throw new IllegalArgumentException("expected AnimalVisual, got " + var1);
         } else {
            this.skinTextureName = var2.skinTextureName;
         }
      }
   }
}
