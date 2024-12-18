package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.CharacterSmartTexture;
import zombie.characterTextures.ItemSmartTexture;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingDecal;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.popman.ObjectPool;
import zombie.util.Lambda;
import zombie.util.StringUtils;

public final class ModelInstanceTextureCreator extends TextureDraw.GenericDrawer {
   private boolean bZombie;
   public int renderRefCount;
   private final CharacterMask mask = new CharacterMask();
   private final boolean[] holeMask;
   private final ItemVisuals itemVisuals;
   private final CharacterData chrData;
   private final ArrayList<ItemData> itemData;
   private final CharacterSmartTexture characterSmartTexture;
   private final ItemSmartTexture itemSmartTexture;
   private final ArrayList<Texture> tempTextures;
   private boolean bRendered;
   private final ArrayList<Texture> texturesNotReady;
   public int testNotReady;
   private final ArrayList<ItemData> localItemData;
   private static final ObjectPool<ModelInstanceTextureCreator> pool = new ObjectPool(ModelInstanceTextureCreator::new);

   public ModelInstanceTextureCreator() {
      this.holeMask = new boolean[BloodBodyPartType.MAX.index()];
      this.itemVisuals = new ItemVisuals();
      this.chrData = new CharacterData();
      this.itemData = new ArrayList();
      this.characterSmartTexture = new CharacterSmartTexture();
      this.itemSmartTexture = new ItemSmartTexture((String)null);
      this.tempTextures = new ArrayList();
      this.bRendered = false;
      this.texturesNotReady = new ArrayList();
      this.testNotReady = -1;
      this.localItemData = new ArrayList();
   }

   public void init(IsoGameCharacter var1) {
      ModelManager.ModelSlot var2 = var1.legsSprite.modelSlot;
      if (var1 instanceof IsoAnimal) {
         this.init(((IsoAnimal)var1).getAnimalVisual(), var2.model);
      } else {
         HumanVisual var3 = ((IHumanVisual)var1).getHumanVisual();
         var1.getItemVisuals(this.itemVisuals);
         this.init(var3, this.itemVisuals, var2.model);
         this.itemVisuals.clear();
      }
   }

   public void init(BaseVisual var1, ItemVisuals var2, ModelInstance var3) {
      if (var1 instanceof AnimalVisual) {
         this.init((AnimalVisual)var1, var3);
      } else if (var1 instanceof HumanVisual) {
         this.init((HumanVisual)var1, var2, var3);
      } else {
         throw new IllegalArgumentException("unhandled BaseVisual " + var1);
      }
   }

   public void init(AnimalVisual var1, ModelInstance var2) {
      this.chrData.modelInstance = var2;
      this.bRendered = false;
      this.bZombie = false;
      Arrays.fill(this.holeMask, false);
      synchronized(this.itemData) {
         ModelInstanceTextureCreator.ItemData.pool.release((List)this.itemData);
         this.itemData.clear();
      }

      this.texturesNotReady.clear();
      this.chrData.mask.setAllVisible(true);
      this.chrData.maskFolder = "media/textures/Body/Masks";
      this.chrData.baseTexture = "media/textures/Body/" + var1.getSkinTexture() + ".png";
      Arrays.fill(this.chrData.blood, 0.0F);
      Arrays.fill(this.chrData.dirt, 0.0F);
      Texture var3 = Texture.getSharedTexture(this.chrData.baseTexture);
      if (var3 != null && !var3.isReady()) {
         this.texturesNotReady.add(var3);
      }

      if (!this.chrData.mask.isAllVisible() && !this.chrData.mask.isNothingVisible()) {
         String var4 = this.chrData.maskFolder;
         Consumer var5 = Lambda.consumer(var4, this.texturesNotReady, (var0, var1x, var2x) -> {
            Texture var3 = Texture.getSharedTexture(var1x + "/" + var0 + ".png");
            if (var3 != null && !var3.isReady()) {
               var2x.add(var3);
            }

         });
         this.chrData.mask.forEachVisible(var5);
      }

   }

