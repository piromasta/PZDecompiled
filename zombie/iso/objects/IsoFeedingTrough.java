package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characters.animals.IsoAnimal;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class IsoFeedingTrough extends IsoObject {
   public HashMap<String, Float> feedingTypes = new HashMap();
   private int linkedX = 0;
   private int linkedY = 0;
   public ArrayList<IsoAnimal> linkedAnimals = new ArrayList();
   private int maxFeed = 0;
   private float water = 0.0F;
   private float maxWater = 0.0F;
   private KahluaTableImpl def = null;
   public boolean north = false;

   public IsoFeedingTrough(IsoCell var1) {
      super(var1);
      this.setSpecialTooltip(true);
   }

   public IsoFeedingTrough(IsoGridSquare var1, String var2, IsoGridSquare var3) {
      super((IsoGridSquare)var1, (String)var2, (String)null);
      if (this.sprite.getProperties().Is("ContainerCapacity")) {
         this.container = new ItemContainer(this.sprite.getProperties().Val("container"), var1, this);
         this.container.Capacity = Integer.parseInt(this.sprite.getProperties().Val("ContainerCapacity"));
         this.container.setExplored(true);
      }

      String var4 = this.sprite.getProperties().Val("Facing");
      this.setNorth("N".equals(var4) || "S".equals(var4));
      if (var3 == null && this.sprite.getSpriteGrid() != null) {
         IsoSpriteGrid var5 = this.sprite.getSpriteGrid();
         int var6 = var5.getWidth() * var5.getHeight() - 1;
         int var7 = var5.getSpriteIndex(this.sprite);
         if (var7 == var6) {
            this.checkZone();
         } else {
            int var8 = var5.getSpriteGridPosX(this.sprite);
            int var9 = var5.getSpriteGridPosY(this.sprite);
            this.setLinkedX(var1.getX() + var5.getWidth() - var8 - 1);
            this.setLinkedY(var1.getY() + var5.getHeight() - var9 - 1);
         }
      } else if (var3 != null) {
         this.setLinkedX(var3.getX());
         this.setLinkedY(var3.getY());
      } else {
         this.checkZone();
      }

      this.initWithDef();
      this.setSpecialTooltip(true);
   }

   public boolean isItemAllowedInContainer(ItemContainer var1, InventoryItem var2) {
      if (var2 instanceof Food) {
         return true;
      } else {
         return !StringUtils.isNullOrEmpty(var2.getAnimalFeedType()) && var2 instanceof DrainableComboItem;
      }
   }

   public void checkZone() {
      DesignationZoneAnimal var1 = DesignationZoneAnimal.getZone((int)this.getX(), (int)this.getY(), (int)this.getZ());
      if (var1 != null && !var1.troughs.contains(this)) {
         var1.troughs.add(this);
      }

   }

   public void removeFromWorld() {
      DesignationZoneAnimal var1 = DesignationZoneAnimal.getZone((int)this.getX(), (int)this.getY(), (int)this.getZ());
      if (var1 != null) {
         var1.troughs.remove(this);
      }

      super.removeFromWorld();
   }

   public void checkIsoRegion() {
      if (this.getLinkedX() <= 0 || this.getLinkedY() <= 0) {
         ;
      }
   }

   public void addToWorld() {
      super.addToWorld();
      this.getCell().addToProcessIsoObject(this);
   }

   public void update() {
      if (!this.isSlave() && RainManager.isRaining() && this.getSquare() != null && !this.getSquare().haveRoofFull() && (this.getContainer() == null || this.getContainer().isEmpty())) {
         this.setWater(this.getWater() + RainManager.getRainIntensity() / 10.0F);
      }

      this.checkIsoRegion();
   }

   public String getObjectName() {
      return "FeedingTrough";
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      int var4 = var1.getInt();

      for(int var5 = 0; var5 < var4; ++var5) {
         this.feedingTypes.put(GameWindow.ReadString(var1), var1.getFloat());
      }

      this.water = var1.getFloat();
      if (var1.get() == 1) {
         this.linkedX = var1.getInt();
         this.linkedY = var1.getInt();
      }

      this.north = var1.get() == 1;
      this.initWithDef();
      if (this.getSquare() != null) {
         this.checkOverlayAfterAnimalEat();
      }

   }

   public void setContainer(ItemContainer var1) {
      if (!this.isSlave()) {
         var1.parent = this;
         this.container = var1;
      }
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.putInt(this.feedingTypes.size());
      Iterator var3 = this.feedingTypes.keySet().iterator();

      while(var3.hasNext()) {
         String var4 = (String)var3.next();
         GameWindow.WriteString(var1, var4);
         var1.putFloat((Float)this.feedingTypes.get(var4));
      }

      var1.putFloat(this.water);
      if (this.isSlave()) {
         var1.put((byte)1);
         var1.putInt(this.linkedX);
         var1.putInt(this.linkedY);
      } else {
         var1.put((byte)0);
      }

      var1.put((byte)(this.north ? 1 : 0));
   }

   public void initWithDef() {
      if (!this.isSlave()) {
         KahluaTableImpl var1 = (KahluaTableImpl)LuaManager.env.rawget("FeedingTroughDef");
         if (var1 != null) {
            KahluaTableIterator var2 = var1.iterator();

            label43:
            while(var2.advance()) {
               KahluaTableImpl var3 = (KahluaTableImpl)var2.getValue();
               KahluaTableIterator var4 = var3.iterator();

               String var5;
               Object var6;
               do {
                  do {
                     if (!var4.advance()) {
                        continue label43;
                     }

                     var5 = var4.getKey().toString();
                     var6 = var4.getValue();
                  } while(!"sprite1".equals(var5) && !"sprite2".equals(var5) && !"spriteNorth1".equals(var5) && !"spriteNorth2".equals(var5));
               } while(!var6.equals(this.getSprite().getName()));

               this.doDef(var3);
               return;
            }

         }
      }
   }

   public void doDef(KahluaTableImpl var1) {
      this.maxWater = var1.rawgetFloat("maxWater");
      this.maxFeed = var1.rawgetInt("maxFeed");
      this.def = var1;
   }

   public void checkOverlayFull() {
      this.checkOverlay(this.def, this.getCurrentFeedAmount(), (float)this.getMaxFeed(), false, true, false);
      this.checkOverlay(this.def, this.water, this.getMaxWater(), true, true, false);
   }

   public void checkOverlayAfterAnimalEat() {
      if (this.getContainer() != null) {
         this.checkOverlay(this.def, this.getCurrentFeedAmount(), (float)this.getMaxFeed(), false, true, true);
      } else {
         this.checkOverlay(this.def, this.water, this.getMaxWater(), true, true, true);
      }

   }

   public void onFoodAdded() {
      this.checkOverlay(this.def, this.getCurrentFeedAmount(), (float)this.getMaxFeed(), false, true, true);
      ArrayList var1 = DesignationZoneAnimal.getAllDZones((ArrayList)null, DesignationZoneAnimal.getZone(this.getSquare().x, this.getSquare().y, this.getSquare().z), (DesignationZoneAnimal)null);

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         DesignationZoneAnimal var3 = (DesignationZoneAnimal)var1.get(var2);

         for(int var4 = 0; var4 < var3.getAnimalsConnected().size(); ++var4) {
            ((IsoAnimal)var3.getAnimalsConnected().get(var4)).getData().callToTrough(this);
         }
      }

   }

   public void onRemoveFood() {
      this.checkOverlay(this.def, this.getCurrentFeedAmount(), (float)this.getMaxFeed(), false, true, true);
   }

   private void checkOverlay(KahluaTableImpl var1, float var2, float var3, boolean var4, boolean var5, boolean var6) {
      if (var1 != null) {
         if (var6) {
            this.setOverlaySprite((String)null, false);
         }

         IsoGridSquare var7;
         int var8;
         IsoFeedingTrough var9;
         if (var5 && this.isSlave() && !StringUtils.isNullOrEmpty(var1.rawgetStr("sprite2"))) {
            var7 = null;
            if (this.north) {
               var7 = IsoWorld.instance.CurrentCell.getGridSquare(this.getSquare().x + 1, this.getSquare().y, this.getSquare().z);
            } else {
               var7 = IsoWorld.instance.CurrentCell.getGridSquare(this.getSquare().x, this.getSquare().y + 1, this.getSquare().z);
            }

            if (var7 != null) {
               for(var8 = 0; var8 < var7.getObjects().size(); ++var8) {
                  var9 = (IsoFeedingTrough)Type.tryCastTo((IsoObject)var7.getObjects().get(var8), IsoFeedingTrough.class);
                  if (var9 != null) {
                     if (var4) {
                        var2 = var9.getWater();
                        var3 = var9.getMaxWater();
                     } else {
                        var2 = var9.getCurrentFeedAmount();
                        var3 = (float)var9.getMaxFeed();
                     }
                  }
               }
            }
         }

         if (var4) {
            if (var2 > 10.0F && var2 < var3 / 2.0F) {
               if (this.getSprite().getName().equals(var1.rawgetStr("sprite1"))) {
                  this.setOverlaySprite(var1.rawgetStr("sprite1WaterOverlay1"), false);
               } else if (this.getSprite().getName().equals(var1.rawgetStr("sprite2"))) {
                  this.setOverlaySprite(var1.rawgetStr("sprite2WaterOverlay1"), false);
               } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth1"))) {
                  this.setOverlaySprite(var1.rawgetStr("spriteNorth1WaterOverlay1"), false);
               } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth2"))) {
                  this.setOverlaySprite(var1.rawgetStr("spriteNorth2WaterOverlay1"), false);
               }
            } else if (var2 >= var3 / 2.0F) {
               if (this.getSprite().getName().equals(var1.rawgetStr("sprite1"))) {
                  this.setOverlaySprite(var1.rawgetStr("sprite1WaterOverlay2"), false);
               } else if (this.getSprite().getName().equals(var1.rawgetStr("sprite2"))) {
                  this.setOverlaySprite(var1.rawgetStr("sprite2WaterOverlay2"), false);
               } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth1"))) {
                  this.setOverlaySprite(var1.rawgetStr("spriteNorth1WaterOverlay2"), false);
               } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth2"))) {
                  this.setOverlaySprite(var1.rawgetStr("spriteNorth2WaterOverlay2"), false);
               }
            }
         } else if (var2 > 1.0F && var2 < var3 / 2.0F) {
            if (this.getSprite().getName().equals(var1.rawgetStr("sprite1"))) {
               this.setOverlaySprite(var1.rawgetStr("sprite1FoodOverlay1"), false);
            } else if (this.getSprite().getName().equals(var1.rawgetStr("sprite2"))) {
               this.setOverlaySprite(var1.rawgetStr("sprite2FoodOverlay1"), false);
            } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth1"))) {
               this.setOverlaySprite(var1.rawgetStr("spriteNorth1FoodOverlay1"), false);
            } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth2"))) {
               this.setOverlaySprite(var1.rawgetStr("spriteNorth2FoodOverlay1"), false);
            }
         } else if (var2 >= var3 / 2.0F) {
            if (this.getSprite().getName().equals(var1.rawgetStr("sprite1"))) {
               this.setOverlaySprite(var1.rawgetStr("sprite1FoodOverlay2"), false);
            } else if (this.getSprite().getName().equals(var1.rawgetStr("sprite2"))) {
               this.setOverlaySprite(var1.rawgetStr("sprite2FoodOverlay2"), false);
            } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth1"))) {
               this.setOverlaySprite(var1.rawgetStr("spriteNorth1FoodOverlay2"), false);
            } else if (this.getSprite().getName().equals(var1.rawgetStr("spriteNorth2"))) {
               this.setOverlaySprite(var1.rawgetStr("spriteNorth2FoodOverlay2"), false);
            }
         }

         if (var5) {
            if (!StringUtils.isNullOrEmpty(var1.rawgetStr("sprite2"))) {
               var7 = null;
               if (this.north) {
                  var7 = IsoWorld.instance.CurrentCell.getGridSquare(this.getSquare().x - 1, this.getSquare().y, this.getSquare().z);
               } else {
                  var7 = IsoWorld.instance.CurrentCell.getGridSquare(this.getSquare().x, this.getSquare().y - 1, this.getSquare().z);
               }

               if (var7 != null) {
                  for(var8 = 0; var8 < var7.getObjects().size(); ++var8) {
                     var9 = (IsoFeedingTrough)Type.tryCastTo((IsoObject)var7.getObjects().get(var8), IsoFeedingTrough.class);
                     if (var9 != null) {
                        var9.checkOverlay(var1, var2, var3, var4, false, var6);
                        return;
                     }
                  }
               }
            }

         }
      }
   }

   public float getFeedAmount(String var1) {
      return this.feedingTypes.get(var1) == null ? 0.0F : (Float)this.feedingTypes.get(var1);
   }

   public void updateLuaObject() {
      SGlobalObjects.OnIsoObjectChangedItself("feedingTrough", this);
   }

   public ArrayList<String> getAllFeedingTypes() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = this.feedingTypes.keySet().iterator();

      while(var2.hasNext()) {
         var1.add((String)var2.next());
      }

      return var1;
   }

   public int getLinkedX() {
      return this.linkedX;
   }

   public int getLinkedY() {
      return this.linkedY;
   }

   public void setLinkedX(int var1) {
      this.linkedX = var1;
   }

   public void setLinkedY(int var1) {
      this.linkedY = var1;
   }

   private boolean isSlave() {
      return this.linkedX > 0 && this.linkedY > 0;
   }

   public float getMaxWater() {
      return this.maxWater;
   }

   public void setMaxWater(float var1) {
      this.maxWater = var1;
   }

   public float getWater() {
      return this.water;
   }

   public void setWater(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.water = var1;
      if (this.water > this.getMaxWater()) {
         this.water = this.getMaxWater();
      }

      if (this.water > 0.0F) {
         this.container = null;
      } else if (this.water == 0.0F && this.getContainer() == null) {
         this.setContainer(new ItemContainer());
      }

      if (this.getContainer() == null || this.getContainer().isEmpty()) {
         this.checkOverlay(this.def, this.water, this.getMaxWater(), true, true, true);
      }

      this.updateLuaObject();
   }

   public ArrayList<IsoAnimal> getLinkedAnimals() {
      return this.linkedAnimals;
   }

   public void setLinkedAnimals(ArrayList<IsoAnimal> var1) {
      this.linkedAnimals = var1;
   }

   public boolean isEmptyFeed() {
      return this.feedingTypes.isEmpty();
   }

   public int getMaxFeed() {
      return this.maxFeed;
   }

   public void setMaxFeed(int var1) {
      this.maxFeed = var1;
   }

   public void setDef(KahluaTableImpl var1) {
      this.def = var1;
   }

   public void setNorth(boolean var1) {
      this.north = var1;
   }

   public void addLinkedAnimal(IsoAnimal var1) {
      if (!this.linkedAnimals.contains(var1)) {
         this.linkedAnimals.add(var1);
      }

   }

   public float getCurrentFeedAmount() {
      if (this.getContainer() == null) {
         return 0.0F;
      } else {
         float var1 = 0.0F;

         for(int var2 = 0; var2 < this.getContainer().getItems().size(); ++var2) {
            InventoryItem var3 = (InventoryItem)this.getContainer().getItems().get(var2);
            if (var3 instanceof Food) {
               var1 += Math.abs(((Food)var3).getHungerChange());
            }

            if (!StringUtils.isNullOrEmpty(var3.getAnimalFeedType()) && var3 instanceof DrainableComboItem) {
               DrainableComboItem var4 = (DrainableComboItem)var3;
               var1 += (float)var4.getCurrentUses() * 0.1F;
            }
         }

         return (float)Math.round(var1 * 100.0F) / 100.0F;
      }
   }
}
