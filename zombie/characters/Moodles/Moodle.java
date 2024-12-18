package zombie.characters.Moodles;

import zombie.SandboxOptions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.core.Color;
import zombie.iso.weather.Temperature;
import zombie.ui.MoodlesUI;

public final class Moodle {
   MoodleType Type;
   private int Level;
   IsoGameCharacter Parent;
   private int painTimer;
   private Color chevronColor;
   private boolean chevronIsUp;
   private int chevronCount;
   private int chevronMax;
   private static Color colorNeg = new Color(0.88235295F, 0.15686275F, 0.15686275F);
   private static Color colorPos = new Color(0.15686275F, 0.88235295F, 0.15686275F);
   private int cantSprintTimer;

   public Moodle(MoodleType var1, IsoGameCharacter var2) {
      this(var1, var2, 0);
   }

   public Moodle(MoodleType var1, IsoGameCharacter var2, int var3) {
      this.painTimer = 0;
      this.chevronColor = Color.white;
      this.chevronIsUp = true;
      this.chevronCount = 0;
      this.chevronMax = 0;
      this.cantSprintTimer = 300;
      this.Parent = var2;
      this.Type = var1;
      this.Level = 0;
      this.chevronMax = var3;
   }

   public int getChevronCount() {
      return this.chevronCount;
   }

   public boolean isChevronIsUp() {
      return this.chevronIsUp;
   }

   public Color getChevronColor() {
      return this.chevronColor;
   }

   public boolean chevronDifference(int var1, boolean var2, Color var3) {
      return var1 != this.chevronCount || var2 != this.chevronIsUp || var3 != this.chevronColor;
   }

   public void setChevron(int var1, boolean var2, Color var3) {
      if (var1 < 0) {
         var1 = 0;
      }

      if (var1 > this.chevronMax) {
         var1 = this.chevronMax;
      }

      this.chevronCount = var1;
      this.chevronIsUp = var2;
      this.chevronColor = var3 != null ? var3 : Color.white;
   }

   public int getLevel() {
      return this.Level;
   }

   public void SetLevel(int var1) {
      if (var1 < 0) {
         var1 = 0;
      }

      if (var1 > 4) {
         var1 = 4;
      }

      this.Level = var1;
   }

   public boolean Update() {
      boolean var1 = false;
      byte var14;
      if (this.Parent.isDead()) {
         boolean var2 = false;
         if (this.Type != MoodleType.Dead && this.Type != MoodleType.Zombie) {
            var14 = 0;
            if (var14 != this.getLevel()) {
               this.SetLevel(var14);
               var1 = true;
            }

            return var1;
         }
      }

      int var7;
      if (this.Type == MoodleType.CantSprint) {
         var7 = 0;
         if (((IsoPlayer)this.Parent).MoodleCantSprint) {
            var7 = 1;
            --this.cantSprintTimer;
            MoodlesUI.getInstance().wiggle(MoodleType.CantSprint);
            if (this.cantSprintTimer == 0) {
               var7 = 0;
               this.cantSprintTimer = 300;
               ((IsoPlayer)this.Parent).MoodleCantSprint = false;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Endurance) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getStats().endurance > 0.75F) {
               var7 = 0;
            } else if (this.Parent.getStats().endurance > 0.5F) {
               var7 = 1;
            } else if (this.Parent.getStats().endurance > 0.25F) {
               var7 = 2;
            } else if (this.Parent.getStats().endurance > 0.1F) {
               var7 = 3;
            } else {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Angry) {
         var7 = 0;
         if (this.Parent.getStats().Anger > 0.75F) {
            var7 = 4;
         } else if (this.Parent.getStats().Anger > 0.5F) {
            var7 = 3;
         } else if (this.Parent.getStats().Anger > 0.25F) {
            var7 = 2;
         } else if (this.Parent.getStats().Anger > 0.1F) {
            var7 = 1;
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Tired) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getStats().fatigue > 0.6F) {
               var7 = 1;
            }

            if (this.Parent.getStats().fatigue > 0.7F) {
               var7 = 2;
            }

            if (this.Parent.getStats().fatigue > 0.8F) {
               var7 = 3;
            }

            if (this.Parent.getStats().fatigue > 0.9F) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Hungry) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getStats().hunger > 0.15F) {
               var7 = 1;
            }

