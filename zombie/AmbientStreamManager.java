package zombie;

import fmod.javafmod;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_EVENT_CALLBACK;
import fmod.fmod.FMOD_STUDIO_EVENT_CALLBACK_TYPE;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import org.joml.Vector2f;
import zombie.Lua.LuaEventManager;
import zombie.audio.parameters.ParameterCameraZoom;
import zombie.audio.parameters.ParameterCharacterElevation;
import zombie.audio.parameters.ParameterClosestWallDistance;
import zombie.audio.parameters.ParameterFogIntensity;
import zombie.audio.parameters.ParameterHardOfHearing;
import zombie.audio.parameters.ParameterInside;
import zombie.audio.parameters.ParameterMoodlePanic;
import zombie.audio.parameters.ParameterPowerSupply;
import zombie.audio.parameters.ParameterRainIntensity;
import zombie.audio.parameters.ParameterRoomSize;
import zombie.audio.parameters.ParameterRoomType;
import zombie.audio.parameters.ParameterRoomTypeEx;
import zombie.audio.parameters.ParameterSeason;
import zombie.audio.parameters.ParameterSnowIntensity;
import zombie.audio.parameters.ParameterStorm;
import zombie.audio.parameters.ParameterStreamerMode;
import zombie.audio.parameters.ParameterTemperature;
import zombie.audio.parameters.ParameterTimeOfDay;
import zombie.audio.parameters.ParameterWaterSupply;
import zombie.audio.parameters.ParameterWeatherEvent;
import zombie.audio.parameters.ParameterWindIntensity;
import zombie.audio.parameters.ParameterZone;
import zombie.audio.parameters.ParameterZoneWaterSide;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugType;
import zombie.input.Mouse;
import zombie.iso.Alarm;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.objects.RainManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;

public final class AmbientStreamManager extends BaseAmbientStreamManager {
   public static int OneInAmbienceChance = 2500;
   public static int MaxAmbientCount = 20;
   public static float MaxRange = 1000.0F;
   public final ArrayList<Alarm> alarmList = new ArrayList();
   public static BaseAmbientStreamManager instance;
   public final ArrayList<Ambient> ambient = new ArrayList();
   public final ArrayList<WorldSoundEmitter> worldEmitters = new ArrayList();
   public final ArrayDeque<WorldSoundEmitter> freeEmitters = new ArrayDeque();
   public final ArrayList<AmbientLoop> allAmbient = new ArrayList();
   public final ArrayList<AmbientLoop> nightAmbient = new ArrayList();
   public final ArrayList<AmbientLoop> dayAmbient = new ArrayList();
   public final ArrayList<AmbientLoop> rainAmbient = new ArrayList();
   public final ArrayList<AmbientLoop> indoorAmbient = new ArrayList();
   public final ArrayList<AmbientLoop> outdoorAmbient = new ArrayList();
   public final ArrayList<AmbientLoop> windAmbient = new ArrayList();
   public boolean initialized = false;
   private FMODSoundEmitter electricityShutOffEmitter = null;
   private long electricityShutOffEvent = 0L;
   private int electricityShutOffState = -1;
   private final ParameterFogIntensity parameterFogIntensity = new ParameterFogIntensity();
   private final ParameterRainIntensity parameterRainIntensity = new ParameterRainIntensity();
   private final ParameterSeason parameterSeason = new ParameterSeason();
   private final ParameterSnowIntensity parameterSnowIntensity = new ParameterSnowIntensity();
   private final ParameterStorm parameterStorm = new ParameterStorm();
   private final ParameterTimeOfDay parameterTimeOfDay = new ParameterTimeOfDay();
   private final ParameterTemperature parameterTemperature = new ParameterTemperature();
   private final ParameterWeatherEvent parameterWeatherEvent = new ParameterWeatherEvent();
   private final ParameterWindIntensity parameterWindIntensity = new ParameterWindIntensity();
   private final ParameterStreamerMode parameterStreamerMode = new ParameterStreamerMode();
   private final ParameterZone parameterZoneDeepForest = new ParameterZone("ZoneDeepForest", "DeepForest");
   private final ParameterZone parameterZoneFarm = new ParameterZone("ZoneFarm", "Farm");
   private final ParameterZone parameterZoneForest = new ParameterZone("ZoneForest", "Forest");
   private final ParameterZone parameterZoneNav = new ParameterZone("ZoneNav", "Nav");
   private final ParameterZone parameterZoneTown = new ParameterZone("ZoneTown", "TownZone");
   private final ParameterZone parameterZoneTrailerPark = new ParameterZone("ZoneTrailerPark", "TrailerPark");
   private final ParameterZone parameterZoneVegetation = new ParameterZone("ZoneVegetation", "Vegitation");
   private final ParameterZoneWaterSide parameterZoneWaterSide = new ParameterZoneWaterSide();
   private final ParameterCameraZoom parameterCameraZoom = new ParameterCameraZoom();
   private final ParameterCharacterElevation parameterCharacterElevation = new ParameterCharacterElevation();
   private final ParameterClosestWallDistance parameterClosestWallDistance = new ParameterClosestWallDistance();
   private final ParameterHardOfHearing parameterHardOfHearing = new ParameterHardOfHearing();
   private final ParameterInside parameterInside = new ParameterInside();
   private final ParameterMoodlePanic parameterMoodlePanic = new ParameterMoodlePanic();
   private final ParameterPowerSupply parameterPowerSupply = new ParameterPowerSupply();
   private final ParameterRoomSize parameterRoomSize = new ParameterRoomSize();
   private final ParameterRoomType parameterRoomType = new ParameterRoomType();
   private final ParameterRoomTypeEx parameterRoomTypeEx = new ParameterRoomTypeEx();
   private final ParameterWaterSupply parameterWaterSupply = new ParameterWaterSupply();
   private final Vector2 tempo = new Vector2();
   private final FMOD_STUDIO_EVENT_CALLBACK electricityShutOffEventCallback = new FMOD_STUDIO_EVENT_CALLBACK() {
      public void timelineMarker(long var1, String var3, int var4) {
         DebugLog.Sound.debugln("timelineMarker %s %d", var3, var4);
         if ("ElectricityOff".equals(var3)) {
            IsoWorld.instance.setHydroPowerOn(false);
            AmbientStreamManager.this.checkHaveElectricity();
         }

      }
   };

