package zombie.randomizedWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.debug.DebugLog;
import zombie.entity.GameEntityFactory;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehiclesDB2;

public class RandomizedWorldBase {
   private static final Vector2 s_tempVector2 = new Vector2();
   protected int minimumDays = 0;
   protected int maximumDays = 0;
   protected int minimumRooms = 0;
   protected boolean unique = false;
   private boolean rvsVehicleKeyAddedToZombie = false;
   protected boolean isRat = false;
   protected String name = null;
   protected String debugLine = "";
   protected boolean reallyAlwaysForce = false;
   private static final ArrayList<String> barnClutter = new ArrayList();
   private static final ArrayList<String> bathroomSinkClutter = new ArrayList();
   private static final ArrayList<String> bedClutter = new ArrayList();
   private static final ArrayList<String> bbqClutter = new ArrayList();
   private static final ArrayList<String> cafeClutter = new ArrayList();
   private static final ArrayList<String> carpentryToolClutter = new ArrayList();
   private static final ArrayList<String> deadEndClutter = new ArrayList();
   private static final ArrayList<String> dormClutter = new ArrayList();
   private static final ArrayList<String> farmStorageClutter = new ArrayList();
   private static final ArrayList<String> footballNightDrinks = new ArrayList();
   private static final ArrayList<String> footballNightSnacks = new ArrayList();
   private static final ArrayList<String> garageStorageClutter = new ArrayList();
   private static final ArrayList<String> gigamartClutter = new ArrayList();
   private static final ArrayList<String> groceryClutter = new ArrayList();
   private static final ArrayList<String> hairSalonClutter = new ArrayList();
   private static final ArrayList<String> hallClutter = new ArrayList();
   private static final ArrayList<String> henDoDrinks = new ArrayList();
   private static final ArrayList<String> henDoSnacks = new ArrayList();
   private static final ArrayList<String> hoedownClutter = new ArrayList();
   private static final ArrayList<String> housePartyClutter = new ArrayList();
   private static final ArrayList<String> judgeClutter = new ArrayList();
   private static final ArrayList<String> kidClutter = new ArrayList();
   private static final ArrayList<String> kitchenSinkClutter = new ArrayList();
   private static final ArrayList<String> kitchenCounterClutter = new ArrayList();
   private static final ArrayList<String> kitchenStoveClutter = new ArrayList();
   private static final ArrayList<String> laundryRoomClutter = new ArrayList();
   private static final ArrayList<String> livingRoomClutter = new ArrayList();
   private static final ArrayList<String> medicalClutter = new ArrayList();
   private static final ArrayList<String> oldShelterClutter = new ArrayList();
   private static final ArrayList<String> officeCarDealerClutter = new ArrayList();
   private static final ArrayList<String> officePaperworkClutter = new ArrayList();
   private static final ArrayList<String> officePenClutter = new ArrayList();
   private static final ArrayList<String> officeOtherClutter = new ArrayList();
   private static final ArrayList<String> officeTreatClutter = new ArrayList();
   private static final ArrayList<String> ovenFoodClutter = new ArrayList();
   private static final ArrayList<String> pillowClutter = new ArrayList();
   private static final ArrayList<String> pokerNightClutter = new ArrayList();
   private static final ArrayList<String> richJerkClutter = new ArrayList();
   private static final ArrayList<String> sadCampsiteClutter = new ArrayList();
   private static final ArrayList<String> sidetableClutter = new ArrayList();
   private static final ArrayList<String> survivalistCampsiteClutter = new ArrayList();
   private static final ArrayList<String> twiggyClutter = new ArrayList();
   private static final ArrayList<String> utilityToolClutter = new ArrayList();
   private static final ArrayList<String> watchClutter = new ArrayList();
   private static final ArrayList<String> woodcraftClutter = new ArrayList();

   public RandomizedWorldBase() {
   }

   public BaseVehicle addVehicle(Zone var1, IsoGridSquare var2, IsoChunk var3, String var4, String var5, IsoDirections var6) {
      return this.addVehicle(var1, var2, var3, var4, var5, (Integer)null, var6, (String)null);
   }

   public BaseVehicle addVehicleFlipped(Zone var1, IsoGridSquare var2, IsoChunk var3, String var4, String var5, Integer var6, IsoDirections var7, String var8) {
      if (var2 == null) {
         return null;
      } else {
         if (var7 == null) {
            var7 = IsoDirections.getRandom();
         }

         Vector2 var9 = var7.ToVector();
         return this.addVehicleFlipped(var1, (float)var2.x, (float)var2.y, (float)var2.z, var9.getDirection(), var4, var5, var6, var8);
      }
   }

   public BaseVehicle addVehicleFlipped(Zone var1, float var2, float var3, float var4, float var5, String var6, String var7, Integer var8, String var9) {
      if (StringUtils.isNullOrEmpty(var6)) {
         var6 = "junkyard";
      }

      IsoGridSquare var10 = IsoWorld.instance.CurrentCell.getGridSquare((double)var2, (double)var3, (double)var4);
      if (var10 == null) {
         return null;
      } else {
         IsoChunk var11 = var10.getChunk();
         IsoDirections var12 = IsoDirections.fromAngle(var5);
         BaseVehicle var13 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         var13.specificDistributionId = var9;
         VehicleType var14 = VehicleType.getRandomVehicleType(var6, false);
         if (!StringUtils.isNullOrEmpty(var7)) {
            var13.setScriptName(var7);
            var13.setScript();
            if (var8 != null) {
               var13.setSkinIndex(var8);
            }
         } else {
            if (var14 == null) {
               return null;
            }

            var13.setVehicleType(var14.name);
            if (!var11.RandomizeModel(var13, var1, var6, var14)) {
               return null;
            }
         }

         if (var14.isSpecialCar) {
            var13.setDoColor(false);
         }

         var13.setDir(var12);

         float var15;
         for(var15 = var5 - 1.5707964F; (double)var15 > 6.283185307179586; var15 = (float)((double)var15 - 6.283185307179586)) {
         }

         var13.savedRot.rotationXYZ(0.0F, -var15, 3.1415927F);
         var13.jniTransform.setRotation(var13.savedRot);
         float var16 = PZMath.max(var4 * 3.0F * 0.8164967F, (float)PZMath.fastfloor(var4) + var13.getScript().getExtents().y() + 0.1F);
         var13.jniTransform.origin.y = var16;
         var4 = var16 / 2.44949F;
         var13.setX(var2);
         var13.setY(var3);
         var13.setZ(var4);
         if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var13)) {
            var13.setSquare(var10);
            var10.chunk.vehicles.add(var13);
            var13.chunk = var10.chunk;
            var13.savedPhysicsZ = var16;
            var13.addToWorld();
            VehiclesDB2.instance.addVehicle(var13);
         }

