package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Multiplayer extends OptionGroup {
   public final DebugOG Debug;
   public final DebugFlagsOG DebugFlags;

   public Multiplayer() {
      this.Debug = new DebugOG(this.Group);
      this.DebugFlags = new DebugFlagsOG(this.Group);
   }

   public static final class DebugOG extends OptionGroup {
      public final BooleanDebugOption PlayerZombie = this.newDebugOnlyOption("PlayerZombie", false);
      public final BooleanDebugOption AttackPlayer = this.newDebugOnlyOption("Attack.Player", false);
      public final BooleanDebugOption FollowPlayer = this.newDebugOnlyOption("Follow.Player", false);
      public final BooleanDebugOption AutoEquip = this.newDebugOnlyOption("AutoEquip", false);
      public final BooleanDebugOption SeeNonPvpZones = this.newDebugOnlyOption("SeeNonPvpZones", false);
      public final BooleanDebugOption AnticlippingAlgorithm = this.newDebugOnlyOption("AnticlippingAlgorithm", true);

      DebugOG(IDebugOptionGroup var1) {
         super(var1, "Debug");
      }
   }

   public static final class DebugFlagsOG extends OptionGroup {
      public final IsoGameCharacterOG Self;
      public final IsoGameCharacterOG Player;
      public final IsoGameCharacterOG Animal;
      public final IsoGameCharacterOG Zombie;
      public final IsoDeadBodyOG DeadBody;

      DebugFlagsOG(IDebugOptionGroup var1) {
         super(var1, "DebugFlags");
         this.Self = new IsoGameCharacterOG(this.Group, "Self");
         this.Player = new IsoGameCharacterOG(this.Group, "Player");
         this.Animal = new IsoGameCharacterOG(this.Group, "Animal");
         this.Zombie = new IsoGameCharacterOG(this.Group, "Zombie");
         this.DeadBody = new IsoDeadBodyOG(this.Group);
      }

      public static final class IsoGameCharacterOG extends OptionGroup {
         public final BooleanDebugOption Enable = this.newDebugOnlyOption("Enable", false);
         public final BooleanDebugOption Owner = this.newDebugOnlyOption("Owner", false);
         public final BooleanDebugOption Position = this.newDebugOnlyOption("Position", false);
         public final BooleanDebugOption Prediction = this.newDebugOnlyOption("Prediction", false);
         public final BooleanDebugOption State = this.newDebugOnlyOption("State", false);
         public final BooleanDebugOption Variables = this.newDebugOnlyOption("Variables", false);
         public final BooleanDebugOption Hit = this.newDebugOnlyOption("Hit", false);
         public final BooleanDebugOption Desync = this.newDebugOnlyOption("Desync", false);

         IsoGameCharacterOG(IDebugOptionGroup var1, String var2) {
            super(var1, var2);
         }
      }

      public static final class IsoDeadBodyOG extends OptionGroup {
         public final BooleanDebugOption Enable = this.newDebugOnlyOption("Enable", false);
         public final BooleanDebugOption Position = this.newDebugOnlyOption("Position", false);

         IsoDeadBodyOG(IDebugOptionGroup var1) {
            super(var1, "DeadBody");
         }
      }
   }
}
