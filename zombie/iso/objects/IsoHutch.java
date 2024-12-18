package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SystemDisabler;
import zombie.Lua.LuaManager;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.popman.animal.HutchManager;
import zombie.util.StringUtils;
import zombie.util.Type;

public class IsoHutch extends IsoObject {
   int linkedX = 0;
   int linkedY = 0;
   int linkedZ = 0;
   boolean open = false;
   boolean openEggHatch = false;
   KahluaTableImpl def;
   public int savedX = 0;
   public int savedY = 0;
   public int savedZ = 0;
   public HashMap<Integer, IsoAnimal> animalInside = new HashMap();
   public HashMap<Integer, IsoDeadBody> deadBodiesInside = new HashMap();
   public ArrayList<IsoAnimal> animalOutside = new ArrayList();
   public String type;
   public int lastHourCheck = -1;
   private float exitTimer = 0.0F;
   private int enterSpotX = 0;
   private int enterSpotY = 0;
   private int maxAnimals = 0;
   private int maxNestBox = 0;
   private HashMap<Integer, NestBox> nestBoxes = new HashMap();
   private float nestBoxDirt = 0.0F;
   private float hutchDirt = 0.0F;
   UpdateLimit updateAnimal = new UpdateLimit(2100L);
   byte animalInsideSize = 0;
   boolean sendUpdate = false;

   public IsoHutch(IsoCell var1) {
      super(var1);
   }

   public IsoHutch(IsoGridSquare var1, boolean var2, String var3, KahluaTableImpl var4, IsoGridSquare var5) {
      super((IsoGridSquare)var1, (String)var3, (String)null);
      this.def = var4;
      var1.AddSpecialObject(this);
      if (var5 != null) {
         this.linkedX = var5.x;
         this.linkedY = var5.y;
         this.linkedZ = var5.z;
         HutchManager.getInstance().remove(this);
      } else if (var4 != null) {
         this.type = var4.rawgetStr("name");
         KahluaTableImpl var6 = (KahluaTableImpl)var4.rawget("extraSprites");
         if (var6 != null) {
            this.savedX = var1.x;
            this.savedY = var1.y;
            this.savedZ = var1.z;
            if (!HutchManager.getInstance().checkHutchExistInList(this)) {
               HutchManager.getInstance().add(this);
            }

            DesignationZoneAnimal var7 = DesignationZoneAnimal.getZone((int)this.getX(), (int)this.getY(), (int)this.getZ());
            if (var7 != null && !var7.hutchs.contains(this)) {
               var7.hutchs.add(this);
            }

            KahluaTableIterator var8 = var6.iterator();

            while(var8.advance()) {
               KahluaTableImpl var9 = (KahluaTableImpl)var8.getValue();
               int var10 = var9.rawgetInt("xoffset");
               int var11 = var9.rawgetInt("yoffset");
               byte var12 = 0;
               String var13 = var9.rawgetStr("sprite");
               IsoGridSquare var14 = IsoWorld.instance.CurrentCell.getGridSquare(var1.getX() + var10, var1.getY() + var11, var1.getZ() + var12);
               if (var14 != null) {
                  new IsoHutch(var14, var2, var13, var4, var1);
               }
            }

            for(int var15 = 0; var15 < this.getMaxNestBox() + 1; ++var15) {
               this.nestBoxes.put(var15, new NestBox(var15));
            }

         }
      }
   }

   public IsoHutch getHutch() {
      if (!this.isSlave()) {
         return this;
      } else {
         IsoGridSquare var1 = IsoWorld.instance.CurrentCell.getGridSquare(this.linkedX, this.linkedY, this.linkedZ);
         if (var1 == null) {
            return null;
         } else {
            for(int var2 = 0; var2 < var1.getSpecialObjects().size(); ++var2) {
               IsoHutch var3 = (IsoHutch)Type.tryCastTo((IsoObject)var1.getSpecialObjects().get(var2), IsoHutch.class);
               if (var3 != null) {
                  return var3;
               }
            }

            return null;
         }
      }
   }

   public static IsoHutch getHutch(int var0, int var1, int var2) {
      IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(var0, var1, var2);
      if (var3 == null) {
         return null;
      } else {
         Iterator var4 = var3.getSpecialObjects().iterator();

         IsoHutch var6;
         do {
            if (!var4.hasNext()) {
               return null;
            }

            IsoObject var5 = (IsoObject)var4.next();
            var6 = (IsoHutch)Type.tryCastTo(var5, IsoHutch.class);
         } while(var6 == null);

         return var6;
      }
   }

