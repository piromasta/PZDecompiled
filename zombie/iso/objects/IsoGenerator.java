package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.WorldSoundManager;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

public class IsoGenerator extends IsoObject {
   public float fuel = 0.0F;
   public boolean activated = false;
   public int condition = 0;
   private int lastHour = -1;
   public boolean connected = false;
   private int numberOfElectricalItems = 0;
   private boolean updateSurrounding = false;
   private final HashMap<String, String> itemsPowered = new HashMap();
   private float totalPowerUsing = 0.0F;
   private static final ArrayList<IsoGenerator> AllGenerators = new ArrayList();
   private static final int GENERATOR_RADIUS = 20;

   public IsoGenerator(IsoCell var1) {
      super(var1);
   }

   public IsoGenerator(InventoryItem var1, IsoCell var2, IsoGridSquare var3) {
      super(var2, var3, IsoSpriteManager.instance.getSprite(var1.getScriptItem().getWorldObjectSprite()));
      String var4 = var1.getScriptItem().getWorldObjectSprite();
      if (var1 != null) {
         this.setInfoFromItem(var1);
      }

      this.sprite = IsoSpriteManager.instance.getSprite(var4);
      this.square = var3;
      var3.AddSpecialObject(this);
      if (GameServer.bServer) {
         this.transmitCompleteItemToClients();
      }

   }

   public IsoGenerator(InventoryItem var1, IsoCell var2, IsoGridSquare var3, boolean var4) {
      super(var2, var3, IsoSpriteManager.instance.getSprite(var1.getScriptItem().getWorldObjectSprite()));
      String var5 = var1.getScriptItem().getWorldObjectSprite();
      if (var1 != null) {
         this.setInfoFromItem(var1);
      }

      this.sprite = IsoSpriteManager.instance.getSprite(var5);
      this.square = var3;
      var3.AddSpecialObject(this);
      if (GameClient.bClient && !var4) {
         this.transmitCompleteItemToServer();
      }

   }

   public void setInfoFromItem(InventoryItem var1) {
      this.condition = var1.getCondition();
      if (var1.getModData().rawget("fuel") instanceof Double) {
         this.fuel = ((Double)var1.getModData().rawget("fuel")).floatValue();
      }

      this.getModData().rawset("generatorFullType", String.valueOf(var1.getFullType()));
   }

   public void update() {
      if (this.updateSurrounding && this.getSquare() != null) {
         this.setSurroundingElectricity();
         this.updateSurrounding = false;
      }

      if (this.isActivated()) {
         if (!GameServer.bServer && (this.emitter == null || !this.emitter.isPlaying("GeneratorLoop"))) {
            if (this.emitter == null) {
               this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, (float)((int)this.getZ()));
               IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            }

            this.emitter.playSoundLoopedImpl("GeneratorLoop");
         }

         if (GameClient.bClient) {
            this.emitter.tick();
            return;
         }

         WorldSoundManager.instance.addSoundRepeating(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 20, 1, false);
         if ((int)GameTime.getInstance().getWorldAgeHours() != this.lastHour) {
            if (!this.getSquare().getProperties().Is(IsoFlagType.exterior) && this.getSquare().getBuilding() != null) {
               this.getSquare().getBuilding().setToxic(false);
               this.getSquare().getBuilding().setToxic(this.isActivated());
            }

            int var1 = (int)GameTime.getInstance().getWorldAgeHours() - this.lastHour;
            float var2 = 0.0F;
            int var3 = 0;
            int var4 = 30;
            if (this.getModData().rawget("generatorFullType") != null && this.getModData().rawget("generatorFullType") instanceof String) {
               String var5 = (String)this.getModData().rawget("generatorFullType");
               if (var5 != null && ScriptManager.instance.getItem(var5) != null) {
                  Item var6 = ScriptManager.instance.getItem(var5);
                  var4 = var6.getConditionLowerChance();
               }
            }

            for(int var7 = 0; var7 < var1; ++var7) {
               float var9 = this.totalPowerUsing;
               var9 = (float)((double)var9 * SandboxOptions.instance.GeneratorFuelConsumption.getValue());
               var2 += var9;
               if (Rand.Next(var4) == 0) {
                  var3 += Rand.Next(2) + 1;
               }

               if (this.fuel - var2 <= 0.0F || this.condition - var3 <= 0) {
                  break;
               }
            }

            this.fuel -= var2;
            if (this.fuel <= 0.0F) {
               this.setActivated(false);
               this.fuel = 0.0F;
            }

            this.condition -= var3;
            if (this.condition <= 0) {
               this.setActivated(false);
               this.condition = 0;
            }

            boolean var8 = false;
            if (this.condition <= 20) {
               var8 = Rand.Next(5) == 0;
            } else if (this.condition <= 30) {
               var8 = Rand.Next(10) == 0;
            } else if (this.condition <= 40) {
               var8 = Rand.Next(15) == 0;
            }

            if (var8) {
               this.emitter.playSound("GeneratorBackfire");
               WorldSoundManager.instance.addSound(this, this.square.getX(), this.square.getY(), this.square.getZ(), 40, 60, false, 0.0F, 15.0F);
            }

            if (this.condition <= 20) {
               if (Rand.Next(10) == 0) {
                  IsoFireManager.StartFire(this.getCell(), this.square, true, 1000);
                  this.condition = 0;
                  this.setActivated(false);
               } else if (Rand.Next(20) == 0) {
                  this.square.explode();
                  this.condition = 0;
                  this.setActivated(false);
               }
            }

            this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
            if (GameServer.bServer) {
               this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
            }
         }
      }

