package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.ui.UIFont;
import zombie.util.StringUtils;

public class AnimalInventoryItem extends InventoryItem {
   private IsoAnimal animal = null;
   private String animalName = null;

   public AnimalInventoryItem(String var1, String var2, String var3, String var4) {
      super(var1, var2, var3, var4);
      this.cat = ItemType.Animal;
   }

   public AnimalInventoryItem(String var1, String var2, String var3, Item var4) {
      super(var1, var2, var3, var4);
      this.cat = ItemType.Animal;
   }

   public void update() {
      if (this.animal != null) {
         this.animal.container = this.getContainer();
         this.animal.square = null;
         this.animal.setCurrent((IsoGridSquare)null);
         this.animal.update();
      }

   }

   public void DoTooltip(ObjectTooltip var1, ObjectTooltip.Layout var2) {
      var1.render();
      UIFont var3 = var1.getFont();
      int var4 = var1.getLineSpacing();
      byte var5 = 5;
      String var6;
      var1.DrawText(var3, var6 = this.animalName, 5.0, (double)var5, 1.0, 1.0, 1.0, 1.0);
      var1.adjustWidth(5, var6);
      ObjectTooltip.LayoutItem var7 = var2.addItem();
      var7.setLabel(Translator.getText("IGUI_AnimalType") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      var7.setValue(Translator.getText("IGUI_AnimalType_" + this.animal.getAnimalType()), 1.0F, 1.0F, 1.0F, 1.0F);
      var7 = var2.addItem();
      var7.setLabel(Translator.getText("UI_characreation_gender") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      String var8 = Translator.getText("IGUI_Animal_Female");
      if (!this.animal.isFemale()) {
         var8 = Translator.getText("IGUI_Animal_Male");
      }

      var7.setValue(var8, 1.0F, 1.0F, 1.0F, 1.0F);
      var7 = var2.addItem();
      var7.setLabel(Translator.getText("IGUI_char_Age") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      var7.setValue(this.animal.getAgeText(Core.getInstance().animalCheat, IsoPlayer.getInstance().getPerkLevel(PerkFactory.Perks.Husbandry)), 1.0F, 1.0F, 1.0F, 1.0F);
      var7 = var2.addItem();
      var7.setLabel(Translator.getText("IGUI_Animal_Appearance") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      var7.setValue(this.animal.getAppearanceText(Core.getInstance().animalCheat), 1.0F, 1.0F, 1.0F, 1.0F);
      var7 = var2.addItem();
      var7.setLabel(Translator.getText("IGUI_XP_Health") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
      var7.setValue(this.animal.getHealthText(Core.getInstance().animalCheat, IsoPlayer.getInstance().getPerkLevel(PerkFactory.Perks.Husbandry)), 1.0F, 1.0F, 1.0F, 1.0F);
      if (Core.getInstance().animalCheat) {
         var7 = var2.addItem();
         var7.setLabel("[DEBUG] Stress:", 1.0F, 1.0F, 0.8F, 1.0F);
         var7.setValue("" + Math.round(this.animal.getStress()), 1.0F, 1.0F, 1.0F, 1.0F);
         if (this.animal.heldBy != null) {
            var7 = var2.addItem();
            var7.setLabel("[DEBUG] Acceptance:", 1.0F, 1.0F, 0.8F, 1.0F);
            var7.setValue("" + Math.round(this.animal.getAcceptanceLevel(this.animal.heldBy)), 1.0F, 1.0F, 1.0F, 1.0F);
         }
      }

   }

   public boolean finishupdate() {
      return false;
   }

   public void initAnimalData() {
      if (!StringUtils.isNullOrEmpty(this.animal.getCustomName())) {
         this.animalName = this.animal.getCustomName();
      } else {
         String var10001 = Translator.getText("IGUI_Breed_" + this.animal.getBreed().getName());
         this.animalName = var10001 + " " + Translator.getText("IGUI_AnimalType_" + this.animal.getAnimalType());
      }

      this.setName(this.animalName);
      this.setWeight(this.animal.adef.baseEncumbrance * this.animal.getAnimalSize());
      this.setActualWeight(this.getWeight());
      String var1 = "";
      if (this.animal.isBaby()) {
         var1 = this.animal.getBreed().invIconBaby;
      } else if (this.animal.isFemale()) {
         var1 = this.animal.getBreed().invIconFemale;
      } else {
         var1 = this.animal.getBreed().invIconMale;
      }

      if (!StringUtils.isNullOrEmpty(var1)) {
         this.setIcon(Texture.getSharedTexture(var1));
      }

      if (this.animal.mother != null) {
         this.animal.attachBackToMother = this.animal.mother.animalID;
      }

   }

   public IsoAnimal getAnimal() {
      return this.animal;
   }

   public void setAnimal(IsoAnimal var1) {
      this.animal = var1;
      this.animal.setItemID(this.id);
      this.initAnimalData();
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      this.animal.save(var1, var2, false);
   }

   public void load(ByteBuffer var1, int var2) throws IOException {
      super.load(var1, var2);
      this.animal = new IsoAnimal(IsoWorld.instance.getCell());
      this.animal.load(var1, var2, false);
   }

   public String getCategory() {
      return "Animal";
   }

   public int getSaveType() {
      return Item.Type.Animal.ordinal();
   }

   public boolean shouldUpdateInWorld() {
      return true;
   }
}
