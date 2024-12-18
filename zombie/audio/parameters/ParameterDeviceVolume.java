package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.radio.devices.DeviceData;

public final class ParameterDeviceVolume extends FMODLocalParameter {
   private final DeviceData deviceData;

   public ParameterDeviceVolume(DeviceData var1) {
      super("DeviceVolume");
      this.deviceData = var1;
   }

   public float calculateCurrentValue() {
      return this.deviceData.getDeviceVolume();
   }
}
