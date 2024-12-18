package zombie.ai.sadisticAIDirector;

import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.Moodles.MoodleType;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoWindow;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class SleepingEvent {
   public static final SleepingEvent instance = new SleepingEvent();
   public static boolean zombiesInvasion = false;

   public SleepingEvent() {
   }

   public void setPlayerFallAsleep(IsoPlayer var1, int var2) {
      this.setPlayerFallAsleep(var1, var2, false, false);
   }

   public void setPlayerFallAsleep(IsoPlayer var1, int var2, boolean var3, boolean var4) {
      SleepingEventData var5 = var1.getOrCreateSleepingEventData();
      var5.reset();
      if (ClimateManager.getInstance().isRaining() && this.isExposedToPrecipitation(var1)) {
         var5.bRaining = true;
         var5.bWasRainingAtStart = true;
         var5.rainTimeStartHours = GameTime.getInstance().getWorldAgeHours();
      }

      var5.sleepingTime = (float)var2;
      var1.setTimeOfSleep(GameTime.instance.getTimeOfDay());
      this.doDelayToSleep(var1);
      this.checkNightmare(var1, var2);
      if (var4) {
         Rand.Next(3, var2 - 2);
      }

      if (var5.nightmareWakeUp <= -1) {
         if (SandboxOptions.instance.SleepingEvent.getValue() != 1 && zombiesInvasion || var3) {
            if (var1.getCurrentSquare() == null || var1.getCurrentSquare().getZone() == null || !var1.getCurrentSquare().getZone().haveConstruction) {
               boolean var6 = false;
               if ((GameTime.instance.getHour() >= 0 && GameTime.instance.getHour() < 5 || GameTime.instance.getHour() > 18) && var2 >= 4) {
                  var6 = true;
               }

               byte var7 = 20;
               if (SandboxOptions.instance.SleepingEvent.getValue() == 3) {
                  var7 = 45;
               }

               if (var3 || Rand.Next(100) <= var7 && var1.getCell().getZombieList().size() >= 1 && var2 >= 4) {
                  int var8 = 0;
                  if (var1.getCurrentBuilding() != null) {
                     if (!var3) {
                        IsoGridSquare var9 = null;
                        IsoWindow var10 = null;
                        int var11 = 0;

                        while(true) {
                           if (var11 >= 3) {
                              if (SandboxOptions.instance.SleepingEvent.getValue() == 3) {
                                 var8 = (int)((double)var8 * 1.5);
                              }

                              if (var8 > 70) {
                                 var8 = 70;
                              }

                              if (!var6) {
                                 var8 /= 2;
                              }
                              break;
                           }

                           for(int var12 = var1.getCurrentBuilding().getDef().getX() - 2; var12 < var1.getCurrentBuilding().getDef().getX2() + 2; ++var12) {
                              for(int var13 = var1.getCurrentBuilding().getDef().getY() - 2; var13 < var1.getCurrentBuilding().getDef().getY2() + 2; ++var13) {
                                 var9 = IsoWorld.instance.getCell().getGridSquare(var12, var13, var11);
                                 if (var9 != null) {
                                    boolean var14 = var9.haveElectricity() || IsoWorld.instance.isHydroPowerOn();
                                    if (var14) {
                                       for(int var15 = 0; var15 < var9.getObjects().size(); ++var15) {
                                          IsoObject var16 = (IsoObject)var9.getObjects().get(var15);
                                          if (var16.getContainer() != null && (var16.getContainer().getType().equals("fridge") || var16.getContainer().getType().equals("freezer"))) {
                                             var8 += 3;
                                          }

                                          if (var16 instanceof IsoStove && ((IsoStove)var16).Activated()) {
                                             var8 += 5;
                                          }

                                          if (var16 instanceof IsoTelevision && ((IsoTelevision)var16).getDeviceData().getIsTurnedOn()) {
                                             var8 += 30;
                                          }

                                          if (var16 instanceof IsoRadio && ((IsoRadio)var16).getDeviceData().getIsTurnedOn()) {
                                             var8 += 30;
                                          }
                                       }
                                    }

                                    var10 = var9.getWindow();
                                    if (var10 != null) {
                                       var8 += this.checkWindowStatus(var10);
                                    }

                                    IsoDoor var17 = var9.getIsoDoor();
                                    if (var17 != null && var17.isExterior() && var17.IsOpen()) {
                                       var8 += 25;
                                       var5.openDoor = var17;
                                    }
                                 }
                              }
                           }

                           ++var11;
                        }
                     }

                     if (var3 || Rand.Next(100) <= var8) {
                        var5.forceWakeUpTime = Rand.Next(var2 - 4, var2 - 1);
                        var5.zombiesIntruders = true;
                     }
                  }
               }

            }
         }
      }
   }

   private void doDelayToSleep(IsoPlayer var1) {
      float var2 = 0.3F;
      float var3 = 2.0F;
      if (var1.Traits.Insomniac.isSet()) {
         var2 = 1.0F;
      }

      if (var1.getMoodles().getMoodleLevel(MoodleType.Pain) > 0) {
         var2 += 1.0F + (float)var1.getMoodles().getMoodleLevel(MoodleType.Pain) * 0.2F;
      }

      if (var1.getMoodles().getMoodleLevel(MoodleType.Stress) > 0) {
         var2 *= 1.2F;
      }

      if ("averageBedPillow".equals(var1.getBedType())) {
         var2 *= 1.0F;
      }

      if ("badBed".equals(var1.getBedType())) {
         var2 *= 1.3F;
      } else if ("badBedPillow".equals(var1.getBedType())) {
         var2 *= 1.25F;
      } else if ("goodBed".equals(var1.getBedType())) {
         var2 *= 0.8F;
      } else if ("goodBedPillow".equals(var1.getBedType())) {
         var2 *= 0.6F;
      } else if ("floor".equals(var1.getBedType())) {
         var2 *= 1.6F;
      } else if ("floorPillow".equals(var1.getBedType())) {
         var2 *= 1.45F;
      }

      if (var1.Traits.NightOwl.isSet()) {
         var2 *= 0.5F;
      }

      if (var1.getSleepingTabletEffect() > 1000.0F) {
         var2 = 0.1F;
      }

      if (var2 > var3) {
         var2 = var3;
      }

      float var4 = Rand.Next(0.0F, var2);
      var1.setDelayToSleep(GameTime.instance.getTimeOfDay() + var4);
   }

   private void checkNightmare(IsoPlayer var1, int var2) {
      if (!GameClient.bClient) {
         SleepingEventData var3 = var1.getOrCreateSleepingEventData();
         if (var2 >= 3) {
            int var4 = 5;
            if (var1.Traits.Desensitized.isSet()) {
               var4 += 5;
            }

            var4 += var1.getMoodles().getMoodleLevel(MoodleType.Stress) * 10;
            if (Rand.Next(100) < var4) {
               var3.nightmareWakeUp = Rand.Next(3, var2 - 2);
            }
         }

      }
   }

   private int checkWindowStatus(IsoWindow var1) {
      IsoGridSquare var2 = var1.getSquare();
      if (var1.getSquare().getRoom() == null) {
         if (!var1.north) {
            var2 = var1.getSquare().getCell().getGridSquare(var1.getSquare().getX() - 1, var1.getSquare().getY(), var1.getSquare().getZ());
         } else {
            var2 = var1.getSquare().getCell().getGridSquare(var1.getSquare().getX(), var1.getSquare().getY() - 1, var1.getSquare().getZ());
         }
      }

      boolean var3 = false;
      boolean var4 = false;

      for(int var5 = 0; var5 < var2.getRoom().lightSwitches.size(); ++var5) {
         if (((IsoLightSwitch)var2.getRoom().lightSwitches.get(var5)).isActivated()) {
            var4 = true;
            break;
         }
      }

      int var6;
      IsoBarricade var7;
      if (var4) {
         var6 = 20;
         if (var1.HasCurtains() != null && !var1.HasCurtains().open) {
            var6 -= 17;
         }

         var7 = var1.getBarricadeOnOppositeSquare();
         if (var7 == null) {
            var7 = var1.getBarricadeOnSameSquare();
         }

         if (var7 != null && (var7.getNumPlanks() > 4 || var7.isMetal())) {
            var6 -= 20;
         }

         if (var6 < 0) {
            var6 = 0;
         }

         if (var2.getZ() > 0) {
            var6 /= 2;
         }

         return var6;
      } else {
         var6 = 5;
         if (var1.HasCurtains() != null && !var1.HasCurtains().open) {
            var6 -= 5;
         }

         var7 = var1.getBarricadeOnOppositeSquare();
         if (var7 == null) {
            var7 = var1.getBarricadeOnSameSquare();
         }

         if (var7 != null && (var7.getNumPlanks() > 3 || var7.isMetal())) {
            var6 -= 5;
         }

         if (var6 < 0) {
            var6 = 0;
         }

         if (var2.getZ() > 0) {
            var6 /= 2;
         }

         return var6;
      }
   }

   public void update(IsoPlayer var1) {
      if (var1 != null) {
         SleepingEventData var2 = var1.getOrCreateSleepingEventData();
         Stats var10000;
         if (var1.getStats().getNumVeryCloseZombies() > 0) {
            var10000 = var1.getStats();
            var10000.Panic += 70.0F;
            var10000 = var1.getStats();
            var10000.stress += 0.5F;
            WorldSoundManager.instance.addSound(var1, PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), 6, 1);
            SoundManager.instance.setMusicWakeState(var1, "WakeZombies");
            var2.bFastWakeup = true;
            this.wakeUp(var1);
         }

         if (var2.nightmareWakeUp == (int)var1.getAsleepTime()) {
            var10000 = var1.getStats();
            var10000.Panic += 70.0F;
            var10000 = var1.getStats();
            var10000.stress += 0.5F;
            WorldSoundManager.instance.addSound(var1, PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), 6, 1);
            SoundManager.instance.setMusicWakeState(var1, "WakeNightmare");
            var2.bFastWakeup = true;
            this.wakeUp(var1);
         }

         if (var2.forceWakeUpTime == (int)var1.getAsleepTime() && var2.zombiesIntruders) {
            this.spawnZombieIntruders(var1);
            WorldSoundManager.instance.addSound(var1, PZMath.fastfloor(var1.getX()), PZMath.fastfloor(var1.getY()), PZMath.fastfloor(var1.getZ()), 6, 1);
            SoundManager.instance.setMusicWakeState(var1, "WakeZombies");
            var2.bFastWakeup = true;
            this.wakeUp(var1);
         }

         this.updateRain(var1);
         this.updateSnow(var1);
         this.updateTemperature(var1);
         this.updateWetness(var1);
      }
   }

   private void updateRain(IsoPlayer var1) {
      SleepingEventData var2 = var1.getOrCreateSleepingEventData();
      if (!ClimateManager.getInstance().isRaining()) {
         var2.bRaining = false;
         var2.bWasRainingAtStart = false;
         var2.rainTimeStartHours = -1.0;
      } else if (this.isExposedToPrecipitation(var1)) {
         double var3 = GameTime.getInstance().getWorldAgeHours();
         if (!var2.bWasRainingAtStart) {
            if (!var2.bRaining) {
               var2.rainTimeStartHours = var3;
            }

            if (var2.getHoursSinceRainStarted() >= 0.16666666666666666) {
            }
         }

         var2.bRaining = true;
      }
   }

   private void updateSnow(IsoPlayer var1) {
      if (ClimateManager.getInstance().isSnowing()) {
         if (this.isExposedToPrecipitation(var1)) {
            ;
         }
      }
   }

   private void updateTemperature(IsoPlayer var1) {
   }

   private void updateWetness(IsoPlayer var1) {
   }

   private boolean isExposedToPrecipitation(IsoGameCharacter var1) {
      if (var1.getCurrentSquare() == null) {
         return false;
      } else if (!var1.getCurrentSquare().isInARoom() && !var1.getCurrentSquare().haveRoof) {
         if (var1.getBed() != null && (var1.getBed().isTent() || "Tent".equals(var1.getBed().getName()))) {
            return false;
         } else {
            BaseVehicle var2 = var1.getVehicle();
            return var2 == null || !var2.hasRoof(var2.getSeat(var1));
         }
      } else {
         return false;
      }
   }

   private void spawnZombieIntruders(IsoPlayer var1) {
      SleepingEventData var2 = var1.getOrCreateSleepingEventData();
      IsoGridSquare var3 = null;
      if (var2.openDoor != null) {
         var3 = var2.openDoor.getSquare();
      } else {
         var2.weakestWindow = this.getWeakestWindow(var1);
         if (var2.weakestWindow != null && var2.weakestWindow.getZ() == 0.0F) {
            if (!var2.weakestWindow.north) {
               if (var2.weakestWindow.getSquare().getRoom() == null) {
                  var3 = var2.weakestWindow.getSquare();
               } else {
                  var3 = var2.weakestWindow.getSquare().getCell().getGridSquare(var2.weakestWindow.getSquare().getX() - 1, var2.weakestWindow.getSquare().getY(), var2.weakestWindow.getSquare().getZ());
               }
            } else if (var2.weakestWindow.getSquare().getRoom() == null) {
               var3 = var2.weakestWindow.getSquare();
            } else {
               var3 = var2.weakestWindow.getSquare().getCell().getGridSquare(var2.weakestWindow.getSquare().getX(), var2.weakestWindow.getSquare().getY() + 1, var2.weakestWindow.getSquare().getZ());
            }

            IsoBarricade var4 = var2.weakestWindow.getBarricadeOnOppositeSquare();
            if (var4 == null) {
               var4 = var2.weakestWindow.getBarricadeOnSameSquare();
            }

            if (var4 != null) {
               var4.Damage(Rand.Next(500, 900));
            } else {
               var2.weakestWindow.Damage(200.0F);
               var2.weakestWindow.smashWindow();
               if (var2.weakestWindow.HasCurtains() != null) {
                  var2.weakestWindow.removeSheet((IsoGameCharacter)null);
               }

               if (var3 != null) {
                  var3.addBrokenGlass();
               }
            }
         }
      }

      var1.getStats().setPanic(var1.getStats().getPanic() + (float)Rand.Next(30, 60));
      if (var3 != null) {
         if (IsoWorld.getZombiesEnabled()) {
            int var7 = Rand.Next(3) + 1;

            for(int var5 = 0; var5 < var7; ++var5) {
               VirtualZombieManager.instance.choices.clear();
               VirtualZombieManager.instance.choices.add(var3);
               IsoZombie var6 = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.fromIndex(Rand.Next(8)).index(), false);
               if (var6 != null) {
                  var6.setTarget(var1);
                  var6.pathToCharacter(var1);
                  var6.spotted(var1, true);
                  ZombieSpawnRecorder.instance.record(var6, this.getClass().getSimpleName());
               }
            }
         }

      }
   }

   private IsoWindow getWeakestWindow(IsoPlayer var1) {
      IsoGridSquare var2 = null;
      IsoWindow var3 = null;
      IsoWindow var4 = null;
      int var5 = 0;

      for(int var6 = var1.getCurrentBuilding().getDef().getX() - 2; var6 < var1.getCurrentBuilding().getDef().getX2() + 2; ++var6) {
         for(int var7 = var1.getCurrentBuilding().getDef().getY() - 2; var7 < var1.getCurrentBuilding().getDef().getY2() + 2; ++var7) {
            var2 = IsoWorld.instance.getCell().getGridSquare(var6, var7, 0);
            if (var2 != null) {
               var3 = var2.getWindow();
               if (var3 != null) {
                  int var8 = this.checkWindowStatus(var3);
                  if (var8 > var5) {
                     var5 = var8;
                     var4 = var3;
                  }
               }
            }
         }
      }

      return var4;
   }

   public void wakeUp(IsoGameCharacter var1) {
      if (var1 != null) {
         this.wakeUp(var1, false);
      }
   }

   public void wakeUp(IsoGameCharacter var1, boolean var2) {
      SleepingEventData var3 = var1.getOrCreateSleepingEventData();
      if (GameClient.bClient && !var2) {
         INetworkPacket.send(PacketTypes.PacketType.WakeUpPlayer, (IsoPlayer)var1);
      }

      boolean var4 = false;
      IsoPlayer var5 = (IsoPlayer)Type.tryCastTo(var1, IsoPlayer.class);
      if (var5 != null && var5.isLocalPlayer()) {
         UIManager.setFadeBeforeUI(var5.getPlayerNum(), true);
         UIManager.FadeIn((double)var5.getPlayerNum(), var3.bFastWakeup ? 0.5 : 2.0);
         if (!GameClient.bClient && IsoPlayer.allPlayersAsleep()) {
            UIManager.getSpeedControls().SetCurrentGameSpeed(1);
            var4 = true;
         }

         var1.setLastHourSleeped((int)var5.getHoursSurvived());
      }

      var1.setForceWakeUpTime(-1.0F);
      var1.setAsleep(false);
      if (var4) {
         try {
            GameWindow.save(true);
         } catch (Throwable var8) {
            ExceptionLogger.logException(var8);
         }
      }

      BodyPart var6 = var1.getBodyDamage().getBodyPart(BodyPartType.Neck);
      float var7 = var3.sleepingTime / 8.0F;
      if (!"goodBed".equals(var1.getBedType()) && !"goodBedPillow".equals(var1.getBedType())) {
         if ("badBed".equals(var1.getBedType())) {
            var1.getStats().setFatigue(var1.getStats().getFatigue() + Rand.Next(0.1F, 0.2F) * var7);
            if (Rand.Next(5) == 0) {
               var6.AddDamage(Rand.Next(5.0F, 15.0F));
               var6.setAdditionalPain(var6.getAdditionalPain() + Rand.Next(30.0F, 50.0F));
            }
         } else if ("badBedPillow".equals(var1.getBedType())) {
            var1.getStats().setFatigue(var1.getStats().getFatigue() + Rand.Next(0.1F, 0.2F) * var7);
            if (Rand.Next(10) == 0) {
               var6.AddDamage(Rand.Next(2.5F, 7.5F));
               var6.setAdditionalPain(var6.getAdditionalPain() + Rand.Next(15.0F, 25.0F));
            }
         } else if ("floor".equals(var1.getBedType())) {
            var1.getStats().setFatigue(var1.getStats().getFatigue() + Rand.Next(0.15F, 0.25F) * var7);
            if (Rand.Next(5) == 0) {
               var6.AddDamage(Rand.Next(10.0F, 20.0F));
               var6.setAdditionalPain(var6.getAdditionalPain() + Rand.Next(30.0F, 50.0F));
            }
         } else if ("floorPillow".equals(var1.getBedType())) {
            var1.getStats().setFatigue(var1.getStats().getFatigue() + Rand.Next(0.15F, 0.25F) * var7);
            if (Rand.Next(10) == 0) {
               var6.AddDamage(Rand.Next(5.0F, 10.0F));
               var6.setAdditionalPain(var6.getAdditionalPain() + Rand.Next(15.0F, 25.0F));
            }
         } else if ("averageBedPillow".equals(var1.getBedType())) {
            if (Rand.Next(20) == 0) {
               var6.AddDamage(Rand.Next(1.5F, 6.0F));
               var6.setAdditionalPain(var6.getAdditionalPain() + Rand.Next(5.0F, 15.0F));
            }
         } else if (Rand.Next(10) == 0) {
            var6.AddDamage(Rand.Next(3.0F, 12.0F));
            var6.setAdditionalPain(var6.getAdditionalPain() + Rand.Next(10.0F, 30.0F));
         }
      } else {
         var1.getStats().setFatigue(var1.getStats().getFatigue() - Rand.Next(0.05F, 0.12F) * var7);
         if (var1.getStats().getFatigue() < 0.0F) {
            var1.getStats().setFatigue(0.0F);
         }
      }

      var3.reset();
   }
}
