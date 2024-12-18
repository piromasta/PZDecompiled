package zombie.randomizedWorld.randomizedBuilding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBandPractice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBanditRaid;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBathroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBedroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBleach;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSCorpsePsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDeadDrunk;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDevouredByRats;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSFootballNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGrouchos;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunmanInBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunslinger;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHenDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHockeyPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHouseParty;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPokerNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPoliceAtHouse;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscape;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscapeWithPolice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRPGNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatInfested;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatKing;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSResourceGarage;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSkeletonPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSpecificProfession;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStagDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStudentNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSuicidePact;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSTinFoilHat;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombieLockedBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombiesEating;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class RBBasic extends RandomizedBuildingBase {
   private final ArrayList<String> specificProfessionDistribution = new ArrayList();
   private final Map<String, String> specificProfessionRoomDistribution = new HashMap();
   private final Map<String, String> plankStash = new HashMap();
   private final ArrayList<RandomizedDeadSurvivorBase> deadSurvivorsStory = new ArrayList();
   private int totalChanceRDS = 0;
   private static final HashMap<RandomizedDeadSurvivorBase, Integer> rdsMap = new HashMap();
   private static final ArrayList<String> uniqueRDSSpawned = new ArrayList();
   private ArrayList<IsoObject> tablesDone = new ArrayList();
   private boolean doneTable = false;
   private static final HashMap<Integer, String> kitchenSinkItemsNew = new HashMap();
   private static final HashMap<Integer, String> kitchenCounterItemsNew = new HashMap();
   private static final HashMap<Integer, String> kitchenStoveItemsNew = new HashMap();
   private static final HashMap<Integer, String> bathroomSinkItemsNew = new HashMap();

   public void randomizeBuilding(BuildingDef var1) {
      this.tablesDone = new ArrayList();
      new ArrayList();
      IsoCell var3 = IsoWorld.instance.CurrentCell;
      boolean var4 = Rand.NextBool(100);
      RoomDef var5;
      int var8;
      if ((this.getRoom(var1, "kitchen") == null || this.getRoom(var1, "hall") == null) && Rand.NextBool(20)) {
         var5 = null;
         IsoGridSquare var6 = null;
         if (Rand.NextBool(2)) {
            var5 = this.getRoom(var1, "hall");
         }

         if (var5 == null) {
            var5 = this.getRoom(var1, "kitchen");
         }

         if (var5 != null) {
            var6 = var5.getExtraFreeSquare();
         }

         if (var6 != null) {
            this.addItemOnGround(var6, "Base.WaterDish");
            if (Rand.NextBool(3)) {
               var6 = var5.getExtraFreeSquare();
               if (var6 != null) {
                  String var7 = "Base.CatToy";
                  var8 = Rand.Next(6);
                  switch (var8) {
                     case 0:
                        var7 = "Base.CatFoodBag";
                        break;
                     case 1:
                        var7 = "Base.CatTreats";
                        break;
                     case 2:
                        var7 = "Base.DogChew";
                        break;
                     case 3:
                        var7 = "Base.DogFoodBag";
                        break;
                     case 4:
                        var7 = "Base.Leash";
                        break;
                     case 5:
                        var7 = "Base.DogTag_Pet";
                  }

                  this.addItemOnGround(var6, var7);
               }
            }
         }
      }

      var5 = null;
      boolean var15 = false;

      for(int var16 = var1.x - 1; var16 < var1.x2 + 1; ++var16) {
         for(var8 = var1.y - 1; var8 < var1.y2 + 1; ++var8) {
            for(int var9 = 0; var9 < 8; ++var9) {
               IsoGridSquare var10 = var3.getGridSquare(var16, var8, var9);
               if (var10 != null) {
                  if (var4 && var10.getFloor() != null && this.plankStash.containsKey(var10.getFloor().getSprite().getName())) {
                     IsoThumpable var11 = new IsoThumpable(var10.getCell(), var10, (String)this.plankStash.get(var10.getFloor().getSprite().getName()), false, (KahluaTable)null);
                     var11.setIsThumpable(false);
                     var11.container = new ItemContainer("plankstash", var10, var11);
                     var10.AddSpecialObject(var11);
                     var10.RecalcAllWithNeighbours(true);
                     var4 = false;
                  }

                  for(int var17 = 0; var17 < var10.getObjects().size(); ++var17) {
                     IsoObject var12 = (IsoObject)var10.getObjects().get(var17);
                     if (Rand.Next(100) <= 65 && var12 instanceof IsoDoor && !((IsoDoor)var12).isExterior()) {
                        ((IsoDoor)var12).ToggleDoorSilent();
                        ((IsoDoor)var12).syncIsoObject(true, (byte)1, (UdpConnection)null, (ByteBuffer)null);
                     }

                     if (var12 instanceof IsoWindow) {
                        IsoWindow var13 = (IsoWindow)var12;
                        if (Rand.NextBool(80)) {
                           var1.bAlarmed = false;
                           var13.ToggleWindow((IsoGameCharacter)null);
                        }

                        IsoCurtain var14 = var13.HasCurtains();
                        if (var14 != null && Rand.NextBool(15)) {
                           var14.ToggleDoorSilent();
                        }
                     }

                     if (SandboxOptions.instance.SurvivorHouseChance.getValue() != 1) {
                        if (Rand.Next(100) < 15 && var12.getContainer() != null && var12.getContainer().getType().equals("stove")) {
                           String var18 = this.getOvenFoodClutterItem();
                           if (var18 != null) {
                              InventoryItem var19 = var12.getContainer().AddItem(var18);
                              var19.setCooked(true);
                              var19.setAutoAge();
                           }
                        }

                        if (!this.tablesDone.contains(var12) && var12.getProperties().isTable() && var12.getContainer() == null && !this.doneTable) {
                           this.checkForTableSpawn(var1, var12);
                        }
                     }
                  }

                  if (SandboxOptions.instance.SurvivorHouseChance.getValue() != 1 && var10.getRoom() != null) {
                     if (var5 == null && var10.getRoom().getRoomDef() != null) {
                        var5 = var10.getRoom().getRoomDef();
                     } else if (var10.getRoom().getRoomDef() != null && var5 != var10.getRoom().getRoomDef()) {
                        var5 = var10.getRoom().getRoomDef();
                        if ("kidsbedroom".equals(var10.getRoom().getName())) {
                           var15 = true;
                        } else if ("bedroom".equals(var10.getRoom().getName())) {
                           var15 = var5.isKidsRoom();
                        } else {
                           var15 = false;
                        }
                     }

                     if (var15) {
                        this.doKidsBedroomStuff(var10);
                     } else if ("kitchen".equals(var10.getRoom().getName())) {
                        this.doKitchenStuff(var10);
                     } else if ("barcountertwiggy".equals(var10.getRoom().getName())) {
                        doTwiggyStuff(var10);
                     } else if ("bathroom".equals(var10.getRoom().getName())) {
                        this.doBathroomStuff(var10);
                     } else if ("bedroom".equals(var10.getRoom().getName())) {
                        this.doBedroomStuff(var10);
                     } else if ("cafe".equals(var10.getRoom().getName())) {
                        doCafeStuff(var10);
                     } else if ("gigamart".equals(var10.getRoom().getName())) {
                        doGroceryStuff(var10);
                     } else if ("grocery".equals(var10.getRoom().getName())) {
                        doGroceryStuff(var10);
                     } else if ("livingroom".equals(var10.getRoom().getName())) {
                        this.doLivingRoomStuff(var10);
                     } else if ("office".equals(var10.getRoom().getName())) {
                        doOfficeStuff(var10);
                     } else if ("jackiejayestudio".equals(var10.getRoom().getName())) {
                        doOfficeStuff(var10);
                     } else if ("judgematthassset".equals(var10.getRoom().getName())) {
                        doJudgeStuff(var10);
                     } else if ("mayorwestpointoffice".equals(var10.getRoom().getName())) {
                        doOfficeStuff(var10);
                     } else if ("nolansoffice".equals(var10.getRoom().getName())) {
                        doNolansOfficeStuff(var10);
                     } else if ("woodcraftset".equals(var10.getRoom().getName())) {
                        doWoodcraftStuff(var10);
                     } else if ("laundry".equals(var10.getRoom().getName())) {
                        this.doLaundryStuff(var10);
                     } else if ("hall".equals(var10.getRoom().getName())) {
                        doGeneralRoom(var10, this.getHallClutter());
                     } else if ("garageStorage".equals(var10.getRoom().getName()) || "garage".equals(var10.getRoom().getName())) {
                        doGeneralRoom(var10, this.getGarageStorageClutter());
                     }
                  }
               }
            }
         }
      }

      if (Rand.Next(100) < 25) {
         this.addRandomDeadSurvivorStory(var1);
         var1.setAllExplored(true);
         var1.bAlarmed = false;
      }

      this.doneTable = false;
   }

   public void forceVehicleDistribution(BaseVehicle var1, String var2) {
      ItemPickerJava.VehicleDistribution var3 = (ItemPickerJava.VehicleDistribution)ItemPickerJava.VehicleDistributions.get(var2);
      ItemPickerJava.ItemPickerRoom var4 = var3.Normal;

      for(int var5 = 0; var5 < var1.getPartCount(); ++var5) {
         VehiclePart var6 = var1.getPartByIndex(var5);
         if (var6.getItemContainer() != null) {
            if (GameServer.bServer && GameServer.bSoftReset) {
               var6.getItemContainer().setExplored(false);
            }

            if (!var6.getItemContainer().bExplored) {
               var6.getItemContainer().clear();
               this.randomizeContainer(var6, var4);
               var6.getItemContainer().setExplored(true);
            }
         }
      }

   }

   private void randomizeContainer(VehiclePart var1, ItemPickerJava.ItemPickerRoom var2) {
      if (!GameClient.bClient) {
         if (var2 != null) {
            ItemPickerJava.fillContainerType(var2, var1.getItemContainer(), "", (IsoGameCharacter)null);
         }
      }
   }

   private void doLivingRoomStuff(IsoGridSquare var1) {
      this.doLivingRoomStuff(var1, this.getLivingroomClutter());
   }

   private void doLivingRoomStuff(IsoGridSquare var1, ArrayList<String> var2) {
      if (var2 == null) {
         var2 = this.getLivingroomClutter();
      }

      IsoObject var3 = null;
      boolean var4 = false;

      for(int var5 = 0; var5 < var1.getObjects().size(); ++var5) {
         IsoObject var6 = (IsoObject)var1.getObjects().get(var5);
         if (var6 instanceof IsoRadio || var6 instanceof IsoTelevision) {
            var4 = true;
            break;
         }

         boolean var7 = var6.getProperties().Val("BedType") == null && var6.getSurfaceOffsetNoTable() > 0.0F && var6.getSurfaceOffsetNoTable() < 30.0F;
         if (var7 && Rand.NextBool(5)) {
            var3 = var6;
         }
      }

      if (!var4 && var3 != null) {
         String var8 = getClutterItem(var2);
         if (var8 != null) {
            var3.addItemToObjectSurface(var8, true);
         }
      }

   }

   private void doBedroomStuff(IsoGridSquare var1) {
      for(int var2 = 0; var2 < var1.getObjects().size(); ++var2) {
         IsoObject var3 = (IsoObject)var1.getObjects().get(var2);
         if (var3.getSprite() != null && var3.getSprite().getName() != null) {
            boolean var4 = var3.getSprite().getName().contains("bedding") && var3.getProperties().Val("BedType") != null;
            boolean var5 = var3.getContainer() != null && "sidetable".equals(var3.getContainer().getType());
            boolean var6 = false;
            IsoDirections var7 = this.getFacing(var3.getSprite());
            IsoSpriteGrid var8 = var3.getSprite().getSpriteGrid();
            if (var4 && var7 != null && var8 != null) {
               int var9 = var8.getSpriteGridPosX(var3.getSprite());
               int var10 = var8.getSpriteGridPosY(var3.getSprite());
               if (var7 == IsoDirections.E && var9 == 0 && (var10 == 0 || var10 == 1)) {
                  var6 = true;
               }

               if (var7 == IsoDirections.W && var9 == 1 && (var10 == 0 || var10 == 1)) {
                  var6 = true;
               }

               if (var7 == IsoDirections.N && (var9 == 0 || var9 == 1) && var10 == 1) {
                  var6 = true;
               }

               if (var7 == IsoDirections.S && (var9 == 0 || var9 == 1) && var10 == 0) {
                  var6 = true;
               }
            }

            byte var11 = 7;
            if (var6) {
               var11 = 3;
            }

            String var12;
            if (var4 && Rand.NextBool(var11)) {
               if (!var6) {
                  var12 = this.getBedClutterItem();
                  var3.addItemToObjectSurface(var12, true);
                  return;
               }

               var12 = "Base.Pillow";
               if (Rand.NextBool(100)) {
                  var12 = this.getPillowClutterItem();
               }

               if (var7 == IsoDirections.E) {
                  this.addWorldItem(var12, var1, 0.42F, Rand.Next(0.34F, 0.74F), var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(85, 95));
                  return;
               }

               if (var7 == IsoDirections.W) {
                  this.addWorldItem(var12, var1, 0.64F, Rand.Next(0.34F, 0.74F), var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(265, 275));
                  return;
               }

               if (var7 == IsoDirections.N) {
                  this.addWorldItem(var12, var1, Rand.Next(0.44F, 0.64F), 0.67F, var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(355, 365));
                  return;
               }

               if (var7 == IsoDirections.S) {
                  this.addWorldItem(var12, var1, Rand.Next(0.44F, 0.64F), 0.42F, var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(175, 185));
                  return;
               }
            } else if (var5 && Rand.NextBool(7)) {
               var12 = this.getSidetableClutterItem();
               if (var12 != null && var7 != null) {
                  var3.addItemToObjectSurface(var12, true);
                  return;
               }
            }
         }
      }

   }

   private void doKidsBedroomStuff(IsoGridSquare var1) {
      for(int var2 = 0; var2 < var1.getObjects().size(); ++var2) {
         IsoObject var3 = (IsoObject)var1.getObjects().get(var2);
         if (var3.getSprite() != null && var3.getSprite().getName() != null) {
            boolean var4 = var3.getSprite().getName().contains("bedding") && var3.getProperties().Val("BedType") != null;
            boolean var5 = var3.getContainer() != null && "sidetable".equals(var3.getContainer().getType());
            boolean var6 = false;
            IsoDirections var7 = this.getFacing(var3.getSprite());
            IsoSpriteGrid var8 = var3.getSprite().getSpriteGrid();
            if (var4 && var7 != null && var8 != null) {
               int var9 = var8.getSpriteGridPosX(var3.getSprite());
               int var10 = var8.getSpriteGridPosY(var3.getSprite());
               if (var7 == IsoDirections.E && var9 == 0 && (var10 == 0 || var10 == 1)) {
                  var6 = true;
               }

               if (var7 == IsoDirections.W && var9 == 1 && (var10 == 0 || var10 == 1)) {
                  var6 = true;
               }

               if (var7 == IsoDirections.N && (var9 == 0 || var9 == 1) && var10 == 1) {
                  var6 = true;
               }

               if (var7 == IsoDirections.S && (var9 == 0 || var9 == 1) && var10 == 0) {
                  var6 = true;
               }
            }

            byte var11 = 7;
            if (var6) {
               var11 = 3;
            }

            String var12;
            if (var4 && Rand.NextBool(var11)) {
               if (!var6) {
                  var12 = this.getBedClutterItem();
                  if (Rand.NextBool(3)) {
                     var12 = this.getKidClutterItem();
                  }

                  var3.addItemToObjectSurface(var12, true);
                  return;
               }

               var12 = "Base.Pillow";
               if (Rand.NextBool(20)) {
                  var12 = this.getKidClutterItem();
               }

               if (var7 == IsoDirections.E) {
                  this.addWorldItem(var12, var1, 0.42F, Rand.Next(0.34F, 0.74F), var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(85, 95));
                  return;
               }

               if (var7 == IsoDirections.W) {
                  this.addWorldItem(var12, var1, 0.64F, Rand.Next(0.34F, 0.74F), var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(265, 275));
                  return;
               }

               if (var7 == IsoDirections.N) {
                  this.addWorldItem(var12, var1, Rand.Next(0.44F, 0.64F), 0.67F, var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(355, 365));
                  return;
               }

               if (var7 == IsoDirections.S) {
                  this.addWorldItem(var12, var1, Rand.Next(0.44F, 0.64F), 0.42F, var3.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(175, 185));
                  return;
               }
            } else if (var5 && Rand.NextBool(7)) {
               var12 = this.getKidClutterItem();
               if (var12 != null && var7 != null) {
                  var3.addItemToObjectSurface(var12, true);
                  return;
               }
            }
         }
      }

   }

   private void doKitchenStuff(IsoGridSquare var1) {
      ArrayList var2 = this.getKitchenSinkClutter();
      if (Rand.NextBool(100)) {
         var2.add("Base.PotScrubberFrog");
      }

      this.doKitchenStuff(var1, this.getClutterCopy(this.getKitchenCounterClutter()), this.getClutterCopy(var2), this.getClutterCopy(this.getKitchenStoveClutter()));
   }

   private void doKitchenStuff(IsoGridSquare var1, HashMap<Integer, String> var2, HashMap<Integer, String> var3, HashMap<Integer, String> var4) {
      if (var2 == null) {
         var2 = this.getClutterCopy(this.getKitchenCounterClutter());
      }

      if (var3 == null) {
         var3 = this.getClutterCopy(this.getKitchenSinkClutter());
      }

      if (var4 == null) {
         var4 = this.getClutterCopy(this.getKitchenStoveClutter());
      }

      boolean var5 = false;
      boolean var6 = false;

      for(int var7 = 0; var7 < var1.getObjects().size(); ++var7) {
         IsoObject var8 = (IsoObject)var1.getObjects().get(var7);
         if (var8.getSprite() == null || var8.getSprite().getName() == null) {
            return;
         }

         IsoDirections var9;
         if (!var5 && var8.getSprite().getName().contains("sink") && Rand.NextBool(4)) {
            var9 = this.getFacing(var8.getSprite());
            if (var9 != null) {
               if (Rand.NextBool(100)) {
                  this.generateSinkClutter(var9, var8, var1, var3);
               }

               var5 = true;
            }
         } else if (!var6 && var8.getContainer() != null && "counter".equals(var8.getContainer().getType()) && Rand.NextBool(6)) {
            boolean var12 = true;

            for(int var10 = 0; var10 < var1.getObjects().size(); ++var10) {
               IsoObject var11 = (IsoObject)var1.getObjects().get(var10);
               if (var11.getSprite() != null && var11.getSprite().getName() != null && var11.getSprite().getName().contains("sink") || var11 instanceof IsoStove || var11 instanceof IsoRadio) {
                  var12 = false;
                  break;
               }
            }

            if (var12) {
               IsoDirections var13 = this.getFacing(var8.getSprite());
               if (var13 != null) {
                  this.generateCounterClutter(var13, var8, var1, var2);
                  var6 = true;
               }
            }
         } else if (var8 instanceof IsoStove && var8.getContainer() != null && "stove".equals(var8.getContainer().getType()) && Rand.NextBool(4)) {
            var9 = this.getFacing(var8.getSprite());
            if (var9 != null) {
               this.generateKitchenStoveClutter(var9, var8, var1, var4);
            }
         }
      }

   }

   private void doBathroomStuff(IsoGridSquare var1) {
      this.doBathroomStuff(var1, this.getClutterCopy(this.getBathroomSinkClutter()));
   }

   private void doBathroomStuff(IsoGridSquare var1, HashMap<Integer, String> var2) {
      if (var2 == null) {
         var2 = this.getClutterCopy(this.getBathroomSinkClutter());
      }

      boolean var3 = false;
      boolean var4 = false;
      boolean var5 = false;

      for(int var6 = 0; var6 < var1.getObjects().size(); ++var6) {
         IsoObject var7 = (IsoObject)var1.getObjects().get(var6);
         if (var7.getSprite() == null || var7.getSprite().getName() == null) {
            return;
         }

         IsoDirections var8;
         if (!var5 && var7.getSprite().getProperties().Is("CustomName") && var7.getSprite().getProperties().Val("CustomName").contains("Toilet") && Rand.NextBool(5)) {
            var8 = this.getFacing(var7.getSprite());
            String var9 = "Base.Plunger";
            if (Rand.NextBool(2)) {
               var9 = "Base.ToiletBrush";
            }

            if (Rand.NextBool(10)) {
               var9 = "Base.ToiletPaper";
            }

            if (var8 != null) {
               var5 = true;
               if (var8 == IsoDirections.E) {
                  ItemSpawner.spawnItem(var9, var1, 0.16F, 0.84F, 0.0F);
               }

               float var13;
               if (var8 == IsoDirections.W) {
                  var13 = 0.16F;
                  if (Rand.NextBool(2)) {
                     var13 = 0.84F;
                  }

                  ItemSpawner.spawnItem(var9, var1, 0.84F, var13, 0.0F);
               }

               if (var8 == IsoDirections.N) {
                  var13 = 0.16F;
                  if (Rand.NextBool(2)) {
                     var13 = 0.84F;
                  }

                  ItemSpawner.spawnItem(var9, var1, var13, 0.84F, 0.0F);
               }

               if (var8 == IsoDirections.S) {
                  ItemSpawner.spawnItem(var9, var1, 0.84F, 0.16F, 0.0F);
               }
               break;
            }
         }

         if (!var3 && !var4 && var7.getSprite().getName().contains("sink") && Rand.NextBool(5) && var7.getSurfaceOffsetNoTable() > 0.0F) {
            var8 = this.getFacing(var7.getSprite());
            if (var8 != null) {
               this.generateSinkClutter(var8, var7, var1, var2);
               var3 = true;
            }
         } else if (!var3 && !var4 && var7.getContainer() != null && "counter".equals(var7.getContainer().getType()) && Rand.NextBool(5)) {
            boolean var11 = true;

            for(int var12 = 0; var12 < var1.getObjects().size(); ++var12) {
               IsoObject var10 = (IsoObject)var1.getObjects().get(var12);
               if (var10.getSprite() != null && var10.getSprite().getName() != null && var10.getSprite().getName().contains("sink") || var10 instanceof IsoStove || var10 instanceof IsoRadio) {
                  var11 = false;
                  break;
               }
            }

            if (var11) {
               IsoDirections var14 = this.getFacing(var7.getSprite());
               if (var14 != null) {
                  this.generateCounterClutter(var14, var7, var1, var2);
                  var4 = true;
               }
            }
         }
      }

   }

   private void generateKitchenStoveClutter(IsoDirections var1, IsoObject var2, IsoGridSquare var3) {
      this.generateKitchenStoveClutter(var1, var2, var3, this.getClutterCopy(this.getKitchenStoveClutter()));
   }

   private void generateKitchenStoveClutter(IsoDirections var1, IsoObject var2, IsoGridSquare var3, HashMap<Integer, String> var4) {
      if (var4 == null) {
         var4 = this.getClutterCopy(this.getKitchenStoveClutter());
      }

      int var5 = Rand.Next(1, 3);
      String var6 = (String)var4.get(Rand.Next(1, var4.size()));
      if (var1 == IsoDirections.W) {
         switch (var5) {
            case 1:
               this.addWorldItem(var6, var3, 0.5703125F, 0.8046875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               break;
            case 2:
               this.addWorldItem(var6, var3, 0.5703125F, 0.2578125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
         }
      }

      if (var1 == IsoDirections.E) {
         switch (var5) {
            case 1:
               this.addWorldItem(var6, var3, 0.5F, 0.7890625F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               break;
            case 2:
               this.addWorldItem(var6, var3, 0.5F, 0.1875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
         }
      }

      if (var1 == IsoDirections.S) {
         switch (var5) {
            case 1:
               this.addWorldItem(var6, var3, 0.3125F, 0.53125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               break;
            case 2:
               this.addWorldItem(var6, var3, 0.875F, 0.53125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
         }
      }

      if (var1 == IsoDirections.N) {
         switch (var5) {
            case 1:
               this.addWorldItem(var6, var3, 0.3203F, 0.523475F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               break;
            case 2:
               this.addWorldItem(var6, var3, 0.8907F, 0.523475F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
         }
      }

   }

   private void generateCounterClutter(IsoDirections var1, IsoObject var2, IsoGridSquare var3, HashMap<Integer, String> var4) {
      int var5 = Math.min(5, var4.size() + 1);
      int var6 = Rand.Next(1, var5);
      ArrayList var7 = new ArrayList();

      int var9;
      for(int var8 = 0; var8 < var6; ++var8) {
         var9 = Rand.Next(1, 5);
         boolean var10 = false;

         while(!var10) {
            if (!var7.contains(var9)) {
               var7.add(var9);
               var10 = true;
            } else {
               var9 = Rand.Next(1, 5);
            }
         }

         if (var7.size() == 4) {
         }
      }

      ArrayList var13 = new ArrayList();

      for(var9 = 0; var9 < var7.size(); ++var9) {
         int var14 = (Integer)var7.get(var9);
         int var11 = Rand.Next(1, var4.size() + 1);
         String var12 = null;

         while(var12 == null) {
            var12 = (String)var4.get(var11);
            if (var13.contains(var12)) {
               var12 = null;
               var11 = Rand.Next(1, var4.size() + 1);
            }
         }

         if (var12 != null) {
            var13.add(var12);
            if (var1 == IsoDirections.S) {
               switch (var14) {
                  case 1:
                     this.addWorldItem(var12, var3, 0.138F, Rand.Next(0.2F, 0.523F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     this.addWorldItem(var12, var3, 0.383F, Rand.Next(0.2F, 0.523F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     this.addWorldItem(var12, var3, 0.633F, Rand.Next(0.2F, 0.523F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     this.addWorldItem(var12, var3, 0.78F, Rand.Next(0.2F, 0.523F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }

            if (var1 == IsoDirections.N) {
               switch (var14) {
                  case 1:
                     ItemSpawner.spawnItem(var12, var3, 0.133F, Rand.Next(0.53125F, 0.9375F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     ItemSpawner.spawnItem(var12, var3, 0.38F, Rand.Next(0.53125F, 0.9375F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     ItemSpawner.spawnItem(var12, var3, 0.625F, Rand.Next(0.53125F, 0.9375F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     ItemSpawner.spawnItem(var12, var3, 0.92F, Rand.Next(0.53125F, 0.9375F), var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }

            if (var1 == IsoDirections.E) {
               switch (var14) {
                  case 1:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.226F, 0.593F), 0.14F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.226F, 0.593F), 0.33F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.226F, 0.593F), 0.64F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.226F, 0.593F), 0.92F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }

            if (var1 == IsoDirections.W) {
               switch (var14) {
                  case 1:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.5859375F, 0.9F), 0.21875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.5859375F, 0.9F), 0.421875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.5859375F, 0.9F), 0.71F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     ItemSpawner.spawnItem(var12, var3, Rand.Next(0.5859375F, 0.9F), 0.9175F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }
         }
      }

   }

   private void generateSinkClutter(IsoDirections var1, IsoObject var2, IsoGridSquare var3, HashMap<Integer, String> var4) {
      int var5 = Math.min(4, var4.size() + 1);
      int var6 = Rand.Next(1, var5);
      ArrayList var7 = new ArrayList();

      int var9;
      for(int var8 = 0; var8 < var6; ++var8) {
         var9 = Rand.Next(1, 5);
         boolean var10 = false;

         while(!var10) {
            if (!var7.contains(var9)) {
               var7.add(var9);
               var10 = true;
            } else {
               var9 = Rand.Next(1, 5);
            }
         }

         if (var7.size() == 4) {
         }
      }

      ArrayList var13 = new ArrayList();

      for(var9 = 0; var9 < var7.size(); ++var9) {
         int var14 = (Integer)var7.get(var9);
         int var11 = Rand.Next(1, var4.size() + 1);
         String var12 = null;

         while(var12 == null) {
            var12 = (String)var4.get(var11);
            if (var13.contains(var12)) {
               var12 = null;
               var11 = Rand.Next(1, var4.size() + 1);
            }
         }

         if (var12 != null) {
            var13.add(var12);
            if (var1 == IsoDirections.S) {
               switch (var14) {
                  case 1:
                     this.addWorldItem(var12, var3, 0.71875F, 0.125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     this.addWorldItem(var12, var3, 0.0935F, 0.21875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     this.addWorldItem(var12, var3, 0.1328125F, 0.589375F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     this.addWorldItem(var12, var3, 0.7890625F, 0.589375F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }

            if (var1 == IsoDirections.N) {
               switch (var14) {
                  case 1:
                     this.addWorldItem(var12, var3, 0.921875F, 0.921875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     this.addWorldItem(var12, var3, 0.1640625F, 0.8984375F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     this.addWorldItem(var12, var3, 0.021875F, 0.5F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     this.addWorldItem(var12, var3, 0.8671875F, 0.5F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }

            if (var1 == IsoDirections.E) {
               switch (var14) {
                  case 1:
                     this.addWorldItem(var12, var3, 0.234375F, 0.859375F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     this.addWorldItem(var12, var3, 0.59375F, 0.875F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     this.addWorldItem(var12, var3, 0.53125F, 0.125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     this.addWorldItem(var12, var3, 0.210937F, 0.1328125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }

            if (var1 == IsoDirections.W) {
               switch (var14) {
                  case 1:
                     this.addWorldItem(var12, var3, 0.515625F, 0.109375F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 2:
                     this.addWorldItem(var12, var3, 0.578125F, 0.890625F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 3:
                     this.addWorldItem(var12, var3, 0.8828125F, 0.8984375F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
                     break;
                  case 4:
                     this.addWorldItem(var12, var3, 0.8671875F, 0.1653125F, var2.getSurfaceOffsetNoTable() / 96.0F, true);
               }
            }
         }
      }

   }

   private IsoDirections getFacing(IsoSprite var1) {
      if (var1 != null && var1.getProperties().Is("Facing")) {
         switch (var1.getProperties().Val("Facing")) {
            case "N":
               return IsoDirections.N;
            case "S":
               return IsoDirections.S;
            case "W":
               return IsoDirections.W;
            case "E":
               return IsoDirections.E;
         }
      }

      return null;
   }

   private void checkForTableSpawn(BuildingDef var1, IsoObject var2) {
      if (var2.getSquare().getRoom() != null && Rand.NextBool(10)) {
         RBTableStoryBase var3 = RBTableStoryBase.getRandomStory(var2.getSquare(), var2);
         if (var3 != null) {
            var3.randomizeBuilding(var1);
            this.doneTable = true;
         }
      }

   }

   private IsoObject checkForTable(IsoGridSquare var1, IsoObject var2) {
      if (!this.tablesDone.contains(var2) && var1 != null) {
         for(int var3 = 0; var3 < var1.getObjects().size(); ++var3) {
            IsoObject var4 = (IsoObject)var1.getObjects().get(var3);
            if (!this.tablesDone.contains(var4) && var4.getProperties().isTable() && var4.getProperties().getSurface() == 34 && var4.getContainer() == null && var4 != var2) {
               return var4;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public void doProfessionStory(BuildingDef var1, String var2) {
      this.spawnItemsInContainers(var1, var2, 70);
   }

   private void addRandomDeadSurvivorStory(BuildingDef var1) {
      this.initRDSMap(var1);
      int var2 = Rand.Next(this.totalChanceRDS);
      Iterator var3 = rdsMap.keySet().iterator();
      int var4 = 0;

      while(var3.hasNext()) {
         RandomizedDeadSurvivorBase var5 = (RandomizedDeadSurvivorBase)var3.next();
         var4 += (Integer)rdsMap.get(var5);
         if (var2 < var4) {
            var5.randomizeDeadSurvivor(var1);
            if (var5.isUnique()) {
               getUniqueRDSSpawned().add(var5.getName());
            }
            break;
         }
      }

   }

   private void initRDSMap(BuildingDef var1) {
      this.totalChanceRDS = 0;
      rdsMap.clear();

      for(int var2 = 0; var2 < this.deadSurvivorsStory.size(); ++var2) {
         RandomizedDeadSurvivorBase var3 = (RandomizedDeadSurvivorBase)this.deadSurvivorsStory.get(var2);
         boolean var4 = SandboxOptions.instance.MaximumRatIndex.getValue() <= 0;
         if (var3.isValid(var1, false) && var3.isTimeValid(false) && (!var3.isUnique() || !getUniqueRDSSpawned().contains(var3.getName())) && (!var4 || !var3.isRat())) {
            this.totalChanceRDS += ((RandomizedDeadSurvivorBase)this.deadSurvivorsStory.get(var2)).getChance();
            rdsMap.put((RandomizedDeadSurvivorBase)this.deadSurvivorsStory.get(var2), ((RandomizedDeadSurvivorBase)this.deadSurvivorsStory.get(var2)).getChance());
         }
      }

   }

   public void doRandomDeadSurvivorStory(BuildingDef var1, RandomizedDeadSurvivorBase var2) {
      var2.randomizeDeadSurvivor(var1);
   }

   public RBBasic() {
      this.name = "RBBasic";
      this.deadSurvivorsStory.add(new RDSBleach());
      this.deadSurvivorsStory.add(new RDSGunslinger());
      this.deadSurvivorsStory.add(new RDSGunmanInBathroom());
      this.deadSurvivorsStory.add(new RDSZombieLockedBathroom());
      this.deadSurvivorsStory.add(new RDSDeadDrunk());
      this.deadSurvivorsStory.add(new RDSSpecificProfession());
      this.deadSurvivorsStory.add(new RDSZombiesEating());
      this.deadSurvivorsStory.add(new RDSBanditRaid());
      this.deadSurvivorsStory.add(new RDSBandPractice());
      this.deadSurvivorsStory.add(new RDSBathroomZed());
      this.deadSurvivorsStory.add(new RDSBedroomZed());
      this.deadSurvivorsStory.add(new RDSFootballNight());
      this.deadSurvivorsStory.add(new RDSHenDo());
      this.deadSurvivorsStory.add(new RDSStagDo());
      this.deadSurvivorsStory.add(new RDSStudentNight());
      this.deadSurvivorsStory.add(new RDSPokerNight());
      this.deadSurvivorsStory.add(new RDSSuicidePact());
      this.deadSurvivorsStory.add(new RDSPrisonEscape());
      this.deadSurvivorsStory.add(new RDSPrisonEscapeWithPolice());
      this.deadSurvivorsStory.add(new RDSSkeletonPsycho());
      this.deadSurvivorsStory.add(new RDSCorpsePsycho());
      this.deadSurvivorsStory.add(new RDSPoliceAtHouse());
      this.deadSurvivorsStory.add(new RDSHouseParty());
      this.deadSurvivorsStory.add(new RDSTinFoilHat());
      this.deadSurvivorsStory.add(new RDSHockeyPsycho());
      this.deadSurvivorsStory.add(new RDSDevouredByRats());
      this.deadSurvivorsStory.add(new RDSRPGNight());
      this.deadSurvivorsStory.add(new RDSRatKing());
      this.deadSurvivorsStory.add(new RDSRatInfested());
      this.deadSurvivorsStory.add(new RDSResourceGarage());
      this.deadSurvivorsStory.add(new RDSGrouchos());
      this.specificProfessionDistribution.add("Carpenter");
      this.specificProfessionDistribution.add("Electrician");
      this.specificProfessionDistribution.add("Farmer");
      this.specificProfessionDistribution.add("Nurse");
      this.specificProfessionDistribution.add("Chef");
      this.specificProfessionRoomDistribution.put("Carpenter", "kitchen");
      this.specificProfessionRoomDistribution.put("Electrician", "kitchen");
      this.specificProfessionRoomDistribution.put("Farmer", "kitchen");
      this.specificProfessionRoomDistribution.put("Nurse", "kitchen;bathroom");
      this.specificProfessionRoomDistribution.put("Chef", "kitchen");
      this.plankStash.put("floors_interior_tilesandwood_01_40", "floors_interior_tilesandwood_01_56");
      this.plankStash.put("floors_interior_tilesandwood_01_41", "floors_interior_tilesandwood_01_57");
      this.plankStash.put("floors_interior_tilesandwood_01_42", "floors_interior_tilesandwood_01_58");
      this.plankStash.put("floors_interior_tilesandwood_01_43", "floors_interior_tilesandwood_01_59");
      this.plankStash.put("floors_interior_tilesandwood_01_44", "floors_interior_tilesandwood_01_60");
      this.plankStash.put("floors_interior_tilesandwood_01_45", "floors_interior_tilesandwood_01_61");
      this.plankStash.put("floors_interior_tilesandwood_01_46", "floors_interior_tilesandwood_01_62");
      this.plankStash.put("floors_interior_tilesandwood_01_47", "floors_interior_tilesandwood_01_63");
      this.plankStash.put("floors_interior_tilesandwood_01_52", "floors_interior_tilesandwood_01_68");
   }

   public ArrayList<RandomizedDeadSurvivorBase> getSurvivorStories() {
      return this.deadSurvivorsStory;
   }

   public ArrayList<String> getSurvivorProfession() {
      return this.specificProfessionDistribution;
   }

   public static ArrayList<String> getUniqueRDSSpawned() {
      return uniqueRDSSpawned;
   }

   public void doProfessionBuilding(BuildingDef var1, String var2, ItemPickerJava.ItemPickerRoom var3) {
      Integer var4 = Integer.valueOf(var3.vehicleChance);
      Integer var5 = Integer.valueOf(var3.femaleOdds);
      if (Core.bDebug) {
         DebugLog.log("Profession Female Chance: " + var5);
      }

      if (var3.vehicleChance == null) {
         var4 = 50;
      }

      if (var3.vehicles == null) {
         var4 = null;
      }

      String var6 = null;
      if (var3.vehicleDistribution != null) {
         var6 = var3.vehicleDistribution;
      }

      if (Core.bDebug) {
         DebugLog.log("Profession House Initialized for " + var2 + " at X: " + (var1.x + var1.x2) / 2 + ", Y: " + (var1.y + var1.y2) / 2);
      }

      String var7 = var3.outfit;
      IsoDeadBody var8 = null;
      Object var9 = null;
      Object var10 = null;
      BaseVehicle var11 = null;
      IsoGridSquare var12 = var1.getFreeSquareInRoom();
      boolean var13 = Rand.Next(2) == 0;
      if (var3.bagType == null) {
         var13 = false;
      }

      boolean var14 = false;
      InventoryContainer var15 = null;
      InventoryItem var16 = null;
      String var17;
      if (var13) {
         DebugLog.log("Trying to spawn Profession Bag: " + var3.bagType);
         var15 = (InventoryContainer)InventoryItemFactory.CreateItem(var3.bagType);
         if (var15 != null) {
            DebugLog.log("Profession Bag Spawned: " + var3.bagType);
            if (var3.bagTable != null) {
               ItemPickerJava.rollContainerItem(var15, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var3.bagTable));
            } else {
               ItemPickerJava.rollContainerItem(var15, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerContainer)ItemPickerJava.getItemPickerContainers().get(var15.getType()));
            }

            var17 = "Base.Key1";
            var16 = InventoryItemFactory.CreateItem(var17);
            if (var16 != null) {
               var16.setKeyId(var1.getKeyId());
               ItemPickerJava.KeyNamer.nameKey(var16, var12);
            }

            if (var15.getItemContainer() != null) {
               var15.getItemContainer().addItem(var16);
            }
         }
      }

      var17 = null;
      if (var3.vehicle != null) {
         var17 = var3.vehicle;
      }

      String var10000;
      if (var17 != null && var4 != null && Rand.Next(100) < var4) {
         if (Core.bDebug) {
            DebugLog.log("Trying to spawn Profession vehicle: " + var17);
         }

         var11 = this.spawnCarOnNearestNav(var17, var1, var6);
         if (var11 != null && Core.bDebug) {
            var10000 = var11.getScriptName();
            DebugLog.log("Profession Vehicle " + var10000 + " for " + var2 + " at X: " + var11.getX() + ", Y: " + var11.getY());
         }
      }

      boolean var18 = var11 == null;
      if (Rand.Next(2) == 0) {
         var18 = true;
      }

      if (var18 && var12 != null) {
         boolean var19 = Rand.Next(100) < var5;
         if (var19) {
            var5 = 100;
         } else {
            var5 = 0;
         }

         if (var19 && var3.outfitFemale != null) {
            var7 = var3.outfitFemale;
         }

         if (!var19 && var3.outfitMale != null) {
            var7 = var3.outfitMale;
         }

         if (var7 != null) {
            var8 = createRandomDeadBody(var12, (IsoDirections)null, false, 0, 0, var7, var5);
         } else {
            var8 = createRandomDeadBody(var12, (IsoDirections)null, false, 0, 0, (String)null, var5);
         }

         if (var8 != null) {
            if (Core.bDebug) {
               DebugLog.log("Profession Corpse for " + var2 + " at X: " + var8.getX() + ", Y: " + var8.getY() + ", Z: " + var8.getZ() + " in Outfit " + var7);
            }

            String var20 = "Base.Key1";
            InventoryItem var21 = var8.getItemContainer().AddItem(var20);
            if (var21 != null) {
               var21.setKeyId(var1.getKeyId());
               ItemPickerJava.KeyNamer.nameKey(var21, var12);
            }

            ItemPickerJava.rollItem((ItemPickerJava.ItemPickerContainer)var3.Containers.get("body"), var8.getContainer(), true, (IsoGameCharacter)null, (ItemPickerJava.ItemPickerRoom)null);
         }

         if (var8 != null && var13 && !var14 && var15 != null) {
            if (var12.getRoom() != null && ((IsoRoom)Objects.requireNonNull(var8.getSquare().getRoom())).getRandomFreeSquare() != null) {
               this.addItemOnGround(var12.getRoom().getRandomFreeSquare(), var15);
            } else {
               this.addItemOnGround(var8.getSquare(), var15);
            }

            var14 = true;
            if (Core.bDebug) {
               DebugLog.log("Profession Bag Spawned With Corpse");
            }
         }
      }

      int var22;
      if (var11 != null) {
         InventoryItem var24 = var11.createVehicleKey();
         if (var10 != null) {
            ((IsoZombie)var10).addItemToSpawnAtDeath(var24);
         }

         if (var8 != null) {
            var8.getContainer().AddItem(var24);
         }

         if (var10 == null && var8 == null && var12 != null) {
            ItemContainer var26 = ((IsoBuilding)Objects.requireNonNull(var12.getBuilding())).getRandomContainerSingle("sidetable");
            if (var26 == null) {
               var26 = var12.getBuilding().getRandomContainerSingle("dresser");
            }

            if (var26 == null) {
               var26 = var12.getBuilding().getRandomContainerSingle("counter");
            }

            if (var26 == null) {
               var26 = var12.getBuilding().getRandomContainerSingle("wardrobe");
            }

            if (var26 != null) {
               if (var13 && !var14 && var15 != null) {
                  var26.addItem(var15);
                  if (var15.getItemContainer() != null) {
                     var15.getItemContainer().addItem(var24);
                  } else {
                     var11.putKeyToContainer(var26, var26.getParent().getSquare(), var26.getParent());
                  }

                  var14 = true;
                  if (Core.bDebug) {
                     var10000 = var26.getType();
                     DebugLog.log("Profession Bag and Profession Vehicle Key Spawned in " + var10000 + " at X: " + var26.getParent().getX() + ", Y: " + var26.getParent().getY() + ", Z: " + var26.getParent().getZ());
                  }
               } else {
                  var11.putKeyToContainer(var26, var26.getParent().getSquare(), var26.getParent());
                  if (Core.bDebug) {
                     var10000 = var26.getType();
                     DebugLog.log("Profession Vehicle Key Spawned in " + var10000 + " at X: " + var26.getParent().getX() + ", Y: " + var26.getParent().getY() + ", Z: " + var26.getParent().getZ());
                  }
               }
            } else if (var13 && !var14 && var15 != null) {
               this.addItemOnGround(var12, var15);
               if (var15.getItemContainer() != null) {
                  var15.getItemContainer().addItem(var24);
               } else {
                  var11.putKeyToWorld(var12);
               }

               var14 = true;
               if (Core.bDebug) {
                  var22 = var12.getX();
                  DebugLog.log("Profession Bag and Profession Vehicle Key Spawned at X: " + var22 + ", Y: " + var12.getY() + ", Z: " + var12.getZ());
               }
            } else {
               var11.putKeyToWorld(var12);
               if (Core.bDebug) {
                  var22 = var12.getX();
                  DebugLog.log("Profession Vehicle Key Spawned at X: " + var22 + ", Y: " + var12.getY() + ", Z: " + var12.getZ());
               }
            }
         }
      }

      float var23;
      if (var8 != null && Rand.Next(2) == 0) {
         var8.reanimateNow();
         if (Core.bDebug) {
            var23 = var8.getX();
            DebugLog.log("Profession Corpse promoted to Zombie at X: " + var23 + ", Y: " + var8.getY() + ", Z: " + var8.getZ());
         }
      }

      if (var10 != null && Rand.Next(2) == 0) {
         ((IsoZombie)var10).Kill((IsoGameCharacter)null, true);
         if (Core.bDebug) {
            var23 = ((IsoZombie)var10).getX();
            DebugLog.log("Profession Zombie promoted to Corpse at X: " + var23 + ", Y: " + ((IsoZombie)var10).getY() + ", Z: " + ((IsoZombie)var10).getZ());
         }
      }

      if (var13 && !var14 && var15 != null && var12 != null) {
         ItemContainer var25 = ((IsoBuilding)Objects.requireNonNull(var12.getBuilding())).getRandomContainer("sidetable");
         if (var25 == null) {
            var25 = var12.getBuilding().getRandomContainerSingle("dresser");
         }

         if (var25 == null) {
            var25 = var12.getBuilding().getRandomContainerSingle("wardrobe");
         }

         if (var25 == null) {
            var25 = var12.getBuilding().getRandomContainerSingle("counter");
         }

         if (var25 != null) {
            var25.addItem(var15);
            var14 = true;
            if (Core.bDebug) {
               var10000 = var25.getType();
               DebugLog.log("Profession Bag Spawned in " + var10000 + " at X: " + var25.getParent().getX() + ", Y: " + var25.getParent().getY() + ", Z: " + var25.getParent().getZ());
            }
         } else {
            this.addItemOnGround(var12, var15);
            var14 = true;
            if (Core.bDebug) {
               var22 = var12.getX();
               DebugLog.log("Profession Bag Spawned at X: " + var22 + ", Y: " + var12.getY() + ", Z: " + var12.getZ());
            }
         }
      }

   }

   static void doOfficeStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.NextBool(2) && var0.getObjects().size() == 2 && var2.getProperties().Val("BedType") == null && var2.isTableSurface() && (var2.getContainer() == null || "desk".equals(var2.getContainer().getType()))) {
            if (Rand.Next(100) < 66) {
               ItemSpawner.spawnItem(getOfficePenClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }

            if (Rand.Next(100) < 66) {
               ItemSpawner.spawnItem(getOfficePaperworkClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }

            if (Rand.Next(100) < 66) {
               ItemSpawner.spawnItem(getOfficeOtherClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }

            if (Rand.Next(100) < 20) {
               ItemSpawner.spawnItem(getOfficeTreatClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }

   static void doNolansOfficeStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.Next(100) < 80 && var2.getProperties().Val("BedType") == null && (var2.getContainer() == null || "desk".equals(var2.getContainer().getType()))) {
            ItemSpawner.spawnItem(getOfficeCarDealerClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            if (Rand.Next(100) < 50) {
               ItemSpawner.spawnItem(getOfficeCarDealerClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
               ItemSpawner.spawnItem(getOfficeCarDealerClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }

   static void doCafeStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface()) {
            if (Rand.NextBool(3)) {
               ItemSpawner.spawnItem("MugWhite", var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
            }

            if (Rand.Next(100) < 40 && var0.getObjects().size() == 2 && var2.getProperties().Val("BedType") == null && (var2.getContainer() == null || "desk".equals(var2.getContainer().getType()))) {
               ItemSpawner.spawnItem(getCafeClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
               if (Rand.Next(100) < 40) {
                  ItemSpawner.spawnItem(getCafeClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
               }
            }
         }
      }

   }

   static void doGigamartStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.Next(100) < 30 && var0.getObjects().size() == 2 && var2.getContainer() != null) {
            ItemSpawner.spawnItem(getGigamartClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            if (Rand.Next(100) < 40) {
               ItemSpawner.spawnItem(getGigamartClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }

   static void doGroceryStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.Next(100) < 20 && var0.getObjects().size() == 2 && var2.getContainer() != null) {
            ItemSpawner.spawnItem(getGroceryClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            if (Rand.Next(100) < 40) {
               ItemSpawner.spawnItem(getGroceryClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }

   static void doGeneralRoom(IsoGridSquare var0, ArrayList<String> var1) {
      for(int var2 = 0; var2 < var0.getObjects().size(); ++var2) {
         IsoObject var3 = (IsoObject)var0.getObjects().get(var2);
         if (var3.isTableSurface() && var0.getObjects().size() <= 3 && Rand.NextBool(3)) {
            String var4 = getClutterItem(var1);
            var3.addItemToObjectSurface(var4, true);
         }
      }

   }

   private void doLaundryStuff(IsoGridSquare var1) {
      HashMap var2 = this.getClutterCopy(this.getLaundryRoomClutter());
      boolean var3 = false;
      boolean var4 = false;

      for(int var5 = 0; var5 < var1.getObjects().size(); ++var5) {
         IsoObject var6 = (IsoObject)var1.getObjects().get(var5);
         IsoDirections var7 = this.getFacing(var6.getSprite());
         boolean var8 = var6.getContainer() != null && "counter".equals(var6.getContainer().getType());
         if (var6.getSprite() == null || var6.getSprite().getName() == null) {
            return;
         }

         if (!var3 && var6.getSprite().getName().contains("sink") && Rand.NextBool(4)) {
            if (var7 != null) {
               this.generateSinkClutter(var7, var6, var1, var2);
               var3 = true;
            }
         } else if (!var4 && var8 && Rand.NextBool(6)) {
            boolean var12 = true;

            for(int var10 = 0; var10 < var1.getObjects().size(); ++var10) {
               IsoObject var11 = (IsoObject)var1.getObjects().get(var10);
               if (var11.getSprite() != null && var11.getSprite().getName() != null && var11.getSprite().getName().contains("sink") || var11 instanceof IsoStove || var11 instanceof IsoRadio) {
                  var12 = false;
                  break;
               }
            }

            if (var12 && var7 != null) {
               this.generateCounterClutter(var7, var6, var1, var2);
               var4 = true;
            }
         } else if (!var8 && var6.isTableSurface() && var1.getObjects().size() <= 3 && Rand.NextBool(3)) {
            String var9 = this.getLaundryRoomClutterItem();
            var6.addItemToObjectSurface(var9, true);
         }
      }

   }

   static void doJudgeStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.Next(100) < 80 && var2.getProperties().Val("BedType") == null && (var2.getContainer() == null || "desk".equals(var2.getContainer().getType()))) {
            ItemSpawner.spawnItem(getJudgeClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            if (Rand.Next(100) < 50) {
               ItemSpawner.spawnItem(getJudgeClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
               ItemSpawner.spawnItem(getJudgeClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }

   static void doTwiggyStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.Next(100) < 50 && var2.getProperties().Val("BedType") == null && (var2.getContainer() == null || "counter".equals(var2.getContainer().getType()))) {
            ItemSpawner.spawnItem(getTwiggyClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            if (Rand.Next(100) < 30) {
               ItemSpawner.spawnItem(getTwiggyClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }

   static void doWoodcraftStuff(IsoGridSquare var0) {
      for(int var1 = 0; var1 < var0.getObjects().size(); ++var1) {
         IsoObject var2 = (IsoObject)var0.getObjects().get(var1);
         if (var2.isTableSurface() && Rand.Next(100) < 80 && var2.getProperties().Val("BedType") == null && (var2.getContainer() == null || "desk".equals(var2.getContainer().getType()))) {
            ItemSpawner.spawnItem(getWoodcraftClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            if (Rand.Next(100) < 50) {
               ItemSpawner.spawnItem(getWoodcraftClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
               ItemSpawner.spawnItem(getWoodcraftClutterItem(), var0, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), var2.getSurfaceOffsetNoTable() / 96.0F);
            }
         }
      }

   }
}