            if (this.Parent.getStats().hunger > 0.25F) {
               var7 = 2;
            }

            if (this.Parent.getStats().hunger > 0.45F) {
               var7 = 3;
            }

            if (this.Parent.getStats().hunger > 0.7F) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Panic) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getStats().Panic > 6.0F) {
               var7 = 1;
            }

            if (this.Parent.getStats().Panic > 30.0F) {
               var7 = 2;
            }

            if (this.Parent.getStats().Panic > 65.0F) {
               var7 = 3;
            }

            if (this.Parent.getStats().Panic > 80.0F) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Sick) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            this.Parent.getStats().Sickness = this.Parent.getBodyDamage().getApparentInfectionLevel() / 100.0F;
            if (this.Parent.getStats().Sickness > 0.25F) {
               var7 = 1;
            }

            if (this.Parent.getStats().Sickness > 0.5F) {
               var7 = 2;
            }

            if (this.Parent.getStats().Sickness > 0.75F) {
               var7 = 3;
            }

            if (this.Parent.getStats().Sickness > 0.9F) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Bored) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            this.Parent.getStats().Boredom = this.Parent.getBodyDamage().getBoredomLevel() / 100.0F;
            if (this.Parent.getStats().Boredom > 0.25F) {
               var7 = 1;
            }

            if (this.Parent.getStats().Boredom > 0.5F) {
               var7 = 2;
            }

            if (this.Parent.getStats().Boredom > 0.75F) {
               var7 = 3;
            }

            if (this.Parent.getStats().Boredom > 0.9F) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Unhappy) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getBodyDamage().getUnhappynessLevel() > 20.0F) {
               var7 = 1;
            }

            if (this.Parent.getBodyDamage().getUnhappynessLevel() > 45.0F) {
               var7 = 2;
            }

            if (this.Parent.getBodyDamage().getUnhappynessLevel() > 60.0F) {
               var7 = 3;
            }

            if (this.Parent.getBodyDamage().getUnhappynessLevel() > 80.0F) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Stress) {
         var7 = 0;
         if (this.Parent.getStats().getStress() > 0.9F) {
            var7 = 4;
         } else if (this.Parent.getStats().getStress() > 0.75F) {
            var7 = 3;
         } else if (this.Parent.getStats().getStress() > 0.5F) {
            var7 = 2;
         } else if (this.Parent.getStats().getStress() > 0.25F) {
            var7 = 1;
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Thirst) {
         var7 = 0;
         if (this.Parent.getStats().thirst > 0.12F) {
            var7 = 1;
         }

         if (this.Parent.getStats().thirst > 0.25F) {
            var7 = 2;
         }

         if (this.Parent.getStats().thirst > 0.7F) {
            var7 = 3;
         }

         if (this.Parent.getStats().thirst > 0.84F) {
            var7 = 4;
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Bleeding) {
         var7 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            var7 = this.Parent.getBodyDamage().getNumPartsBleeding();
            if (var7 > 4) {
               var7 = 4;
            }
         }

         if (var7 != this.getLevel()) {
            this.SetLevel(var7);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Wet) {
         var14 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getBodyDamage().getWetness() > 15.0F) {
               var14 = 1;
            }

            if (this.Parent.getBodyDamage().getWetness() > 40.0F) {
               var14 = 2;
            }

            if (this.Parent.getBodyDamage().getWetness() > 70.0F) {
               var14 = 3;
            }

            if (this.Parent.getBodyDamage().getWetness() > 90.0F) {
               var14 = 4;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.HasACold) {
         var14 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getBodyDamage().getColdStrength() > 20.0F) {
               var14 = 1;
            }

            if (this.Parent.getBodyDamage().getColdStrength() > 40.0F) {
               var14 = 2;
            }

            if (this.Parent.getBodyDamage().getColdStrength() > 60.0F) {
               var14 = 3;
            }

            if (this.Parent.getBodyDamage().getColdStrength() > 75.0F) {
               var14 = 4;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Injured) {
         var14 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (100.0F - this.Parent.getBodyDamage().getHealth() > 20.0F) {
               var14 = 1;
            }

            if (100.0F - this.Parent.getBodyDamage().getHealth() > 40.0F) {
               var14 = 2;
            }

            if (100.0F - this.Parent.getBodyDamage().getHealth() > 60.0F) {
               var14 = 3;
            }

            if (100.0F - this.Parent.getBodyDamage().getHealth() > 75.0F) {
               var14 = 4;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Pain) {
         ++this.painTimer;
         if (this.painTimer < 120) {
            return false;
         }

         this.painTimer = 0;
         var14 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getStats().Pain > 10.0F) {
               var14 = 1;
            }

            if (this.Parent.getStats().Pain > 20.0F) {
               var14 = 2;
            }

            if (this.Parent.getStats().Pain > 50.0F) {
               var14 = 3;
            }

            if (this.Parent.getStats().Pain > 75.0F) {
               var14 = 4;
            }
         }

         if (var14 != this.getLevel()) {
            if (var14 >= 3 && this.getLevel() < 3) {
               IsoGameCharacter var4 = this.Parent;
               if (var4 instanceof IsoPlayer) {
                  IsoPlayer var3 = (IsoPlayer)var4;
                  if (var3.isLocalPlayer()) {
                     var3.playerVoiceSound("PainMoodle");
                  }
               }
            }

            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.HeavyLoad) {
         var14 = 0;
         float var8 = this.Parent.getInventory().getCapacityWeight();
         float var10 = (float)this.Parent.getMaxWeight();
         float var5 = var8 / var10;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if ((double)var5 >= 1.75) {
               var14 = 4;
            } else if ((double)var5 >= 1.5) {
               var14 = 3;
            } else if ((double)var5 >= 1.25) {
               var14 = 2;
            } else if (var5 > 1.0F) {
               var14 = 1;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Drunk) {
         var14 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getStats().Drunkenness > 10.0F) {
               var14 = 1;
            }

            if (this.Parent.getStats().Drunkenness > 30.0F) {
               var14 = 2;
            }

            if (this.Parent.getStats().Drunkenness > 50.0F) {
               var14 = 3;
            }

            if (this.Parent.getStats().Drunkenness > 70.0F) {
               var14 = 4;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Dead) {
         var14 = 0;
         if (this.Parent.isDead()) {
            var14 = 4;
            if (!this.Parent.getBodyDamage().IsFakeInfected() && this.Parent.getBodyDamage().getInfectionLevel() >= 0.001F) {
               var14 = 0;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Zombie) {
         var14 = 0;
         if (this.Parent.isDead() && !this.Parent.getBodyDamage().IsFakeInfected() && this.Parent.getBodyDamage().getInfectionLevel() >= 0.001F) {
            var14 = 4;
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.FoodEaten) {
         var14 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getBodyDamage().getHealthFromFoodTimer() > 0.0F) {
               var14 = 1;
            }

            if (this.Parent.getBodyDamage().getHealthFromFoodTimer() > (float)this.Parent.getBodyDamage().getStandardHealthFromFoodTime()) {
               var14 = 2;
            }

            if (this.Parent.getBodyDamage().getHealthFromFoodTimer() > (float)this.Parent.getBodyDamage().getStandardHealthFromFoodTime() * 2.0F) {
               var14 = 3;
            }

            if (this.Parent.getBodyDamage().getHealthFromFoodTimer() > (float)this.Parent.getBodyDamage().getStandardHealthFromFoodTime() * 3.0F) {
               var14 = 4;
            }
         }

         if (var14 != this.getLevel()) {
            this.SetLevel(var14);
            var1 = true;
         }
      }

      var7 = this.chevronCount;
      boolean var9 = this.chevronIsUp;
      Color var11 = this.chevronColor;
      if ((this.Type == MoodleType.Hyperthermia || this.Type == MoodleType.Hypothermia) && this.Parent instanceof IsoPlayer) {
         if (!(this.Parent.getBodyDamage().getTemperature() < 36.5F) && !(this.Parent.getBodyDamage().getTemperature() > 37.5F)) {
            var7 = 0;
         } else {
            Thermoregulator var12 = this.Parent.getBodyDamage().getThermoregulator();
            if (var12 == null) {
               var7 = 0;
            } else {
               var9 = var12.thermalChevronUp();
               var7 = var12.thermalChevronCount();
            }
         }
      }

      byte var13;
      if (this.Type == MoodleType.Hyperthermia) {
         var13 = 0;
         if (var7 > 0) {
            var11 = var9 ? colorNeg : colorPos;
         }

         if (this.Parent.getBodyDamage().getTemperature() != 0.0F) {
            if (this.Parent.getBodyDamage().getTemperature() > 37.5F) {
               var13 = 1;
            }

            if (this.Parent.getBodyDamage().getTemperature() > 39.0F) {
               var13 = 2;
            }

            if (this.Parent.getBodyDamage().getTemperature() > 40.0F) {
               var13 = 3;
            }

            if (this.Parent.getBodyDamage().getTemperature() > 41.0F) {
               var13 = 4;
            }
         }

         if (var13 != this.getLevel() || var13 > 0 && this.chevronDifference(var7, var9, var11)) {
            this.SetLevel(var13);
            this.setChevron(var7, var9, var11);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Hypothermia) {
         var13 = 0;
         if (var7 > 0) {
            var11 = var9 ? colorPos : colorNeg;
         }

         if (this.Parent.getBodyDamage().getTemperature() != 0.0F) {
            if (this.Parent.getBodyDamage().getTemperature() < 36.5F && this.Parent.getStats().Drunkenness <= 30.0F) {
               var13 = 1;
            }

            if (this.Parent.getBodyDamage().getTemperature() < 35.0F && this.Parent.getStats().Drunkenness <= 70.0F) {
               var13 = 2;
            }

            if (this.Parent.getBodyDamage().getTemperature() < 30.0F) {
               var13 = 3;
            }

            if (this.Parent.getBodyDamage().getTemperature() < 25.0F) {
               var13 = 4;
            }
         }

         if (var13 != this.getLevel() || var13 > 0 && this.chevronDifference(var7, var9, var11)) {
            this.SetLevel(var13);
            this.setChevron(var7, var9, var11);
            var1 = true;
         }
      }

      float var6;
      if (this.Type == MoodleType.Windchill) {
         var13 = 0;
         if (this.Parent instanceof IsoPlayer) {
            var6 = Temperature.getWindChillAmountForPlayer((IsoPlayer)this.Parent);
            if (var6 > 5.0F) {
               var13 = 1;
            }

            if (var6 > 10.0F) {
               var13 = 2;
            }

            if (var6 > 15.0F) {
               var13 = 3;
            }

            if (var6 > 20.0F) {
               var13 = 4;
            }
         }

         if (var13 != this.getLevel()) {
            this.SetLevel(var13);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.Uncomfortable) {
         var13 = 0;
         if (this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getBodyDamage().getDiscomfortLevel() >= 80.0F) {
               var13 = 4;
            } else if (this.Parent.getBodyDamage().getDiscomfortLevel() >= 60.0F) {
               var13 = 3;
            } else if (this.Parent.getBodyDamage().getDiscomfortLevel() >= 40.0F) {
               var13 = 2;
            } else if (this.Parent.getBodyDamage().getDiscomfortLevel() >= 20.0F) {
               var13 = 1;
            }
         }

         if (var13 != this.getLevel()) {
            this.SetLevel(var13);
            var1 = true;
         }
      }

      if (this.Type == MoodleType.NoxiousSmell) {
         var13 = 0;
         if (this.Parent.getBodyDamage() != null && this.Parent.getBodyDamage().getHealth() != 0.0F) {
            if (this.Parent.getCurrentBuilding() != null && this.Parent.getCurrentBuilding().isToxic() && !this.Parent.isProtectedFromToxic(false)) {
               var13 = 4;
            } else if (SandboxOptions.instance.DecayingCorpseHealthImpact.getValue() != 1) {
               var6 = this.Parent.getCorpseSicknessRate();
               if (var6 > 0.0F) {
                  if ((double)var6 > 0.002) {
                     var13 = 3;
                  } else if ((double)var6 > 0.001) {
                     var13 = 2;
                  } else {
                     var13 = 1;
                  }
               }
            }
         }

         if (var13 != this.getLevel()) {
            this.SetLevel(var13);
            var1 = true;
         }
      }

      return var1;
   }
}