   public void init(HumanVisual var1, ItemVisuals var2, ModelInstance var3) {
      boolean var4 = DebugLog.isEnabled(DebugType.Clothing);
      this.chrData.modelInstance = var3;
      this.bRendered = false;
      this.bZombie = var1.isZombie();
      CharacterMask var5 = this.mask;
      var5.setAllVisible(true);
      String var6 = "media/textures/Body/Masks";
      Arrays.fill(this.holeMask, false);
      synchronized(this.itemData) {
         ModelInstanceTextureCreator.ItemData.pool.release((List)this.itemData);
         this.itemData.clear();
      }

      this.texturesNotReady.clear();
      BodyLocationGroup var7 = BodyLocations.getGroup("Human");

      int var8;
      String var12;
      for(var8 = var2.size() - 1; var8 >= 0; --var8) {
         ItemVisual var9 = (ItemVisual)var2.get(var8);
         ClothingItem var10 = var9.getClothingItem();
         if (var10 == null) {
            if (var4) {
               DebugLog.Clothing.warn("ClothingItem not found for ItemVisual:" + var9);
            }
         } else if (!var10.isReady()) {
            if (var4) {
               DebugLog.Clothing.warn("ClothingItem not ready for ItemVisual:" + var9);
            }
         } else if (!PopTemplateManager.instance.isItemModelHidden(var7, var2, var9)) {
            ModelInstance var11 = this.findModelInstance(var3.sub, var9);
            if (var11 == null) {
               var12 = var10.getModel(var1.isFemale());
               if (!StringUtils.isNullOrWhitespace(var12)) {
                  if (var4) {
                     DebugLog.Clothing.warn("ModelInstance not found for ItemVisual:" + var9);
                  }
                  continue;
               }
            }

            this.addClothingItem(var11, var9, var10, var5, var6);

            int var28;
            for(var28 = 0; var28 < BloodBodyPartType.MAX.index(); ++var28) {
               BloodBodyPartType var13 = BloodBodyPartType.FromIndex(var28);
               if (var9.getHole(var13) > 0.0F && var5.isBloodBodyPartVisible(var13)) {
                  this.holeMask[var28] = true;
               }
            }

            for(var28 = 0; var28 < var10.m_Masks.size(); ++var28) {
               CharacterMask.Part var29 = CharacterMask.Part.fromInt((Integer)var10.m_Masks.get(var28));
               BloodBodyPartType[] var14 = var29.getBloodBodyPartTypes();
               int var15 = var14.length;

               for(int var16 = 0; var16 < var15; ++var16) {
                  BloodBodyPartType var17 = var14[var16];
                  if (var9.getHole(var17) <= 0.0F) {
                     this.holeMask[var17.index()] = false;
                  }
               }
            }

            var9.getClothingItemCombinedMask(var5);
            if (!StringUtils.equalsIgnoreCase(var10.m_UnderlayMasksFolder, "media/textures/Body/Masks")) {
               var6 = var10.m_UnderlayMasksFolder;
            }
         }
      }

      this.chrData.mask.copyFrom(var5);
      this.chrData.maskFolder = var6;
      this.chrData.baseTexture = "media/textures/Body/" + var1.getSkinTexture() + ".png";
      Arrays.fill(this.chrData.blood, 0.0F);

      for(var8 = 0; var8 < BloodBodyPartType.MAX.index(); ++var8) {
         BloodBodyPartType var20 = BloodBodyPartType.FromIndex(var8);
         this.chrData.blood[var8] = var1.getBlood(var20);
         this.chrData.dirt[var8] = var1.getDirt(var20);
      }

      Texture var19 = getTextureWithFlags(this.chrData.baseTexture);
      if (var19 != null && !var19.isReady()) {
         this.texturesNotReady.add(var19);
      }

      if (!this.chrData.mask.isAllVisible() && !this.chrData.mask.isNothingVisible()) {
         String var21 = this.chrData.maskFolder;
         Consumer var23 = Lambda.consumer(var21, this.texturesNotReady, (var0, var1x, var2x) -> {
            Texture var3 = getTextureWithFlags(var1x + "/" + var0 + ".png");
            if (var3 != null && !var3.isReady()) {
               var2x.add(var3);
            }

         });
         this.chrData.mask.forEachVisible(var23);
      }

      var19 = getTextureWithFlags("media/textures/BloodTextures/BloodOverlay.png");
      if (var19 != null && !var19.isReady()) {
         this.texturesNotReady.add(var19);
      }

      var19 = getTextureWithFlags("media/textures/BloodTextures/GrimeOverlay.png");
      if (var19 != null && !var19.isReady()) {
         this.texturesNotReady.add(var19);
      }

      var19 = getTextureWithFlags("media/textures/patches/patchesmask.png");
      if (var19 != null && !var19.isReady()) {
         this.texturesNotReady.add(var19);
      }

      int var22;
      String var30;
      for(var22 = 0; var22 < BloodBodyPartType.MAX.index(); ++var22) {
         BloodBodyPartType var24 = BloodBodyPartType.FromIndex(var22);
         String[] var10000 = CharacterSmartTexture.MaskFiles;
         String var26 = "media/textures/BloodTextures/" + var10000[var24.index()] + ".png";
         var19 = getTextureWithFlags(var26);
         if (var19 != null && !var19.isReady()) {
            this.texturesNotReady.add(var19);
         }

         var10000 = CharacterSmartTexture.MaskFiles;
         var12 = "media/textures/HoleTextures/" + var10000[var24.index()] + ".png";
         var19 = getTextureWithFlags(var12);
         if (var19 != null && !var19.isReady()) {
            this.texturesNotReady.add(var19);
         }

         var10000 = CharacterSmartTexture.BasicPatchesMaskFiles;
         var30 = "media/textures/patches/" + var10000[var24.index()] + ".png";
         var19 = getTextureWithFlags(var30);
         if (var19 != null && !var19.isReady()) {
            this.texturesNotReady.add(var19);
         }

         var10000 = CharacterSmartTexture.DenimPatchesMaskFiles;
         String var32 = "media/textures/patches/" + var10000[var24.index()] + ".png";
         var19 = getTextureWithFlags(var32);
         if (var19 != null && !var19.isReady()) {
            this.texturesNotReady.add(var19);
         }

         var10000 = CharacterSmartTexture.LeatherPatchesMaskFiles;
         String var33 = "media/textures/patches/" + var10000[var24.index()] + ".png";
         var19 = getTextureWithFlags(var33);
         if (var19 != null && !var19.isReady()) {
            this.texturesNotReady.add(var19);
         }
      }

      var5.setAllVisible(true);
      var6 = "media/textures/Body/Masks";

      for(var22 = var1.getBodyVisuals().size() - 1; var22 >= 0; --var22) {
         ItemVisual var25 = (ItemVisual)var1.getBodyVisuals().get(var22);
         ClothingItem var27 = var25.getClothingItem();
         if (var27 == null) {
            if (var4) {
               DebugLog.Clothing.warn("ClothingItem not found for ItemVisual:" + var25);
            }
         } else if (!var27.isReady()) {
            if (var4) {
               DebugLog.Clothing.warn("ClothingItem not ready for ItemVisual:" + var25);
            }
         } else {
            ModelInstance var31 = this.findModelInstance(var3.sub, var25);
            if (var31 == null) {
               var30 = var27.getModel(var1.isFemale());
               if (!StringUtils.isNullOrWhitespace(var30)) {
                  if (var4) {
                     DebugLog.Clothing.warn("ModelInstance not found for ItemVisual:" + var25);
                  }
                  continue;
               }
            }

            this.addClothingItem(var31, var25, var27, var5, var6);
         }
      }

   }