         var13.savedPhysicsZ = var16;
         var13.setGeneralPartCondition(0.2F, 70.0F);
         var13.rust = Rand.Next(100) < 70 ? 1.0F : 0.0F;
         return var13;
      }
   }

   public BaseVehicle addVehicle(Zone var1, IsoGridSquare var2, IsoChunk var3, String var4, String var5, Integer var6, IsoDirections var7, String var8) {
      if (var2 == null) {
         return null;
      } else {
         if (var7 == null) {
            var7 = IsoDirections.getRandom();
         }

         Vector2 var9 = var7.ToVector();
         var9.rotate(Rand.Next(-0.5F, 0.5F));
         return this.addVehicle(var1, (float)var2.x, (float)var2.y, (float)var2.z, var9.getDirection(), var4, var5, var6, var8);
      }
   }

   public BaseVehicle addVehicle(IsoGridSquare var1, IsoChunk var2, String var3, String var4, Integer var5, IsoDirections var6, String var7) {
      if (var1 == null) {
         return null;
      } else {
         if (var6 == null) {
            var6 = IsoDirections.getRandom();
         }

         Vector2 var8 = var6.ToVector();
         var8.rotate(Rand.Next(-0.5F, 0.5F));
         return this.addVehicle((float)var1.x, (float)var1.y, (float)var1.z, var8.getDirection(), var3, var4, var5, var7);
      }
   }

   public BaseVehicle addVehicle(Zone var1, float var2, float var3, float var4, float var5, String var6, String var7, Integer var8, String var9) {
      if (StringUtils.isNullOrEmpty(var6)) {
         var6 = "junkyard";
      }

      IsoGridSquare var10 = IsoWorld.instance.CurrentCell.getGridSquare((double)var2, (double)var3, (double)var4);
      if (var10 == null) {
         return null;
      } else {
         IsoChunk var11 = var10.getChunk();
         IsoDirections var12 = IsoDirections.fromAngle(var5);
         BaseVehicle var13 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         var13.specificDistributionId = var9;
         VehicleType var14 = VehicleType.getRandomVehicleType(var6, false);
         if (!StringUtils.isNullOrEmpty(var7)) {
            var13.setScriptName(var7);
            var13.setScript();
            if (var8 != null) {
               var13.setSkinIndex(var8);
            }
         } else {
            if (var14 == null) {
               return null;
            }

            var13.setVehicleType(var14.name);
            if (!var11.RandomizeModel(var13, var1, var6, var14)) {
               return null;
            }
         }

         if (var14.isSpecialCar) {
            var13.setDoColor(false);
         }

         var13.setDir(var12);

         float var15;
         for(var15 = var5 - 1.5707964F; (double)var15 > 6.283185307179586; var15 = (float)((double)var15 - 6.283185307179586)) {
         }

         var13.savedRot.setAngleAxis(-var15, 0.0F, 1.0F, 0.0F);
         var13.jniTransform.setRotation(var13.savedRot);
         var13.setX(var2);
         var13.setY(var3);
         var13.setZ(var4);
         if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var13)) {
            var13.setSquare(var10);
            var10.chunk.vehicles.add(var13);
            var13.chunk = var10.chunk;
            var13.addToWorld();
            VehiclesDB2.instance.addVehicle(var13);
         }

         var13.setGeneralPartCondition(0.2F, 70.0F);
         var13.rust = Rand.Next(100) < 70 ? 1.0F : 0.0F;
         var13.setPreviouslyMoved(true);
         var13.setAlarmed(false);
         return var13;
      }
   }

   public BaseVehicle addVehicle(float var1, float var2, float var3, float var4, String var5, String var6, Integer var7, String var8) {
      if (StringUtils.isNullOrEmpty(var5)) {
         var5 = "junkyard";
      }

      IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare((double)var1, (double)var2, (double)var3);
      if (var9 == null) {
         return null;
      } else {
         IsoChunk var10 = var9.getChunk();
         IsoDirections var11 = IsoDirections.fromAngle(var4);
         BaseVehicle var12 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         var12.specificDistributionId = var8;
         VehicleType var13 = VehicleType.getRandomVehicleType(var5, false);
         if (!StringUtils.isNullOrEmpty(var6)) {
            var12.setScriptName(var6);
            var12.setScript();
            if (var7 != null) {
               var12.setSkinIndex(var7);
            }
         } else {
            if (var13 == null) {
               return null;
            }

            var12.setVehicleType(var13.name);
         }

         if (var13.isSpecialCar) {
            var12.setDoColor(false);
         }

         var12.setDir(var11);

         float var14;
         for(var14 = var4 - 1.5707964F; (double)var14 > 6.283185307179586; var14 = (float)((double)var14 - 6.283185307179586)) {
         }

         var12.savedRot.setAngleAxis(-var14, 0.0F, 1.0F, 0.0F);
         var12.jniTransform.setRotation(var12.savedRot);
         var12.setX(var1);
         var12.setY(var2);
         var12.setZ(var3);
         if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var12)) {
            var12.setSquare(var9);
            var9.chunk.vehicles.add(var12);
            var12.chunk = var9.chunk;
            var12.addToWorld();
            VehiclesDB2.instance.addVehicle(var12);
         }

         var12.setGeneralPartCondition(0.2F, 70.0F);
         var12.rust = Rand.Next(100) < 70 ? 1.0F : 0.0F;
         var12.setPreviouslyMoved(true);
         var12.setAlarmed(false);
         return var12;
      }
   }

   public static void removeAllVehiclesOnZone(Zone var0) {
      for(int var1 = var0.x; var1 < var0.x + var0.w; ++var1) {
         for(int var2 = var0.y; var2 < var0.y + var0.h; ++var2) {
            IsoGridSquare var3 = IsoCell.getInstance().getGridSquare(var1, var2, 0);
            if (var3 != null) {
               BaseVehicle var4 = var3.getVehicleContainer();
               if (var4 != null) {
                  var4.permanentlyRemove();
               }
            }
         }
      }

   }

   public ArrayList<IsoZombie> addZombiesOnVehicle(int var1, String var2, Integer var3, BaseVehicle var4) {
      ArrayList var5 = new ArrayList();
      if (var4 == null) {
         return var5;
      } else {
         int var6 = 100;
         IsoGridSquare var7 = var4.getSquare();
         if (var7 != null && var7.getCell() != null) {
            for(; var1 > 0; var6 = 100) {
               while(var6 > 0) {
                  IsoGridSquare var8 = var7.getCell().getGridSquare(Rand.Next(var7.x - 4, var7.x + 4), Rand.Next(var7.y - 4, var7.y + 4), var7.z);
                  if (var8 != null && var8.getVehicleContainer() == null) {
                     --var1;
                     var5.addAll(this.addZombiesOnSquare(1, var2, var3, var8));
                     break;
                  }

                  --var6;
               }
            }

            if (!this.rvsVehicleKeyAddedToZombie && !var5.isEmpty()) {
               IsoZombie var9 = (IsoZombie)var5.get(Rand.Next(0, var5.size()));
               var9.addItemToSpawnAtDeath(var4.createVehicleKey());
               this.rvsVehicleKeyAddedToZombie = true;
            }

            return var5;
         } else {
            return var5;
         }
      }
   }

   public static IsoDeadBody createRandomDeadBody(RoomDef var0, int var1) {
      if (IsoWorld.getZombiesDisabled()) {
         return null;
      } else if (var0 == null) {
         return null;
      } else {
         IsoGridSquare var2 = getRandomSquareForCorpse(var0);
         return var2 == null ? null : createRandomDeadBody(var2, (IsoDirections)null, var1, 0, (String)null);
      }
   }

   public ArrayList<IsoZombie> addZombiesOnSquare(int var1, String var2, Integer var3, IsoGridSquare var4) {
      ArrayList var5 = new ArrayList();
      if (IsoWorld.getZombiesDisabled()) {
         return var5;
      } else if (var4 == null) {
         return var5;
      } else if (var4.isWaterSquare()) {
         return var5;
      } else {
         for(int var6 = 0; var6 < var1; ++var6) {
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(var4);
            IsoZombie var7 = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom().index(), false);
            if (var7 != null) {
               if (var3 != null) {
                  var7.setFemaleEtc(Rand.Next(100) < var3);
               }

               if (var2 != null) {
                  var7.dressInPersistentOutfit(var2);
                  var7.bDressInRandomOutfit = false;
               } else {
                  var7.dressInRandomOutfit();
                  var7.bDressInRandomOutfit = false;
               }

               var5.add(var7);
            }
         }

         ZombieSpawnRecorder.instance.record(var5, this.getClass().getSimpleName());
         return var5;
      }
   }

   public static IsoDeadBody createRandomDeadBody(int var0, int var1, int var2, IsoDirections var3, int var4) {
      return createRandomDeadBody(var0, var1, var2, var3, var4, 0);
   }

   public static IsoDeadBody createRandomDeadBody(int var0, int var1, int var2, IsoDirections var3, int var4, int var5) {
      IsoGridSquare var6 = IsoCell.getInstance().getGridSquare(var0, var1, var2);
      return createRandomDeadBody(var6, var3, var4, var5, (String)null);
   }

   public static IsoDeadBody createRandomDeadBody(IsoGridSquare var0, IsoDirections var1, int var2, int var3, String var4) {
      if (var0 == null) {
         return null;
      } else {
         boolean var5 = var1 == null;
         if (var5) {
            var1 = IsoDirections.getRandom();
         }

         return createRandomDeadBody((float)var0.x + Rand.Next(0.05F, 0.95F), (float)var0.y + Rand.Next(0.05F, 0.95F), (float)var0.z, var1.ToVector().getDirection(), var5, var2, var3, var4);
      }
   }

   public static IsoDeadBody createRandomDeadBody(float var0, float var1, float var2, float var3, boolean var4, int var5, int var6, String var7) {
      if (IsoWorld.getZombiesDisabled()) {
         return null;
      } else {
         IsoGridSquare var8 = IsoCell.getInstance().getGridSquare((double)var0, (double)var1, (double)var2);
         if (var8 == null) {
            return null;
         } else {
            IsoDirections var9 = IsoDirections.fromAngle(var3);
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(var8);
            IsoZombie var10 = VirtualZombieManager.instance.createRealZombieAlways(var9.index(), false);
            if (var10 == null) {
               return null;
            } else {
               if (var7 != null) {
                  var10.dressInPersistentOutfit(var7);
                  var10.bDressInRandomOutfit = false;
               } else {
                  var10.dressInRandomOutfit();
               }

               if (Rand.Next(100) < var6) {
                  var10.setFakeDead(true);
                  var10.setCrawler(true);
                  var10.setCanWalk(false);
                  var10.setCrawlerType(1);
               } else {
                  var10.setFakeDead(false);
                  var10.setHealth(0.0F);
               }

               var10.upKillCount = false;
               var10.getHumanVisual().zombieRotStage = ((HumanVisual)var10.getVisual()).pickRandomZombieRotStage();

               for(int var11 = 0; var11 < var5; ++var11) {
                  var10.addBlood((BloodBodyPartType)null, false, true, true);
               }

               var10.DoCorpseInventory();
               var10.setX(var0);
               var10.setY(var1);
               var10.getForwardDirection().setLengthAndDirection(var3, 1.0F);
               if (var4 || var10.isSkeleton()) {
                  alignCorpseToSquare(var10, var8);
               }

               IsoDeadBody var12 = new IsoDeadBody(var10, true);
               if (!var12.isFakeDead() && !var12.isSkeleton() && Rand.Next(20) == 0) {
                  var12.setFakeDead(true);
                  if (Rand.Next(5) == 0) {
                     var12.setCrawling(true);
                  }
               }

               return var12;
            }
         }
      }
   }

   public static IsoDeadBody createRandomDeadBody(IsoGridSquare var0, IsoDirections var1, boolean var2, int var3, int var4, String var5, Integer var6) {
      float var7 = (float)var0.x + Rand.Next(0.05F, 0.95F);
      float var8 = (float)var0.y + Rand.Next(0.05F, 0.95F);
      if (IsoWorld.getZombiesDisabled()) {
         return null;
      } else if (var0 == null) {
         return null;
      } else {
         boolean var9 = var1 == null;
         if (var9) {
            var1 = IsoDirections.getRandom();
         }

         float var10 = var1.ToVector().getDirection();
         IsoDirections var11 = IsoDirections.fromAngle(var10);
         VirtualZombieManager.instance.choices.clear();
         VirtualZombieManager.instance.choices.add(var0);
         IsoZombie var12 = VirtualZombieManager.instance.createRealZombieAlways(var11.index(), false);
         if (var12 == null) {
            return null;
         } else {
            if (var6 != null) {
               var12.setFemaleEtc(Rand.Next(100) < var6);
            }

            if (var5 != null) {
               var12.dressInPersistentOutfit(var5);
               var12.bDressInRandomOutfit = false;
            } else {
               var12.dressInRandomOutfit();
            }

            if (Rand.Next(100) < var4) {
               var12.setFakeDead(true);
               var12.setCrawler(true);
               var12.setCanWalk(false);
               var12.setCrawlerType(1);
            } else {
               var12.setFakeDead(false);
               var12.setHealth(0.0F);
            }

            var12.upKillCount = false;
            var12.getHumanVisual().zombieRotStage = ((HumanVisual)var12.getVisual()).pickRandomZombieRotStage();

            for(int var13 = 0; var13 < var3; ++var13) {
               var12.addBlood((BloodBodyPartType)null, false, true, true);
            }

            var12.DoCorpseInventory();
            var12.setX(var7);
            var12.setY(var8);
            var12.getForwardDirection().setLengthAndDirection(var10, 1.0F);
            if (var2 || var12.isSkeleton()) {
               alignCorpseToSquare(var12, var0);
            }

            IsoDeadBody var14 = new IsoDeadBody(var12, true);
            if (!var14.isFakeDead() && !var14.isSkeleton() && Rand.Next(20) == 0) {
               var14.setFakeDead(true);
               if (Rand.Next(5) == 0) {
                  var14.setCrawling(true);
               }
            }

            return var14;
         }
      }
   }

   public void addTraitOfBlood(IsoDirections var1, int var2, int var3, int var4, int var5) {
      for(int var6 = 0; var6 < var2; ++var6) {
         float var7 = 0.0F;
         float var8 = 0.0F;
         if (var1 == IsoDirections.S) {
            var8 = Rand.Next(-2.0F, 0.5F);
         }

         if (var1 == IsoDirections.N) {
            var8 = Rand.Next(-0.5F, 2.0F);
         }

         if (var1 == IsoDirections.E) {
            var7 = Rand.Next(-2.0F, 0.5F);
         }

         if (var1 == IsoDirections.W) {
            var7 = Rand.Next(-0.5F, 2.0F);
         }

         new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, IsoCell.getInstance(), (float)var3, (float)var4, (float)var5 + 0.2F, var7, var8);
      }

   }

   public void addTrailOfBlood(float var1, float var2, float var3, float var4, int var5) {
      Vector2 var6 = s_tempVector2;

      for(int var7 = 0; var7 < var5; ++var7) {
         float var8 = Rand.Next(-0.5F, 2.0F);
         if (var8 < 0.0F) {
            var6.setLengthAndDirection(var4 + 3.1415927F, -var8);
         } else {
            var6.setLengthAndDirection(var4, var8);
         }

         new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, IsoCell.getInstance(), var1, var2, var3 + 0.2F, var6.x, var6.y);
      }

   }

   public void addBloodSplat(IsoGridSquare var1, int var2) {
      for(int var3 = 0; var3 < var2; ++var3) {
         var1.getChunk().addBloodSplat((float)var1.x + Rand.Next(-0.5F, 0.5F), (float)var1.y + Rand.Next(-0.5F, 0.5F), (float)var1.z, Rand.Next(8));
      }

   }

   public void setAttachedItem(IsoZombie var1, String var2, String var3, String var4) {
      InventoryItem var5 = InventoryItemFactory.CreateItem(var3);
      if (var5 != null) {
         var5.setCondition(Rand.Next(Math.max(2, var5.getConditionMax() - 5), var5.getConditionMax()), false);
         if (var5 instanceof HandWeapon) {
            ((HandWeapon)var5).randomizeBullets();
         }

         var1.setAttachedItem(var2, var5);
         if (!StringUtils.isNullOrEmpty(var4)) {
            var1.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem(var4));
         }

      }
   }

   public static IsoGameCharacter createRandomZombie(RoomDef var0) {
      IsoGridSquare var1 = getRandomSpawnSquare(var0);
      return createRandomZombie(var1.getX(), var1.getY(), var1.getZ());
   }

   public static IsoGameCharacter createRandomZombieForCorpse(RoomDef var0) {
      IsoGridSquare var1 = getRandomSquareForCorpse(var0);
      if (var1 == null) {
         return null;
      } else {
         IsoGameCharacter var2 = createRandomZombie(var1.getX(), var1.getY(), var1.getZ());
         if (var2 != null) {
            alignCorpseToSquare(var2, var1);
         }

         return var2;
      }
   }

   public static IsoDeadBody createBodyFromZombie(IsoGameCharacter var0) {
      if (IsoWorld.getZombiesDisabled()) {
         return null;
      } else {
         for(int var1 = 0; var1 < 6; ++var1) {
            var0.splatBlood(Rand.Next(1, 4), 0.3F);
         }

         IsoDeadBody var2 = new IsoDeadBody(var0, true);
         return var2;
      }
   }

   public static IsoGameCharacter createRandomZombie(int var0, int var1, int var2) {
      RandomizedBuildingBase.HumanCorpse var3 = new RandomizedBuildingBase.HumanCorpse(IsoWorld.instance.getCell(), (float)var0, (float)var1, (float)var2);
      var3.setDescriptor(SurvivorFactory.CreateSurvivor());
      var3.setFemale(var3.getDescriptor().isFemale());
      var3.setDir(IsoDirections.fromIndex(Rand.Next(8)));
      var3.initWornItems("Human");
      var3.initAttachedItems("Human");
      Outfit var4 = var3.getRandomDefaultOutfit();
      var3.dressInNamedOutfit(var4.m_Name);
      var3.initSpritePartsEmpty();
      var3.Dressup(var3.getDescriptor());
      return var3;
   }

   private static boolean isSquareClear(IsoGridSquare var0) {
      return var0 != null && canSpawnAt(var0) && !var0.HasStairs() && !var0.HasTree() && !var0.getProperties().Is(IsoFlagType.bed) && !var0.getProperties().Is(IsoFlagType.waterPiped);
   }

   private static boolean isSquareClear(IsoGridSquare var0, IsoDirections var1) {
      IsoGridSquare var2 = var0.getAdjacentSquare(var1);
      return isSquareClear(var2) && !var0.isSomethingTo(var2) && var0.getRoomID() == var2.getRoomID();
   }

   public static boolean is1x2AreaClear(IsoGridSquare var0) {
      return isSquareClear(var0) && isSquareClear(var0, IsoDirections.N);
   }

   public static boolean is2x1AreaClear(IsoGridSquare var0) {
      return isSquareClear(var0) && isSquareClear(var0, IsoDirections.W);
   }

   public static boolean is2x1or1x2AreaClear(IsoGridSquare var0) {
      return isSquareClear(var0) && (isSquareClear(var0, IsoDirections.W) || isSquareClear(var0, IsoDirections.N));
   }

   public static boolean is2x2AreaClear(IsoGridSquare var0) {
      return isSquareClear(var0) && isSquareClear(var0, IsoDirections.N) && isSquareClear(var0, IsoDirections.W) && isSquareClear(var0, IsoDirections.NW);
   }

   public static void alignCorpseToSquare(IsoGameCharacter var0, IsoGridSquare var1) {
      int var2 = var1.x;
      int var3 = var1.y;
      IsoDirections var4 = IsoDirections.fromIndex(Rand.Next(8));
      boolean var5 = is1x2AreaClear(var1);
      boolean var6 = is2x1AreaClear(var1);
      if (var5 && var6) {
         var5 = Rand.Next(2) == 0;
         var6 = !var5;
      }

      if (is2x2AreaClear(var1)) {
         var0.setX((float)var2);
         var0.setY((float)var3);
      } else if (var5) {
         var0.setX((float)var2 + 0.5F);
         var0.setY((float)var3);
         var4 = Rand.Next(2) == 0 ? IsoDirections.N : IsoDirections.S;
      } else if (var6) {
         var0.setX((float)var2);
         var0.setY((float)var3 + 0.5F);
         var4 = Rand.Next(2) == 0 ? IsoDirections.W : IsoDirections.E;
      } else if (is1x2AreaClear(var1.getAdjacentSquare(IsoDirections.S))) {
         var0.setX((float)var2 + 0.5F);
         var0.setY((float)var3 + 0.99F);
         var4 = Rand.Next(2) == 0 ? IsoDirections.N : IsoDirections.S;
      } else if (is2x1AreaClear(var1.getAdjacentSquare(IsoDirections.E))) {
         var0.setX((float)var2 + 0.99F);
         var0.setY((float)var3 + 0.5F);
         var4 = Rand.Next(2) == 0 ? IsoDirections.W : IsoDirections.E;
      }

      var0.setDir(var4);
      var0.setLastX(var0.setNextX(var0.getX()));
      var0.setLastY(var0.setNextY(var0.getY()));
      var0.setScriptnx(var0.getX());
      var0.setScriptny(var0.getY());
   }

   public RoomDef getRandomRoom(BuildingDef var1, int var2) {
      return var1.getRandomRoom(var2, false);
   }

   public RoomDef getRandomRoomNoKids(BuildingDef var1, int var2) {
      return var1.getRandomRoom(var2, true);
   }

   public RoomDef getRoom(BuildingDef var1, String var2) {
      return var1.getRoom(var2);
   }

   public RoomDef getRoomNoKids(BuildingDef var1, String var2) {
      return var1.getRoom(var2, true);
   }

   public RoomDef getLivingRoomOrKitchen(BuildingDef var1) {
      RoomDef var2 = var1.getRoom("livingroom");
      if (var2 == null) {
         var2 = var1.getRoom("kitchen");
      }

      return var2;
   }

   private static boolean canSpawnAt(IsoGridSquare var0) {
      if (var0 == null) {
         return false;
      } else {
         return var0.HasStairs() ? false : VirtualZombieManager.instance.canSpawnAt(var0.x, var0.y, var0.z);
      }
   }

   public static IsoGridSquare getRandomSpawnSquare(RoomDef var0) {
      return var0 == null ? null : var0.getRandomSquare(RandomizedWorldBase::canSpawnAt);
   }

   public static IsoGridSquare getRandomSquareForCorpse(RoomDef var0) {
      IsoGridSquare var1 = var0.getRandomSquare(RandomizedWorldBase::is2x2AreaClear);
      IsoGridSquare var2 = var0.getRandomSquare(RandomizedWorldBase::is2x1or1x2AreaClear);
      if (var1 == null || var2 != null && Rand.Next(4) == 0) {
         var1 = var2;
      }

      return var1;
   }

   public BaseVehicle spawnCarOnNearestNav(String var1, BuildingDef var2) {
      IsoGridSquare var3 = null;
      int var4 = (var2.x + var2.x2) / 2;
      int var5 = (var2.y + var2.y2) / 2;

      int var6;
      IsoGridSquare var7;
      for(var6 = var4; var6 < var4 + 20; ++var6) {
         var7 = IsoCell.getInstance().getGridSquare(var6, var5, 0);
         if (var7 != null && "Nav".equals(var7.getZoneType()) && !this.checkAreaForCarsSpawn(var7)) {
            var3 = var7;
            break;
         }
      }

      if (var3 != null) {
         return this.spawnCar(var1, var3);
      } else {
         for(var6 = var4; var6 > var4 - 20; --var6) {
            var7 = IsoCell.getInstance().getGridSquare(var6, var5, 0);
            if (var7 != null && "Nav".equals(var7.getZoneType()) && !this.checkAreaForCarsSpawn(var7)) {
               var3 = var7;
               break;
            }
         }

         if (var3 != null) {
            return this.spawnCar(var1, var3);
         } else {
            for(var6 = var5; var6 < var5 + 20; ++var6) {
               var7 = IsoCell.getInstance().getGridSquare(var4, var6, 0);
               if (var7 != null && "Nav".equals(var7.getZoneType()) && !this.checkAreaForCarsSpawn(var7)) {
                  var3 = var7;
                  break;
               }
            }

            if (var3 != null) {
               return this.spawnCar(var1, var3);
            } else {
               for(var6 = var5; var6 > var5 - 20; --var6) {
                  var7 = IsoCell.getInstance().getGridSquare(var4, var6, 0);
                  if (var7 != null && "Nav".equals(var7.getZoneType()) && !this.checkAreaForCarsSpawn(var7)) {
                     var3 = var7;
                     break;
                  }
               }

               return var3 != null && !this.checkAreaForCarsSpawn(var3) ? this.spawnCar(var1, var3) : null;
            }
         }
      }
   }

   public BaseVehicle spawnCarOnNearestNav(String var1, BuildingDef var2, String var3) {
      IsoGridSquare var4 = null;
      int var5 = (var2.x + var2.x2) / 2;
      int var6 = (var2.y + var2.y2) / 2;

      int var7;
      IsoGridSquare var8;
      for(var7 = var5; var7 < var5 + 20; ++var7) {
         var8 = IsoCell.getInstance().getGridSquare(var7, var6, 0);
         if (var8 != null && "Nav".equals(var8.getZoneType()) && !this.checkAreaForCarsSpawn(var8)) {
            var4 = var8;
            break;
         }
      }

      if (var4 != null) {
         return this.spawnCar(var1, var4);
      } else {
         for(var7 = var5; var7 > var5 - 20; --var7) {
            var8 = IsoCell.getInstance().getGridSquare(var7, var6, 0);
            if (var8 != null && "Nav".equals(var8.getZoneType()) && !this.checkAreaForCarsSpawn(var8)) {
               var4 = var8;
               break;
            }
         }

         if (var4 != null) {
            return this.spawnCar(var1, var4);
         } else {
            for(var7 = var6; var7 < var6 + 20; ++var7) {
               var8 = IsoCell.getInstance().getGridSquare(var5, var7, 0);
               if (var8 != null && "Nav".equals(var8.getZoneType()) && !this.checkAreaForCarsSpawn(var8)) {
                  var4 = var8;
                  break;
               }
            }

            if (var4 != null) {
               return this.addVehicle((Zone)null, var4, (IsoChunk)null, (String)null, var1, (Integer)null, (IsoDirections)null, var3);
            } else {
               for(var7 = var6; var7 > var6 - 20; --var7) {
                  var8 = IsoCell.getInstance().getGridSquare(var5, var7, 0);
                  if (var8 != null && "Nav".equals(var8.getZoneType()) && !this.checkAreaForCarsSpawn(var8)) {
                     var4 = var8;
                     break;
                  }
               }

               return var4 != null && !this.checkAreaForCarsSpawn(var4) ? this.addVehicle((Zone)null, var4, (IsoChunk)null, (String)null, var1, (Integer)null, (IsoDirections)null, var3) : null;
            }
         }
      }
   }

   public boolean checkAreaForCarsSpawn(IsoGridSquare var1) {
      return this.checkRadiusForCarSpawn(var1, 2);
   }

   public boolean checkRadiusForCarSpawn(IsoGridSquare var1, int var2) {
      if (var2 < 1) {
         return false;
      } else {
         for(int var3 = 0 - var2; var3 < var2; ++var3) {
            for(int var4 = 0 - var2; var4 < var2; ++var4) {
               if (var1 != null && !var1.isVehicleIntersecting() && !var1.isSolid() && !var1.isSolidTrans() && !var1.HasTree()) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private BaseVehicle spawnCar(String var1, IsoGridSquare var2) {
      BaseVehicle var3 = new BaseVehicle(IsoWorld.instance.CurrentCell);
      var3.setScriptName(var1);
      var3.setX((float)var2.x + 0.5F);
      var3.setY((float)var2.y + 0.5F);
      var3.setZ(0.0F);
      var3.savedRot.setAngleAxis(Rand.Next(0.0F, 6.2831855F), 0.0F, 1.0F, 0.0F);
      var3.jniTransform.setRotation(var3.savedRot);
      if (IsoChunk.doSpawnedVehiclesInInvalidPosition(var3)) {
         var3.keySpawned = 1;
         var3.setSquare(var2);
         var3.square.chunk.vehicles.add(var3);
         var3.chunk = var3.square.chunk;
         var3.addToWorld();
         VehiclesDB2.instance.addVehicle(var3);
      }

      var3.setGeneralPartCondition(0.3F, 70.0F);
      return var3;
   }

   public InventoryItem addItemOnGround(IsoGridSquare var1, String var2) {
      if (SandboxOptions.instance.RemoveStoryLoot.getValue() && ItemPickerJava.getLootModifier(var2) == 0.0F) {
         return null;
      } else {
         return var1 != null && !StringUtils.isNullOrWhitespace(var2) ? ItemSpawner.spawnItem(var2, var1, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, true) : null;
      }
   }

   public InventoryItem addItemOnGroundNoLoot(IsoGridSquare var1, String var2) {
      if (SandboxOptions.instance.RemoveStoryLoot.getValue() && ItemPickerJava.getLootModifier(var2) == 0.0F) {
         return null;
      } else {
         return var1 != null && !StringUtils.isNullOrWhitespace(var2) ? ItemSpawner.spawnItem(var2, var1, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, false) : null;
      }
   }

   public static InventoryItem addItemOnGroundStatic(IsoGridSquare var0, String var1) {
      if (SandboxOptions.instance.RemoveStoryLoot.getValue() && ItemPickerJava.getLootModifier(var1) == 0.0F) {
         return null;
      } else {
         return var0 != null && !StringUtils.isNullOrWhitespace(var1) ? ItemSpawner.spawnItem(var1, var0, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F) : null;
      }
   }

   public InventoryItem addItemOnGround(IsoGridSquare var1, InventoryItem var2) {
      if (SandboxOptions.instance.RemoveStoryLoot.getValue() && ItemPickerJava.getLootModifier(var2.getFullType()) == 0.0F) {
         return null;
      } else {
         return var1 != null && var2 != null ? ItemSpawner.spawnItem(var2, var1, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F) : null;
      }
   }

   public InventoryItem addItemOnGroundNoLoot(IsoGridSquare var1, InventoryItem var2) {
      if (SandboxOptions.instance.RemoveStoryLoot.getValue() && ItemPickerJava.getLootModifier(var2.getFullType()) == 0.0F) {
         return null;
      } else {
         return var1 != null && var2 != null ? ItemSpawner.spawnItem(var2, var1, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, false) : null;
      }
   }

   public static InventoryItem addItemOnGroundStatic(IsoGridSquare var0, InventoryItem var1) {
      if (SandboxOptions.instance.RemoveStoryLoot.getValue() && ItemPickerJava.getLootModifier(var1.getFullType()) == 0.0F) {
         return null;
      } else {
         return var0 != null && var1 != null ? ItemSpawner.spawnItem(var1, var0, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F) : null;
      }
   }

   public void addRandomItemsOnGround(RoomDef var1, String var2, int var3) {
      for(int var4 = 0; var4 < var3; ++var4) {
         IsoGridSquare var5 = getRandomSpawnSquare(var1);
         this.addItemOnGround(var5, var2);
      }

   }

   public void addRandomItemsOnGround(RoomDef var1, ArrayList<String> var2, int var3) {
      for(int var4 = 0; var4 < var3; ++var4) {
         IsoGridSquare var5 = getRandomSpawnSquare(var1);
         this.addRandomItemOnGround(var5, var2);
      }

   }

   public InventoryItem addRandomItemOnGround(IsoGridSquare var1, ArrayList<String> var2) {
      if (var1 != null && !var2.isEmpty()) {
         String var3 = (String)PZArrayUtil.pickRandom((List)var2);
         return this.addItemOnGround(var1, var3);
      } else {
         return null;
      }
   }

   public HandWeapon addWeapon(String var1, boolean var2) {
      HandWeapon var3 = (HandWeapon)InventoryItemFactory.CreateItem(var1);
      if (var3 == null) {
         return null;
      } else {
         if (var3.isRanged() && var2) {
            if (!StringUtils.isNullOrWhitespace(var3.getMagazineType())) {
               var3.setContainsClip(true);
            }

            var3.setCurrentAmmoCount(Rand.Next(Math.max(var3.getMaxAmmo() - 8, 0), var3.getMaxAmmo() - 2));
         }

         return var3;
      }
   }

   public IsoDeadBody createSkeletonCorpse(RoomDef var1) {
      if (var1 == null) {
         return null;
      } else {
         IsoGridSquare var2 = var1.getRandomSquare(RandomizedWorldBase::is2x1or1x2AreaClear);
         return var2 == null ? null : this.createSkeletonCorpse(var2);
      }
   }

   public IsoDeadBody createSkeletonCorpse(IsoGridSquare var1) {
      if (var1 == null) {
         return null;
      } else {
         VirtualZombieManager.instance.choices.clear();
         VirtualZombieManager.instance.choices.add(var1);
         IsoZombie var2 = VirtualZombieManager.instance.createRealZombieAlways(Rand.Next(8), false);
         if (var2 == null) {
            return null;
         } else {
            ZombieSpawnRecorder.instance.record(var2, this.getClass().getSimpleName());
            alignCorpseToSquare(var2, var1);
            var2.setFakeDead(false);
            var2.setHealth(0.0F);
            var2.upKillCount = false;
            var2.setSkeleton(true);
            var2.getHumanVisual().setSkinTextureIndex(Rand.Next(1, 3));
            return new IsoDeadBody(var2, true);
         }
      }
   }

   public boolean isTimeValid(boolean var1) {
      if (this.minimumDays == 0 && this.maximumDays == 0) {
         return true;
      } else {
         float var2 = (float)GameTime.getInstance().getWorldAgeHours() / 24.0F;
         var2 += (float)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30);
         if (this.minimumDays > 0 && var2 < (float)this.minimumDays) {
            return false;
         } else {
            return this.maximumDays <= 0 || !(var2 > (float)this.maximumDays);
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public String getDebugLine() {
      return this.debugLine;
   }

   public void setDebugLine(String var1) {
      this.debugLine = var1;
   }

   public int getMaximumDays() {
      return this.maximumDays;
   }

   public void setMaximumDays(int var1) {
      this.maximumDays = var1;
   }

   public boolean isUnique() {
      return this.unique;
   }

   public boolean isRat() {
      return this.isRat;
   }

   public void setUnique(boolean var1) {
      this.unique = var1;
   }

   public static IsoGridSquare getSq(int var0, int var1, int var2) {
      return IsoWorld.instance.getCell().getGridSquare(var0, var1, var2);
   }

   public IsoObject addTileObject(int var1, int var2, int var3, String var4) {
      return this.addTileObject(getSq(var1, var2, var3), var4, false);
   }

   public IsoObject addTileObject(int var1, int var2, int var3, String var4, boolean var5) {
      return this.addTileObject(getSq(var1, var2, var3), var4, var5);
   }

   public IsoObject addTileObject(IsoGridSquare var1, String var2) {
      return this.addTileObject(var1, var2, false);
   }

   public IsoObject addTileObject(IsoGridSquare var1, String var2, boolean var3) {
      if (var1 == null) {
         return null;
      } else {
         RandomizedZoneStoryBase.cleanSquareForStory(getSq(var1.x, var1.y, var1.z));
         if (var3) {
            var1.dirtStamp();
         }

         IsoObject var4 = IsoObject.getNew(var1, var2, (String)null, false);
         if (GameServer.bServer) {
            var1.transmitAddObjectToSquare(var4, -1);
         } else {
            var1.AddTileObject(var4);
         }

         MapObjects.newGridSquare(var1);
         MapObjects.loadGridSquare(var1);
         return var4;
      }
   }

   public IsoObject addTileObject(IsoGridSquare var1, IsoObject var2) {
      return this.addTileObject(var1, var2, false);
   }

   public IsoObject addTileObject(IsoGridSquare var1, IsoObject var2, boolean var3) {
      if (var1 == null) {
         return null;
      } else {
         RandomizedZoneStoryBase.cleanSquareForStory(getSq(var1.x, var1.y, var1.z));
         if (var3) {
            var1.dirtStamp();
         }

         if (GameServer.bServer) {
            var1.transmitAddObjectToSquare(var2, -1);
         } else {
            var1.AddTileObject(var2);
         }

         MapObjects.newGridSquare(var1);
         MapObjects.loadGridSquare(var1);
         return var2;
      }
   }

   public void addSleepingBagOrTentNorthSouth(int var1, int var2, int var3) {
      if (Rand.NextBool(2)) {
         this.addRandomTentNorthSouth(var1, var2, var3);
      } else {
         this.addSleepingBagNorthSouth(var1, var2 + 1, var3);
      }

   }

   public void addSleepingBagOrTentWestEast(int var1, int var2, int var3) {
      if (Rand.NextBool(2)) {
         this.addRandomTentWestEast(var1, var2, var3);
      } else {
         this.addSleepingBagWestEast(var1 + 1, var2, var3);
      }

   }

   public void addRandomTentNorthSouth(int var1, int var2, int var3) {
      if (Rand.NextBool(5)) {
         this.addTentNorthSouth(var1, var2 - 1, var3);
      } else {
         this.addTentNorthSouthNew(var1, var2, var3);
      }

   }

   public void addRandomTentWestEast(int var1, int var2, int var3) {
      if (Rand.NextBool(5)) {
         this.addTentWestEast(var1 - 1, var2, var3);
      } else {
         this.addTentWestEastNew(var1, var2, var3);
      }

   }

   public void addRandomShelterNorthSouth(int var1, int var2, int var3) {
      if (Rand.NextBool(2)) {
         this.addSleepingBagOrTentNorthSouth(var1, var2, var3);
      } else {
         this.addShelterWestEast(var1, var2, var3);
      }

   }

   public void addRandomShelterWestEast(int var1, int var2, int var3) {
      if (Rand.NextBool(2)) {
         this.addSleepingBagOrTentWestEast(var1, var2, var3);
      } else {
         this.addShelterNorthSouth(var1, var2, var3);
      }

   }

   public void addTentNorthSouth(int var1, int var2, int var3) {
      this.addTileObject(var1, var2 - 1, var3, "camping_01_1");
      this.addTileObject(var1, var2, var3, "camping_01_0");
   }

   public void addTentWestEast(int var1, int var2, int var3) {
      this.addTileObject(var1 - 1, var2, var3, "camping_01_2");
      this.addTileObject(var1, var2, var3, "camping_01_3");
   }

   public void addMattressNorthSouth(int var1, int var2, int var3) {
      this.addTileObject(var1, var2 - 1, var3, "carpentry_02_79", true);
      this.addTileObject(var1, var2, var3, "carpentry_02_78", true);
   }

   public void addMattressWestEast(int var1, int var2, int var3) {
      this.addTileObject(var1 - 1, var2, var3, "carpentry_02_76", true);
      this.addTileObject(var1, var2, var3, "carpentry_02_77", true);
   }

   public void addSleepingBagNorthSouth(int var1, int var2, int var3) {
      int var4 = Rand.Next(10) * 8 + 3;
      if (Rand.NextBool(2)) {
         var4 += 4;
      }

      this.addTileObject(var1, var2 - 1, var3, "camping_02_" + var4);
      this.addTileObject(var1, var2, var3, "camping_02_" + (var4 - 1));
   }

   public void addSleepingBagWestEast(int var1, int var2, int var3) {
      int var4 = Rand.Next(10) * 8;
      if (Rand.NextBool(2)) {
         var4 += 4;
      }

      this.addTileObject(var1 - 1, var2, var3, "camping_02_" + var4);
      this.addTileObject(var1, var2, var3, "camping_02_" + (var4 + 1));
   }

   public void addShelterNorthSouth(int var1, int var2, int var3) {
      int var4 = Rand.Next(3);
      switch (var4) {
         case 0:
            this.addTileObject(var1, var2 - 1, var3, "camping_03_5", true);
            this.addTileObject(var1, var2, var3, "camping_03_4", true);
            break;
         case 1:
            this.addTileObject(var1, var2 - 1, var3, "camping_03_13", true);
            this.addTileObject(var1, var2, var3, "camping_03_12", true);
            break;
         case 2:
            this.addTileObject(var1 - 1, var2, var3, "camping_03_26", true);
            this.addTileObject(var1, var2, var3, "camping_03_27", true);
      }

   }

   public void addShelterWestEast(int var1, int var2, int var3) {
      int var4 = Rand.Next(3);
      switch (var4) {
         case 0:
            this.addTileObject(var1 - 1, var2, var3, "camping_03_2", true);
            this.addTileObject(var1, var2, var3, "camping_03_3", true);
            break;
         case 1:
            this.addTileObject(var1 - 1, var2, var3, "camping_03_14", true);
            this.addTileObject(var1, var2, var3, "camping_03_15", true);
            break;
         case 2:
            this.addTileObject(var1, var2 - 1, var3, "camping_03_25", true);
            this.addTileObject(var1, var2, var3, "camping_03_24", true);
      }

   }

   public void addTentNorthSouthNew(int var1, int var2, int var3) {
      var2 -= 3;
      int var4 = Rand.Next(4) * 32;
      this.addTileObject(var1 - 1, var2 + 3, var3, "camping_04_" + (0 + var4));
      this.addTileObject(var1 - 1, var2 + 2, var3, "camping_04_" + (1 + var4));
      this.addTileObject(var1 - 1, var2 + 1, var3, "camping_04_" + (2 + var4));
      this.addTileObject(var1 - 1, var2, var3, "camping_04_" + (3 + var4));
      this.addTileObject(var1, var2 + 3, var3, "camping_04_" + (4 + var4));
      this.addTileObject(var1, var2 + 2, var3, "camping_04_" + (5 + var4));
      this.addTileObject(var1, var2 + 1, var3, "camping_04_" + (6 + var4));
      this.addTileObject(var1, var2, var3, "camping_04_" + (7 + var4));
   }

   public void addTentWestEastNew(int var1, int var2, int var3) {
      int var4 = Rand.Next(4) * 32;
      this.addTileObject(var1 - 3, var2 - 1, var3, "camping_04_" + (16 + var4));
      this.addTileObject(var1 - 2, var2 - 1, var3, "camping_04_" + (17 + var4));
      this.addTileObject(var1 - 1, var2 - 1, var3, "camping_04_" + (18 + var4));
      this.addTileObject(var1, var2 - 1, var3, "camping_04_" + (19 + var4));
      this.addTileObject(var1 - 3, var2, var3, "camping_04_" + (20 + var4));
      this.addTileObject(var1 - 2, var2, var3, "camping_04_" + (21 + var4));
      this.addTileObject(var1 - 1, var2, var3, "camping_04_" + (22 + var4));
      this.addTileObject(var1, var2, var3, "camping_04_" + (23 + var4));
   }

   public BaseVehicle addTrailer(BaseVehicle var1, Zone var2, IsoChunk var3, String var4, String var5, String var6) {
      IsoGridSquare var7 = var1.getSquare();
      IsoDirections var8 = var1.getDir();
      byte var9 = 0;
      byte var10 = 0;
      if (var8 == IsoDirections.S) {
         var10 = -3;
      }

      if (var8 == IsoDirections.N) {
         var10 = 3;
      }

      if (var8 == IsoDirections.W) {
         var9 = 3;
      }

      if (var8 == IsoDirections.E) {
         var9 = -3;
      }

      BaseVehicle var11 = this.addVehicle(var2, getSq(var7.x + var9, var7.y + var10, var7.z), var3, var4, var6, (Integer)null, var8, var5);
      if (var11 != null) {
         var1.positionTrailer(var11);
      }

      return var11;
   }

   public void addCampfire(IsoGridSquare var1) {
      var1.dirtStamp();
      this.addTileObject(var1, "camping_01_6", true);
   }

   public void addSimpleCookingPit(IsoGridSquare var1) {
      var1.dirtStamp();
      IsoFireplace var2 = new IsoFireplace(IsoWorld.instance.CurrentCell, var1, LuaManager.GlobalObject.getSprite("camping_03_16"));
      this.addTileObject(var1, (IsoObject)var2, true);
   }

   public void addCookingPit(IsoGridSquare var1) {
      var1.dirtStamp();
      IsoFireplace var2 = new IsoFireplace(IsoWorld.instance.CurrentCell, var1, LuaManager.GlobalObject.getSprite("camping_03_19"));
      this.addTileObject(var1, (IsoObject)var2, true);
   }

   public void addBrazier(IsoGridSquare var1) {
      var1.dirtStamp();
      IsoFireplace var2 = new IsoFireplace(IsoWorld.instance.CurrentCell, var1, LuaManager.GlobalObject.getSprite("crafted_02_42"));
      this.addTileObject(var1, (IsoObject)var2, true);
   }

   public void addSimpleFire(IsoGridSquare var1) {
      int var2 = Rand.Next(2);
      Object var3 = null;
      switch (var2) {
         case 0:
            this.addCampfire(var1);
            break;
         case 1:
            this.addSimpleCookingPit(var1);
      }

   }

   public void addRandomFirepit(IsoGridSquare var1) {
      int var2 = Rand.Next(3);
      Object var3 = null;
      switch (var2) {
         case 0:
            this.addCampfire(var1);
            break;
         case 1:
            this.addSimpleCookingPit(var1);
            break;
         case 2:
            this.addCookingPit(var1);
      }

   }

   public void addCampfireOrPit(IsoGridSquare var1) {
      int var2 = Rand.Next(2);
      Object var3 = null;
      switch (var2) {
         case 0:
            this.addCampfire(var1);
            break;
         case 1:
            this.addCookingPit(var1);
      }

   }

   public void dirtBomb(IsoGridSquare var1) {
      this.cleanSquareAndNeighbors(var1);
      var1.dirtStamp();
   }

   public void cleanSquareAndNeighbors(IsoGridSquare var1) {
      RandomizedZoneStoryBase.cleanSquareForStory(var1);
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.S));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.SE));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.E));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.NE));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.N));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.NW));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.W));
      RandomizedZoneStoryBase.cleanSquareForStory(var1.getAdjacentSquare(IsoDirections.SW));
   }

   public void addGrindstone(IsoGridSquare var1) {
      GameEntityScript var2 = ScriptManager.instance.getGameEntityScript("Base.Grindstone");
      if (var2 != null) {
         this.addWorkstationEntity(var1, var2, "crafted_01_122");
      }
   }

   public void addStoneAnvil(IsoGridSquare var1) {
      this.dirtBomb(var1);
      GameEntityScript var2 = ScriptManager.instance.getGameEntityScript("Base.Stone_Anvil");
      if (var2 != null) {
         this.addWorkstationEntity(var1, var2, "crafted_03_20");
      }
   }

   public void addCharcoalBurner(IsoGridSquare var1) {
      this.dirtBomb(var1);
      GameEntityScript var2 = null;
      String var3 = null;
      int var4 = Rand.Next(4);
      switch (var4) {
         case 0:
            var2 = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_DarkGreenBarrel");
            var3 = "crafted_02_50";
            break;
         case 1:
            var2 = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_LightGreenBarrel");
            var3 = "crafted_02_48";
            break;
         case 2:
            var2 = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_OrangeBarrel");
            var3 = "crafted_02_49";
            break;
         case 3:
            var2 = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_MetalDrum");
            var3 = "crafted_02_51";
      }

      if (var2 != null) {
         IsoBarbecue var5 = new IsoBarbecue(IsoWorld.instance.getCell(), var1, (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var3));
         GameEntityFactory.CreateIsoObjectEntity(var5, var2, true);
         var1.AddSpecialObject(var5);
         var5.transmitCompleteItemToClients();
      }
   }

   public void addWorkstationEntity(IsoGridSquare var1, GameEntityScript var2, String var3) {
      IsoThumpable var4 = new IsoThumpable(IsoWorld.instance.getCell(), var1, var3, false, (KahluaTable)null);
      this.addWorkstationEntity(var4, var1, var2, var3);
   }

   public void addWorkstationEntity(IsoThumpable var1, IsoGridSquare var2, GameEntityScript var3, String var4) {
      if (var3 != null) {
         var1.setHealth(var1.getMaxHealth());
         var1.setBreakSound("BreakObject");
         GameEntityFactory.CreateIsoObjectEntity(var1, var3, true);
         var2.AddSpecialObject(var1);
         var1.transmitCompleteItemToClients();
      }
   }

   public InventoryItem addItemToObjectSurface(String var1, IsoObject var2) {
      return var2.addItemToObjectSurface(var1);
   }

   public boolean isValidGraffSquare(IsoGridSquare var1, boolean var2, boolean var3) {
      if (var1 != null && var1.hasRoomDef()) {
         if (var1.getRoom().getRoomDef().isKidsRoom()) {
            return false;
         } else if (var2 && !var1.getProperties().Is(IsoFlagType.WallN)) {
            return false;
         } else if (!var2 && !var1.getProperties().Is(IsoFlagType.WallW)) {
            return false;
         } else if (var2 && !var1.getProperties().Is(IsoFlagType.WallNTrans)) {
            return false;
         } else if (!var2 && !var1.getProperties().Is(IsoFlagType.WallWTrans)) {
            return false;
         } else if (var2 && var1.Is(IsoFlagType.HoppableN)) {
            return false;
         } else if (!var2 && var1.Is(IsoFlagType.HoppableW)) {
            return false;
         } else if (var2 && var1.getProperties().Is(IsoFlagType.DoorWallN)) {
            return false;
         } else if (!var2 && var1.getProperties().Is(IsoFlagType.DoorWallW)) {
            return false;
         } else if (var2 && var1.getProperties().Is(IsoFlagType.WindowN)) {
            return false;
         } else if (!var2 && var1.getProperties().Is(IsoFlagType.WindowW)) {
            return false;
         } else if (var1.getObjects().size() <= 2 && !var1.isOutside()) {
            if (var1.getWall(var2) != null && var1.getDoor(var2) == null && var1.getWindow(var2) == null) {
               if (var3 && var1.getWall(!var2) != null) {
                  return false;
               } else if (var2 && var3) {
                  return this.isValidGraffSquare(var1.getAdjacentSquare(IsoDirections.W), true, false);
               } else {
                  return var3 ? this.isValidGraffSquare(var1.getAdjacentSquare(IsoDirections.S), false, false) : true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void graffSquare(IsoGridSquare var1, boolean var2) {
      this.graffSquare(var1, (String)null, var2);
   }

   public void graffSquare(IsoGridSquare var1, String var2, boolean var3) {
      ArrayList var4 = new ArrayList();
      var4.add("overlay_graffiti_wall_01_103");
      ArrayList var5 = new ArrayList();
      var5.add("overlay_graffiti_wall_01_10");
      var5.add("overlay_graffiti_wall_01_85");
      var5.add("overlay_graffiti_wall_01_86");
      if (var2 == null) {
         if (var3) {
            var2 = (String)var4.get(Rand.Next(var4.size()));
         } else {
            var2 = (String)var5.get(Rand.Next(var5.size()));
         }
      }

      if (var1 != null) {
         IsoSprite var6 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var2);
         if (Objects.equals(var2, "overlay_graffiti_wall_01_40") || Objects.equals(var2, "overlay_graffiti_wall_01_85")) {
            var1 = var1.getAdjacentSquare(IsoDirections.S);
         }

         if (var1 != null && var6.getProperties().Is(IsoFlagType.WallOverlay)) {
            IsoObject var7 = null;
            if (!var3 && var6.getProperties().Is(IsoFlagType.attachedW)) {
               var7 = var1.getWall(false);
            } else if (var3 && var6.getProperties().Is(IsoFlagType.attachedN)) {
               var7 = var1.getWall(true);
            }

            if (var7 != null) {
               if (var7.AttachedAnimSprite == null) {
                  var7.AttachedAnimSprite = new ArrayList(4);
               }

               var7.AttachedAnimSprite.add(IsoSpriteInstance.get(var6));
            }
         }

         if (var1 != null) {
            var1.RecalcAllWithNeighbours(true);
         }

      }
   }

   public void trashSquare(IsoGridSquare var1) {
      if (var1.getRoom() == null || var1.getRoom().getRoomDef() == null || !var1.getRoom().getRoomDef().isKidsRoom()) {
         int var2 = Rand.Next(51);
         if (var2 > 12) {
            var2 += 12;
         }

         String var3 = "trash_01_" + var2;
         IsoObject var4 = new IsoObject(IsoWorld.instance.CurrentCell, var1, var3);
         var1.getObjects().add(var4);
         var1.RecalcProperties();
         if (GameServer.bServer) {
            var4.transmitCompleteItemToClients();
         }

      }
   }

   public static String getBarnClutterItem() {
      if (barnClutter != null && barnClutter.size() >= 1) {
         return (String)barnClutter.get(Rand.Next(barnClutter.size()));
      } else {
         DebugLog.log("Error: clutter array barnClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getBarnClutter() {
      return barnClutter;
   }

   public String getBathroomSinkClutterItem() {
      if (bathroomSinkClutter != null && bathroomSinkClutter.size() >= 1) {
         return (String)bathroomSinkClutter.get(Rand.Next(bathroomSinkClutter.size()));
      } else {
         DebugLog.log("Error: clutter array bathroomSinkClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getBathroomSinkClutter() {
      return bathroomSinkClutter;
   }

   public String getBedClutterItem() {
      if (bedClutter != null && bedClutter.size() >= 1) {
         return (String)bedClutter.get(Rand.Next(bedClutter.size()));
      } else {
         DebugLog.log("Error: clutter array bedClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getBedClutter() {
      return bedClutter;
   }

   public String getCarpentryToolClutterItem() {
      if (carpentryToolClutter != null && carpentryToolClutter.size() >= 1) {
         return (String)carpentryToolClutter.get(Rand.Next(carpentryToolClutter.size()));
      } else {
         DebugLog.log("Error: clutter array carpentryToolClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getCarpentryToolClutter() {
      return carpentryToolClutter;
   }

   public String getBBQClutterItem() {
      if (bbqClutter != null && bbqClutter.size() >= 1) {
         return (String)bbqClutter.get(Rand.Next(bbqClutter.size()));
      } else {
         DebugLog.log("Error: clutter array bbqFloorClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getBBQClutter() {
      return bbqClutter;
   }

   public static String getCafeClutterItem() {
      if (cafeClutter != null && cafeClutter.size() >= 1) {
         return (String)cafeClutter.get(Rand.Next(cafeClutter.size()));
      } else {
         DebugLog.log("Error: clutter array cafeClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getCafeClutter() {
      return cafeClutter;
   }

   public static String getDeadEndClutterItem() {
      if (deadEndClutter != null && deadEndClutter.size() >= 1) {
         return (String)deadEndClutter.get(Rand.Next(deadEndClutter.size()));
      } else {
         DebugLog.log("Error: clutter array deadEndClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getDeadEndClutter() {
      return deadEndClutter;
   }

   public static String getDormClutterItem() {
      if (dormClutter != null && dormClutter.size() >= 1) {
         return (String)dormClutter.get(Rand.Next(dormClutter.size()));
      } else {
         DebugLog.log("Error: clutter array dormClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getDormClutter() {
      return dormClutter;
   }

   public static String getFarmStorageClutterItem() {
      if (farmStorageClutter != null && farmStorageClutter.size() >= 1) {
         return (String)farmStorageClutter.get(Rand.Next(farmStorageClutter.size()));
      } else {
         DebugLog.log("Error: clutter array farmStorageClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getFarmStorageClutter() {
      return farmStorageClutter;
   }

   public static String getFootballNightDrinkItem() {
      if (footballNightDrinks != null && footballNightDrinks.size() >= 1) {
         return (String)footballNightDrinks.get(Rand.Next(footballNightDrinks.size()));
      } else {
         DebugLog.log("Error: clutter array footballNightDrinks doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getFootballNightDrinks() {
      return footballNightDrinks;
   }

   public static String getFootballNightSnackItem() {
      if (footballNightSnacks != null && footballNightSnacks.size() >= 1) {
         return (String)footballNightSnacks.get(Rand.Next(footballNightSnacks.size()));
      } else {
         DebugLog.log("Error: clutter array footballSnacks doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getFootballNightSnacks() {
      return footballNightSnacks;
   }

   public static String getGarageStorageClutterItem() {
      if (garageStorageClutter != null && garageStorageClutter.size() >= 1) {
         return (String)garageStorageClutter.get(Rand.Next(garageStorageClutter.size()));
      } else {
         DebugLog.log("Error: clutter array garageStorageClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getGarageStorageClutter() {
      return garageStorageClutter;
   }

   public static String getGigamartClutterItem() {
      if (gigamartClutter != null && gigamartClutter.size() >= 1) {
         return (String)gigamartClutter.get(Rand.Next(gigamartClutter.size()));
      } else {
         DebugLog.log("Error: clutter array gigamartClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getGigamartClutter() {
      return gigamartClutter;
   }

   public static String getGroceryClutterItem() {
      if (groceryClutter != null && groceryClutter.size() >= 1) {
         return (String)groceryClutter.get(Rand.Next(groceryClutter.size()));
      } else {
         DebugLog.log("Error: clutter array groceryClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getGroceryClutter() {
      return groceryClutter;
   }

   public static String getHairSalonClutterItem() {
      if (hairSalonClutter != null && hairSalonClutter.size() >= 1) {
         return (String)hairSalonClutter.get(Rand.Next(hairSalonClutter.size()));
      } else {
         DebugLog.log("Error: clutter array hairSalonClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getHairSalonClutter() {
      return hairSalonClutter;
   }

   public static String getHallClutterItem() {
      if (hallClutter != null && hallClutter.size() >= 1) {
         return (String)hallClutter.get(Rand.Next(hallClutter.size()));
      } else {
         DebugLog.log("Error: clutter array hallClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getHallClutter() {
      return hallClutter;
   }

   public static String getHenDoDrinkItem() {
      if (henDoDrinks != null && henDoDrinks.size() >= 1) {
         return (String)henDoDrinks.get(Rand.Next(henDoDrinks.size()));
      } else {
         DebugLog.log("Error: clutter array henDoDrinks doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getHenDoDrinks() {
      return henDoDrinks;
   }

   public static String getHenDoSnackItem() {
      if (henDoSnacks != null && henDoSnacks.size() >= 1) {
         return (String)henDoSnacks.get(Rand.Next(henDoSnacks.size()));
      } else {
         DebugLog.log("Error: clutter array henDoSnacks doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getHenDoSnacks() {
      return henDoSnacks;
   }

   public String getHoedownClutterItem() {
      if (hoedownClutter != null && hoedownClutter.size() >= 1) {
         return (String)hoedownClutter.get(Rand.Next(hoedownClutter.size()));
      } else {
         DebugLog.log("Error: clutter array hoedownClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getHoedownClutter() {
      return hoedownClutter;
   }

   public String getHousePartyClutterItem() {
      if (housePartyClutter != null && housePartyClutter.size() >= 1) {
         return (String)housePartyClutter.get(Rand.Next(housePartyClutter.size()));
      } else {
         DebugLog.log("Error: clutter array housePartyClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getHousePartyClutter() {
      return housePartyClutter;
   }

   public static String getJudgeClutterItem() {
      if (judgeClutter != null && judgeClutter.size() >= 1) {
         return (String)judgeClutter.get(Rand.Next(judgeClutter.size()));
      } else {
         DebugLog.log("Error: clutter array judgeClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getJudgeClutter() {
      return judgeClutter;
   }

   public String getKidClutterItem() {
      if (kidClutter != null && kidClutter.size() >= 1) {
         return Rand.NextBool(100) ? "Base.SpiffoBig" : (String)kidClutter.get(Rand.Next(kidClutter.size()));
      } else {
         DebugLog.log("Error: clutter array kidClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getKidClutter() {
      return kidClutter;
   }

   public String getKitchenCounterClutterItem() {
      if (kitchenCounterClutter != null && kitchenCounterClutter.size() >= 1) {
         return (String)kitchenCounterClutter.get(Rand.Next(kitchenCounterClutter.size()));
      } else {
         DebugLog.log("Error: clutter array kitchenCounterClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getKitchenCounterClutter() {
      return kitchenCounterClutter;
   }

   public String getKitchenSinkClutterItem() {
      if (kitchenSinkClutter != null && kitchenSinkClutter.size() >= 1) {
         return (String)kitchenSinkClutter.get(Rand.Next(kitchenSinkClutter.size()));
      } else {
         DebugLog.log("Error: clutter array kitchenSinkClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getKitchenSinkClutter() {
      return kitchenSinkClutter;
   }

   public String getKitchenStoveClutterItem() {
      if (kitchenStoveClutter != null && kitchenStoveClutter.size() >= 1) {
         return (String)kitchenStoveClutter.get(Rand.Next(kitchenStoveClutter.size()));
      } else {
         DebugLog.log("Error: clutter array kitchenStoveClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getKitchenStoveClutter() {
      return kitchenStoveClutter;
   }

   public String getLaundryRoomClutterItem() {
      if (laundryRoomClutter != null && laundryRoomClutter.size() >= 1) {
         return (String)laundryRoomClutter.get(Rand.Next(laundryRoomClutter.size()));
      } else {
         DebugLog.log("Error: clutter array laundryRoomClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getLaundryRoomClutter() {
      return laundryRoomClutter;
   }

   public String getLivingroomClutterItem() {
      if (livingRoomClutter != null && livingRoomClutter.size() >= 1) {
         return (String)livingRoomClutter.get(Rand.Next(livingRoomClutter.size()));
      } else {
         DebugLog.log("Error: clutter array livingRoomClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getLivingroomClutter() {
      return livingRoomClutter;
   }

   public static String getMedicallutterItem() {
      if (medicalClutter != null && medicalClutter.size() >= 1) {
         return (String)medicalClutter.get(Rand.Next(medicalClutter.size()));
      } else {
         DebugLog.log("Error: clutter array medicalClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getMedicalClutter() {
      return medicalClutter;
   }

   public static String getOldShelterClutterItem() {
      if (oldShelterClutter != null && oldShelterClutter.size() >= 1) {
         return (String)oldShelterClutter.get(Rand.Next(oldShelterClutter.size()));
      } else {
         DebugLog.log("Error: clutter array officePaperworkClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getOldShelterClutter() {
      return oldShelterClutter;
   }

   public static String getOfficeCarDealerClutterItem() {
      if (officeCarDealerClutter != null && officeCarDealerClutter.size() >= 1) {
         return (String)officeCarDealerClutter.get(Rand.Next(officeCarDealerClutter.size()));
      } else {
         DebugLog.log("Error: clutter array officeCarDealerClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getOfficeCarDealerClutter() {
      return officeCarDealerClutter;
   }

   public static String getOfficePaperworkClutterItem() {
      if (officePaperworkClutter != null && officePaperworkClutter.size() >= 1) {
         return (String)officePaperworkClutter.get(Rand.Next(officePaperworkClutter.size()));
      } else {
         DebugLog.log("Error: clutter array officePaperworkClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getOfficePaperworkClutter() {
      return officePaperworkClutter;
   }

   public static String getOfficePenClutterItem() {
      if (officePenClutter != null && officePenClutter.size() >= 1) {
         return (String)officePenClutter.get(Rand.Next(officePenClutter.size()));
      } else {
         DebugLog.log("Error: clutter array officePenClutter. doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getOfficePenClutter() {
      return officePenClutter;
   }

   public static String getOfficeOtherClutterItem() {
      if (officeOtherClutter != null && officeOtherClutter.size() >= 1) {
         return (String)officeOtherClutter.get(Rand.Next(officeOtherClutter.size()));
      } else {
         DebugLog.log("Error: clutter array officeOtherClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getOfficeOtherClutter() {
      return officeOtherClutter;
   }

   public static String getOfficeTreatClutterItem() {
      if (officeTreatClutter != null && officeTreatClutter.size() >= 1) {
         return (String)officeTreatClutter.get(Rand.Next(officeTreatClutter.size()));
      } else {
         DebugLog.log("Error: clutter array officeTreatClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getOfficeTreatClutter() {
      return officeTreatClutter;
   }

   public String getOvenFoodClutterItem() {
      if (ovenFoodClutter != null && ovenFoodClutter.size() >= 1) {
         return (String)ovenFoodClutter.get(Rand.Next(ovenFoodClutter.size()));
      } else {
         DebugLog.log("Error: clutter array ovenFood doesn't exist - will use jave-defined coldFood instead");
         return null;
      }
   }

   public ArrayList<String> getOvenFoodClutter() {
      return ovenFoodClutter;
   }

   public String getPillowClutterItem() {
      if (pillowClutter != null && pillowClutter.size() >= 1) {
         return (String)pillowClutter.get(Rand.Next(pillowClutter.size()));
      } else {
         DebugLog.log("Error: clutter array pillowClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getPillowClutter() {
      return pillowClutter;
   }

   public String getPokerNightClutterItem() {
      if (pokerNightClutter != null && pokerNightClutter.size() >= 1) {
         return (String)pokerNightClutter.get(Rand.Next(pokerNightClutter.size()));
      } else {
         DebugLog.log("Error: clutter array pokerNightClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getPokerNightClutter() {
      return pokerNightClutter;
   }

   public String getRichJerkClutterItem() {
      if (richJerkClutter != null && richJerkClutter.size() >= 1) {
         return (String)richJerkClutter.get(Rand.Next(richJerkClutter.size()));
      } else {
         DebugLog.log("Error: clutter array richJerkClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getRichJerkClutter() {
      return richJerkClutter;
   }

   public String getSadCampsiteClutterItem() {
      if (sadCampsiteClutter != null && sadCampsiteClutter.size() >= 1) {
         return (String)sadCampsiteClutter.get(Rand.Next(sadCampsiteClutter.size()));
      } else {
         DebugLog.log("Error: clutter array sadCampsiteClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getSadCampsiteClutter() {
      return sadCampsiteClutter;
   }

   public String getSidetableClutterItem() {
      if (sidetableClutter != null && sidetableClutter.size() >= 1) {
         return Rand.NextBool(10) ? this.getWatchClutterItem() : (String)sidetableClutter.get(Rand.Next(sidetableClutter.size()));
      } else {
         DebugLog.log("Error: clutter array sidetableClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getSidetableClutter() {
      return sidetableClutter;
   }

   public String getSurvivalistCampsiteClutterItem() {
      if (survivalistCampsiteClutter != null && survivalistCampsiteClutter.size() >= 1) {
         return (String)survivalistCampsiteClutter.get(Rand.Next(survivalistCampsiteClutter.size()));
      } else {
         DebugLog.log("Error: clutter array survivalistCampsiteClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getSurvivalistCampsiteClutter() {
      return survivalistCampsiteClutter;
   }

   public static String getTwiggyClutterItem() {
      if (twiggyClutter != null && twiggyClutter.size() >= 1) {
         return (String)twiggyClutter.get(Rand.Next(twiggyClutter.size()));
      } else {
         DebugLog.log("Error: clutter array woodcraftClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getTwiggyClutter() {
      return twiggyClutter;
   }

   public String getUtilityToolClutterItem() {
      if (utilityToolClutter != null && utilityToolClutter.size() >= 1) {
         return (String)utilityToolClutter.get(Rand.Next(utilityToolClutter.size()));
      } else {
         DebugLog.log("Error: clutter array utilityToolClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getUtilityToolClutter() {
      return utilityToolClutter;
   }

   public String getWatchClutterItem() {
      if (watchClutter != null && watchClutter.size() >= 1) {
         return (String)watchClutter.get(Rand.Next(watchClutter.size()));
      } else {
         DebugLog.log("Error: clutter array watchClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getWatchClutter() {
      return watchClutter;
   }

   public static String getWoodcraftClutterItem() {
      if (woodcraftClutter != null && woodcraftClutter.size() >= 1) {
         return (String)woodcraftClutter.get(Rand.Next(woodcraftClutter.size()));
      } else {
         DebugLog.log("Error: clutter array woodcraftClutter doesn't exist");
         return null;
      }
   }

   public ArrayList<String> getWoodcraftClutter() {
      return woodcraftClutter;
   }

   public static String getClutterItem(ArrayList<String> var0) {
      if (var0 != null && var0.size() >= 1) {
         return (String)var0.get(Rand.Next(var0.size()));
      } else {
         DebugLog.log("Error: clutterArray doesn't exist");
         return null;
      }
   }

   public HashMap<Integer, String> getClutterCopy(ArrayList<String> var1) {
      HashMap var2 = new HashMap();

      for(int var3 = 0; var3 < var1.size(); ++var3) {
         if (var1.get(var3) != null) {
            String var4 = (String)var1.get(var3);
            var2.put(var3 + 1, var4);
         }
      }

      return var2;
   }
}