   public void transmitCompleteItemToClients() {
      if (GameServer.bServer) {
         if (GameServer.udpEngine == null) {
            return;
         }

         if (SystemDisabler.doWorldSyncEnable) {
            return;
         }

         KahluaTableImpl var1 = (KahluaTableImpl)this.def.rawget("extraSprites");
         if (var1 != null) {
            KahluaTableIterator var2 = var1.iterator();

            while(var2.advance()) {
               KahluaTableImpl var3 = (KahluaTableImpl)var2.getValue();
               int var4 = var3.rawgetInt("xoffset");
               int var5 = var3.rawgetInt("yoffset");
               byte var6 = 0;
               IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX() + var4, this.square.getY() + var5, this.square.getZ() + var6);

               for(int var8 = 0; var8 < var7.getSpecialObjects().size(); ++var8) {
                  IsoHutch var9 = (IsoHutch)Type.tryCastTo((IsoObject)var7.getSpecialObjects().get(var8), IsoHutch.class);
                  if (var9 != null) {
                     INetworkPacket.sendToRelative(PacketTypes.PacketType.AddItemToMap, (float)this.square.x, (float)this.square.y, var9);
                  }
               }
            }
         }
      }

   }

   public void syncIsoObjectSend(ByteBufferWriter var1) {
      byte var2 = (byte)this.getObjectIndex();
      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      var1.putByte(var2);
      var1.putByte((byte)1);
      var1.putByte((byte)0);
      var1.putByte((byte)(this.open ? 1 : 0));
      var1.putByte((byte)(this.openEggHatch ? 1 : 0));
      var1.putFloat(this.getHutchDirt());
      var1.putFloat(this.getNestBoxDirt());
      var1.putByte((byte)this.nestBoxes.size());
      Iterator var3 = this.nestBoxes.values().iterator();

      while(var3.hasNext()) {
         NestBox var4 = (NestBox)var3.next();
         var1.putByte((byte)var4.eggs.size());
         Iterator var5 = var4.eggs.iterator();

         while(var5.hasNext()) {
            Food var6 = (Food)var5.next();

            try {
               var6.saveWithSize(var1.bb, true);
            } catch (IOException var8) {
               throw new RuntimeException(var8);
            }
         }
      }

      var1.putByte((byte)this.animalInside.size());
      var3 = this.animalInside.values().iterator();

      while(var3.hasNext()) {
         IsoAnimal var9 = (IsoAnimal)var3.next();
         var1.putInt(var9.getData().getHutchPosition());
         var1.putInt(var9.getAnimalID());
      }

   }

   public void syncIsoObjectReceive(ByteBuffer var1) {
      boolean var2 = var1.get() == 1;
      boolean var3 = var1.get() == 1;
      float var4 = var1.getFloat();
      float var5 = var1.getFloat();
      if (this.open != var2) {
         this.toggleDoor();
      }

      if (this.openEggHatch != var3) {
         this.toggleEggHatchDoor();
      }

      this.setHutchDirt(var4);
      this.setNestBoxDirt(var5);
      byte var6 = var1.get();

      byte var8;
      int var10;
      for(int var7 = 0; var7 < var6; ++var7) {
         var8 = var1.get();
         NestBox var9 = this.getNestBox(var7);
         var9.eggs.clear();

         for(var10 = 0; var10 < var8; ++var10) {
            try {
               InventoryItem var11 = InventoryItem.loadItem(var1, IsoWorld.getWorldVersion());
               if (var11 instanceof Food) {
                  var9.eggs.add((Food)var11);
               }
            } catch (IOException var16) {
               throw new RuntimeException(var16);
            }
         }
      }

      ArrayList var17 = new ArrayList();
      var8 = var1.get();

      class HutchAnimal {
         int position;
         int animalId;

         public HutchAnimal(int var2, int var3) {
            this.position = var2;
            this.animalId = var3;
         }
      }

      for(int var18 = 0; var18 < var8; ++var18) {
         var10 = var1.getInt();
         int var21 = var1.getInt();
         var17.add(new HutchAnimal(var10, var21));
      }

      ArrayList var19 = new ArrayList();
      Iterator var20 = this.animalInside.entrySet().iterator();

      while(var20.hasNext()) {
         Map.Entry var22 = (Map.Entry)var20.next();
         IsoAnimal var12 = (IsoAnimal)var22.getValue();
         boolean var13 = false;
         Iterator var14 = var17.iterator();

         while(var14.hasNext()) {
            HutchAnimal var15 = (HutchAnimal)var14.next();
            if (var15.animalId == var12.getAnimalID()) {
               var13 = true;
               break;
            }
         }

         if (!var13) {
            var19.add((IsoAnimal)var22.getValue());
         }
      }

      var20 = var19.iterator();

      while(var20.hasNext()) {
         IsoAnimal var23 = (IsoAnimal)var20.next();
         this.removeAnimal(var23);
         var23.remove();
      }

   }

   public boolean haveRoomForNewEggs() {
      Iterator var1 = this.nestBoxes.values().iterator();

      NestBox var2;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         var2 = (NestBox)var1.next();
      } while(var2.getEggsNb() >= 10);

      return true;
   }

   public void update() {
      if (!this.isSlave() && this.isExistInTheWorld()) {
         if (!this.isOwner()) {
            ArrayList var9 = new ArrayList();
            var9.addAll(this.animalInside.values());
            Iterator var10 = this.nestBoxes.values().iterator();

            while(var10.hasNext()) {
               NestBox var11 = (NestBox)var10.next();
               if (var11.animal != null) {
                  var9.add(var11.animal);
               }
            }

            var10 = var9.iterator();

            while(var10.hasNext()) {
               IsoAnimal var12 = (IsoAnimal)var10.next();
               if (var12 != null) {
                  AnimalInstanceManager.getInstance().update(var12);
               }
            }

         } else {
            this.sendUpdate = this.updateAnimal.Check() || this.animalInsideSize != this.animalInside.size();
            boolean var1 = false;
            if (GameTime.getInstance().getHour() != this.lastHourCheck) {
               this.lastHourCheck = GameTime.getInstance().getHour();
               var1 = true;
            }

            int var2 = 8000 - this.animalInside.size() * 100;
            if (var2 < 4500) {
               var2 = 4500;
            }

            if (!this.animalInside.isEmpty() && Rand.NextBool(var2)) {
               this.hutchDirt = Math.min(this.hutchDirt + 1.0F, 100.0F);
               this.sync();
            }

            Iterator var3 = this.animalInside.keySet().iterator();

            IsoAnimal var5;
            while(var3.hasNext()) {
               Integer var4 = (Integer)var3.next();
               var5 = (IsoAnimal)this.animalInside.get(var4);
               if (var5 != null) {
                  if (var5.nestBox > -1) {
                     var3.remove();
                  } else {
                     this.updateAnimalInside((IsoAnimal)this.animalInside.get(var4), var1);
                  }
               }
            }

            var3 = this.nestBoxes.keySet().iterator();

            while(var3.hasNext()) {
               NestBox var13 = (NestBox)this.nestBoxes.get(var3.next());
               var5 = var13.animal;
               if (var5 != null && this.nestBoxes.get(var5.nestBox) != null) {
                  this.updateAnimalInside(var5, var1);
                  if (var5 != null && var5.getHealth() <= 0.0F) {
                     ((NestBox)this.nestBoxes.get(var5.nestBox)).animal = null;
                     var5.nestBox = -1;
                     this.addAnimalInside(var5);
                     break;
                  }
               }
            }

            if (this.exitTimer > 0.0F) {
               this.exitTimer = Math.max(0.0F, this.exitTimer - GameTime.getInstance().getMultiplier());
            } else if (!this.animalInside.isEmpty()) {
               ArrayList var14 = new ArrayList();
               ArrayList var16 = new ArrayList();
               Iterator var6 = this.animalInside.keySet().iterator();

               label129:
               while(true) {
                  while(true) {
                     int var7;
                     IsoAnimal var8;
                     do {
                        do {
                           if (!var6.hasNext()) {
                              IsoAnimal var19 = null;
                              if (!var16.isEmpty()) {
                                 var19 = (IsoAnimal)this.animalInside.get(var16.get(Rand.Next(0, var16.size())));
                              } else if (!var14.isEmpty()) {
                                 var19 = (IsoAnimal)this.animalInside.get(var14.get(Rand.Next(0, var14.size())));
                              }

                              this.checkAnimalExitHutch(var19);
                              break label129;
                           }

                           var7 = (Integer)var6.next();
                           var8 = (IsoAnimal)this.animalInside.get(var7);
                        } while(var8 == null);
                     } while(var8.isDead());

                     if (!var8.isFemale() && !var8.isBaby()) {
                        var16.add(var7);
                     } else {
                        var14.add(var7);
                     }
                  }
               }
            }

            for(int var15 = 0; var15 < this.nestBoxes.size(); ++var15) {
               NestBox var17 = (NestBox)this.nestBoxes.get(var15);

               for(int var18 = 0; var18 < var17.getEggsNb(); ++var18) {
                  Food var20 = var17.getEgg(var18);
                  if (var20.checkEggHatch(this)) {
                     var17.removeEgg(var18);
                     --var18;
                  }

                  var20.update();
               }
            }

            if (this.sendUpdate) {
               this.sync();
               this.animalInsideSize = (byte)this.animalInside.size();
            }

         }
      }
   }

   private void updateAnimalInside(IsoAnimal var1, boolean var2) {
      if (var1 != null) {
         if (var1.isDead()) {
            if (this.sendUpdate) {
               var1.getNetworkCharacterAI().getAnimalPacket().reset(var1);
               AnimalSynchronizationManager.getInstance().setReceived(var1.OnlineID);
            }

         } else {
            if (var1.nestBox > -1) {
               var1.eggTimerInHutch = Float.valueOf(Math.max(0.0F, (float)var1.eggTimerInHutch - GameTime.getInstance().getMultiplier())).intValue();
               if (Rand.NextBool(300)) {
                  this.nestBoxDirt = Math.min(this.nestBoxDirt + 1.0F, 100.0F);
               }

               if (var1.eggTimerInHutch <= 0) {
                  var1.eggTimerInHutch = 0;
                  this.addEgg(var1);
                  var1.getNetworkCharacterAI().getAnimalPacket().reset(var1);
                  AnimalSynchronizationManager.getInstance().setReceived(var1.OnlineID);
                  this.sync();
               }
            }

            if (var1.adef.isInsideHutchTime((Integer)null)) {
               var1.updateStress();
            } else {
               var1.changeStress(GameTime.getInstance().getMultiplier() / 15000.0F);
            }

            this.updateAnimalHealthInside(var1);
            if (var1.getHealth() <= 0.0F) {
               this.killAnimal(var1);
               var1.getNetworkCharacterAI().getAnimalPacket().reset(var1);
               AnimalSynchronizationManager.getInstance().setReceived(var1.OnlineID);
               this.sync();
            }

            var1.getData().checkEggs(GameTime.instance.getCalender(), false);
            if (var2) {
               var1.getData().checkFertilizedTime();
               if (this.getHutchDirt() < 20.0F) {
                  var1.setHealth(Math.min(1.0F, var1.getHealth() + var1.getData().getHealthLoss(1.0F)));
               }

               var1.setHoursSurvived(var1.getHoursSurvived() + 1.0);
               var1.getData().updateHungerAndThirst(false);
               if (!this.isDoorClosed()) {
                  var1.checkKilledByMetaPredator(GameTime.getInstance().getHour());
               }

               if (var1.getData().getAge() != var1.getData().getDaysSurvived()) {
                  float var3 = var1.getData().getAgeGrowModifier();
                  var1.getData().setAge(Float.valueOf((float)var1.getData().getDaysSurvived() + (var3 - 1.0F)).intValue());
                  var1.setHoursSurvived((double)(var1.getData().getAge() * 24));
                  var1.getData().growUp(false);
               }
            }

            if (this.sendUpdate) {
               var1.getNetworkCharacterAI().getAnimalPacket().reset(var1);
               AnimalSynchronizationManager.getInstance().setReceived(var1.OnlineID);
            }

         }
      }
   }

   public void doMeta(int var1) {
      for(int var2 = 0; var2 < var1; ++var2) {
         int var3 = 25 - (this.animalInside.size() + this.animalOutside.size());
         if (var3 > 10) {
            var3 = 10;
         }

         if (Rand.NextBool(var3)) {
            this.hutchDirt = Math.min(this.hutchDirt + 1.0F, 100.0F);
         }

         if (Rand.NextBool(var3)) {
            this.nestBoxDirt = Math.min(this.nestBoxDirt + 1.0F, 100.0F);
         }
      }

   }

   private void updateAnimalHealthInside(IsoAnimal var1) {
      float var2 = this.getHutchDirt();
      if (var1.nestBox > -1) {
         var2 = this.getNestBoxDirt();
      }

      if (var2 > 20.0F && Rand.NextBool(250 - (int)var2)) {
         var1.setHealth(var1.getHealth() - 0.01F * (var2 / 1000.0F) * GameTime.getInstance().getMultiplier());
      }

      var1.getData().updateHealth();
   }

   public void killAnimal(IsoAnimal var1) {
      var1.setHealth(0.0F);
      int var2 = var1.getData().getHutchPosition();
      IsoDeadBody var3 = new IsoDeadBody(var1, false);
      this.deadBodiesInside.put(var2, var3);
      var3.getSquare().removeCorpse(var3, false);
      var3.invalidateCorpse();
   }

   private boolean checkAnimalExitHutch(IsoAnimal var1) {
      if (var1 != null && var1.adef != null && !var1.isDead()) {
         boolean var2 = false;
         if (var1.getBehavior().forcedOutsideHutch > 0L && GameTime.getInstance().getCalender().getTimeInMillis() > var1.getBehavior().forcedOutsideHutch) {
            var2 = true;
            var1.getBehavior().forcedOutsideHutch = 0L;
         }

         if (var1.getBehavior().forcedOutsideHutch == 0L && var1.adef.isOutsideHutchTime() && this.isOpen() && var1.nestBox == -1) {
            var2 = true;
         }

         if (var2) {
            IsoGridSquare var3 = IsoWorld.instance.CurrentCell.getGridSquare(this.savedX + this.getEnterSpotX(), this.savedY + this.getEnterSpotY(), this.savedZ);
            if (var3 == null) {
               return false;
            } else if (var1.nestBox > -1) {
               return false;
            } else {
               this.releaseAnimal(var3, var1);
               this.exitTimer = Rand.Next(200.0F, 300.0F);
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void releaseAnimal(IsoGridSquare var1, IsoAnimal var2) {
      if (var1 == null) {
         var1 = IsoWorld.instance.CurrentCell.getGridSquare(this.savedX + this.getEnterSpotX(), this.savedY + this.getEnterSpotY(), this.savedZ);
      }

      if (var1 != null) {
         if (!GameClient.bClient) {
            IsoAnimal var3 = new IsoAnimal(var1.getCell(), var1.x, var1.y, var1.z, var2.getAnimalType(), var2.getBreed());
            var3.copyFrom(var2);
            var2.hutch = null;
            var2.getData().enterHutchTimerAfterDestroy = 300;
            var3.hutch = null;
            var3.addToWorld();
            var3.setStateEventDelayTimer(0.0F);
         }

         this.animalInside.remove(var2.getData().getHutchPosition());
         var2.getData().setHutchPosition(-1);
         this.animalOutside.add(var2);
      }
   }

   public void removeAnimal(IsoAnimal var1) {
      var1.hutch = null;
      this.animalInside.remove(var1.getData().getHutchPosition());
      this.deadBodiesInside.remove(var1.getData().getHutchPosition());
      var1.getData().setHutchPosition(-1);
   }

   private void removeAnimalFromNestBox(NestBox var1) {
      IsoAnimal var2 = var1.animal;
      var1.animal.nestBox = -1;
      var1.animal.getData().eggTime = (long)(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 86400));
      var1.animal = null;
      this.addAnimalInside(var2);
   }

   public void tryFindAndRemoveAnimalFromNestBox(IsoAnimal var1) {
      this.nestBoxes.values().forEach((var1x) -> {
         if (var1x.animal != null && var1x.animal.getAnimalID() == var1.getAnimalID()) {
            var1x.animal.nestBox = -1;
            var1x.animal.getData().eggTime = (long)(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 86400));
            var1x.animal = null;
         }

      });
   }

   public boolean addAnimalInNestBox(IsoAnimal var1) {
      for(int var2 = 0; var2 < this.getMaxNestBox() + 1; ++var2) {
         if (((NestBox)this.nestBoxes.get(var2)).animal == null && ((NestBox)this.nestBoxes.get(var2)).eggs.size() < 10) {
            ((NestBox)this.nestBoxes.get(var2)).animal = var1;
            var1.hutch = this;
            var1.nestBox = var2;
            var1.eggTimerInHutch = Rand.Next(350, 600);
            var1.getData().setHutchPosition(-1);
            if (this.isOwner()) {
               var1.getNetworkCharacterAI().getAnimalPacket().reset(var1);
               AnimalSynchronizationManager.getInstance().setReceived(var1.OnlineID);
               this.sync();
            }

            return true;
         }
      }

      return false;
   }

   public void addEgg(IsoAnimal var1) {
      Food var2 = var1.createEgg();
      ((NestBox)this.nestBoxes.get(var1.nestBox)).addEgg(var2);
      this.removeAnimalFromNestBox((NestBox)this.nestBoxes.get(var1.nestBox));
   }

   public void toggleEggHatchDoor() {
      this.openEggHatch = !this.openEggHatch;
      KahluaTableImpl var1 = (KahluaTableImpl)this.def.rawget("eggHatchDoors");
      KahluaTableIterator var2 = var1.iterator();

      while(true) {
         String var4;
         String var5;
         int var6;
         int var7;
         int var8;
         do {
            do {
               if (!var2.advance()) {
                  return;
               }

               KahluaTableImpl var3 = (KahluaTableImpl)var2.getValue();
               var4 = var3.rawgetStr("sprite");
               var5 = var3.rawgetStr("closedSprite");
               if (this.openEggHatch) {
                  var4 = var3.rawgetStr("closedSprite");
                  var5 = var3.rawgetStr("sprite");
               }

               var6 = var3.rawgetInt("xoffset");
               var7 = var3.rawgetInt("yoffset");
               var8 = var3.rawgetInt("zoffset");
            } while(StringUtils.isNullOrEmpty(var4));
         } while(StringUtils.isNullOrEmpty(var5));

         IsoSprite var9 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var4);
         IsoSprite var10 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var5);
         if (var9 == null || var10 == null) {
            return;
         }

         IsoGridSquare var11 = IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX() + var6, this.square.getY() + var7, this.square.getZ() + var8);
         if (var11 == null) {
            return;
         }

         for(int var12 = 0; var12 < var11.getSpecialObjects().size(); ++var12) {
            IsoHutch var13 = (IsoHutch)Type.tryCastTo((IsoObject)var11.getSpecialObjects().get(var12), IsoHutch.class);
            if (var13 != null && var4.equals(var13.sprite.getName())) {
               var13.setSprite(var10);
            }
         }
      }
   }

   public void reforceUpdate() {
      HutchManager.getInstance().reforceUpdate(this);
   }

   public void toggleDoor() {
      this.reforceUpdate();
      this.open = !this.open;
      if (this.open) {
         for(int var1 = 0; var1 < this.animalOutside.size(); ++var1) {
            ((IsoAnimal)this.animalOutside.get(var1)).getBehavior().callToHutch(this, false);
         }
      }

      KahluaTableImpl var14 = (KahluaTableImpl)this.def.rawget("extraSprites");
      if (var14 != null) {
         KahluaTableIterator var2 = var14.iterator();

         while(true) {
            String var7;
            IsoSprite var10;
            IsoGridSquare var11;
            do {
               int var4;
               int var5;
               byte var6;
               IsoSprite var9;
               do {
                  do {
                     String var8;
                     do {
                        do {
                           if (!var2.advance()) {
                              return;
                           }

                           KahluaTableImpl var3 = (KahluaTableImpl)var2.getValue();
                           var4 = var3.rawgetInt("xoffset");
                           var5 = var3.rawgetInt("yoffset");
                           var6 = 0;
                           var7 = var3.rawgetStr("sprite");
                           var8 = var3.rawgetStr("spriteOpen");
                           if (!this.open) {
                              var7 = var3.rawgetStr("spriteOpen");
                              var8 = var3.rawgetStr("sprite");
                           }
                        } while(StringUtils.isNullOrEmpty(var8));
                     } while(StringUtils.isNullOrEmpty(var7));

                     var9 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var7);
                     var10 = (IsoSprite)IsoSpriteManager.instance.NamedMap.get(var8);
                  } while(var9 == null);
               } while(var10 == null);

               var11 = IsoWorld.instance.CurrentCell.getGridSquare(this.square.getX() + var4, this.square.getY() + var5, this.square.getZ() + var6);
            } while(var11 == null);

            for(int var12 = 0; var12 < var11.getSpecialObjects().size(); ++var12) {
               IsoHutch var13 = (IsoHutch)Type.tryCastTo((IsoObject)var11.getSpecialObjects().get(var12), IsoHutch.class);
               if (var13 != null && var7.equals(var13.sprite.getName())) {
                  var13.setSprite(var10);
               }
            }
         }
      }
   }

   public boolean isOpen() {
      return this.open;
   }

   private KahluaTableImpl getDefFromSprite() {
      if (StringUtils.isNullOrEmpty(this.getSprite().getName())) {
         return null;
      } else {
         KahluaTableImpl var1 = (KahluaTableImpl)LuaManager.env.rawget("HutchDefinitions");
         if (var1 == null) {
            return null;
         } else {
            KahluaTableImpl var2 = (KahluaTableImpl)var1.rawget("hutchs");
            KahluaTableIterator var3 = var2.iterator();

            while(var3.advance()) {
               KahluaTableImpl var4 = (KahluaTableImpl)var3.getValue();
               KahluaTableImpl var5 = (KahluaTableImpl)var4.rawget("extraSprites");
               String var6 = var4.rawgetStr("baseSprite");
               if (!StringUtils.isNullOrEmpty(this.getSprite().getName()) && this.getSprite().getName().equals(var6)) {
                  return var4;
               }

               KahluaTableIterator var7 = var5.iterator();

               while(var7.advance()) {
                  KahluaTableImpl var8 = (KahluaTableImpl)var7.getValue();
                  if (!StringUtils.isNullOrEmpty(this.getSprite().getName()) && (this.getSprite().getName().equals(var8.rawgetStr("sprite")) || this.getSprite().getName().equals(var8.rawgetStr("spriteOpen")))) {
                     return var4;
                  }
               }
            }

            return null;
         }
      }
   }

   private boolean checkNestBoxPrefPosition(int var1) {
      for(int var2 = 0; var2 < this.getMaxNestBox() + 1; ++var2) {
         IsoAnimal var3 = ((NestBox)this.nestBoxes.get(var2)).animal;
         if (var3 != null && var3.getData().getPreferredHutchPosition() == var1) {
            return true;
         }
      }

      return false;
   }

   public boolean addAnimalInside(IsoAnimal var1) {
      if (var1.getData().getPreferredHutchPosition() == -1) {
         var1.getData().setPreferredHutchPosition(Rand.Next(0, this.getMaxAnimals()));
      }

      int var2 = 0;

      while(this.animalInside.get(var1.getData().getPreferredHutchPosition()) != null || this.deadBodiesInside.get(var1.getData().getPreferredHutchPosition()) != null || this.checkNestBoxPrefPosition(var1.getData().getPreferredHutchPosition())) {
         ++var2;
         if (var2 > 100) {
            break;
         }

         var1.getData().setPreferredHutchPosition(Rand.Next(0, this.getMaxAnimals()));
      }

      if (this.animalInside.get(var1.getData().getPreferredHutchPosition()) == null) {
         this.animalInside.put(var1.getData().getPreferredHutchPosition(), var1);
         var1.hutch = this;
         var1.getData().setHutchPosition(var1.getData().getPreferredHutchPosition());
         if (this.isOwner()) {
            var1.getNetworkCharacterAI().getAnimalPacket().reset(var1);
            this.sync();
         }

         return true;
      } else {
         return false;
      }
   }

   public void addAnimalOutside(IsoAnimal var1) {
      if (!GameClient.bClient) {
         var1.hutch = null;
      }
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.linkedX = var1.getInt();
      this.linkedY = var1.getInt();
      this.linkedZ = var1.getInt();
      if (!this.isSlave()) {
         if (var2 >= 204) {
            this.spriteName = GameWindow.ReadString(var1);
            this.sprite = IsoSpriteManager.instance.getSprite(this.spriteName);
         }

         this.def = this.getDefFromSprite();
         if (this.def == null) {
            throw new IOException("hutch definition not found");
         }

         this.type = this.def.rawgetStr("name");
         this.open = var1.get() != 0;
         if (var2 >= 204) {
            this.openEggHatch = var1.get() != 0;
         }

         this.savedX = var1.getInt();
         this.savedY = var1.getInt();
         this.savedZ = var1.getInt();
         ArrayList var4 = new ArrayList();
         int var5;
         int var6;
         IsoAnimal var7;
         if (var2 >= 212) {
            var5 = var1.getInt();
            if (GameClient.bClient) {
               var1.position(var1.position() + var5);
            } else {
               byte var12 = var1.get();

               for(int var13 = 0; var13 < var12; ++var13) {
                  IsoAnimal var15 = new IsoAnimal(IsoWorld.instance.getCell());
                  boolean var16 = var1.get() == 1;
                  byte var10 = var1.get();
                  var15.load(var1, var2, var3);
                  var4.add(var15);
                  var15.removeFromSquare();
               }
            }
         } else {
            var5 = var1.get();

            for(var6 = 0; var6 < var5; ++var6) {
               var7 = new IsoAnimal(IsoWorld.instance.getCell());
               boolean var8 = var1.get() == 1;
               byte var9 = var1.get();
               var7.load(var1, var2, var3);
               var4.add(var7);
               var7.removeFromSquare();
            }
         }

         this.hutchDirt = var1.getFloat();
         this.nestBoxDirt = var1.getFloat();
         byte var11 = var1.get();

         for(var6 = 0; var6 < var11; ++var6) {
            NestBox var14 = var6 < this.nestBoxes.size() ? (NestBox)this.nestBoxes.get(var6) : new NestBox(var6);
            var14.load(var1, var2);
            if (!this.nestBoxes.containsKey(var6) && var6 <= this.getMaxNestBox()) {
               this.nestBoxes.put(var6, var14);
            }
         }

         for(var6 = 0; var6 < var4.size(); ++var6) {
            var7 = (IsoAnimal)var4.get(var6);
            this.addAnimalInside(var7);
         }
      }

   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.putInt(this.linkedX);
      var1.putInt(this.linkedY);
      var1.putInt(this.linkedZ);
      if (!this.isSlave()) {
         GameWindow.WriteString(var1, this.spriteName);
         var1.put((byte)(this.isOpen() ? 1 : 0));
         var1.put((byte)(this.isEggHatchDoorOpen() ? 1 : 0));
         var1.putInt(this.savedX);
         var1.putInt(this.savedY);
         var1.putInt(this.savedZ);
         int var3 = var1.position();
         var1.putInt(0);
         int var4 = var1.position();
         ArrayList var5 = new ArrayList(this.animalInside.values());
         var1.put((byte)var5.size());

         int var6;
         for(var6 = 0; var6 < var5.size(); ++var6) {
            ((IsoAnimal)var5.get(var6)).save(var1, var2);
         }

         var6 = var1.position();
         var1.position(var3);
         var1.putInt(var6 - var4);
         var1.position(var6);
         var1.putFloat(this.hutchDirt);
         var1.putFloat(this.nestBoxDirt);
         var1.put((byte)this.nestBoxes.size());

         for(int var7 = 0; var7 < this.nestBoxes.size(); ++var7) {
            NestBox var8 = (NestBox)this.nestBoxes.get(var7);
            var8.save(var1);
         }
      }

   }

   public boolean addMetaEgg(IsoAnimal var1) {
      for(int var2 = 0; var2 < this.getMaxNestBox() + 1; ++var2) {
         if (((NestBox)this.nestBoxes.get(var2)).animal == null && ((NestBox)this.nestBoxes.get(var2)).eggs.size() < 10) {
            ((NestBox)this.nestBoxes.get(var2)).addEgg(var1.createEgg());
            return true;
         }
      }

      return false;
   }

   public boolean isSlave() {
      return this.linkedX > 0 && this.linkedY > 0;
   }

   public String getObjectName() {
      return "IsoHutch";
   }

   public void addToWorld() {
      super.addToWorld();
      if (!this.isSlave() && !HutchManager.getInstance().checkHutchExistInList(this)) {
         HutchManager.getInstance().add(this);
      }

   }

   public void removeHutch() {
      for(int var1 = this.square.x - 2; var1 < this.square.x + 3; ++var1) {
         for(int var2 = this.square.y - 2; var2 < this.square.y + 3; ++var2) {
            IsoGridSquare var3 = this.square.getCell().getGridSquare(var1, var2, this.square.z);
            if (var3 != null) {
               ArrayList var4 = var3.getHutchTiles();

               for(int var5 = 0; var5 < var4.size(); ++var5) {
                  ((IsoHutch)var4.get(var5)).releaseAllAnimals();
                  ((IsoHutch)var4.get(var5)).dropAllEggs();
                  ((IsoHutch)var4.get(var5)).removeFromWorld();
                  ((IsoHutch)var4.get(var5)).getSquare().transmitRemoveItemFromSquare((IsoObject)var4.get(var5));
               }
            }
         }
      }

   }

   public void removeFromWorld() {
      super.removeFromWorld();
      HutchManager.getInstance().remove(this);
   }

   public void dropAllEggs() {
      Iterator var1 = this.nestBoxes.values().iterator();

      while(var1.hasNext()) {
         NestBox var2 = (NestBox)var1.next();

         for(int var3 = 0; var3 < var2.eggs.size(); ++var3) {
            this.square.AddWorldInventoryItem((InventoryItem)var2.eggs.get(var3), Rand.Next(0.0F, 0.8F), Rand.Next(0.0F, 0.8F), 0.0F);
         }
      }

   }

   public void releaseAllAnimals() {
      if (!this.animalInside.isEmpty()) {
         ArrayList var1 = new ArrayList();
         Iterator var2 = this.animalInside.values().iterator();

         while(var2.hasNext()) {
            IsoAnimal var3 = (IsoAnimal)var2.next();
            var1.add(var3);
         }

         for(int var4 = 0; var4 < var1.size(); ++var4) {
            this.releaseAnimal((IsoGridSquare)null, (IsoAnimal)var1.get(var4));
         }

      }
   }

   public HashMap<Integer, IsoAnimal> getAnimalInside() {
      return this.animalInside;
   }

   public IsoAnimal getAnimal(Integer var1) {
      return (IsoAnimal)this.animalInside.get(var1);
   }

   public IsoDeadBody getDeadBody(Integer var1) {
      return (IsoDeadBody)this.deadBodiesInside.get(var1);
   }

   public int getMaxAnimals() {
      if (this.maxAnimals == 0) {
         this.maxAnimals = this.def.rawgetInt("maxAnimals");
      }

      return this.maxAnimals;
   }

   public int getMaxNestBox() {
      if (this.maxNestBox == 0) {
         this.maxNestBox = this.def.rawgetInt("maxNestBox");
      }

      return this.maxNestBox;
   }

   public int getEnterSpotX() {
      if (this.enterSpotX == 0) {
         this.enterSpotX = this.def.rawgetInt("enterSpotX");
      }

      return this.enterSpotX;
   }

   public int getEnterSpotY() {
      if (this.enterSpotY == 0) {
         this.enterSpotY = this.def.rawgetInt("enterSpotY");
      }

      return this.enterSpotY;
   }

   public boolean haveEggHatchDoor() {
      return !StringUtils.isNullOrEmpty(this.def.rawgetStr("openHatchSprite"));
   }

   public boolean isEggHatchDoorOpen() {
      return this.openEggHatch;
   }

   public IsoGridSquare getEntrySq() {
      return this.getSquare().getCell().getGridSquare(this.getSquare().x + this.getEnterSpotX(), this.getSquare().y + this.getEnterSpotY(), this.getSquare().z);
   }

   public IsoAnimal getAnimalInNestBox(Integer var1) {
      return this.nestBoxes.get(var1) != null ? ((NestBox)this.nestBoxes.get(var1)).animal : null;
   }

   public NestBox getNestBox(Integer var1) {
      return (NestBox)this.nestBoxes.get(var1);
   }

   public float getHutchDirt() {
      return this.hutchDirt;
   }

   public void setHutchDirt(float var1) {
      this.hutchDirt = var1;
   }

   public float getNestBoxDirt() {
      return this.nestBoxDirt;
   }

   public void setNestBoxDirt(float var1) {
      this.nestBoxDirt = var1;
   }

   public boolean isDoorClosed() {
      return !this.open;
   }

   public boolean isAllDoorClosed() {
      return !this.open && !this.openEggHatch;
   }

   public boolean isOwner() {
      return !GameServer.bServer && !GameClient.bClient || GameServer.bServer;
   }

   public class NestBox {
      public IsoAnimal animal = null;
      ArrayList<Food> eggs = new ArrayList();
      public static final int maxEggs = 10;
      final int index;

      public NestBox(int var2) {
         this.index = var2;
      }

      public int getIndex() {
         return this.index;
      }

      public int getEggsNb() {
         return this.eggs.size();
      }

      public void addEgg(Food var1) {
         this.eggs.add(var1);
      }

      public Food getEgg(int var1) {
         return (Food)this.eggs.get(var1);
      }

      public Food removeEgg(int var1) {
         return (Food)this.eggs.remove(var1);
      }

      void save(ByteBuffer var1) throws IOException {
         var1.put((byte)this.eggs.size());

         for(int var2 = 0; var2 < this.eggs.size(); ++var2) {
            Food var3 = (Food)this.eggs.get(var2);
            var3.saveWithSize(var1, false);
         }

         var1.put((byte)(this.animal != null ? 1 : 0));
         if (this.animal != null) {
            this.animal.save(var1, false, false);
         }

      }

      void load(ByteBuffer var1, int var2) throws IOException {
         byte var3 = var1.get();

         for(int var4 = 0; var4 < var3; ++var4) {
            InventoryItem var5 = InventoryItem.loadItem(var1, var2);
            if (var5 instanceof Food) {
               this.eggs.add((Food)var5);
            }
         }

         boolean var6 = var1.get() != 0;
         if (var6) {
            this.animal = new IsoAnimal(IsoWorld.instance.getCell());
            this.animal.load(var1, var2);
            this.animal.nestBox = this.index;
         }

      }
   }

   class AgeComparator implements Comparator<IsoAnimal> {
      AgeComparator() {
      }

      public int compare(IsoAnimal var1, IsoAnimal var2) {
         return var2.getData().getAge() - var1.getData().getAge();
      }
   }
}