   public AmbientStreamManager() {
   }

   public static BaseAmbientStreamManager getInstance() {
      return instance;
   }

   public void update() {
      if (this.initialized) {
         if (!GameTime.isGamePaused()) {
            IsoPlayer var1 = null;
            IsoPlayer var2 = null;

            for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
               IsoPlayer var4 = IsoPlayer.players[var3];
               if (var4 != null) {
                  if (!var4.Traits.Deaf.isSet()) {
                     var1 = var4;
                     break;
                  }

                  if (var2 == null) {
                     var2 = var4;
                  }
               }
            }

            if (var1 == null) {
               var1 = var2;
            }

            if (var1 != null) {
               IsoGridSquare var9 = var1.getCurrentSquare();
               if (var9 != null) {
                  this.updatePowerSupply();
                  this.parameterFogIntensity.update();
                  this.parameterRainIntensity.update();
                  this.parameterSeason.update();
                  this.parameterSnowIntensity.update();
                  this.parameterStorm.update();
                  this.parameterStreamerMode.update();
                  this.parameterTemperature.update();
                  this.parameterTimeOfDay.update();
                  this.parameterWeatherEvent.update();
                  this.parameterWindIntensity.update();
                  this.parameterZoneDeepForest.update();
                  this.parameterZoneFarm.update();
                  this.parameterZoneForest.update();
                  this.parameterZoneNav.update();
                  this.parameterZoneVegetation.update();
                  this.parameterZoneTown.update();
                  this.parameterZoneTrailerPark.update();
                  this.parameterZoneWaterSide.update();
                  this.parameterCameraZoom.update();
                  this.parameterCharacterElevation.update();
                  this.parameterClosestWallDistance.update();
                  this.parameterHardOfHearing.update();
                  this.parameterInside.update();
                  this.parameterMoodlePanic.update();
                  this.parameterPowerSupply.update();
                  this.parameterRoomSize.update();
                  this.parameterRoomTypeEx.update();
                  this.parameterWaterSupply.update();
                  float var10 = GameTime.instance.getTimeOfDay();

                  for(int var5 = 0; var5 < this.worldEmitters.size(); ++var5) {
                     WorldSoundEmitter var6 = (WorldSoundEmitter)this.worldEmitters.get(var5);
                     IsoGridSquare var7;
                     if (var6.daytime != null) {
                        var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)var6.x, (double)var6.y, (double)var6.z);
                        if (var7 == null) {
                           var6.fmodEmitter.stopAll();
                           SoundManager.instance.unregisterEmitter(var6.fmodEmitter);
                           this.worldEmitters.remove(var6);
                           this.freeEmitters.add(var6);
                           --var5;
                        } else {
                           if (var10 > var6.dawn && var10 < var6.dusk) {
                              if (var6.fmodEmitter.isEmpty()) {
                                 var6.channel = var6.fmodEmitter.playAmbientLoopedImpl(var6.daytime);
                              }
                           } else if (!var6.fmodEmitter.isEmpty()) {
                              var6.fmodEmitter.stopSound(var6.channel);
                              var6.channel = 0L;
                           }

                           if (!var6.fmodEmitter.isEmpty() && (IsoWorld.instance.emitterUpdate || var6.fmodEmitter.hasSoundsToStart())) {
                              var6.fmodEmitter.tick();
                           }
                        }
                     } else if (var1 != null && var1.Traits.Deaf.isSet()) {
                        var6.fmodEmitter.stopAll();
                        SoundManager.instance.unregisterEmitter(var6.fmodEmitter);
                        this.worldEmitters.remove(var6);
                        this.freeEmitters.add(var6);
                        --var5;
                     } else {
                        var7 = IsoWorld.instance.CurrentCell.getGridSquare((double)var6.x, (double)var6.y, (double)var6.z);
                        if (var7 != null && !var6.fmodEmitter.isEmpty()) {
                           var6.fmodEmitter.x = var6.x;
                           var6.fmodEmitter.y = var6.y;
                           var6.fmodEmitter.z = var6.z;
                           if (IsoWorld.instance.emitterUpdate || var6.fmodEmitter.hasSoundsToStart()) {
                              var6.fmodEmitter.tick();
                           }
                        } else {
                           var6.fmodEmitter.stopAll();
                           SoundManager.instance.unregisterEmitter(var6.fmodEmitter);
                           this.worldEmitters.remove(var6);
                           this.freeEmitters.add(var6);
                           --var5;
                        }
                     }
                  }

                  float var12 = GameTime.instance.getNight();
                  boolean var11 = var9.isInARoom();
                  boolean var13 = RainManager.isRaining();

                  int var8;
                  for(var8 = 0; var8 < this.allAmbient.size(); ++var8) {
                     ((AmbientLoop)this.allAmbient.get(var8)).targVol = 1.0F;
                  }

                  AmbientLoop var10000;
                  for(var8 = 0; var8 < this.nightAmbient.size(); ++var8) {
                     var10000 = (AmbientLoop)this.nightAmbient.get(var8);
                     var10000.targVol *= var12;
                  }

                  for(var8 = 0; var8 < this.dayAmbient.size(); ++var8) {
                     var10000 = (AmbientLoop)this.dayAmbient.get(var8);
                     var10000.targVol *= 1.0F - var12;
                  }

                  for(var8 = 0; var8 < this.indoorAmbient.size(); ++var8) {
                     var10000 = (AmbientLoop)this.indoorAmbient.get(var8);
                     var10000.targVol *= var11 ? 0.8F : 0.0F;
                  }

                  for(var8 = 0; var8 < this.outdoorAmbient.size(); ++var8) {
                     var10000 = (AmbientLoop)this.outdoorAmbient.get(var8);
                     var10000.targVol *= var11 ? 0.15F : 0.8F;
                  }

                  for(var8 = 0; var8 < this.rainAmbient.size(); ++var8) {
                     var10000 = (AmbientLoop)this.rainAmbient.get(var8);
                     var10000.targVol *= var13 ? 1.0F : 0.0F;
                     if (((AmbientLoop)this.rainAmbient.get(var8)).channel != 0L) {
                        javafmod.FMOD_Studio_EventInstance_SetParameterByName(((AmbientLoop)this.rainAmbient.get(var8)).channel, "RainIntensity", ClimateManager.getInstance().getPrecipitationIntensity());
                     }
                  }

                  for(var8 = 0; var8 < this.allAmbient.size(); ++var8) {
                     ((AmbientLoop)this.allAmbient.get(var8)).update();
                  }

                  for(var8 = 0; var8 < this.alarmList.size(); ++var8) {
                     ((Alarm)this.alarmList.get(var8)).update();
                     if (((Alarm)this.alarmList.get(var8)).finished) {
                        this.alarmList.remove(var8);
                        --var8;
                     }
                  }

                  this.doOneShotAmbients();
               }
            }
         }
      }
   }

   public void doOneShotAmbients() {
      for(int var1 = 0; var1 < this.ambient.size(); ++var1) {
         Ambient var2 = (Ambient)this.ambient.get(var1);
         if (var2.finished()) {
            DebugLog.log(DebugType.Sound, "ambient: removing ambient sound " + var2.name);
            this.ambient.remove(var1--);
         } else {
            var2.update();
         }
      }

   }

   public void addRandomAmbient() {
      // $FF: Couldn't be decompiled
   }

   public void addBlend(String var1, float var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      AmbientLoop var7 = new AmbientLoop(0.0F, var1, var2);
      this.allAmbient.add(var7);
      if (var3) {
         this.indoorAmbient.add(var7);
      } else {
         this.outdoorAmbient.add(var7);
      }

      if (var4) {
         this.rainAmbient.add(var7);
      }

      if (var5) {
         this.nightAmbient.add(var7);
      }

      if (var6) {
         this.dayAmbient.add(var7);
      }

   }

   public void init() {
      if (!this.initialized) {
         this.initialized = true;
      }
   }

   public void doGunEvent() {
      // $FF: Couldn't be decompiled
   }

   public void doAlarm(RoomDef var1) {
      if (var1 != null && var1.building != null && var1.building.bAlarmed) {
         DebugLog.Sound.debugln("Elec shutoff = " + SandboxOptions.getInstance().getElecShutModifier());
         DebugLog.Sound.debugln("alarm decay = " + var1.building.bAlarmDecay);
         DebugLogStream var10000 = DebugLog.Sound;
         double var10001 = GameTime.getInstance().getWorldAgeHours() / 24.0;
         int var10002 = SandboxOptions.instance.TimeSinceApo.getValue() - 1;
         var10000.debugln("nights survived = " + (float)(var10001 + (double)(var10002 * 30)));
         if ((float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) <= (float)(SandboxOptions.getInstance().getElecShutModifier() + var1.building.bAlarmDecay)) {
            this.alarmList.add(new Alarm(var1.x + var1.getW() / 2, var1.y + var1.getH() / 2));
         }

         var1.building.bAlarmed = false;
         var1.building.setAllExplored(true);
      }

   }

   private int GetDistance(int var1, int var2, int var3, int var4) {
      return (int)Math.sqrt(Math.pow((double)(var1 - var3), 2.0) + Math.pow((double)(var2 - var4), 2.0));
   }

   public void handleThunderEvent(int var1, int var2) {
      int var3 = 9999999;

      for(int var4 = 0; var4 < IsoPlayer.numPlayers; ++var4) {
         IsoPlayer var5 = IsoPlayer.players[var4];
         if (var5 != null && var5.isAlive()) {
            int var6 = this.GetDistance((int)var5.getX(), (int)var5.getY(), var1, var2);
            if (var6 < var3) {
               var3 = var6;
            }
         }
      }

      if (var3 <= 5000) {
         WorldSoundManager.instance.addSound((Object)null, PZMath.fastfloor((float)var1), PZMath.fastfloor((float)var2), 0, 5000, 5000);
         Ambient var7 = new Ambient("", (float)var1, (float)var2, 5100.0F, 1.0F);
         this.ambient.add(var7);
      }

   }

   public void stop() {
      Iterator var1 = this.allAmbient.iterator();

      while(var1.hasNext()) {
         AmbientLoop var2 = (AmbientLoop)var1.next();
         var2.stop();
      }

      this.allAmbient.clear();
      this.ambient.clear();
      this.dayAmbient.clear();
      this.indoorAmbient.clear();
      this.nightAmbient.clear();
      this.outdoorAmbient.clear();
      this.rainAmbient.clear();
      this.windAmbient.clear();
      this.alarmList.clear();
      if (this.electricityShutOffEmitter != null) {
         this.electricityShutOffEmitter.stopAll();
         this.electricityShutOffEvent = 0L;
      }

      this.electricityShutOffState = -1;
      this.initialized = false;
   }

   public void addAmbient(String var1, int var2, int var3, int var4, float var5) {
      if (GameClient.bClient) {
         Ambient var6 = new Ambient(var1, (float)var2, (float)var3, (float)var4, var5, true);
         this.ambient.add(var6);
      }
   }

   public void addAmbientEmitter(float var1, float var2, int var3, String var4) {
      WorldSoundEmitter var5 = this.freeEmitters.isEmpty() ? new WorldSoundEmitter() : (WorldSoundEmitter)this.freeEmitters.pop();
      var5.x = var1;
      var5.y = var2;
      var5.z = (float)var3;
      var5.daytime = null;
      if (var5.fmodEmitter == null) {
         var5.fmodEmitter = new FMODSoundEmitter();
      }

      var5.fmodEmitter.x = var1;
      var5.fmodEmitter.y = var2;
      var5.fmodEmitter.z = (float)var3;
      var5.channel = var5.fmodEmitter.playAmbientLoopedImpl(var4);
      var5.fmodEmitter.randomStart();
      SoundManager.instance.registerEmitter(var5.fmodEmitter);
      this.worldEmitters.add(var5);
   }

   public void addDaytimeAmbientEmitter(float var1, float var2, int var3, String var4) {
      WorldSoundEmitter var5 = this.freeEmitters.isEmpty() ? new WorldSoundEmitter() : (WorldSoundEmitter)this.freeEmitters.pop();
      var5.x = var1;
      var5.y = var2;
      var5.z = (float)var3;
      if (var5.fmodEmitter == null) {
         var5.fmodEmitter = new FMODSoundEmitter();
      }

      var5.fmodEmitter.x = var1;
      var5.fmodEmitter.y = var2;
      var5.fmodEmitter.z = (float)var3;
      var5.daytime = var4;
      var5.dawn = Rand.Next(7.0F, 8.0F);
      var5.dusk = Rand.Next(19.0F, 20.0F);
      SoundManager.instance.registerEmitter(var5.fmodEmitter);
      this.worldEmitters.add(var5);
   }

   private void updatePowerSupply() {
      boolean var1 = (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (double)((SandboxOptions.instance.TimeSinceApo.getValue() - 1) * 30)) < (float)SandboxOptions.getInstance().getElecShutModifier();
      if (this.electricityShutOffState == -1) {
         IsoWorld.instance.setHydroPowerOn(var1);
      }

      if (this.electricityShutOffState == 0 && var1) {
         IsoWorld.instance.setHydroPowerOn(true);
         this.checkHaveElectricity();
      }

      if (this.electricityShutOffState == 1 && !var1) {
         if (this.electricityShutOffEmitter == null) {
            this.electricityShutOffEmitter = new FMODSoundEmitter();
         }

         if (!this.electricityShutOffEmitter.isPlaying(this.electricityShutOffEvent)) {
            Vector2f var2 = new Vector2f();
            this.getListenerPos(var2);
            BuildingDef var3 = getNearestBuilding(var2.x, var2.y, var2);
            if (var3 == null) {
               this.electricityShutOffEmitter.setPos(-1000.0F, -1000.0F, 0.0F);
            } else {
               this.electricityShutOffEmitter.setPos(var2.x, var2.y, 0.0F);
            }

            this.electricityShutOffEvent = this.electricityShutOffEmitter.playSound("WorldEventElectricityShutdown");
            if (this.electricityShutOffEvent != 0L) {
               javafmod.FMOD_Studio_EventInstance_SetCallback(this.electricityShutOffEvent, this.electricityShutOffEventCallback, FMOD_STUDIO_EVENT_CALLBACK_TYPE.FMOD_STUDIO_EVENT_CALLBACK_TIMELINE_MARKER.bit);
            }
         }
      }

      this.electricityShutOffState = var1 ? 1 : 0;
      if (this.electricityShutOffEmitter != null) {
         this.electricityShutOffEmitter.tick();
      }

   }

   public void checkHaveElectricity() {
      for(int var1 = 0; var1 < IsoPlayer.numPlayers; ++var1) {
         IsoChunkMap var2 = IsoWorld.instance.CurrentCell.ChunkMap[var1];
         if (!var2.ignore) {
            for(int var3 = -32; var3 <= 31; ++var3) {
               for(int var4 = var2.getWorldYMinTiles(); var4 <= var2.getWorldYMaxTiles(); ++var4) {
                  for(int var5 = var2.getWorldXMinTiles(); var5 <= var2.getWorldXMaxTiles(); ++var5) {
                     IsoGridSquare var6 = IsoWorld.instance.CurrentCell.getGridSquare(var5, var4, var3);
                     if (var6 != null) {
                        for(int var7 = 0; var7 < var6.getObjects().size(); ++var7) {
                           IsoObject var8 = (IsoObject)var6.getObjects().get(var7);
                           var8.checkHaveElectricity();
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public boolean isParameterInsideTrue() {
      return this.parameterInside.getCurrentValue() > 0.0F;
   }

   public static BuildingDef getNearestBuilding(float var0, float var1, Vector2f var2) {
      IsoMetaGrid var3 = IsoWorld.instance.getMetaGrid();
      int var4 = PZMath.fastfloor(var0 / (float)IsoCell.CellSizeInSquares);
      int var5 = PZMath.fastfloor(var1 / (float)IsoCell.CellSizeInSquares);
      BuildingDef var6 = null;
      float var7 = 3.4028235E38F;
      var2.set(0.0F);
      Vector2f var8 = new Vector2f();

      for(int var9 = var5 - 1; var9 <= var5 + 1; ++var9) {
         for(int var10 = var4 - 1; var10 <= var4 + 1; ++var10) {
            IsoMetaCell var11 = var3.getCellData(var10, var9);
            if (var11 != null && var11.info != null) {
               Iterator var12 = var11.info.Buildings.iterator();

               while(var12.hasNext()) {
                  BuildingDef var13 = (BuildingDef)var12.next();
                  float var14 = var13.getClosestPoint(var0, var1, var8);
                  if (var14 < var7) {
                     var7 = var14;
                     var6 = var13;
                     var2.set(var8);
                  }
               }
            }
         }
      }

      return var6;
   }

   private void getListenerPos(Vector2f var1) {
      IsoPlayer var2 = null;
      var1.set(0.0F);

      for(int var3 = 0; var3 < IsoPlayer.numPlayers; ++var3) {
         IsoPlayer var4 = IsoPlayer.players[var3];
         if (var4 != null && (var2 == null || var2.isDead() && var4.isAlive() || var2.Traits.Deaf.isSet() && !var4.Traits.Deaf.isSet())) {
            var2 = var4;
            var1.set(var4.getX(), var4.getY());
         }
      }

   }

   public void save(ByteBuffer var1) {
      var1.putShort((short)this.alarmList.size());

      for(int var2 = 0; var2 < this.alarmList.size(); ++var2) {
         ((Alarm)this.alarmList.get(var2)).save(var1);
      }

   }

   public void load(ByteBuffer var1, int var2) {
      short var3 = var1.getShort();

      for(int var4 = 0; var4 < var3; ++var4) {
         Alarm var5 = new Alarm(0, 0);
         var5.load(var1, var2);
         this.alarmList.add(var5);
      }

   }

   public static final class WorldSoundEmitter {
      public FMODSoundEmitter fmodEmitter;
      public float x;
      public float y;
      public float z;
      public long channel = -1L;
      public String daytime;
      public float dawn;
      public float dusk;

      public WorldSoundEmitter() {
      }
   }

   public static final class AmbientLoop {
      public static float volChangeAmount = 0.01F;
      public float targVol;
      public float currVol;
      public String name;
      public float volumedelta = 1.0F;
      public long channel = -1L;
      public final FMODSoundEmitter emitter = new FMODSoundEmitter();

      public AmbientLoop(float var1, String var2, float var3) {
         this.volumedelta = var3;
         this.channel = this.emitter.playAmbientLoopedImpl(var2);
         this.targVol = var1;
         this.currVol = 0.0F;
         this.update();
      }

      public void update() {
         if (this.targVol > this.currVol) {
            this.currVol += volChangeAmount;
            if (this.currVol > this.targVol) {
               this.currVol = this.targVol;
            }
         }

         if (this.targVol < this.currVol) {
            this.currVol -= volChangeAmount;
            if (this.currVol < this.targVol) {
               this.currVol = this.targVol;
            }
         }

         this.emitter.setVolumeAll(this.currVol * this.volumedelta);
         this.emitter.tick();
      }

      public void stop() {
         this.emitter.stopAll();
      }
   }

   public static final class Ambient {
      public float x;
      public float y;
      public String name;
      float radius;
      float volume;
      int worldSoundRadius;
      int worldSoundVolume;
      public boolean trackMouse;
      final FMODSoundEmitter emitter;

      public Ambient(String var1, float var2, float var3, float var4, float var5) {
         this(var1, var2, var3, var4, var5, false);
      }

      public Ambient(String var1, float var2, float var3, float var4, float var5, boolean var6) {
         this.trackMouse = false;
         this.emitter = new FMODSoundEmitter();
         this.name = var1;
         this.x = var2;
         this.y = var3;
         this.radius = var4;
         this.volume = var5;
         this.emitter.x = var2;
         this.emitter.y = var3;
         this.emitter.z = 0.0F;
         this.emitter.playAmbientSound(var1);
         this.update();
         LuaEventManager.triggerEvent("OnAmbientSound", var1, var2, var3);
      }

      public boolean finished() {
         return this.emitter.isEmpty();
      }

      public void update() {
         this.emitter.tick();
         if (this.trackMouse && IsoPlayer.getInstance() != null) {
            float var1 = (float)Mouse.getXA();
            float var2 = (float)Mouse.getYA();
            var1 -= (float)IsoCamera.getScreenLeft(IsoPlayer.getPlayerIndex());
            var2 -= (float)IsoCamera.getScreenTop(IsoPlayer.getPlayerIndex());
            var1 *= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
            var2 *= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
            int var3 = (int)IsoPlayer.getInstance().getZ();
            this.emitter.x = (float)((int)IsoUtils.XToIso(var1, var2, (float)var3));
            this.emitter.y = (float)((int)IsoUtils.YToIso(var1, var2, (float)var3));
         }

         if (!GameClient.bClient && this.worldSoundRadius > 0 && this.worldSoundVolume > 0) {
            WorldSoundManager.instance.addSound((Object)null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, this.worldSoundRadius, this.worldSoundVolume);
         }

      }

      public void repeatWorldSounds(int var1, int var2) {
         this.worldSoundRadius = var1;
         this.worldSoundVolume = var2;
      }

      private IsoGameCharacter getClosestListener(float var1, float var2) {
         IsoPlayer var3 = null;
         float var4 = 3.4028235E38F;

         for(int var5 = 0; var5 < IsoPlayer.numPlayers; ++var5) {
            IsoPlayer var6 = IsoPlayer.players[var5];
            if (var6 != null && var6.getCurrentSquare() != null) {
               float var7 = var6.getX();
               float var8 = var6.getY();
               float var9 = IsoUtils.DistanceToSquared(var7, var8, var1, var2);
               var9 *= PZMath.pow(var6.getHearDistanceModifier(), 2.0F);
               if (var6.Traits.Deaf.isSet()) {
                  var9 = 3.4028235E38F;
               }

               if (var9 < var4) {
                  var3 = var6;
                  var4 = var9;
               }
            }
         }

         return var3;
      }
   }
}
