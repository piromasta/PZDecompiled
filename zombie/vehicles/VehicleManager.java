package zombie.vehicles;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortShortHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Bullet;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.vehicle.AuthorizationPacket;
import zombie.network.packets.vehicle.PhysicsPacket;
import zombie.scripting.objects.VehicleScript;
import zombie.util.Type;

public final class VehicleManager {
   public static VehicleManager instance;
   private final VehicleIDMap IDToVehicle;
   private final ArrayList<BaseVehicle> vehicles;
   private boolean idMapDirty;
   private final Transform tempTransform;
   private final ArrayList<BaseVehicle> sendReliable;
   private final ArrayList<BaseVehicle> sendUnreliable;
   private final TShortArrayList vehiclesWaitUpdates;
   private final TShortShortHashMap towedVehicleMap;
   public static HashMap<Byte, String> vehiclePacketTypes = new HashMap();
   public final UdpConnection[] connected;
   private final float[] tempFloats;
   private final float[] engineSound;
   private final PosUpdateVars posUpdateVars;
   private final UpdateLimit vehiclesWaitUpdatesFrequency;
   private BaseVehicle tempVehicle;
   private final ArrayList<BaseVehicle.ModelInfo> oldModels;
   private final ArrayList<BaseVehicle.ModelInfo> curModels;
   private final UpdateLimit sendRequestGetPositionFrequency;
   private final UpdateLimit VehiclePhysicSyncPacketLimit;

   public VehicleManager() {
      this.IDToVehicle = VehicleIDMap.instance;
      this.vehicles = new ArrayList();
      this.idMapDirty = true;
      this.tempTransform = new Transform();
      this.sendReliable = new ArrayList();
      this.sendUnreliable = new ArrayList();
      this.vehiclesWaitUpdates = new TShortArrayList(128);
      this.towedVehicleMap = new TShortShortHashMap();
      this.connected = new UdpConnection[512];
      this.tempFloats = new float[27];
      this.engineSound = new float[2];
      this.posUpdateVars = new PosUpdateVars();
      this.vehiclesWaitUpdatesFrequency = new UpdateLimit(1000L);
      this.oldModels = new ArrayList();
      this.curModels = new ArrayList();
      this.sendRequestGetPositionFrequency = new UpdateLimit(500L);
      this.VehiclePhysicSyncPacketLimit = new UpdateLimit(500L);
   }

   public void registerVehicle(BaseVehicle var1) {
      this.IDToVehicle.put(var1.VehicleID, var1);
      this.idMapDirty = true;
   }

   public void unregisterVehicle(BaseVehicle var1) {
      this.IDToVehicle.remove(var1.VehicleID);
      this.idMapDirty = true;
   }

   public BaseVehicle getVehicleByID(short var1) {
      return this.IDToVehicle.get(var1);
   }

   public ArrayList<BaseVehicle> getVehicles() {
      if (this.idMapDirty) {
         this.vehicles.clear();
         this.IDToVehicle.toArrayList(this.vehicles);
         this.idMapDirty = false;
      }

      return this.vehicles;
   }

   public void removeFromWorld(BaseVehicle var1) {
      if (var1.VehicleID != -1) {
         DebugLog.Vehicle.trace("removeFromWorld vehicle id=%d", var1.VehicleID);
         this.unregisterVehicle(var1);
         if (GameServer.bServer) {
            for(int var2 = 0; var2 < GameServer.udpEngine.connections.size(); ++var2) {
               UdpConnection var3 = (UdpConnection)GameServer.udpEngine.connections.get(var2);
               if (var1.connectionState[var3.index] != null) {
                  ByteBufferWriter var4 = var3.startPacket();
                  PacketTypes.PacketType.Vehicles.doPacket(var4);
                  var4.bb.put((byte)8);
                  var4.bb.putShort(var1.VehicleID);
                  PacketTypes.PacketType.Vehicles.send(var3);
               }
            }
         }

         if (GameClient.bClient) {
            var1.serverRemovedFromWorld = false;
            if (var1.interpolation != null) {
               var1.interpolation.clear();
            }
         }
      }

   }

   public void serverUpdate() {
      ArrayList var1 = IsoWorld.instance.CurrentCell.getVehicles();

      int var2;
      for(var2 = 0; var2 < this.connected.length; ++var2) {
         int var3;
         if (this.connected[var2] != null && !GameServer.udpEngine.connections.contains(this.connected[var2])) {
            DebugLog.Vehicle.trace("vehicles: dropped connection %d", var2);

            for(var3 = 0; var3 < var1.size(); ++var3) {
               ((BaseVehicle)var1.get(var3)).connectionState[var2] = null;
            }

            this.connected[var2] = null;
         } else {
            for(var3 = 0; var3 < var1.size(); ++var3) {
               if (((BaseVehicle)var1.get(var3)).connectionState[var2] != null) {
                  BaseVehicle.ServerVehicleState var10000 = ((BaseVehicle)var1.get(var3)).connectionState[var2];
                  var10000.flags |= ((BaseVehicle)var1.get(var3)).updateFlags;
               }
            }
         }
      }

      for(var2 = 0; var2 < GameServer.udpEngine.connections.size(); ++var2) {
         UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var2);
         this.sendVehicles(var6, PacketTypes.PacketType.VehiclesUnreliable.getId());
         this.connected[var6.index] = var6;
      }

