package zombie.radio.devices;

import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatManager;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWaveSignal;
import zombie.radio.ZomboidRadio;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclePart;

public interface WaveSignalDevice {
   DeviceData getDeviceData();

   void setDeviceData(DeviceData var1);

   float getDelta();

   void setDelta(float var1);

   IsoGridSquare getSquare();

   float getX();

   float getY();

   float getZ();

   void AddDeviceText(String var1, float var2, float var3, float var4, String var5, String var6, int var7);

   boolean HasPlayerInRange();

   default void AddDeviceText(IsoPlayer var1, String var2, float var3, float var4, float var5, String var6, String var7, int var8) {
      if (this.getDeviceData() != null && this.getDeviceData().getDeviceVolume() > 0.0F) {
         if (!ZomboidRadio.isStaticSound(var2)) {
            this.getDeviceData().doReceiveSignal(var8);
         }

         if (var1 != null && var1.isLocalPlayer() && !var1.Traits.Deaf.isSet()) {
            if (this.getDeviceData().getParent() instanceof InventoryItem && var1.isEquipped((InventoryItem)this.getDeviceData().getParent())) {
               var1.getChatElement().addChatLine(var2, var3, var4, var5, UIFont.Medium, (float)this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, false, false, true);
            } else if (this.getDeviceData().getParent() instanceof IsoWaveSignal) {
               ((IsoWaveSignal)this.getDeviceData().getParent()).getChatElement().addChatLine(var2, var3, var4, var5, UIFont.Medium, (float)this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, true, true, true);
            } else if (this.getDeviceData().getParent() instanceof VehiclePart) {
               ((VehiclePart)this.getDeviceData().getParent()).getChatElement().addChatLine(var2, var3, var4, var5, UIFont.Medium, (float)this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, true, true, true);
            }

            if (ZomboidRadio.isStaticSound(var2)) {
               ChatManager.getInstance().showStaticRadioSound(var2);
            } else {
               ChatManager.getInstance().showRadioMessage(var2, this.getDeviceData().getChannel());
            }

            if (var7 != null) {
               LuaEventManager.triggerEvent("OnDeviceText", var6, var7, -1, -1, -1, var2, this);
            }
         }
      }

   }
}
