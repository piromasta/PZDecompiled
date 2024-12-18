package zombie.iso.objects;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.RoomID;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public class IsoLightSwitch extends IsoObject {
   boolean Activated = false;
   public final ArrayList<IsoLightSource> lights = new ArrayList();
   public boolean lightRoom = false;
   public long RoomID = -1L;
   public boolean bStreetLight = false;
   private boolean canBeModified = false;
   private boolean useBattery = false;
   private boolean hasBattery = false;
   private String bulbItem = "Base.LightBulb";
   private float power = 0.0F;
   private float delta = 2.5E-4F;
   private float primaryR = 1.0F;
   private float primaryG = 1.0F;
   private float primaryB = 1.0F;
   private static final ArrayList<IsoObject> s_tempObjects = new ArrayList();
   protected long lastMinuteStamp = -1L;
   protected int bulbBurnMinutes = -1;
   protected int lastMin = 0;
   protected int nextBreakUpdate = 60;

   public String getObjectName() {
      return "LightSwitch";
   }

   public IsoLightSwitch(IsoCell var1) {
      super(var1);
   }

   public IsoLightSwitch(IsoCell var1, IsoGridSquare var2, IsoSprite var3, long var4) {
      super(var1, var2, var3);
      this.RoomID = var4;
      if (var3 != null && var3.getProperties().Is("lightR")) {
         if (var3.getProperties().Is("IsMoveAble")) {
            this.canBeModified = true;
         }

         this.primaryR = Float.parseFloat(var3.getProperties().Val("lightR")) / 255.0F;
         this.primaryG = Float.parseFloat(var3.getProperties().Val("lightG")) / 255.0F;
         this.primaryB = Float.parseFloat(var3.getProperties().Val("lightB")) / 255.0F;
      } else {
         this.lightRoom = true;
      }

      this.bStreetLight = var3 != null && var3.getProperties().Is("streetlight");
      IsoRoom var6 = this.square.getRoom();
      if (var6 != null && this.lightRoom) {
         if (!var2.haveElectricity() && !IsoWorld.instance.isHydroPowerOn()) {
            var6.def.bLightsActive = false;
         }

         this.Activated = var6.def.bLightsActive;
         var6.lightSwitches.add(this);
      } else {
         this.Activated = true;
      }

   }

   public void addLightSourceFromSprite() {
      if (this.sprite != null && this.sprite.getProperties().Is("lightR")) {
         float var1 = Float.parseFloat(this.sprite.getProperties().Val("lightR")) / 255.0F;
         float var2 = Float.parseFloat(this.sprite.getProperties().Val("lightG")) / 255.0F;
         float var3 = Float.parseFloat(this.sprite.getProperties().Val("lightB")) / 255.0F;
         this.Activated = false;
         this.setActive(true, true);
         int var4 = 10;
         if (this.sprite.getProperties().Is("LightRadius") && Integer.parseInt(this.sprite.getProperties().Val("LightRadius")) > 0) {
            var4 = Integer.parseInt(this.sprite.getProperties().Val("LightRadius"));
         }

         IsoLightSource var5 = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), var1, var2, var3, var4);
         var5.bActive = this.Activated;
         var5.bHydroPowered = true;
         var5.switches.add(this);
         this.lights.add(var5);
      }

   }

   public boolean getCanBeModified() {
      return this.canBeModified;
   }

   public void setCanBeModified(boolean var1) {
      this.canBeModified = var1;
   }

   public float getPower() {
      return this.power;
   }

   public void setPower(float var1) {
      this.power = var1;
   }

   public void setDelta(float var1) {
      this.delta = var1;
   }

   public float getDelta() {
      return this.delta;
   }

   public void setUseBattery(boolean var1) {
      this.setActive(false);
      this.useBattery = var1;
      if (GameServer.bServer) {
         this.syncCustomizedSettings((UdpConnection)null);
      }

   }

   public void setUseBatteryDirect(boolean var1) {
      this.useBattery = var1;
   }

   public boolean getUseBattery() {
      return this.useBattery;
   }

   public boolean getHasBattery() {
      return this.hasBattery;
   }

   public void setHasBattery(boolean var1) {
      this.hasBattery = var1;
   }

   public void setHasBatteryRaw(boolean var1) {
      this.hasBattery = var1;
   }

   public void addBattery(IsoGameCharacter var1, InventoryItem var2) {
      if (this.canBeModified && this.useBattery && !this.hasBattery && var2 != null && var2.getFullType().equals("Base.Battery")) {
         this.power = ((DrainableComboItem)var2).getCurrentUsesFloat();
         this.hasBattery = true;
         var1.removeFromHands(var2);
         var1.getInventory().Remove(var2);
         if (GameServer.bServer) {
            GameServer.sendRemoveItemFromContainer(var1.getInventory(), var2);
            this.syncCustomizedSettings((UdpConnection)null);
         }
      }

   }

   public DrainableComboItem removeBattery(IsoGameCharacter var1) {
      if (this.canBeModified && this.useBattery && this.hasBattery) {
         DrainableComboItem var2 = (DrainableComboItem)InventoryItemFactory.CreateItem("Base.Battery");
         if (var2 != null) {
            this.hasBattery = false;
            var2.setCurrentUses(this.power >= 0.0F ? (int)((float)var2.getMaxUses() * this.power) : 0);
            this.power = 0.0F;
            this.setActive(false, false, true);
            var1.getInventory().AddItem((InventoryItem)var2);
            if (GameServer.bServer) {
               GameServer.sendAddItemToContainer(var1.getInventory(), var2);
               this.syncCustomizedSettings((UdpConnection)null);
            }

            return var2;
         }
      }

      return null;
   }

   public boolean hasLightBulb() {
      return this.bulbItem != null;
   }

   public String getBulbItem() {
      return this.bulbItem;
   }

   public void setBulbItemRaw(String var1) {
      this.bulbItem = var1;
   }

   public void addLightBulb(IsoGameCharacter var1, InventoryItem var2) {
      if (!this.hasLightBulb() && var2 != null && var2.getType().startsWith("LightBulb")) {
         IsoLightSource var3 = this.getPrimaryLight();
         if (var3 != null) {
            this.setPrimaryR(var2.getColorRed());
            this.setPrimaryG(var2.getColorGreen());
            this.setPrimaryB(var2.getColorBlue());
            this.bulbItem = var2.getFullType();
            var1.removeFromHands(var2);
            var1.getInventory().Remove(var2);
            if (GameServer.bServer) {
               GameServer.sendRemoveItemFromContainer(var1.getInventory(), var2);
               this.syncCustomizedSettings((UdpConnection)null);
            }
         }
      }

   }

   public InventoryItem removeLightBulb(IsoGameCharacter var1) {
      IsoLightSource var2 = this.getPrimaryLight();
      if (var2 != null && this.hasLightBulb()) {
         InventoryItem var3 = InventoryItemFactory.CreateItem(this.bulbItem);
         if (var3 != null) {
            var3.setColorRed(this.getPrimaryR());
            var3.setColorGreen(this.getPrimaryG());
            var3.setColorBlue(this.getPrimaryB());
            var3.setColor(new Color(var2.r, var2.g, var2.b));
            this.bulbItem = null;
            var1.getInventory().AddItem(var3);
            if (GameServer.bServer) {
               GameServer.sendAddItemToContainer(var1.getInventory(), var3);
            }

            this.setActive(false, false, true);
            if (GameServer.bServer) {
               this.syncCustomizedSettings((UdpConnection)null);
            }

            return var3;
         }
      }

      return null;
   }

   private IsoLightSource getPrimaryLight() {
      return this.lights.size() > 0 ? (IsoLightSource)this.lights.get(0) : null;
   }

   public float getPrimaryR() {
      return this.getPrimaryLight() != null ? this.getPrimaryLight().r : this.primaryR;
   }

   public float getPrimaryG() {
      return this.getPrimaryLight() != null ? this.getPrimaryLight().g : this.primaryG;
   }

   public float getPrimaryB() {
      return this.getPrimaryLight() != null ? this.getPrimaryLight().b : this.primaryB;
   }

   public void setPrimaryR(float var1) {
      this.primaryR = var1;
      if (this.getPrimaryLight() != null) {
         this.getPrimaryLight().r = var1;
      }

   }

   public void setPrimaryG(float var1) {
      this.primaryG = var1;
      if (this.getPrimaryLight() != null) {
         this.getPrimaryLight().g = var1;
      }

   }

   public void setPrimaryB(float var1) {
      this.primaryB = var1;
      if (this.getPrimaryLight() != null) {
         this.getPrimaryLight().b = var1;
      }

   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.lightRoom = var1.get() == 1;
      if (var2 >= 206) {
         this.RoomID = var1.getLong();
      } else {
         int var4 = var1.getInt();
         this.RoomID = zombie.iso.RoomID.makeID(this.square.x / IsoCell.CellSizeInSquares, this.square.y / IsoCell.CellSizeInSquares, var4);
      }

      this.Activated = var1.get() == 1;
      this.canBeModified = var1.get() == 1;
      if (this.canBeModified) {
         this.useBattery = var1.get() == 1;
         this.hasBattery = var1.get() == 1;
         if (var1.get() == 1) {
            this.bulbItem = GameWindow.ReadString(var1);
         } else {
            this.bulbItem = null;
         }

         this.power = var1.getFloat();
         this.delta = var1.getFloat();
         this.setPrimaryR(var1.getFloat());
         this.setPrimaryG(var1.getFloat());
         this.setPrimaryB(var1.getFloat());
      }

      this.lastMinuteStamp = var1.getLong();
      this.bulbBurnMinutes = var1.getInt();
      this.bStreetLight = this.sprite != null && this.sprite.getProperties().Is("streetlight");
      if (this.square != null) {
         IsoRoom var10 = this.square.getRoom();
         if (var10 != null && this.lightRoom) {
            this.Activated = var10.def.bLightsActive;
            var10.lightSwitches.add(this);
         } else {
            float var5 = 0.9F;
            float var6 = 0.8F;
            float var7 = 0.7F;
            if (this.sprite != null && this.sprite.getProperties().Is("lightR")) {
               if (this.canBeModified) {
                  var5 = this.primaryR;
                  var6 = this.primaryG;
                  var7 = this.primaryB;
               } else {
                  var5 = Float.parseFloat(this.sprite.getProperties().Val("lightR")) / 255.0F;
                  var6 = Float.parseFloat(this.sprite.getProperties().Val("lightG")) / 255.0F;
                  var7 = Float.parseFloat(this.sprite.getProperties().Val("lightB")) / 255.0F;
                  this.primaryR = var5;
                  this.primaryG = var6;
                  this.primaryB = var7;
               }
            }

            int var8 = 8;
            if (this.sprite.getProperties().Is("LightRadius") && Integer.parseInt(this.sprite.getProperties().Val("LightRadius")) > 0) {
               var8 = Integer.parseInt(this.sprite.getProperties().Val("LightRadius"));
            }

            IsoLightSource var9 = new IsoLightSource((int)this.getX(), (int)this.getY(), (int)this.getZ(), var5, var6, var7, var8);
            var9.bActive = this.Activated;
            var9.bWasActive = var9.bActive;
            var9.bHydroPowered = true;
            var9.switches.add(this);
            this.lights.add(var9);
         }

      }
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)(this.lightRoom ? 1 : 0));
      var1.putLong(this.RoomID);
      var1.put((byte)(this.Activated ? 1 : 0));
      var1.put((byte)(this.canBeModified ? 1 : 0));
      if (this.canBeModified) {
         var1.put((byte)(this.useBattery ? 1 : 0));
         var1.put((byte)(this.hasBattery ? 1 : 0));
         var1.put((byte)(this.hasLightBulb() ? 1 : 0));
         if (this.hasLightBulb()) {
            GameWindow.WriteString(var1, this.bulbItem);
         }

         var1.putFloat(this.power);
         var1.putFloat(this.delta);
         var1.putFloat(this.getPrimaryR());
         var1.putFloat(this.getPrimaryG());
         var1.putFloat(this.getPrimaryB());
      }

      var1.putLong(this.lastMinuteStamp);
      var1.putInt(this.bulbBurnMinutes);
   }

   public boolean onMouseLeftClick(int var1, int var2) {
      return false;
   }

   public boolean canSwitchLight() {
      if (this.bulbItem != null) {
         boolean var1 = this.hasElectricityAround();
         if (!this.useBattery && var1 || this.canBeModified && this.useBattery && this.hasBattery && this.power > 0.0F) {
            return true;
         }
      }

      return false;
   }

   private boolean hasElectricityAround() {
      if (this.getObjectIndex() == -1) {
         return false;
      } else {
         boolean var1 = IsoWorld.instance.isHydroPowerOn();
         boolean var2 = var1 ? this.isBuildingSquare(this.square) || this.hasFasciaAdjacentToBuildingSquare(this.square) || this.bStreetLight : this.square.haveElectricity();
         if (!var2 && this.getCell() != null) {
            for(int var3 = 0; var3 >= (this.getZ() >= 1.0F ? -1 : 0); --var3) {
               for(int var4 = -1; var4 < 2; ++var4) {
                  for(int var5 = -1; var5 < 2; ++var5) {
                     if (var4 != 0 || var5 != 0 || var3 != 0) {
                        IsoGridSquare var6 = this.getCell().getGridSquare((double)(this.getX() + (float)var4), (double)(this.getY() + (float)var5), (double)(this.getZ() + (float)var3));
                        if (var6 != null) {
                           if (var1 && (this.isBuildingSquare(var6) || this.hasFasciaAdjacentToBuildingSquare(var6))) {
                              return true;
                           }

                           if (var6.haveElectricity()) {
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }

         return var2;
      }
   }

   private boolean isBuildingSquare(IsoGridSquare var1) {
      if (var1 == null) {
         return false;
      } else if (var1.getRoom() != null) {
         return true;
      } else {
         return var1.getRoofHideBuilding() != null;
      }
   }

   private boolean hasFasciaAdjacentToBuildingSquare(IsoGridSquare var1) {
      IsoObject[] var2 = (IsoObject[])var1.getObjects().getElements();
      int var3 = 0;

      for(int var4 = var1.getObjects().size(); var3 < var4; ++var3) {
         IsoObject var5 = var2[var3];
         if (var5.isFascia()) {
            IsoGridSquare var6 = var5.getFasciaAttachedSquare();
            return this.isBuildingSquare(var6);
         }
      }

      return false;
   }

   public boolean setActive(boolean var1) {
      return this.setActive(var1, false, false);
   }

   public boolean setActive(boolean var1, boolean var2) {
      return this.setActive(var1, var2, false);
   }

   public boolean setActive(boolean var1, boolean var2, boolean var3) {
      if (this.bulbItem == null) {
         var1 = false;
      }

      if (var1 == this.Activated) {
         return this.Activated;
      } else if (this.square.getRoom() == null && !this.canBeModified) {
         return this.Activated;
      } else {
         if (var3 || this.canSwitchLight()) {
            this.Activated = var1;
            if (!var2) {
               this.switchLight(this.Activated);
               LightingJNI.doInvalidateGlobalLights(IsoPlayer.getPlayerIndex());
               this.syncIsoObject(false, (byte)(this.Activated ? 1 : 0), (UdpConnection)null);
            }
         }

         return this.Activated;
      }
   }

   public boolean toggle() {
      return this.setActive(!this.Activated);
   }

   public void switchLight(boolean var1) {
      int var2;
      if (this.lightRoom && this.square.getRoom() != null) {
         this.square.getRoom().def.bLightsActive = var1;

         for(var2 = 0; var2 < this.square.getRoom().lightSwitches.size(); ++var2) {
            ((IsoLightSwitch)this.square.getRoom().lightSwitches.get(var2)).Activated = var1;
         }

         if (GameServer.bServer) {
            var2 = this.square.getX() / IsoCell.CellSizeInSquares;
            int var3 = this.square.getY() / IsoCell.CellSizeInSquares;
            long var4 = this.square.getRoom().def.ID;
            GameServer.sendMetaGrid(var2, var3, zombie.iso.RoomID.getIndex(var4));
         }
      }

      for(var2 = 0; var2 < this.lights.size(); ++var2) {
         IsoLightSource var6 = (IsoLightSource)this.lights.get(var2);
         var6.bActive = var1;
      }

      this.getSpriteGridObjects(s_tempObjects);
      if (!s_tempObjects.isEmpty()) {
         for(var2 = 0; var2 < s_tempObjects.size(); ++var2) {
            IsoObject var7 = (IsoObject)s_tempObjects.get(var2);
            if (var7 != this) {
               if (var7 instanceof IsoLightSwitch) {
                  IsoLightSwitch var8 = (IsoLightSwitch)var7;
                  if (var8.isActivated() != var1) {
                     var8.setActive(var1);
                  }
               } else if (var7.getLightSource() != null) {
                  var7.checkLightSourceActive();
               }
            }
         }
      }

      IsoGridSquare.RecalcLightTime = -1.0F;
      ++Core.dirtyGlobalLightsCount;
      GameTime.instance.lightSourceUpdate = 100.0F;
      LightingJNI.doInvalidateGlobalLights(IsoPlayer.getPlayerIndex());
      IsoGenerator.updateGenerator(this.getSquare());
   }

   public void getCustomSettingsFromItem(InventoryItem var1) {
      if (var1 instanceof Moveable var2) {
         if (var2.isLight()) {
            this.useBattery = var2.isLightUseBattery();
            this.hasBattery = var2.isLightHasBattery();
            this.bulbItem = var2.getLightBulbItem();
            this.power = var2.getLightPower();
            this.delta = var2.getLightDelta();
            this.setPrimaryR(var2.getLightR());
            this.setPrimaryG(var2.getLightG());
            this.setPrimaryB(var2.getLightB());
         }
      }

   }

   public void setCustomSettingsToItem(InventoryItem var1) {
      if (var1 instanceof Moveable var2) {
         var2.setLightUseBattery(this.useBattery);
         var2.setLightHasBattery(this.hasBattery);
         var2.setLightBulbItem(this.bulbItem);
         var2.setLightPower(this.power);
         var2.setLightDelta(this.delta);
         var2.setLightR(this.primaryR);
         var2.setLightG(this.primaryG);
         var2.setLightB(this.primaryB);
      }

   }

   public void syncCustomizedSettings(UdpConnection var1) {
      if (GameClient.bClient) {
         INetworkPacket.send(PacketTypes.PacketType.SyncCustomLightSettings, this);
      } else if (GameServer.bServer) {
         INetworkPacket.sendToAll(PacketTypes.PacketType.SyncCustomLightSettings, var1, this);
      }

   }

   public void syncIsoObjectSend(ByteBufferWriter var1) {
      var1.putInt(this.square.getX());
      var1.putInt(this.square.getY());
      var1.putInt(this.square.getZ());
      byte var2 = (byte)this.square.getObjects().indexOf(this);
      var1.putByte(var2);
      var1.putByte((byte)1);
      var1.putByte((byte)(this.Activated ? 1 : 0));
   }

   public void syncIsoObject(boolean var1, byte var2, UdpConnection var3, ByteBuffer var4) {
      this.syncIsoObject(var1, var2, var3);
   }

   public void syncIsoObject(boolean var1, byte var2, UdpConnection var3) {
      if (this.square == null) {
         System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
      } else if (this.getObjectIndex() == -1) {
         PrintStream var10000 = System.out;
         String var10001 = this.getClass().getSimpleName();
         var10000.println("ERROR: " + var10001 + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
      } else {
         if (GameServer.bServer) {
            Iterator var4 = GameServer.udpEngine.connections.iterator();

            while(var4.hasNext()) {
               UdpConnection var5 = (UdpConnection)var4.next();
               ByteBufferWriter var6;
               if (var3 != null) {
                  if (var5.getConnectedGUID() != var3.getConnectedGUID()) {
                     var6 = var5.startPacket();
                     PacketTypes.PacketType.SyncIsoObject.doPacket(var6);
                     this.syncIsoObjectSend(var6);
                     PacketTypes.PacketType.SyncIsoObject.send(var5);
                  }
               } else if (var5.RelevantTo((float)this.square.x, (float)this.square.y)) {
                  var6 = var5.startPacket();
                  PacketTypes.PacketType.SyncIsoObject.doPacket(var6);
                  var6.putInt(this.square.getX());
                  var6.putInt(this.square.getY());
                  var6.putInt(this.square.getZ());
                  byte var7 = (byte)this.square.getObjects().indexOf(this);
                  if (var7 != -1) {
                     var6.putByte(var7);
                  } else {
                     var6.putByte((byte)this.square.getObjects().size());
                  }

                  var6.putByte((byte)1);
                  var6.putByte((byte)(this.Activated ? 1 : 0));
                  PacketTypes.PacketType.SyncIsoObject.send(var5);
               }
            }
         } else if (GameClient.bClient && !var1) {
            ByteBufferWriter var8 = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(var8);
            this.syncIsoObjectSend(var8);
            PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
         } else if (var1) {
            if (var2 == 1) {
               this.switchLight(true);
               this.Activated = true;
            } else {
               this.switchLight(false);
               this.Activated = false;
            }
         }

      }
   }

   public void update() {
      if (!GameServer.bServer && !GameClient.bClient || GameServer.bServer) {
         boolean var1 = false;
         if (!this.Activated) {
            this.lastMinuteStamp = -1L;
         }

         if (!this.lightRoom && this.canBeModified && this.Activated) {
            if (this.lastMinuteStamp == -1L) {
               this.lastMinuteStamp = GameTime.instance.getMinutesStamp();
            }

            if (GameTime.instance.getMinutesStamp() > this.lastMinuteStamp) {
               if (this.bulbBurnMinutes == -1) {
                  int var2 = SandboxOptions.instance.getElecShutModifier() * 24 * 60;
                  if (this.lastMinuteStamp < (long)var2) {
                     this.bulbBurnMinutes = (int)this.lastMinuteStamp;
                  } else {
                     this.bulbBurnMinutes = var2;
                  }
               }

               long var12 = GameTime.instance.getMinutesStamp() - this.lastMinuteStamp;
               this.lastMinuteStamp = GameTime.instance.getMinutesStamp();
               boolean var4 = false;
               boolean var5 = this.hasElectricityAround();
               boolean var6 = this.useBattery && this.hasBattery && this.power > 0.0F;
               if (var6 || !this.useBattery && var5) {
                  var4 = true;
               }

               double var7 = SandboxOptions.instance.LightBulbLifespan.getValue();
               if (var7 <= 0.0) {
                  var4 = false;
               }

               if (this.Activated && this.hasLightBulb() && var4) {
                  this.bulbBurnMinutes = (int)((long)this.bulbBurnMinutes + var12);
               }

               this.nextBreakUpdate = (int)((long)this.nextBreakUpdate - var12);
               if (this.nextBreakUpdate <= 0) {
                  if (this.Activated && this.hasLightBulb() && var4) {
                     int var9 = (int)(1000.0 * var7);
                     if (var9 < 1) {
                        var9 = 1;
                     }

                     int var10 = Rand.Next(0, var9);
                     int var11 = this.bulbBurnMinutes / 10000;
                     if (var10 < var11) {
                        this.bulbBurnMinutes = 0;
                        this.setActive(false, true, true);
                        this.bulbItem = null;
                        IsoWorld.instance.getFreeEmitter().playSound("LightbulbBurnedOut", this.square);
                        var1 = true;
                        if (Core.bDebug) {
                           PrintStream var10000 = System.out;
                           float var10001 = this.getX();
                           var10000.println("broke bulb at x=" + var10001 + ", y=" + this.getY() + ", z=" + this.getZ());
                        }
                     }
                  }

                  this.nextBreakUpdate = 60;
               }

               if (this.Activated && var6 && this.hasLightBulb()) {
                  float var13 = this.power - this.power % 0.01F;
                  this.power -= this.delta * (float)var12;
                  if (this.power < 0.0F) {
                     this.power = 0.0F;
                  }

                  if (var12 == 1L || this.power < var13) {
                     var1 = true;
                  }
               }
            }

            if (this.useBattery && this.Activated && (this.power <= 0.0F || !this.hasBattery)) {
               this.power = 0.0F;
               this.setActive(false, true, true);
               var1 = true;
            }
         }

         if (this.Activated && !this.hasLightBulb()) {
            this.setActive(false, true, true);
            var1 = true;
         }

         if (var1 && GameServer.bServer) {
            this.syncCustomizedSettings((UdpConnection)null);
         }
      }

   }

   public boolean hasAnimatedAttachments() {
      return super.hasAnimatedAttachments();
   }

   public void renderAnimatedAttachments(float var1, float var2, float var3, ColorInfo var4) {
      super.renderAnimatedAttachments(var1, var2, var3, var4);
   }

   public boolean isActivated() {
      return this.Activated;
   }

   public void setActivated(boolean var1) {
      this.Activated = var1;
   }

   public void addToWorld() {
      if (!this.Activated) {
         this.lastMinuteStamp = -1L;
      }

      if (!this.lightRoom && !this.lights.isEmpty()) {
         for(int var1 = 0; var1 < this.lights.size(); ++var1) {
            IsoWorld.instance.CurrentCell.getLamppostPositions().add((IsoLightSource)this.lights.get(var1));
         }
      }

      if (this.getCell() != null && this.canBeModified && !this.lightRoom && (!GameServer.bServer && !GameClient.bClient || GameServer.bServer)) {
         this.getCell().addToStaticUpdaterObjectList(this);
      }

      this.checkAmbientSound();
   }

   public void removeFromWorld() {
      if (!this.lightRoom && !this.lights.isEmpty()) {
         for(int var1 = 0; var1 < this.lights.size(); ++var1) {
            ((IsoLightSource)this.lights.get(var1)).setActive(false);
            IsoWorld.instance.CurrentCell.removeLamppost((IsoLightSource)this.lights.get(var1));
         }

         this.lights.clear();
      }

      if (this.square != null && this.lightRoom) {
         IsoRoom var2 = this.square.getRoom();
         if (var2 != null) {
            var2.lightSwitches.remove(this);
         }
      }

      this.clearOnOverlay();
      super.removeFromWorld();
   }

   public static void chunkLoaded(IsoChunk var0) {
      ArrayList var1 = new ArrayList();

      int var2;
      int var4;
      for(var2 = 0; var2 < 8; ++var2) {
         for(int var3 = 0; var3 < 8; ++var3) {
            for(var4 = var0.minLevel; var4 <= var0.maxLevel; ++var4) {
               IsoGridSquare var5 = var0.getGridSquare(var2, var3, var4);
               if (var5 != null) {
                  IsoRoom var6 = var5.getRoom();
                  if (var6 != null && var6.hasLightSwitches() && !var1.contains(var6)) {
                     var1.add(var6);
                  }
               }
            }
         }
      }

      for(var2 = 0; var2 < var1.size(); ++var2) {
         IsoRoom var7 = (IsoRoom)var1.get(var2);
         var7.createLights(var7.def.bLightsActive);

         for(var4 = 0; var4 < var7.roomLights.size(); ++var4) {
            IsoRoomLight var8 = (IsoRoomLight)var7.roomLights.get(var4);
            if (!var0.roomLights.contains(var8)) {
               var0.roomLights.add(var8);
            }
         }
      }

   }

   public ArrayList<IsoLightSource> getLights() {
      return this.lights;
   }

   public boolean shouldShowOnOverlay() {
      int var1 = IsoCamera.frameState.playerIndex;
      if (this.getSquare() != null && this.getSquare().isSeen(var1)) {
         boolean var2 = this.hasElectricityAround();
         boolean var3 = this.useBattery && this.hasBattery && this.power > 0.0F;
         boolean var4 = this.isActivated() && (var3 || !this.useBattery && var2);
         if (this.lightRoom) {
            var4 = !var4 && IsoWorld.instance.isHydroPowerOn();
         }

         if (this.bStreetLight && (GameTime.getInstance().getNight() < 0.5F || !IsoWorld.instance.isHydroPowerOn())) {
            var4 = false;
         }

         return var4;
      } else {
         return false;
      }
   }
}