      if (this.emitter != null) {
         this.emitter.tick();
      }

   }

   public void setSurroundingElectricity() {
      this.itemsPowered.clear();
      this.totalPowerUsing = 0.02F;
      this.numberOfElectricalItems = 1;
      if (this.square != null && this.square.chunk != null) {
         int var1 = this.square.chunk.wx;
         int var2 = this.square.chunk.wy;

         int var4;
         for(int var3 = -2; var3 <= 2; ++var3) {
            for(var4 = -2; var4 <= 2; ++var4) {
               IsoChunk var5 = GameServer.bServer ? ServerMap.instance.getChunk(var1 + var4, var2 + var3) : IsoWorld.instance.CurrentCell.getChunk(var1 + var4, var2 + var3);
               if (var5 != null && this.touchesChunk(var5)) {
                  if (this.isActivated()) {
                     var5.addGeneratorPos(this.square.x, this.square.y, this.square.z);
                  } else {
                     var5.removeGeneratorPos(this.square.x, this.square.y, this.square.z);
                  }
               }
            }
         }

         boolean var19 = SandboxOptions.getInstance().AllowExteriorGenerator.getValue();
         var4 = this.square.getX() - 20;
         int var20 = this.square.getX() + 20;
         int var6 = this.square.getY() - 20;
         int var7 = this.square.getY() + 20;
         int var8 = Math.max(0, this.getSquare().getZ() - 3);
         int var9 = Math.min(8, this.getSquare().getZ() + 3);

         for(int var10 = var8; var10 < var9; ++var10) {
            for(int var11 = var4; var11 <= var20; ++var11) {
               for(int var12 = var6; var12 <= var7; ++var12) {
                  if (!(IsoUtils.DistanceToSquared((float)var11 + 0.5F, (float)var12 + 0.5F, (float)this.getSquare().getX() + 0.5F, (float)this.getSquare().getY() + 0.5F) > 400.0F)) {
                     IsoGridSquare var13 = this.getCell().getGridSquare(var11, var12, var10);
                     if (var13 != null) {
                        boolean var14 = this.isActivated();
                        if (!var19 && var13.Is(IsoFlagType.exterior)) {
                           var14 = false;
                        }

                        for(int var15 = 0; var15 < var13.getObjects().size(); ++var15) {
                           IsoObject var16 = (IsoObject)var13.getObjects().get(var15);
                           if (var16 != null && !(var16 instanceof IsoWorldInventoryObject)) {
                              if (var16 instanceof IsoClothingDryer && ((IsoClothingDryer)var16).isActivated()) {
                                 this.addPoweredItem(var16, 0.09F);
                              }

                              if (var16 instanceof IsoClothingWasher && ((IsoClothingWasher)var16).isActivated()) {
                                 this.addPoweredItem(var16, 0.09F);
                              }

                              if (var16 instanceof IsoCombinationWasherDryer && ((IsoCombinationWasherDryer)var16).isActivated()) {
                                 this.addPoweredItem(var16, 0.09F);
                              }

                              if (var16 instanceof IsoStackedWasherDryer) {
                                 IsoStackedWasherDryer var17 = (IsoStackedWasherDryer)var16;
                                 float var18 = 0.0F;
                                 if (var17.isDryerActivated()) {
                                    var18 += 0.9F;
                                 }

                                 if (var17.isWasherActivated()) {
                                    var18 += 0.9F;
                                 }

                                 if (var18 > 0.0F) {
                                    this.addPoweredItem(var16, var18);
                                 }
                              }

                              if (var16 instanceof IsoTelevision && ((IsoTelevision)var16).getDeviceData().getIsTurnedOn()) {
                                 this.addPoweredItem(var16, 0.03F);
                              }

                              if (var16 instanceof IsoRadio && ((IsoRadio)var16).getDeviceData().getIsTurnedOn() && !((IsoRadio)var16).getDeviceData().getIsBatteryPowered()) {
                                 this.addPoweredItem(var16, 0.01F);
                              }

                              if (var16 instanceof IsoStove && ((IsoStove)var16).Activated()) {
                                 this.addPoweredItem(var16, 0.09F);
                              }

                              boolean var21 = var16.getContainerByType("fridge") != null;
                              boolean var22 = var16.getContainerByType("freezer") != null;
                              if (var21 && var22) {
                                 this.addPoweredItem(var16, 0.13F);
                              } else if (var21 || var22) {
                                 this.addPoweredItem(var16, 0.08F);
                              }

                              if (var16 instanceof IsoLightSwitch && ((IsoLightSwitch)var16).Activated && !((IsoLightSwitch)var16).bStreetLight) {
                                 this.addPoweredItem(var16, 0.002F);
                              }

                              if (var16.getPipedFuelAmount() > 0) {
                                 this.addPoweredItem(var16, 0.03F);
                              }

                              var16.checkHaveElectricity();
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private void addPoweredItem(IsoObject var1, float var2) {
      String var3 = Translator.getText("IGUI_VehiclePartCatOther");
      if (var1.getPipedFuelAmount() > 0) {
         var3 = Translator.getText("IGUI_GasPump");
      }

      PropertyContainer var4 = var1.getProperties();
      if (var4 != null && var4.Is("CustomName")) {
         String var5 = "Moveable Object";
         if (var4.Is("CustomName")) {
            if (var4.Is("GroupName")) {
               String var10000 = var4.Val("GroupName");
               var5 = var10000 + " " + var4.Val("CustomName");
            } else {
               var5 = var4.Val("CustomName");
            }
         }

         var3 = Translator.getMoveableDisplayName(var5);
      }

      if (var1 instanceof IsoLightSwitch) {
         var3 = Translator.getText("IGUI_Lights");
      }

      int var8 = 1;
      Iterator var6 = this.itemsPowered.keySet().iterator();

      while(var6.hasNext()) {
         String var7 = (String)var6.next();
         if (var7.startsWith(var3)) {
            var8 = Integer.parseInt(var7.replaceAll("[\\D]", ""));
            this.totalPowerUsing -= var2 * (float)var8;
            ++var8;
            this.itemsPowered.remove(var7);
            break;
         }
      }

      this.itemsPowered.put(var3 + " x" + var8, (new DecimalFormat(" (#.### L/h)")).format((double)(var2 * (float)var8)));
      this.totalPowerUsing += var2 * (float)var8;
   }

   private void updateFridgeFreezerItems(IsoObject var1) {
      for(int var2 = 0; var2 < var1.getContainerCount(); ++var2) {
         ItemContainer var3 = var1.getContainerByIndex(var2);
         if ("fridge".equals(var3.getType()) || "freezer".equals(var3.getType())) {
            ArrayList var4 = var3.getItems();

            for(int var5 = 0; var5 < var4.size(); ++var5) {
               InventoryItem var6 = (InventoryItem)var4.get(var5);
               if (var6 instanceof Food) {
                  var6.updateAge();
               }
            }
         }
      }

   }

   private void updateFridgeFreezerItems(IsoGridSquare var1) {
      int var2 = var1.getObjects().size();
      IsoObject[] var3 = (IsoObject[])var1.getObjects().getElements();

      for(int var4 = 0; var4 < var2; ++var4) {
         IsoObject var5 = var3[var4];
         this.updateFridgeFreezerItems(var5);
      }

   }

   private void updateFridgeFreezerItems() {
      if (this.square != null) {
         int var1 = this.square.getX() - 20;
         int var2 = this.square.getX() + 20;
         int var3 = this.square.getY() - 20;
         int var4 = this.square.getY() + 20;
         int var5 = Math.max(0, this.square.getZ() - 3);
         int var6 = Math.min(8, this.square.getZ() + 3);

         for(int var7 = var5; var7 < var6; ++var7) {
            for(int var8 = var1; var8 <= var2; ++var8) {
               for(int var9 = var3; var9 <= var4; ++var9) {
                  if (IsoUtils.DistanceToSquared((float)var8, (float)var9, (float)this.square.x, (float)this.square.y) <= 400.0F) {
                     IsoGridSquare var10 = this.getCell().getGridSquare(var8, var9, var7);
                     if (var10 != null) {
                        this.updateFridgeFreezerItems(var10);
                     }
                  }
               }
            }
         }

      }
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.connected = var1.get() == 1;
      this.activated = var1.get() == 1;
      this.fuel = var1.getFloat();
      this.condition = var1.getInt();
      this.lastHour = var1.getInt();
      this.numberOfElectricalItems = var1.getInt();
      this.updateSurrounding = true;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)(this.isConnected() ? 1 : 0));
      var1.put((byte)(this.isActivated() ? 1 : 0));
      var1.putFloat(this.getFuel());
      var1.putInt(this.getCondition());
      var1.putInt(this.lastHour);
      var1.putInt(this.numberOfElectricalItems);
   }

   public void remove() {
      if (this.getSquare() != null) {
         this.getSquare().transmitRemoveItemFromSquare(this);
      }
   }

   public void addToWorld() {
      this.getCell().addToProcessIsoObject(this);
      if (!AllGenerators.contains(this)) {
         AllGenerators.add(this);
      }

   }

   public void removeFromWorld() {
      AllGenerators.remove(this);
      if (this.emitter != null) {
         this.emitter.stopAll();
         IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
         this.emitter = null;
      }

      super.removeFromWorld();
   }

   public String getObjectName() {
      return "IsoGenerator";
   }

   public float getFuel() {
      return this.fuel;
   }

   public void setFuel(float var1) {
      this.fuel = var1;
      if (this.fuel > 100.0F) {
         this.fuel = 100.0F;
      }

      if (this.fuel < 0.0F) {
         this.fuel = 0.0F;
      }

      if (GameServer.bServer) {
         this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
      }

      if (GameClient.bClient) {
         this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
      }

   }

   public boolean isActivated() {
      return this.activated;
   }

   public void setActivated(boolean var1) {
      if (var1 != this.activated) {
         if (!this.getSquare().getProperties().Is(IsoFlagType.exterior) && this.getSquare().getBuilding() != null) {
            this.getSquare().getBuilding().setToxic(false);
            this.getSquare().getBuilding().setToxic(var1);
         }

         if (!GameServer.bServer && this.emitter == null) {
            this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
         }

         if (var1) {
            this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
            if (this.emitter != null) {
               this.emitter.playSound("GeneratorStarting");
            }
         } else if (this.emitter != null) {
            if (!this.emitter.isEmpty()) {
               this.emitter.stopAll();
            }

            this.emitter.playSound("GeneratorStopping");
         }

         try {
            this.updateFridgeFreezerItems();
         } catch (Throwable var3) {
            ExceptionLogger.logException(var3);
         }

         this.activated = var1;
         this.setSurroundingElectricity();
         if (GameClient.bClient) {
            this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
         }

         if (GameServer.bServer) {
            this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
         }

      }
   }

   public void failToStart() {
      if (!GameServer.bServer) {
         if (this.emitter == null) {
            this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
         }

         this.emitter.playSound("GeneratorFailedToStart");
      }
   }

   public int getCondition() {
      return this.condition;
   }

   public void setCondition(int var1) {
      this.condition = var1;
      if (this.condition > 100) {
         this.condition = 100;
      }

      if (this.condition < 0) {
         this.condition = 0;
      }

      if (GameServer.bServer) {
         this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
      }

      if (GameClient.bClient) {
         this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
      }

   }

   public boolean isConnected() {
      return this.connected;
   }

   public void setConnected(boolean var1) {
      this.connected = var1;
      if (GameClient.bClient) {
         this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
      }

      if (GameServer.bServer) {
         this.syncIsoObject(false, (byte)0, (UdpConnection)null, (ByteBuffer)null);
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
      var1.putFloat(this.fuel);
      var1.putInt(this.condition);
      var1.putByte((byte)(this.activated ? 1 : 0));
      var1.putByte((byte)(this.connected ? 1 : 0));
   }

   public void syncIsoObjectReceive(ByteBuffer var1) {
      float var2 = var1.getFloat();
      int var3 = var1.getInt();
      boolean var4 = var1.get() == 1;
      boolean var5 = var1.get() == 1;
      this.fuel = var2;
      this.condition = var3;
      this.connected = var5;
      if (this.activated != var4) {
         try {
            this.updateFridgeFreezerItems();
         } catch (Throwable var7) {
            ExceptionLogger.logException(var7);
         }

         this.activated = var4;
         if (var4) {
            this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
         } else if (this.emitter != null) {
            this.emitter.stopAll();
         }

         this.setSurroundingElectricity();
      }

   }

   private boolean touchesChunk(IsoChunk var1) {
      IsoGridSquare var2 = this.getSquare();

      assert var2 != null;

      if (var2 == null) {
         return false;
      } else {
         int var3 = var1.wx * 8;
         int var4 = var1.wy * 8;
         int var5 = var3 + 8 - 1;
         int var6 = var4 + 8 - 1;
         if (var2.x - 20 > var5) {
            return false;
         } else if (var2.x + 20 < var3) {
            return false;
         } else if (var2.y - 20 > var6) {
            return false;
         } else {
            return var2.y + 20 >= var4;
         }
      }
   }

   public static void chunkLoaded(IsoChunk var0) {
      var0.checkForMissingGenerators();

      int var1;
      for(var1 = -2; var1 <= 2; ++var1) {
         for(int var2 = -2; var2 <= 2; ++var2) {
            if (var2 != 0 || var1 != 0) {
               IsoChunk var3 = GameServer.bServer ? ServerMap.instance.getChunk(var0.wx + var2, var0.wy + var1) : IsoWorld.instance.CurrentCell.getChunk(var0.wx + var2, var0.wy + var1);
               if (var3 != null) {
                  var3.checkForMissingGenerators();
               }
            }
         }
      }

      for(var1 = 0; var1 < AllGenerators.size(); ++var1) {
         IsoGenerator var4 = (IsoGenerator)AllGenerators.get(var1);
         if (!var4.updateSurrounding && var4.touchesChunk(var0)) {
            var4.updateSurrounding = true;
         }
      }

   }

   public static void updateSurroundingNow() {
      for(int var0 = 0; var0 < AllGenerators.size(); ++var0) {
         IsoGenerator var1 = (IsoGenerator)AllGenerators.get(var0);
         if (var1.updateSurrounding && var1.getSquare() != null) {
            var1.updateSurrounding = false;
            var1.setSurroundingElectricity();
         }
      }

   }

   public static void updateGenerator(IsoGridSquare var0) {
      if (var0 != null) {
         for(int var1 = 0; var1 < AllGenerators.size(); ++var1) {
            IsoGenerator var2 = (IsoGenerator)AllGenerators.get(var1);
            if (var2.getSquare() != null) {
               float var3 = IsoUtils.DistanceToSquared((float)var0.x + 0.5F, (float)var0.y + 0.5F, (float)var2.getSquare().getX() + 0.5F, (float)var2.getSquare().getY() + 0.5F);
               if (var3 <= 400.0F) {
                  var2.updateSurrounding = true;
               }
            }
         }

      }
   }

   public static void Reset() {
      assert AllGenerators.isEmpty();

      AllGenerators.clear();
   }

   public static boolean isPoweringSquare(int var0, int var1, int var2, int var3, int var4, int var5) {
      int var6 = Math.max(0, var2 - 3);
      int var7 = Math.min(8, var2 + 3);
      if (var5 >= var6 && var5 < var7) {
         return IsoUtils.DistanceToSquared((float)var0 + 0.5F, (float)var1 + 0.5F, (float)var3 + 0.5F, (float)var4 + 0.5F) <= 400.0F;
      } else {
         return false;
      }
   }

   public ArrayList<String> getItemsPowered() {
      ArrayList var1 = new ArrayList();
      Iterator var2 = this.itemsPowered.keySet().iterator();

      while(var2.hasNext()) {
         String var3 = (String)var2.next();
         var1.add(var3 + (String)this.itemsPowered.get(var3));
      }

      var1.sort(String::compareToIgnoreCase);
      return var1;
   }

   public float getTotalPowerUsing() {
      return this.totalPowerUsing;
   }

   public void setTotalPowerUsing(float var1) {
      this.totalPowerUsing = var1;
   }
}