      for(var2 = 0; var2 < var1.size(); ++var2) {
         BaseVehicle var7 = (BaseVehicle)var1.get(var2);
         if ((var7.updateFlags & 19440) != 0) {
            for(int var4 = 0; var4 < var7.getPartCount(); ++var4) {
               VehiclePart var5 = var7.getPartByIndex(var4);
               var5.updateFlags = 0;
            }
         }

         var7.updateFlags = 0;
      }

   }

   private void sendVehicles(UdpConnection var1, short var2) {
      if (var1.isFullyConnected()) {
         this.sendReliable.clear();
         this.sendUnreliable.clear();
         ArrayList var3 = IsoWorld.instance.CurrentCell.getVehicles();

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            BaseVehicle var5 = (BaseVehicle)var3.get(var4);
            if (var5.VehicleID == -1) {
               var5.VehicleID = this.IDToVehicle.allocateID();
               this.registerVehicle(var5);
            }

            if (var1.RelevantTo(var5.getX(), var5.getY())) {
               if (var5.connectionState[var1.index] == null) {
                  var5.connectionState[var1.index] = new BaseVehicle.ServerVehicleState();
               }

               BaseVehicle.ServerVehicleState var6 = var5.connectionState[var1.index];
               if (var6.shouldSend(var5)) {
                  if (!var5.isReliable && PacketTypes.PacketType.Vehicles.getId() != var2) {
                     this.sendUnreliable.add(var5);
                  } else {
                     this.sendReliable.add(var5);
                  }
               }
            }
         }

         this.sendVehiclesInternal(var1, this.sendReliable, PacketTypes.PacketType.Vehicles);
         this.sendVehiclesInternal(var1, this.sendUnreliable, PacketTypes.PacketType.VehiclesUnreliable);
      }
   }

   private void sendVehiclesInternal(UdpConnection var1, ArrayList<BaseVehicle> var2, PacketTypes.PacketType var3) {
      if (!var2.isEmpty()) {
         ByteBufferWriter var4 = var1.startPacket();
         var3.doPacket(var4);

         try {
            ByteBuffer var5 = var4.bb;
            var5.put((byte)5);
            var5.putShort((short)var2.size());

            for(int var6 = 0; var6 < var2.size(); ++var6) {
               BaseVehicle var7 = (BaseVehicle)var2.get(var6);
               BaseVehicle.ServerVehicleState var8 = var7.connectionState[var1.index];
               var5.putShort(var7.VehicleID);
               var5.putShort(var8.flags);
               var5.putFloat(var7.getX());
               var5.putFloat(var7.getY());
               var5.putFloat(var7.jniTransform.origin.y);
               int var9 = var5.position();
               var5.putShort((short)0);
               int var10 = var5.position();
               boolean var11 = (var8.flags & 1) != 0;
               int var23;
               int var26;
               if (var11) {
                  var8.flags = (short)(var8.flags & -2);
                  var7.netPlayerServerSendAuthorisation(var5);
                  var8.setAuthorization(var7);
                  var23 = var5.position();
                  var5.putShort((short)0);
                  var7.save(var5);
                  var26 = var5.position();
                  var5.position(var23);
                  var5.putShort((short)(var26 - var23));
                  var5.position(var26);
                  int var24 = var5.position();
                  int var15 = var5.position() - var10;
                  var5.position(var9);
                  var5.putShort((short)var15);
                  var5.position(var24);
                  this.writePositionOrientation(var5, var7);
                  var8.x = var7.getX();
                  var8.y = var7.getY();
                  var8.z = var7.jniTransform.origin.y;
                  var8.orient.set(var7.savedRot);
               } else {
                  if ((var8.flags & 2) != 0) {
                     this.writePositionOrientation(var5, var7);
                     var8.x = var7.getX();
                     var8.y = var7.getY();
                     var8.z = var7.jniTransform.origin.y;
                     var8.orient.set(var7.savedRot);
                  }

                  if ((var8.flags & 4) != 0) {
                     var5.put((byte)var7.engineState.ordinal());
                     var5.putInt(var7.engineLoudness);
                     var5.putInt(var7.enginePower);
                     var5.putInt(var7.engineQuality);
                  }

                  if ((var8.flags & 4096) != 0) {
                     var5.put((byte)(var7.isHotwired() ? 1 : 0));
                     var5.put((byte)(var7.isHotwiredBroken() ? 1 : 0));
                     var5.putFloat(var7.getRegulatorSpeed());
                     var5.put((byte)(var7.isPreviouslyEntered() ? 1 : 0));
                     var5.put((byte)(var7.isKeysInIgnition() ? 1 : 0));
                     var5.put((byte)(var7.isKeyIsOnDoor() ? 1 : 0));
                     InventoryItem var12 = var7.getCurrentKey();
                     if (var12 == null) {
                        var5.put((byte)0);
                     } else {
                        var5.put((byte)1);
                        var12.saveWithSize(var5, false);
                     }

                     var5.putFloat(var7.getRust());
                     var5.putFloat(var7.getBloodIntensity("Front"));
                     var5.putFloat(var7.getBloodIntensity("Rear"));
                     var5.putFloat(var7.getBloodIntensity("Left"));
                     var5.putFloat(var7.getBloodIntensity("Right"));
                     var5.putFloat(var7.getColorHue());
                     var5.putFloat(var7.getColorSaturation());
                     var5.putFloat(var7.getColorValue());
                     var5.putInt(var7.getSkinIndex());
                  }

                  if ((var8.flags & 8) != 0) {
                     var5.put((byte)(var7.getHeadlightsOn() ? 1 : 0));
                     var5.put((byte)(var7.getStoplightsOn() ? 1 : 0));

                     for(var23 = 0; var23 < var7.getLightCount(); ++var23) {
                        var5.put((byte)(var7.getLightByIndex(var23).getLight().getActive() ? 1 : 0));
                     }
                  }

                  if ((var8.flags & 1024) != 0) {
                     var5.put((byte)(var7.soundHornOn ? 1 : 0));
                     var5.put((byte)(var7.soundBackMoveOn ? 1 : 0));
                     var5.put((byte)var7.lightbarLightsMode.get());
                     var5.put((byte)var7.lightbarSirenMode.get());
                  }

                  VehiclePart var13;
                  if ((var8.flags & 2048) != 0) {
                     for(var23 = 0; var23 < var7.getPartCount(); ++var23) {
                        var13 = var7.getPartByIndex(var23);
                        if ((var13.updateFlags & 2048) != 0) {
                           var5.put((byte)var23);
                           var5.putInt(var13.getCondition());
                        }
                     }

                     var5.put((byte)-1);
                  }

                  if ((var8.flags & 16) != 0) {
                     for(var23 = 0; var23 < var7.getPartCount(); ++var23) {
                        var13 = var7.getPartByIndex(var23);
                        if ((var13.updateFlags & 16) != 0) {
                           var5.put((byte)var23);
                           var13.getModData().save(var5);
                        }
                     }

                     var5.put((byte)-1);
                  }

                  InventoryItem var14;
                  if ((var8.flags & 32) != 0) {
                     for(var23 = 0; var23 < var7.getPartCount(); ++var23) {
                        var13 = var7.getPartByIndex(var23);
                        if ((var13.updateFlags & 32) != 0) {
                           var14 = var13.getInventoryItem();
                           if (var14 instanceof DrainableComboItem) {
                              var5.put((byte)var23);
                              var5.putFloat(((DrainableComboItem)var14).getCurrentUsesFloat());
                           }
                        }
                     }

                     var5.put((byte)-1);
                  }

                  if ((var8.flags & 128) != 0) {
                     for(var23 = 0; var23 < var7.getPartCount(); ++var23) {
                        var13 = var7.getPartByIndex(var23);
                        if ((var13.updateFlags & 128) != 0) {
                           var5.put((byte)var23);
                           var14 = var13.getInventoryItem();
                           if (var14 == null) {
                              var5.put((byte)0);
                           } else {
                              var5.put((byte)1);

                              try {
                                 var13.getInventoryItem().saveWithSize(var5, false);
                              } catch (Exception var16) {
                                 var16.printStackTrace();
                              }
                           }
                        }
                     }

                     var5.put((byte)-1);
                  }

                  if ((var8.flags & 512) != 0) {
                     for(var23 = 0; var23 < var7.getPartCount(); ++var23) {
                        var13 = var7.getPartByIndex(var23);
                        if ((var13.updateFlags & 512) != 0) {
                           var5.put((byte)var23);
                           var13.getDoor().save(var5);
                        }
                     }

                     var5.put((byte)-1);
                  }

                  if ((var8.flags & 256) != 0) {
                     for(var23 = 0; var23 < var7.getPartCount(); ++var23) {
                        var13 = var7.getPartByIndex(var23);
                        if ((var13.updateFlags & 256) != 0) {
                           var5.put((byte)var23);
                           var13.getWindow().save(var5);
                        }
                     }

                     var5.put((byte)-1);
                  }

                  if ((var8.flags & 64) != 0) {
                     var5.put((byte)var7.models.size());

                     for(var23 = 0; var23 < var7.models.size(); ++var23) {
                        BaseVehicle.ModelInfo var25 = (BaseVehicle.ModelInfo)var7.models.get(var23);
                        var5.put((byte)var25.part.getIndex());
                        var5.put((byte)var25.part.getScriptPart().models.indexOf(var25.scriptModel));
                     }
                  }

                  var23 = var5.position();
                  var26 = var5.position() - var10;
                  var5.position(var9);
                  var5.putShort((short)var26);
                  var5.position(var23);
               }
            }

            var3.send(var1);
         } catch (Exception var17) {
            var1.cancelPacket();
            var17.printStackTrace();
         }

         for(int var18 = 0; var18 < var2.size(); ++var18) {
            BaseVehicle var19 = (BaseVehicle)var2.get(var18);
            BaseVehicle.ServerVehicleState var20 = var19.connectionState[var1.index];
            if ((var20.flags & 16384) != 0) {
               AuthorizationPacket var21 = new AuthorizationPacket();
               var21.set(var19, var1);
               ByteBufferWriter var22 = var1.startPacket();
               PacketTypes.PacketType.VehicleAuthorization.doPacket(var22);
               var21.write(var22);
               PacketTypes.PacketType.VehicleAuthorization.send(var1);
            }
         }

      }
   }

   public void serverPacket(ByteBuffer var1, UdpConnection var2, short var3) {
      byte var4 = var1.get();
      short var5;
      int var6;
      BaseVehicle var8;
      IsoPlayer var10;
      short var16;
      byte var17;
      String var10001;
      IsoPlayer var23;
      DebugLogStream var25;
      switch (var4) {
         case 1:
            var5 = var1.getShort();
            DebugLog.Vehicle.trace("%s vid=%d", vehiclePacketTypes.get(var4), var5);
            byte var19 = var1.get();
            String var20 = GameWindow.ReadString(var1);
            var8 = this.IDToVehicle.get(var5);
            if (var8 != null) {
               IsoGameCharacter var24 = var8.getCharacter(var19);
               if (var24 != null) {
                  var8.setCharacterPosition(var24, var19, var20);
                  this.sendPassengerPosition(var8, var19, var20, var2);
               }
            }
            break;
         case 2:
            var5 = var1.getShort();
            var16 = var1.getShort();
            var17 = var1.get();
            DebugLog.Vehicle.trace("Vehicle enter vid=%d pid=%d seat=%d", var5, var16, Integer.valueOf(var17));
            var8 = this.IDToVehicle.get(var5);
            if (var8 == null) {
               DebugLog.Vehicle.warn("Vehicle vid=%d not found", var5);
            } else {
               var23 = (IsoPlayer)GameServer.IDToPlayerMap.get(var16);
               if (var23 == null) {
                  DebugLog.Vehicle.warn("Player pid=%d not found", var16);
               } else {
                  var10 = (IsoPlayer)Type.tryCastTo(var8.getCharacter(var17), IsoPlayer.class);
                  if (var10 != null && var10 != var23) {
                     var25 = DebugLog.DetailedInfo;
                     var10001 = var23.getUsername();
                     var25.warn(var10001 + " got in same seat as " + var10.getUsername());
                  } else {
                     var8.enter(var17, var23);
                     if (var17 == 0 && var8.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
                        var8.authorizationServerOnSeat(var23, true);
                     }

                     this.sendEnter(var8, var23, var17);
                  }
               }
            }
            break;
         case 3:
            var5 = var1.getShort();
            var16 = var1.getShort();
            var17 = var1.get();
            DebugLog.Vehicle.trace("Vehicle exit vid=%d pid=%d seat=%d", var5, var16, Integer.valueOf(var17));
            var8 = this.IDToVehicle.get(var5);
            if (var8 == null) {
               DebugLog.Vehicle.warn("Vehicle vid=%d not found", var5);
            } else {
               var23 = (IsoPlayer)GameServer.IDToPlayerMap.get(var16);
               if (var23 == null) {
                  DebugLog.Vehicle.warn("Player pid=%d not found", var16);
               } else {
                  var8.exit(var23);
                  if (var17 == 0) {
                     var8.authorizationServerOnSeat(var23, false);
                  }

                  this.sendExit(var8, var23, var17);
               }
            }
            break;
         case 4:
            var5 = var1.getShort();
            var16 = var1.getShort();
            var17 = var1.get();
            byte var21 = var1.get();
            DebugLog.Vehicle.trace("Vehicle switch seat vid=%d pid=%d seats=%d=>%d", var5, var16, Integer.valueOf(var17), Integer.valueOf(var21));
            BaseVehicle var22 = this.IDToVehicle.get(var5);
            if (var22 == null) {
               DebugLog.Vehicle.warn("Vehicle vid=%d not found", var5);
            } else {
               var10 = (IsoPlayer)GameServer.IDToPlayerMap.get(var16);
               if (var10 == null) {
                  DebugLog.Vehicle.warn("Player pid=%d not found", var16);
               } else {
                  IsoPlayer var11 = (IsoPlayer)Type.tryCastTo(var22.getCharacter(var21), IsoPlayer.class);
                  if (var11 != null && var11 != var10) {
                     var25 = DebugLog.DetailedInfo;
                     var10001 = var10.getUsername();
                     var25.warn(var10001 + " switched to same seat as " + var11.getUsername());
                  } else {
                     var22.switchSeat(var10, var21);
                     if (var21 == 0 && var22.isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
                        var22.authorizationServerOnSeat(var10, true);
                     } else if (var17 == 0) {
                        var22.authorizationServerOnSeat(var10, false);
                     }

                     this.sendSwitchSeat(var22, var10, var17, var21);
                  }
               }
            }
            break;
         case 5:
         case 6:
         case 7:
         case 8:
         case 10:
         case 13:
         case 14:
         default:
            DebugLog.Vehicle.warn("Unknown vehicle packet %d", var4);
            break;
         case 9:
            PhysicsPacket var12 = new PhysicsPacket();
            var12.parse(var1, var2);
            var12.process(var2);

            for(var6 = 0; var6 < GameServer.udpEngine.connections.size(); ++var6) {
               UdpConnection var15 = (UdpConnection)GameServer.udpEngine.connections.get(var6);
               if (var2 != var15 && var12.isRelevant(var15)) {
                  ByteBufferWriter var18 = var15.startPacket();
                  PacketTypes.PacketType var9 = (PacketTypes.PacketType)PacketTypes.packetTypes.get(var3);
                  var9.doPacket(var18);
                  var18.bb.put((byte)9);
                  var12.write(var18);
                  var9.send(var15);
               }
            }

            return;
         case 11:
            var5 = var1.getShort();

            for(var6 = 0; var6 < var5; ++var6) {
               short var14 = var1.getShort();
               DebugLog.Vehicle.trace("Vehicle vid=%d full update response ", var14);
               var8 = this.IDToVehicle.get(var14);
               if (var8 != null) {
                  if (var8.connectionState[var2.index] == null) {
                     var8.connectionState[var2.index] = new BaseVehicle.ServerVehicleState();
                  }

                  BaseVehicle.ServerVehicleState var10000 = var8.connectionState[var2.index];
                  var10000.flags = (short)(var10000.flags | 1);
                  this.sendVehicles(var2, var3);
               }
            }

            return;
         case 12:
            var5 = var1.getShort();
            DebugLog.Vehicle.trace("%s vid=%d", vehiclePacketTypes.get(var4), var5);
            BaseVehicle var13 = this.IDToVehicle.get(var5);
            if (var13 != null) {
               var13.updateFlags = (short)(var13.updateFlags | 2);
               this.sendVehicles(var2, var3);
            }
            break;
         case 15:
            var5 = var1.getShort();
            var6 = var1.getShort();
            boolean var7 = var1.get() == 1;
            DebugLog.Vehicle.trace("%s vid=%d pid=%d %b", vehiclePacketTypes.get(var4), var5, Short.valueOf((short)var6), var7);
            var8 = this.IDToVehicle.get(var5);
            if (var8 != null) {
               var8.authorizationServerCollide((short)var6, var7);
            }
      }

   }

   public void serverSendInitialWorldState(UdpConnection var1) {
      ByteBufferWriter var2 = var1.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var2);
      var2.bb.put((byte)19);
      var2.bb.putShort((short)this.towedVehicleMap.size());
      this.towedVehicleMap.forEachEntry((var1x, var2x) -> {
         var2.putShort(var1x);
         var2.putShort(var2x);
         return true;
      });
      PacketTypes.PacketType.Vehicles.send(var1);
   }

   private void vehiclePosUpdate(BaseVehicle var1, float[] var2) {
      int var3 = 0;
      Transform var4 = this.posUpdateVars.transform;
      Vector3f var5 = this.posUpdateVars.vector3f;
      Quaternionf var6 = this.posUpdateVars.quatf;
      float[] var7 = this.posUpdateVars.wheelSteer;
      float[] var8 = this.posUpdateVars.wheelRotation;
      float[] var9 = this.posUpdateVars.wheelSkidInfo;
      float[] var10 = this.posUpdateVars.wheelSuspensionLength;
      float var11 = var2[var3++] - WorldSimulation.instance.offsetX;
      float var12 = var2[var3++] - WorldSimulation.instance.offsetY;
      float var13 = var2[var3++];
      var4.origin.set(var11, var13, var12);
      float var14 = var2[var3++];
      float var15 = var2[var3++];
      float var16 = var2[var3++];
      float var17 = var2[var3++];
      var6.set(var14, var15, var16, var17);
      var6.normalize();
      var4.setRotation(var6);
      float var18 = var2[var3++];
      float var19 = var2[var3++];
      float var20 = var2[var3++];
      var5.set(var18, var19, var20);
      int var21 = (int)var2[var3++];

      for(int var22 = 0; var22 < var21; ++var22) {
         var7[var22] = var2[var3++];
         var8[var22] = var2[var3++];
         var9[var22] = var2[var3++];
         var10[var22] = var2[var3++];
      }

      var1.jniTransform.set(var4);
      var1.jniLinearVelocity.set(var5);
      var1.jniTransform.basis.getScale(var5);
      if ((double)var5.x < 0.99 || (double)var5.y < 0.99 || (double)var5.z < 0.99) {
         var1.jniTransform.basis.scale(1.0F / var5.x, 1.0F / var5.y, 1.0F / var5.z);
      }

      var1.jniSpeed = var1.jniLinearVelocity.length() * 3.6F;
      Vector3f var24 = var1.getForwardVector(BaseVehicle.allocVector3f());
      if (var24.dot(var1.jniLinearVelocity) < 0.0F) {
         var1.jniSpeed *= -1.0F;
      }

      BaseVehicle.releaseVector3f(var24);

      for(int var23 = 0; var23 < 4; ++var23) {
         var1.wheelInfo[var23].steering = var7[var23];
         var1.wheelInfo[var23].rotation = var8[var23];
         var1.wheelInfo[var23].skidInfo = var9[var23];
         var1.wheelInfo[var23].suspensionLength = var10[var23];
      }

      var1.polyDirty = true;
   }

   public void clientUpdate() {
      int var2;
      if (this.vehiclesWaitUpdatesFrequency.Check()) {
         if (this.vehiclesWaitUpdates.size() > 0) {
            ByteBufferWriter var1 = GameClient.connection.startPacket();
            PacketTypes.PacketType.Vehicles.doPacket(var1);
            var1.bb.put((byte)11);
            var1.bb.putShort((short)this.vehiclesWaitUpdates.size());

            for(var2 = 0; var2 < this.vehiclesWaitUpdates.size(); ++var2) {
               var1.bb.putShort(this.vehiclesWaitUpdates.get(var2));
            }

            PacketTypes.PacketType.Vehicles.send(GameClient.connection);
         }

         this.vehiclesWaitUpdates.clear();
      }

      ArrayList var9 = this.getVehicles();

      for(var2 = 0; var2 < var9.size(); ++var2) {
         BaseVehicle var3 = (BaseVehicle)var9.get(var2);
         if (GameClient.bClient) {
            if (var3.isNetPlayerAuthorization(BaseVehicle.Authorization.Local) || var3.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
               var3.interpolation.clear();
               continue;
            }
         } else if (var3.isKeyboardControlled() || var3.getJoypad() != -1) {
            var3.interpolation.clear();
            continue;
         }

         float[] var4 = this.tempFloats;
         if (var3.interpolation.interpolationDataGet(var4, this.engineSound)) {
            if (!var3.isNetPlayerAuthorization(BaseVehicle.Authorization.Local) && !var3.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
               Bullet.setOwnVehiclePhysics(var3.VehicleID, var4);
               float var5 = var4[0];
               float var6 = var4[1];
               float var7 = var4[2];
               IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare((double)var5, (double)var6, 0.0);
               this.clientUpdateVehiclePos(var3, var5, var6, var7, var8);
               var3.limitPhysicValid.BlockCheck();
               if (GameClient.bClient) {
                  this.vehiclePosUpdate(var3, var4);
               }

               var3.engineSpeed = (double)this.engineSound[0];
               var3.throttle = this.engineSound[1];
            }
         } else {
            var3.getController().control_NoControl();
            var3.throttle = 0.0F;
            var3.jniSpeed = 0.0F;
         }
      }

   }

   private void clientUpdateVehiclePos(BaseVehicle var1, float var2, float var3, float var4, IsoGridSquare var5) {
      var1.setX(var2);
      var1.setY(var3);
      var1.setZ(0.0F);
      var1.square = var5;
      var1.setCurrent(var5);
      if (var5 != null) {
         if (var1.chunk != null && var1.chunk != var5.chunk) {
            var1.chunk.vehicles.remove(var1);
         }

         var1.chunk = var1.square.chunk;
         if (!var1.chunk.vehicles.contains(var1)) {
            var1.chunk.vehicles.add(var1);
            IsoChunk.addFromCheckedVehicles(var1);
         }

         if (!var1.addedToWorld) {
            var1.addToWorld();
         }
      } else {
         var1.removeFromWorld();
         var1.removeFromSquare();
      }

      var1.polyDirty = true;
   }

   private void clientReceiveUpdateFull(ByteBuffer var1, short var2, float var3, float var4, float var5) throws IOException {
      BaseVehicle.Authorization var6 = BaseVehicle.Authorization.valueOf(var1.get());
      short var7 = var1.getShort();
      short var8 = var1.getShort();
      IsoGridSquare var9 = IsoWorld.instance.CurrentCell.getGridSquare((double)var3, (double)var4, 0.0);
      if (this.IDToVehicle.containsKey(var2)) {
         BaseVehicle var10 = this.IDToVehicle.get(var2);
         DebugLog.Vehicle.noise("ERROR: got full update for KNOWN vehicle id=%d", var2);
         var1.get();
         var1.get();
         this.tempVehicle.parts.clear();
         this.tempVehicle.load(var1, 219);
         if (var10.physics != null && (var10.getDriver() == null || !var10.getDriver().isLocal())) {
            this.tempTransform.setRotation(this.tempVehicle.savedRot);
            this.tempTransform.origin.set(var3 - WorldSimulation.instance.offsetX, var5, var4 - WorldSimulation.instance.offsetY);
            var10.setWorldTransform(this.tempTransform);
         }

         var10.netPlayerFromServerUpdate(var6, var7);
         this.clientUpdateVehiclePos(var10, var3, var4, var5, var9);
      } else {
         boolean var15 = var1.get() != 0;
         byte var11 = var1.get();
         if (!var15 || var11 != IsoObject.getFactoryVehicle().getClassID()) {
            DebugLog.Vehicle.error("clientReceiveUpdateFull: packet broken");
         }

         BaseVehicle var12 = new BaseVehicle(IsoWorld.instance.CurrentCell);
         var12.VehicleID = var2;
         var12.square = var9;
         var12.setCurrent(var9);
         var12.load(var1, 219);
         if (var9 != null) {
            var12.chunk = var12.square.chunk;
            var12.chunk.vehicles.add(var12);
            var12.addToWorld();
         }

         IsoChunk.addFromCheckedVehicles(var12);
         var12.netPlayerFromServerUpdate(var6, var7);
         this.registerVehicle(var12);

         for(int var13 = 0; var13 < IsoPlayer.numPlayers; ++var13) {
            IsoPlayer var14 = IsoPlayer.players[var13];
            if (var14 != null && !var14.isDead() && var14.getVehicle() == null) {
               IsoWorld.instance.CurrentCell.putInVehicle(var14);
            }
         }

         DebugLog.Vehicle.trace("added vehicle id=%d %s", var12.VehicleID, var9 == null ? " (delayed)" : "");
      }

   }

   private void clientReceiveUpdate(ByteBuffer var1) throws IOException {
      short var2 = var1.getShort();
      DebugLog.Vehicle.trace("%s vid=%d", vehiclePacketTypes.get((byte)5), var2);
      short var3 = var1.getShort();
      float var4 = var1.getFloat();
      float var5 = var1.getFloat();
      float var6 = var1.getFloat();
      short var7 = var1.getShort();
      VehicleCache.vehicleUpdate(var2, var4, var5, 0.0F);
      IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare((double)var4, (double)var5, 0.0);
      BaseVehicle var9 = this.IDToVehicle.get(var2);
      if (var9 == null && var8 == null) {
         if (var1.limit() > var1.position() + var7) {
            var1.position(var1.position() + var7);
         }

      } else {
         boolean var22;
         int var30;
         if (var9 != null && var8 == null) {
            var22 = true;

            for(var30 = 0; var30 < IsoPlayer.numPlayers; ++var30) {
               IsoPlayer var36 = IsoPlayer.players[var30];
               if (var36 != null && var36.getVehicle() == var9) {
                  var22 = false;
                  var36.setPosition(var4, var5, 0.0F);
                  this.sendRequestGetPosition(var2, PacketTypes.PacketType.VehiclesUnreliable);
               }
            }

            if (var22) {
               var9.removeFromWorld();
               var9.removeFromSquare();
            }

            if (var1.limit() > var1.position() + var7) {
               var1.position(var1.position() + var7);
            }

         } else {
            int var10;
            byte var23;
            if ((var3 & 1) != 0) {
               DebugLog.Vehicle.trace("Vehicle vid=%d full update received", var2);
               this.clientReceiveUpdateFull(var1, var2, var4, var5, var6);
               if (var9 == null) {
                  var9 = this.IDToVehicle.get(var2);
               }

               if (!var9.isKeyboardControlled() && var9.getJoypad() == -1) {
                  var1.getLong();
                  var23 = 0;
                  float[] var38 = this.tempFloats;
                  var10 = var23 + 1;
                  var38[var23] = var4;
                  var38[var10++] = var5;

                  for(var38[var10++] = var6; var10 < 10; var38[var10++] = var1.getFloat()) {
                  }

                  float var35 = var1.getFloat();
                  float var34 = var1.getFloat();
                  short var31 = var1.getShort();
                  var38[var10++] = (float)var31;

                  for(int var37 = 0; var37 < var31; ++var37) {
                     float var39 = var1.getFloat();
                     float var17 = var1.getFloat();
                     float var18 = var1.getFloat();
                     float var19 = var1.getFloat();
                     var38[var10++] = var39;
                     var38[var10++] = var17;
                     var38[var10++] = var18;
                     var38[var10++] = var19;
                     var9.wheelInfo[var37].steering = var39;
                     var9.wheelInfo[var37].rotation = var17;
                     var9.wheelInfo[var37].skidInfo = var18;
                     var9.wheelInfo[var37].suspensionLength = var19;
                  }

                  Bullet.setOwnVehiclePhysics(var2, var38);
               } else if (var1.limit() > var1.position() + 102) {
                  var1.position(var1.position() + 102);
               }

               var10 = this.vehiclesWaitUpdates.indexOf(var2);
               if (var10 >= 0) {
                  this.vehiclesWaitUpdates.removeAt(var10);
               }

            } else if (var9 == null && var8 != null) {
               this.sendRequestGetFull(var2, PacketTypes.PacketType.Vehicles);
               if (var1.limit() > var1.position() + var7) {
                  var1.position(var1.position() + var7);
               }

            } else {
               if ((var3 & 2) != 0) {
                  if (!var9.isKeyboardControlled() && var9.getJoypad() == -1) {
                     var9.interpolation.interpolationDataAdd(var1, var1.getLong(), var4, var5, var6, GameTime.getServerTimeMills());
                  } else if (var1.limit() > var1.position() + 102) {
                     var1.position(var1.position() + 102);
                  }
               }

               if ((var3 & 4) != 0) {
                  DebugLog.Vehicle.trace("received update Engine id=%d", var2);
                  var10 = var1.get();
                  if (var10 >= 0 && var10 < BaseVehicle.engineStateTypes.Values.length) {
                     switch (BaseVehicle.engineStateTypes.Values[var10]) {
                        case Idle:
                           var9.engineDoIdle();
                        case Starting:
                        default:
                           break;
                        case RetryingStarting:
                           var9.engineDoRetryingStarting();
                           break;
                        case StartingSuccess:
                           var9.engineDoStartingSuccess();
                           break;
                        case StartingFailed:
                           var9.engineDoStartingFailed();
                           break;
                        case StartingFailedNoPower:
                           var9.engineDoStartingFailedNoPower();
                           break;
                        case Running:
                           var9.engineDoRunning();
                           break;
                        case Stalling:
                           var9.engineDoStalling();
                           break;
                        case ShutingDown:
                           var9.engineDoShuttingDown();
                     }

                     var9.engineLoudness = var1.getInt();
                     var9.enginePower = var1.getInt();
                     var9.engineQuality = var1.getInt();
                  } else {
                     DebugLog.Vehicle.error("VehicleManager.clientReceiveUpdate get invalid data");
                  }
               }

               boolean var11;
               if ((var3 & 4096) != 0) {
                  DebugLog.Vehicle.trace("received car properties update id=%d", var2);
                  var9.setHotwired(var1.get() == 1);
                  var9.setHotwiredBroken(var1.get() == 1);
                  var9.setRegulatorSpeed(var1.getFloat());
                  var9.setPreviouslyEntered(var1.get() == 1);
                  var22 = var1.get() == 1;
                  var11 = var1.get() == 1;
                  InventoryItem var12 = null;
                  if (var1.get() == 1) {
                     try {
                        var12 = InventoryItem.loadItem(var1, 219);
                     } catch (Exception var21) {
                        var21.printStackTrace();
                     }
                  }

                  var9.syncKeyInIgnition(var22, var11, var12);
                  var9.setRust(var1.getFloat());
                  var9.setBloodIntensity("Front", var1.getFloat());
                  var9.setBloodIntensity("Rear", var1.getFloat());
                  var9.setBloodIntensity("Left", var1.getFloat());
                  var9.setBloodIntensity("Right", var1.getFloat());
                  var9.setColorHSV(var1.getFloat(), var1.getFloat(), var1.getFloat());
                  var9.setSkinIndex(var1.getInt());
                  var9.updateSkin();
               }

               if ((var3 & 8) != 0) {
                  DebugLog.Vehicle.trace("received update Lights id=%d", var2);
                  var9.setHeadlightsOn(var1.get() == 1);
                  var9.setStoplightsOn(var1.get() == 1);

                  for(var10 = 0; var10 < var9.getLightCount(); ++var10) {
                     var11 = var1.get() == 1;
                     var9.getLightByIndex(var10).getLight().setActive(var11);
                  }
               }

               int var13;
               byte var26;
               if ((var3 & 1024) != 0) {
                  DebugLog.Vehicle.trace("received update Sounds id=%d", var2);
                  var22 = var1.get() == 1;
                  var11 = var1.get() == 1;
                  var26 = var1.get();
                  var13 = var1.get();
                  if (var22 != var9.soundHornOn) {
                     if (var22) {
                        var9.onHornStart();
                     } else {
                        var9.onHornStop();
                     }
                  }

                  if (var11 != var9.soundBackMoveOn) {
                     if (var11) {
                        var9.onBackMoveSignalStart();
                     } else {
                        var9.onBackMoveSignalStop();
                     }
                  }

                  if (var9.lightbarLightsMode.get() != var26) {
                     var9.setLightbarLightsMode(var26);
                  }

                  if (var9.lightbarSirenMode.get() != var13) {
                     var9.setLightbarSirenMode(var13);
                  }
               }

               VehiclePart var24;
               if ((var3 & 2048) != 0) {
                  for(var23 = var1.get(); var23 != -1; var23 = var1.get()) {
                     var24 = var9.getPartByIndex(var23);
                     DebugLog.Vehicle.trace("received update PartCondition id=%d part=%s", var2, var24.getId());
                     var24.updateFlags = (short)(var24.updateFlags | 2048);
                     var24.setCondition(var1.getInt());
                  }

                  var9.doDamageOverlay();
               }

               if ((var3 & 16) != 0) {
                  for(var23 = var1.get(); var23 != -1; var23 = var1.get()) {
                     var24 = var9.getPartByIndex(var23);
                     DebugLog.Vehicle.trace("received update PartModData id=%d part=%s", var2, var24.getId());
                     var24.getModData().load(var1, 219);
                     if (var24.isContainer()) {
                        var24.setContainerContentAmount(var24.getContainerContentAmount());
                     }
                  }
               }

               VehiclePart var27;
               InventoryItem var28;
               if ((var3 & 32) != 0) {
                  for(var23 = var1.get(); var23 != -1; var23 = var1.get()) {
                     float var25 = var1.getFloat();
                     var27 = var9.getPartByIndex(var23);
                     DebugLog.Vehicle.trace("received update PartUsedDelta id=%d part=%s", var2, var27.getId());
                     var28 = var27.getInventoryItem();
                     if (var28 instanceof DrainableComboItem) {
                        var28.setCurrentUses((int)((float)var28.getMaxUses() * var25));
                     }
                  }
               }

               if ((var3 & 128) != 0) {
                  for(var23 = var1.get(); var23 != -1; var23 = var1.get()) {
                     var24 = var9.getPartByIndex(var23);
                     DebugLog.Vehicle.trace("received update PartItem id=%d part=%s", var2, var24.getId());
                     var24.updateFlags = (short)(var24.updateFlags | 128);
                     boolean var29 = var1.get() != 0;
                     if (var29) {
                        try {
                           var28 = InventoryItem.loadItem(var1, 219);
                        } catch (Exception var20) {
                           var20.printStackTrace();
                           return;
                        }

                        if (var28 != null) {
                           var24.setInventoryItem(var28);
                        }
                     } else {
                        var24.setInventoryItem((InventoryItem)null);
                     }

                     var13 = var24.getWheelIndex();
                     if (var13 != -1) {
                        var9.setTireRemoved(var13, !var29);
                     }

                     if (var24.isContainer()) {
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                     }
                  }
               }

               if ((var3 & 512) != 0) {
                  for(var23 = var1.get(); var23 != -1; var23 = var1.get()) {
                     var24 = var9.getPartByIndex(var23);
                     DebugLog.Vehicle.trace("received update PartDoor id=%d part=%s", var2, var24.getId());
                     var24.getDoor().load(var1, 219);
                  }

                  LuaEventManager.triggerEvent("OnContainerUpdate");
                  var9.doDamageOverlay();
               }

               if ((var3 & 256) != 0) {
                  for(var23 = var1.get(); var23 != -1; var23 = var1.get()) {
                     var24 = var9.getPartByIndex(var23);
                     DebugLog.Vehicle.trace("received update PartWindow id=%d part=%s", var2, var24.getId());
                     var24.getWindow().load(var1, 219);
                  }

                  var9.doDamageOverlay();
               }

               if ((var3 & 64) != 0) {
                  this.oldModels.clear();
                  this.oldModels.addAll(var9.models);
                  this.curModels.clear();
                  var23 = var1.get();

                  for(var30 = 0; var30 < var23; ++var30) {
                     var26 = var1.get();
                     byte var32 = var1.get();
                     VehiclePart var14 = var9.getPartByIndex(var26);
                     VehicleScript.Model var15 = (VehicleScript.Model)var14.getScriptPart().models.get(var32);
                     BaseVehicle.ModelInfo var16 = var9.setModelVisible(var14, var15, true);
                     this.curModels.add(var16);
                  }

                  for(var30 = 0; var30 < this.oldModels.size(); ++var30) {
                     BaseVehicle.ModelInfo var33 = (BaseVehicle.ModelInfo)this.oldModels.get(var30);
                     if (!this.curModels.contains(var33)) {
                        var9.setModelVisible(var33.part, var33.scriptModel, false);
                     }
                  }

                  var9.doDamageOverlay();
               }

               var22 = false;

               for(var30 = 0; var30 < var9.getPartCount(); ++var30) {
                  var27 = var9.getPartByIndex(var30);
                  if (var27.updateFlags != 0) {
                     if ((var27.updateFlags & 2048) != 0 && (var27.updateFlags & 128) == 0) {
                        var27.doInventoryItemStats(var27.getInventoryItem(), var27.getMechanicSkillInstaller());
                        var22 = true;
                     }

                     var27.updateFlags = 0;
                  }
               }

               if (var22) {
                  var9.updatePartStats();
                  var9.updateBulletStats();
               }

            }
         }
      }
   }

   public void clientPacket(ByteBuffer var1) {
      byte var2 = var1.get();
      short var3;
      int var4;
      BaseVehicle var7;
      short var15;
      DebugLogStream var10000;
      String var10001;
      String var18;
      byte var21;
      BaseVehicle var22;
      IsoPlayer var27;
      IsoPlayer var28;
      switch (var2) {
         case 1:
            var3 = var1.getShort();
            DebugLog.Vehicle.trace("%s vid=%d", vehiclePacketTypes.get(var2), var3);
            byte var26 = var1.get();
            var18 = GameWindow.ReadString(var1);
            var22 = this.IDToVehicle.get(var3);
            if (var22 != null) {
               IsoGameCharacter var29 = var22.getCharacter(var26);
               if (var29 != null) {
                  var22.setCharacterPosition(var29, var26, var18);
               }
            }
            break;
         case 2:
            var3 = var1.getShort();
            var15 = var1.getShort();
            var21 = var1.get();
            DebugLog.Vehicle.trace("Vehicle enter vid=%d pid=%d seat=%d", var3, var15, Integer.valueOf(var21));
            var22 = this.IDToVehicle.get(var3);
            if (var22 == null) {
               DebugLog.Vehicle.warn("Vehicle vid=%d not found", var3);
            } else {
               var27 = (IsoPlayer)GameClient.IDToPlayerMap.get(var15);
               if (var27 == null) {
                  DebugLog.Vehicle.warn("Player pid=%d not found", var15);
               } else {
                  var28 = (IsoPlayer)Type.tryCastTo(var22.getCharacter(var21), IsoPlayer.class);
                  if (var28 != null && var28 != var27) {
                     var10000 = DebugLog.DetailedInfo;
                     var10001 = var27.getUsername();
                     var10000.warn(var10001 + " got in same seat as " + var28.getUsername());
                  } else {
                     var22.enterRSync(var21, var27, var22);
                  }
               }
            }
            break;
         case 3:
            var3 = var1.getShort();
            var15 = var1.getShort();
            var21 = var1.get();
            DebugLog.Vehicle.trace("Vehicle exit vid=%d pid=%d seat=%d", var3, var15, Integer.valueOf(var21));
            var22 = this.IDToVehicle.get(var3);
            if (var22 == null) {
               DebugLog.Vehicle.warn("Vehicle vid=%d not found", var3);
            } else {
               var27 = (IsoPlayer)GameClient.IDToPlayerMap.get(var15);
               if (var27 == null) {
                  DebugLog.Vehicle.warn("Player pid=%d not found", var15);
               } else {
                  var22.exitRSync(var27);
               }
            }
            break;
         case 4:
            var3 = var1.getShort();
            var15 = var1.getShort();
            var21 = var1.get();
            byte var24 = var1.get();
            DebugLog.Vehicle.trace("Vehicle switch seat vid=%d pid=%d seats=%d=>%d", var3, var15, Integer.valueOf(var21), Integer.valueOf(var24));
            var7 = this.IDToVehicle.get(var3);
            if (var7 == null) {
               DebugLog.Vehicle.warn("Vehicle vid=%d not found", var3);
            } else {
               var28 = (IsoPlayer)GameClient.IDToPlayerMap.get(var15);
               if (var28 == null) {
                  DebugLog.Vehicle.warn("Player pid=%d not found", var15);
               } else {
                  IsoPlayer var9 = (IsoPlayer)Type.tryCastTo(var7.getCharacter(var24), IsoPlayer.class);
                  if (var9 != null && var9 != var28) {
                     var10000 = DebugLog.DetailedInfo;
                     var10001 = var28.getUsername();
                     var10000.warn(var10001 + " switched to same seat as " + var9.getUsername());
                  } else {
                     var7.switchSeat(var28, var24);
                  }
               }
            }
            break;
         case 5:
            if (this.tempVehicle == null || this.tempVehicle.getCell() != IsoWorld.instance.CurrentCell) {
               this.tempVehicle = new BaseVehicle(IsoWorld.instance.CurrentCell);
            }

            var3 = var1.getShort();

            for(var4 = 0; var4 < var3; ++var4) {
               try {
                  this.clientReceiveUpdate(var1);
               } catch (Exception var13) {
                  var13.printStackTrace();
                  return;
               }
            }

            return;
         case 6:
         case 7:
         case 10:
         case 11:
         case 12:
         case 14:
         case 15:
         case 16:
         default:
            DebugLog.Vehicle.warn("Unknown vehicle packet %d", var2);
            break;
         case 8:
            var3 = var1.getShort();
            DebugLog.Vehicle.trace("%s vid=%d", vehiclePacketTypes.get(var2), var3);
            if (this.IDToVehicle.containsKey(var3)) {
               BaseVehicle var19 = this.IDToVehicle.get(var3);
               var19.serverRemovedFromWorld = true;

               try {
                  var19.removeFromWorld();
                  var19.removeFromSquare();
               } finally {
                  if (this.IDToVehicle.containsKey(var3)) {
                     this.unregisterVehicle(var19);
                  }

               }
            }

            VehicleCache.remove(var3);
            break;
         case 9:
            PhysicsPacket var14 = new PhysicsPacket();
            var14.parse(var1, GameClient.connection);
            var14.process(GameClient.connection);
            break;
         case 13:
            var3 = var1.getShort();
            DebugLog.Vehicle.trace("%s vid=%d", vehiclePacketTypes.get(var2), var3);
            Vector3f var16 = new Vector3f();
            Vector3f var20 = new Vector3f();
            var16.x = var1.getFloat();
            var16.y = var1.getFloat();
            var16.z = var1.getFloat();
            var20.x = var1.getFloat();
            var20.y = var1.getFloat();
            var20.z = var1.getFloat();
            var22 = this.IDToVehicle.get(var3);
            if (var22 != null) {
               Bullet.applyCentralForceToVehicle(var22.VehicleID, var16.x, var16.y, var16.z);
               Vector3f var25 = var20.cross(var16);
               Bullet.applyTorqueToVehicle(var22.VehicleID, var25.x, var25.y, var25.z);
            }
            break;
         case 17:
            var3 = var1.getShort();
            var15 = var1.getShort();
            var18 = GameWindow.ReadString(var1);
            String var23 = GameWindow.ReadString(var1);
            DebugLog.Vehicle.trace("Vehicle attach A=%d/%s B=%d/%s", var3, var18, var15, var23);
            this.towedVehicleMap.put(var3, var15);
            var7 = this.IDToVehicle.get(var3);
            BaseVehicle var8 = this.IDToVehicle.get(var15);
            if (var7 != null && var8 != null) {
               var7.addPointConstraint((IsoPlayer)null, var8, var18, var23);
            }
            break;
         case 18:
            var3 = var1.getShort();
            var15 = var1.getShort();
            DebugLog.Vehicle.trace("Vehicle detach A=%d B=%d", var3, var15);
            if (this.towedVehicleMap.containsKey(var3)) {
               this.towedVehicleMap.remove(var3);
            }

            if (this.towedVehicleMap.containsKey(var15)) {
               this.towedVehicleMap.remove(var15);
            }

            BaseVehicle var17 = this.IDToVehicle.get(var3);
            var22 = this.IDToVehicle.get(var15);
            if (var17 != null) {
               var17.breakConstraint(true, true);
            }

            if (var22 != null) {
               var22.breakConstraint(true, true);
            }
            break;
         case 19:
            var3 = var1.getShort();

            for(var4 = 0; var4 < var3; ++var4) {
               short var5 = var1.getShort();
               short var6 = var1.getShort();
               this.towedVehicleMap.put(var5, var6);
            }
      }

   }

   public void sendCollide(BaseVehicle var1, IsoGameCharacter var2, boolean var3) {
      short var4 = var2 == null ? -1 : ((IsoPlayer)var2).OnlineID;
      ByteBufferWriter var5 = GameClient.connection.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var5);
      var5.bb.put((byte)15);
      var5.bb.putShort(var1.VehicleID);
      var5.bb.putShort(var4);
      var5.bb.put((byte)(var3 ? 1 : 0));
      PacketTypes.PacketType.Vehicles.send(GameClient.connection);
      DebugLog.Vehicle.trace("vid=%d pid=%d collide=%b", var1.VehicleID, var4, var3);
   }

   public void sendPassengerPosition(BaseVehicle var1, int var2, String var3) {
      ByteBufferWriter var4 = GameClient.connection.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var4);
      var4.bb.put((byte)1);
      var4.bb.putShort(var1.VehicleID);
      var4.bb.put((byte)var2);
      var4.putUTF(var3);
      PacketTypes.PacketType.Vehicles.send(GameClient.connection);
   }

   public void sendPassengerPosition(BaseVehicle var1, int var2, String var3, UdpConnection var4) {
      for(int var5 = 0; var5 < GameServer.udpEngine.connections.size(); ++var5) {
         UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var5);
         if (var6 != var4) {
            ByteBufferWriter var7 = var6.startPacket();
            PacketTypes.PacketType.Vehicles.doPacket(var7);
            var7.bb.put((byte)1);
            var7.bb.putShort(var1.VehicleID);
            var7.bb.put((byte)var2);
            var7.putUTF(var3);
            PacketTypes.PacketType.Vehicles.send(var6);
         }
      }

   }

   public void sendRequestGetFull(short var1, PacketTypes.PacketType var2) {
      if (!this.vehiclesWaitUpdates.contains(var1)) {
         ByteBufferWriter var3 = GameClient.connection.startPacket();
         PacketTypes.PacketType.Vehicles.doPacket(var3);
         var3.bb.put((byte)11);
         var3.bb.putShort((short)1);
         var3.bb.putShort(var1);
         PacketTypes.PacketType.Vehicles.send(GameClient.connection);
         this.vehiclesWaitUpdates.add(var1);
      }

   }

   public void sendRequestGetFull(List<VehicleCache> var1) {
      if (var1 != null && !var1.isEmpty()) {
         ByteBufferWriter var2 = GameClient.connection.startPacket();
         PacketTypes.PacketType.Vehicles.doPacket(var2);
         var2.bb.put((byte)11);
         var2.bb.putShort((short)var1.size());

         for(int var3 = 0; var3 < var1.size(); ++var3) {
            var2.bb.putShort(((VehicleCache)var1.get(var3)).id);
            this.vehiclesWaitUpdates.add(((VehicleCache)var1.get(var3)).id);
         }

         PacketTypes.PacketType.Vehicles.send(GameClient.connection);
      }

   }

   public void sendRequestGetPosition(short var1, PacketTypes.PacketType var2) {
      if (this.sendRequestGetPositionFrequency.Check()) {
         ByteBufferWriter var3 = GameClient.connection.startPacket();
         var2.doPacket(var3);
         var3.bb.put((byte)12);
         var3.bb.putShort(var1);
         var2.send(GameClient.connection);
         this.vehiclesWaitUpdates.add(var1);
      }

   }

   public void sendAddImpulse(BaseVehicle var1, Vector3f var2, Vector3f var3) {
      UdpConnection var4 = null;

      for(int var5 = 0; var5 < GameServer.udpEngine.connections.size() && var4 == null; ++var5) {
         UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var5);

         for(int var7 = 0; var7 < var6.players.length; ++var7) {
            IsoPlayer var8 = var6.players[var7];
            if (var8 != null && var8.getVehicle() != null && var8.getVehicle().VehicleID == var1.VehicleID) {
               var4 = var6;
               break;
            }
         }
      }

      if (var4 != null) {
         ByteBufferWriter var9 = var4.startPacket();
         PacketTypes.PacketType.Vehicles.doPacket(var9);
         var9.bb.put((byte)13);
         var9.bb.putShort(var1.VehicleID);
         var9.bb.putFloat(var2.x);
         var9.bb.putFloat(var2.y);
         var9.bb.putFloat(var2.z);
         var9.bb.putFloat(var3.x);
         var9.bb.putFloat(var3.y);
         var9.bb.putFloat(var3.z);
         PacketTypes.PacketType.Vehicles.send(var4);
      }

   }

   public void sendSwitchSeat(UdpConnection var1, BaseVehicle var2, IsoGameCharacter var3, int var4, int var5) {
      ByteBufferWriter var6 = var1.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var6);
      var6.bb.put((byte)4);
      var6.bb.putShort(var2.getId());
      var6.bb.putShort(var3.getOnlineID());
      var6.bb.put((byte)var4);
      var6.bb.put((byte)var5);
      PacketTypes.PacketType.Vehicles.send(var1);
   }

   public void sendSwitchSeat(BaseVehicle var1, IsoGameCharacter var2, int var3, int var4) {
      Iterator var5 = GameServer.udpEngine.connections.iterator();

      while(var5.hasNext()) {
         UdpConnection var6 = (UdpConnection)var5.next();
         this.sendSwitchSeat(var6, var1, var2, var3, var4);
      }

   }

   public void sendEnter(UdpConnection var1, BaseVehicle var2, IsoGameCharacter var3, int var4) {
      ByteBufferWriter var5 = var1.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var5);
      var5.bb.put((byte)2);
      var5.bb.putShort(var2.getId());
      var5.bb.putShort(var3.getOnlineID());
      var5.bb.put((byte)var4);
      PacketTypes.PacketType.Vehicles.send(var1);
   }

   public void sendEnter(BaseVehicle var1, IsoGameCharacter var2, int var3) {
      Iterator var4 = GameServer.udpEngine.connections.iterator();

      while(var4.hasNext()) {
         UdpConnection var5 = (UdpConnection)var4.next();
         this.sendEnter(var5, var1, var2, var3);
      }

   }

   public void sendExit(UdpConnection var1, BaseVehicle var2, IsoGameCharacter var3, int var4) {
      ByteBufferWriter var5 = var1.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var5);
      var5.bb.put((byte)3);
      var5.bb.putShort(var2.getId());
      var5.bb.putShort(var3.getOnlineID());
      var5.bb.put((byte)var4);
      PacketTypes.PacketType.Vehicles.send(var1);
   }

   public void sendExit(BaseVehicle var1, IsoGameCharacter var2, int var3) {
      Iterator var4 = GameServer.udpEngine.connections.iterator();

      while(var4.hasNext()) {
         UdpConnection var5 = (UdpConnection)var4.next();
         this.sendExit(var5, var1, var2, (byte)var3);
      }

   }

   public void sendPhysic(BaseVehicle var1) {
      ByteBufferWriter var2 = GameClient.connection.startPacket();
      PacketTypes.PacketType var3 = var1.isReliable ? PacketTypes.PacketType.Vehicles : PacketTypes.PacketType.VehiclesUnreliable;
      var3.doPacket(var2);
      var2.bb.put((byte)9);
      PhysicsPacket var4 = new PhysicsPacket();
      if (var4.set(var1)) {
         var4.write(var2);
         var3.send(GameClient.connection);
      } else {
         GameClient.connection.cancelPacket();
      }

   }

   public void sendTowing(UdpConnection var1, BaseVehicle var2, BaseVehicle var3, String var4, String var5) {
      DebugLog.Vehicle.trace("vidA=%d vidB=%d", var2.VehicleID, var3.VehicleID);
      ByteBufferWriter var6 = var1.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var6);
      var6.bb.put((byte)17);
      var6.bb.putShort(var2.VehicleID);
      var6.bb.putShort(var3.VehicleID);
      GameWindow.WriteString(var6.bb, var4);
      GameWindow.WriteString(var6.bb, var5);
      PacketTypes.PacketType.Vehicles.send(var1);
   }

   public void sendTowing(BaseVehicle var1, BaseVehicle var2, String var3, String var4) {
      if (!this.towedVehicleMap.containsKey(var1.VehicleID)) {
         this.towedVehicleMap.put(var1.VehicleID, var2.VehicleID);

         for(int var5 = 0; var5 < GameServer.udpEngine.connections.size(); ++var5) {
            UdpConnection var6 = (UdpConnection)GameServer.udpEngine.connections.get(var5);
            this.sendTowing(var6, var1, var2, var3, var4);
         }
      }

   }

   public void sendDetachTowing(UdpConnection var1, BaseVehicle var2, BaseVehicle var3) {
      ByteBufferWriter var4 = var1.startPacket();
      PacketTypes.PacketType.Vehicles.doPacket(var4);
      var4.bb.put((byte)18);
      var4.bb.putShort(var2 == null ? -1 : var2.VehicleID);
      var4.bb.putShort(var3 == null ? -1 : var3.VehicleID);
      PacketTypes.PacketType.Vehicles.send(var1);
   }

   public void sendDetachTowing(BaseVehicle var1, BaseVehicle var2) {
      if (var1 != null && this.towedVehicleMap.containsKey(var1.VehicleID)) {
         this.towedVehicleMap.remove(var1.VehicleID);
      }

      if (var2 != null && this.towedVehicleMap.containsKey(var2.VehicleID)) {
         this.towedVehicleMap.remove(var2.VehicleID);
      }

      for(int var3 = 0; var3 < GameServer.udpEngine.connections.size(); ++var3) {
         UdpConnection var4 = (UdpConnection)GameServer.udpEngine.connections.get(var3);
         this.sendDetachTowing(var4, var1, var2);
      }

   }

   public short getTowedVehicleID(short var1) {
      return this.towedVehicleMap.containsKey(var1) ? this.towedVehicleMap.get(var1) : -1;
   }

   private void writePositionOrientation(ByteBuffer var1, BaseVehicle var2) {
      var1.putLong(WorldSimulation.instance.time);
      Quaternionf var3 = var2.savedRot;
      Transform var4 = var2.getWorldTransform(this.tempTransform);
      var4.getRotation(var3);
      var1.putFloat(var3.x);
      var1.putFloat(var3.y);
      var1.putFloat(var3.z);
      var1.putFloat(var3.w);
      var1.putFloat(var2.jniLinearVelocity.x);
      var1.putFloat(var2.jniLinearVelocity.y);
      var1.putFloat(var2.jniLinearVelocity.z);
      var1.putFloat((float)var2.engineSpeed);
      var1.putFloat(var2.throttle);
      var1.putShort((short)var2.wheelInfo.length);

      for(int var5 = 0; var5 < var2.wheelInfo.length; ++var5) {
         var1.putFloat(var2.wheelInfo[var5].steering);
         var1.putFloat(var2.wheelInfo[var5].rotation);
         var1.putFloat(var2.wheelInfo[var5].skidInfo);
         var1.putFloat(var2.wheelInfo[var5].suspensionLength);
      }

   }

   static {
      vehiclePacketTypes.put((byte)1, "PassengerPosition");
      vehiclePacketTypes.put((byte)2, "Enter");
      vehiclePacketTypes.put((byte)3, "Exit");
      vehiclePacketTypes.put((byte)4, "SwitchSeat");
      vehiclePacketTypes.put((byte)5, "Update");
      vehiclePacketTypes.put((byte)8, "Remove");
      vehiclePacketTypes.put((byte)9, "Physic");
      vehiclePacketTypes.put((byte)10, "Config");
      vehiclePacketTypes.put((byte)11, "RequestGetFull");
      vehiclePacketTypes.put((byte)12, "RequestGetPosition");
      vehiclePacketTypes.put((byte)13, "AddImpulse");
      vehiclePacketTypes.put((byte)15, "Collide");
      vehiclePacketTypes.put((byte)17, "TowingCar");
      vehiclePacketTypes.put((byte)18, "DetachTowingCar");
      vehiclePacketTypes.put((byte)19, "InitialWorldState");
   }

   public static final class PosUpdateVars {
      final Transform transform = new Transform();
      final Vector3f vector3f = new Vector3f();
      final Quaternionf quatf = new Quaternionf();
      final float[] wheelSteer = new float[4];
      final float[] wheelRotation = new float[4];
      final float[] wheelSkidInfo = new float[4];
      final float[] wheelSuspensionLength = new float[4];

      public PosUpdateVars() {
      }
   }

   public static final class VehiclePacket {
      public static final byte PassengerPosition = 1;
      public static final byte Enter = 2;
      public static final byte Exit = 3;
      public static final byte SwitchSeat = 4;
      public static final byte Update = 5;
      public static final byte Remove = 8;
      public static final byte Physic = 9;
      public static final byte Config = 10;
      public static final byte RequestGetFull = 11;
      public static final byte RequestGetPosition = 12;
      public static final byte AddImpulse = 13;
      public static final byte Collide = 15;
      public static final byte TowingCar = 17;
      public static final byte DetachTowingCar = 18;
      public static final byte InitialWorldState = 19;

      public VehiclePacket() {
      }
   }
}
