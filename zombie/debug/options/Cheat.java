package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Cheat extends OptionGroup {
   public final ClockOG Clock = (ClockOG)this.newOptionGroup(new ClockOG());
   public final DoorOG Door = (DoorOG)this.newOptionGroup(new DoorOG());
   public final PlayerOG Player = (PlayerOG)this.newOptionGroup(new PlayerOG());
   public final RecipeOG Recipe = (RecipeOG)this.newOptionGroup(new RecipeOG());
   public final TimedActionOG TimedAction = (TimedActionOG)this.newOptionGroup(new TimedActionOG());
   public final VehicleOG Vehicle = (VehicleOG)this.newOptionGroup(new VehicleOG());
   public final WindowOG Window = (WindowOG)this.newOptionGroup(new WindowOG());
   public final FarmingOG Farming = (FarmingOG)this.newOptionGroup(new FarmingOG());

   public Cheat() {
   }

   public static final class ClockOG extends OptionGroup {
      public final BooleanDebugOption Visible = this.newDebugOnlyOption("Visible", false);

      public ClockOG() {
      }
   }

   public static final class DoorOG extends OptionGroup {
      public final BooleanDebugOption Unlock = this.newDebugOnlyOption("Unlock", false);

      public DoorOG() {
      }
   }

   public static final class PlayerOG extends OptionGroup {
      public final BooleanDebugOption StartInvisible = this.newDebugOnlyOption("StartInvisible", false);
      public final BooleanDebugOption InvisibleSprint = this.newDebugOnlyOption("InvisibleSprint", false);
      public final BooleanDebugOption SeeEveryone = this.newDebugOnlyOption("SeeEveryone", false);
      public final BooleanDebugOption FastMovement = this.newDebugOnlyOption("FastMovement", false);
      public final BooleanDebugOption UnlimitedAmmo = this.newDebugOnlyOption("UnlimitedAmmo", false);
      public final BooleanDebugOption UnlimitedCondition = this.newDebugOnlyOption("UnlimitedCondition", false);

      public PlayerOG() {
      }
   }

   public static final class RecipeOG extends OptionGroup {
      public final BooleanDebugOption KnowAll = this.newDebugOnlyOption("KnowAll", false);
      public final BooleanDebugOption SeeAll = this.newDebugOnlyOption("SeeAll", false);

      public RecipeOG() {
      }
   }

   public static final class TimedActionOG extends OptionGroup {
      public final BooleanDebugOption Instant = this.newDebugOnlyOption("Instant", false);

      public TimedActionOG() {
      }
   }

   public static final class VehicleOG extends OptionGroup {
      public final BooleanDebugOption MechanicsAnywhere = this.newDebugOnlyOption("MechanicsAnywhere", false);
      public final BooleanDebugOption StartWithoutKey = this.newDebugOnlyOption("StartWithoutKey", false);

      public VehicleOG() {
      }
   }

   public static final class WindowOG extends OptionGroup {
      public final BooleanDebugOption Unlock = this.newDebugOnlyOption("Unlock", false);

      public WindowOG() {
      }
   }

   public static final class FarmingOG extends OptionGroup {
      public final BooleanDebugOption FastGrow = this.newDebugOnlyOption("FastGrow", false);

      public FarmingOG() {
      }
   }
}