   private ModelInstance findModelInstance(ArrayList<ModelInstance> var1, ItemVisual var2) {
      for(int var3 = 0; var3 < var1.size(); ++var3) {
         ModelInstance var4 = (ModelInstance)var1.get(var3);
         ItemVisual var5 = var4.getItemVisual();
         if (var5 != null && var5.getClothingItem() == var2.getClothingItem()) {
            return var4;
         }
      }

      return null;
   }

   private void addClothingItem(ModelInstance var1, ItemVisual var2, ClothingItem var3, CharacterMask var4, String var5) {
      String var6 = var1 == null ? var2.getBaseTexture(var3) : var2.getTextureChoice(var3);
      ImmutableColor var7 = var2.getTint(var3);
      float var8 = var2.getHue(var3);
      ItemData var9 = (ItemData)ModelInstanceTextureCreator.ItemData.pool.alloc();
      var9.modelInstance = var1;
      var9.category = CharacterSmartTexture.ClothingItemCategory;
      var9.mask.copyFrom(var4);
      var9.maskFolder = var3.m_MasksFolder;
      if (StringUtils.equalsIgnoreCase(var9.maskFolder, "media/textures/Body/Masks")) {
         var9.maskFolder = var5;
      }

      if (StringUtils.equalsIgnoreCase(var9.maskFolder, "none")) {
         var9.mask.setAllVisible(true);
      }

      if (var9.maskFolder.contains("Clothes/Hat/Masks")) {
         var9.mask.setAllVisible(true);
      }

      var9.baseTexture = "media/textures/" + var6 + ".png";
      var9.tint = var7;
      var9.hue = var8;
      var9.decalTexture = null;
      Arrays.fill(var9.basicPatches, 0.0F);
      Arrays.fill(var9.denimPatches, 0.0F);
      Arrays.fill(var9.leatherPatches, 0.0F);
      Arrays.fill(var9.blood, 0.0F);
      Arrays.fill(var9.dirt, 0.0F);
      Arrays.fill(var9.hole, 0.0F);
      int var10 = ModelManager.instance.getTextureFlags();
      Texture var11 = Texture.getSharedTexture(var9.baseTexture, var10);
      if (var11 != null && !var11.isReady()) {
         this.texturesNotReady.add(var11);
      }

      String var12;
      if (!var9.mask.isAllVisible() && !var9.mask.isNothingVisible()) {
         var12 = var9.maskFolder;
         Consumer var13 = Lambda.consumer(var12, this.texturesNotReady, (var0, var1x, var2x) -> {
            Texture var3 = getTextureWithFlags(var1x + "/" + var0 + ".png");
            if (var3 != null && !var3.isReady()) {
               var2x.add(var3);
            }

         });
         var9.mask.forEachVisible(var13);
      }

      if (Core.getInstance().isOptionSimpleClothingTextures(this.bZombie)) {
         synchronized(this.itemData) {
            this.itemData.add(var9);
         }
      } else {
         var12 = var2.getDecal(var3);
         if (!StringUtils.isNullOrWhitespace(var12)) {
            ClothingDecal var19 = ClothingDecals.instance.getDecal(var12);
            if (var19 != null && var19.isValid()) {
               var9.decalTexture = var19.texture;
               var9.decalX = var19.x;
               var9.decalY = var19.y;
               var9.decalWidth = var19.width;
               var9.decalHeight = var19.height;
               var11 = getTextureWithFlags("media/textures/" + var9.decalTexture + ".png");
               if (var11 != null && !var11.isReady()) {
                  this.texturesNotReady.add(var11);
               }
            }
         }

         for(int var20 = 0; var20 < BloodBodyPartType.MAX.index(); ++var20) {
            BloodBodyPartType var14 = BloodBodyPartType.FromIndex(var20);
            var9.blood[var20] = var2.getBlood(var14);
            var9.dirt[var20] = var2.getDirt(var14);
            var9.basicPatches[var20] = var2.getBasicPatch(var14);
            var9.denimPatches[var20] = var2.getDenimPatch(var14);
            var9.leatherPatches[var20] = var2.getLeatherPatch(var14);
            var9.hole[var20] = var2.getHole(var14);
            if (var9.hole[var20] > 0.0F) {
               String[] var10000 = CharacterSmartTexture.MaskFiles;
               String var15 = "media/textures/HoleTextures/" + var10000[var14.index()] + ".png";
               var11 = getTextureWithFlags(var15);
               if (var11 != null && !var11.isReady()) {
                  this.texturesNotReady.add(var11);
               }
            }

            if (var9.hole[var20] == 0.0F && this.holeMask[var20]) {
               var9.hole[var20] = -1.0F;
               if (var9.mask.isBloodBodyPartVisible(var14)) {
               }
            }
         }

         synchronized(this.itemData) {
            this.itemData.add(var9);
         }
      }
   }

