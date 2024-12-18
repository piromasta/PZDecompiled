package zombie;

public final class SoundAssetManager {
   private static SoundAssetManager soundAssetManager;
   public static final String UIActivateButton = "UIActivateButton";
   public static final String UISelectListItem = "UISelectListItem";
   public static final String UIToggleComboBox = "UIToggleComboBox";
   public static final String UIToggleTickBox = "UIToggleTickBox";
   public static final String UIIClickToStart = "UIClickToStart";
   public static final String UIActivateTab = "UIActivateTab";
   public static final String UIHighlightMainMenuItem = "UIHighlightMainMenuItem";
   public static final String UIActivatePlayButton = "UIActivatePlayButton";
   public static final String UIPauseMenuEnter = "UIPauseMenuEnter";
   public static final String UIPauseMenuExit = "UIPauseMenuExit";
   public static final String UIActivateMainMenuItem = "UIActivateMainMenuItem";
   public static final String VehicleRadioButton = "VehicleRadioButton";
   public static final String UIVehicleMenuOpen = "UIVehicleMenuOpen";
   public static final String UIObjectMenuEnter = "UIObjectMenuEnter";
   public static final String UIObjectMenuObjectRotateOutline = "UIObjectMenuObjectRotateOutline";
   public static final String UIObjectMenuObjectPickup = "UIObjectMenuObjectPickup";
   public static final String UIObjectMenuObjectPlace = "UIObjectMenuObjectPlace";
   public static final String UIObjectMenuObjectRotate = "UIObjectMenuObjectRotate";
   private static final String UIAchievement = "UIAchievement";
   public static final String UIDocked = "UIDocked";
   public static final String UIUndocked = "UIUndocked";

   public static SoundAssetManager getInstance() {
      if (soundAssetManager == null) {
         soundAssetManager = new SoundAssetManager();
      }

      return soundAssetManager;
   }

   public SoundAssetManager() {
      this.initialize();
   }

   private void initialize() {
   }

   public String getAchievementSound() {
      return "UIAchievement";
   }
}
