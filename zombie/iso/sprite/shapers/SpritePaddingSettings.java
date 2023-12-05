package zombie.iso.sprite.shapers;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.TransformerException;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;

public class SpritePaddingSettings {
   private static Settings m_settings = null;
   private static String m_settingsFilePath = null;
   private static PredicatedFileWatcher m_fileWatcher = null;

   public SpritePaddingSettings() {
   }

   public static void settingsFileChanged(Settings var0) {
      DebugLog.General.println("Settings file changed.");
      m_settings = var0;
   }

   private static void loadSettings() {
      String var0 = getSettingsFilePath();
      File var1 = (new File(var0)).getAbsoluteFile();
      if (var1.isFile()) {
         try {
            m_settings = (Settings)PZXmlUtil.parse(Settings.class, var1.getPath());
         } catch (PZXmlParserException var3) {
            DebugLog.General.printException(var3, "Error parsing file: " + var0, LogSeverity.Warning);
            m_settings = new Settings();
         }
      } else {
         m_settings = new Settings();
         saveSettings();
      }

      if (m_fileWatcher == null) {
         m_fileWatcher = new PredicatedFileWatcher(var0, Settings.class, SpritePaddingSettings::settingsFileChanged);
         DebugFileWatcher.instance.add(m_fileWatcher);
      }

   }

   private static String getSettingsFilePath() {
      if (m_settingsFilePath == null) {
         m_settingsFilePath = ZomboidFileSystem.instance.getLocalWorkDirSub("SpritePaddingSettings.xml");
      }

      return m_settingsFilePath;
   }

   private static void saveSettings() {
      try {
         PZXmlUtil.write((Object)m_settings, (new File(getSettingsFilePath())).getAbsoluteFile());
      } catch (IOException | JAXBException | TransformerException var1) {
         var1.printStackTrace();
      }

   }

   public static Settings getSettings() {
      if (m_settings == null) {
         loadSettings();
      }

      return m_settings;
   }

   @XmlRootElement(
      name = "FloorShaperDeDiamondSettings"
   )
   public static class Settings {
      public SpritePadding.IsoPaddingSettings IsoPadding = new SpritePadding.IsoPaddingSettings();
      public FloorShaperDeDiamond.Settings FloorDeDiamond = new FloorShaperDeDiamond.Settings();
      public FloorShaperAttachedSprites.Settings AttachedSprites = new FloorShaperAttachedSprites.Settings();

      public Settings() {
      }
   }

   public abstract static class GenericZoomBasedSettingGroup {
      public GenericZoomBasedSettingGroup() {
      }

      public abstract <ZoomBasedSetting> ZoomBasedSetting getCurrentZoomSetting();

      public static <ZoomBasedSetting> ZoomBasedSetting getCurrentZoomSetting(ZoomBasedSetting var0, ZoomBasedSetting var1, ZoomBasedSetting var2) {
         float var3 = Core.getInstance().getCurrentPlayerZoom();
         if (var3 < 1.0F) {
            return var0;
         } else {
            return var3 == 1.0F ? var1 : var2;
         }
      }
   }
}