   public void render() {
      if (!this.bRendered) {
         if (this.chrData.modelInstance != null) {
            for(int var1 = 0; var1 < this.texturesNotReady.size(); ++var1) {
               Texture var2 = (Texture)this.texturesNotReady.get(var1);
               if (!var2.isReady()) {
                  return;
               }
            }

            GL11.glPushAttrib(2048);

            try {
               this.tempTextures.clear();
               CharacterSmartTexture var11 = this.createFullCharacterTexture();

               assert var11 == this.characterSmartTexture;

               if (!(this.chrData.modelInstance.tex instanceof CharacterSmartTexture)) {
                  this.chrData.modelInstance.tex = new CharacterSmartTexture();
               }

               ((CharacterSmartTexture)this.chrData.modelInstance.tex).clear();
               this.applyCharacterTexture(var11.result, (CharacterSmartTexture)this.chrData.modelInstance.tex);
               var11.clear();
               this.tempTextures.add(var11.result);
               var11.result = null;
               var11 = (CharacterSmartTexture)this.chrData.modelInstance.tex;
               this.localItemData.clear();
               synchronized(this.itemData) {
                  this.localItemData.addAll(this.itemData);
               }

               int var12 = this.localItemData.size() - 1;

               while(true) {
                  if (var12 < 0) {
                     var11.calculate();
                     var11.clear();
                     this.itemSmartTexture.clear();

                     for(var12 = 0; var12 < this.tempTextures.size(); ++var12) {
                        for(int var13 = 0; var13 < this.localItemData.size(); ++var13) {
                           ModelInstance var15 = ((ItemData)this.localItemData.get(var13)).modelInstance;

                           assert var15 == null || this.tempTextures.get(var12) != var15.tex;
                        }

                        TextureCombiner.instance.releaseTexture((Texture)this.tempTextures.get(var12));
                     }

                     this.tempTextures.clear();
                     break;
                  }

                  label208: {
                     ItemData var3 = (ItemData)this.localItemData.get(var12);
                     Texture var4;
                     if (this.isSimpleTexture(var3)) {
                        int var5 = ModelManager.instance.getTextureFlags();
                        var4 = Texture.getSharedTexture(var3.baseTexture, var5);
                        if (!this.isItemSmartTextureRequired(var3)) {
                           var3.modelInstance.tex = var4;
                           break label208;
                        }
                     } else {
                        ItemSmartTexture var14 = this.createFullItemTexture(var3);

                        assert var14 == this.itemSmartTexture;

                        var4 = var14.result;
                        this.tempTextures.add(var14.result);
                        var14.result = null;
                     }

                     if (var3.modelInstance == null) {
                        this.applyItemTexture(var3, var4, var11);
                     } else {
                        if (!(var3.modelInstance.tex instanceof ItemSmartTexture)) {
                           var3.modelInstance.tex = new ItemSmartTexture((String)null);
                        }

                        ((ItemSmartTexture)var3.modelInstance.tex).clear();
                        this.applyItemTexture(var3, var4, (ItemSmartTexture)var3.modelInstance.tex);
                        ((ItemSmartTexture)var3.modelInstance.tex).calculate();
                        ((ItemSmartTexture)var3.modelInstance.tex).clear();
                     }
                  }

                  --var12;
               }
            } finally {
               GL11.glPopAttrib();
            }

            this.bRendered = true;
         }
      }
   }

   private CharacterSmartTexture createFullCharacterTexture() {
      CharacterSmartTexture var1 = this.characterSmartTexture;
      var1.clear();
      var1.addTexture(this.chrData.baseTexture, CharacterSmartTexture.BodyCategory, ImmutableColor.white, 0.0F);

      for(int var2 = 0; var2 < BloodBodyPartType.MAX.index(); ++var2) {
         BloodBodyPartType var3 = BloodBodyPartType.FromIndex(var2);
         if (this.chrData.dirt[var2] > 0.0F) {
            var1.addDirt(var3, this.chrData.dirt[var2], (IsoGameCharacter)null);
         }

         if (this.chrData.blood[var2] > 0.0F) {
            var1.addBlood(var3, this.chrData.blood[var2], (IsoGameCharacter)null);
         }
      }

      var1.calculate();
      return var1;
   }

   private void applyCharacterTexture(Texture var1, CharacterSmartTexture var2) {
      var2.addMaskedTexture(this.chrData.mask, this.chrData.maskFolder, var1, CharacterSmartTexture.BodyCategory, ImmutableColor.white, 0.0F);

      for(int var3 = 0; var3 < BloodBodyPartType.MAX.index(); ++var3) {
         BloodBodyPartType var4 = BloodBodyPartType.FromIndex(var3);
         if (this.holeMask[var3]) {
            var2.removeHole(var1, var4);
         }
      }

   }

   private boolean isSimpleTexture(ItemData var1) {
      if (var1.hue != 0.0F) {
         return false;
      } else {
         ImmutableColor var2 = var1.tint;
         if (var1.modelInstance != null) {
            var2 = ImmutableColor.white;
         }

         if (!var2.equals(ImmutableColor.white)) {
            return false;
         } else if (var1.decalTexture != null) {
            return false;
         } else {
            for(int var3 = 0; var3 < BloodBodyPartType.MAX.index(); ++var3) {
               if (var1.blood[var3] > 0.0F) {
                  return false;
               }

               if (var1.dirt[var3] > 0.0F) {
                  return false;
               }

               if (var1.hole[var3] > 0.0F) {
                  return false;
               }

               if (var1.basicPatches[var3] > 0.0F) {
                  return false;
               }

               if (var1.denimPatches[var3] > 0.0F) {
                  return false;
               }

               if (var1.leatherPatches[var3] > 0.0F) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private ItemSmartTexture createFullItemTexture(ItemData var1) {
      ItemSmartTexture var2 = this.itemSmartTexture;
      var2.clear();
      ImmutableColor var3 = var1.tint;
      if (var1.modelInstance != null) {
         var1.modelInstance.tintR = var1.modelInstance.tintG = var1.modelInstance.tintB = 1.0F;
      }

      var2.addTexture(var1.baseTexture, var1.category, var3, var1.hue);
      if (var1.decalTexture != null) {
         var2.addRect("media/textures/" + var1.decalTexture + ".png", var1.decalX, var1.decalY, var1.decalWidth, var1.decalHeight);
      }

      int var4;
      BloodBodyPartType var5;
      for(var4 = 0; var4 < BloodBodyPartType.MAX.index(); ++var4) {
         if (var1.blood[var4] > 0.0F) {
            var5 = BloodBodyPartType.FromIndex(var4);
            var2.addBlood("media/textures/BloodTextures/BloodOverlay.png", var5, var1.blood[var4]);
         }

         if (var1.dirt[var4] > 0.0F) {
            var5 = BloodBodyPartType.FromIndex(var4);
            var2.addDirt("media/textures/BloodTextures/GrimeOverlay.png", var5, var1.dirt[var4]);
         }

         if (var1.basicPatches[var4] > 0.0F) {
            var5 = BloodBodyPartType.FromIndex(var4);
            var2.setBasicPatches(var5);
         }

         if (var1.denimPatches[var4] > 0.0F) {
            var5 = BloodBodyPartType.FromIndex(var4);
            var2.setDenimPatches(var5);
         }

         if (var1.leatherPatches[var4] > 0.0F) {
            var5 = BloodBodyPartType.FromIndex(var4);
            var2.setLeatherPatches(var5);
         }
      }

      for(var4 = 0; var4 < BloodBodyPartType.MAX.index(); ++var4) {
         if (var1.hole[var4] > 0.0F) {
            var5 = BloodBodyPartType.FromIndex(var4);
            Texture var6 = var2.addHole(var5);

            assert var6 != var2.result;

            this.tempTextures.add(var6);
         }
      }

      var2.calculate();
      return var2;
   }

   private boolean isItemSmartTextureRequired(ItemData var1) {
      if (var1.modelInstance == null) {
         return true;
      } else if (var1.modelInstance.tex instanceof ItemSmartTexture) {
         return true;
      } else {
         for(int var2 = 0; var2 < BloodBodyPartType.MAX.index(); ++var2) {
            if (var1.hole[var2] < 0.0F) {
               return true;
            }
         }

         return !var1.mask.isAllVisible();
      }
   }

   private void applyItemTexture(ItemData var1, Texture var2, SmartTexture var3) {
      var3.addMaskedTexture(var1.mask, var1.maskFolder, var2, var1.category, ImmutableColor.white, 0.0F);

      for(int var4 = 0; var4 < BloodBodyPartType.MAX.index(); ++var4) {
         if (var1.hole[var4] < 0.0F) {
            BloodBodyPartType var5 = BloodBodyPartType.FromIndex(var4);
            var3.removeHole(var2, var5);
         }
      }

   }

   public void postRender() {
      if (!this.bRendered) {
         boolean var1;
         if (this.chrData.modelInstance.character == null) {
            var1 = true;
         } else {
            var1 = true;
         }
      }

      synchronized(this.itemData) {
         int var2 = 0;

         while(true) {
            if (var2 >= this.itemData.size()) {
               this.chrData.modelInstance = null;
               this.texturesNotReady.clear();
               ModelInstanceTextureCreator.ItemData.pool.release((List)this.itemData);
               this.itemData.clear();
               break;
            }

            ((ItemData)this.itemData.get(var2)).modelInstance = null;
            ++var2;
         }
      }

      pool.release((Object)this);
   }

   public boolean isRendered() {
      return this.testNotReady > 0 ? false : this.bRendered;
   }

   private static Texture getTextureWithFlags(String var0) {
      return Texture.getSharedTexture(var0, ModelManager.instance.getTextureFlags());
   }

   public static ModelInstanceTextureCreator alloc() {
      return (ModelInstanceTextureCreator)pool.alloc();
   }

   private static final class CharacterData {
      ModelInstance modelInstance;
      final CharacterMask mask = new CharacterMask();
      String maskFolder;
      String baseTexture;
      final float[] blood;
      final float[] dirt;

      private CharacterData() {
         this.blood = new float[BloodBodyPartType.MAX.index()];
         this.dirt = new float[BloodBodyPartType.MAX.index()];
      }
   }

   private static final class ItemData {
      ModelInstance modelInstance;
      final CharacterMask mask = new CharacterMask();
      String maskFolder;
      String baseTexture;
      int category;
      ImmutableColor tint;
      float hue;
      String decalTexture;
      int decalX;
      int decalY;
      int decalWidth;
      int decalHeight;
      final float[] blood;
      final float[] dirt;
      final float[] basicPatches;
      final float[] denimPatches;
      final float[] leatherPatches;
      final float[] hole;
      static final ObjectPool<ItemData> pool = new ObjectPool(ItemData::new);

      private ItemData() {
         this.blood = new float[BloodBodyPartType.MAX.index()];
         this.dirt = new float[BloodBodyPartType.MAX.index()];
         this.basicPatches = new float[BloodBodyPartType.MAX.index()];
         this.denimPatches = new float[BloodBodyPartType.MAX.index()];
         this.leatherPatches = new float[BloodBodyPartType.MAX.index()];
         this.hole = new float[BloodBodyPartType.MAX.index()];
      }
   }
}
