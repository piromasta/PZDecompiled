package zombie.core.raknet;

import java.util.ArrayList;
import zombie.radio.devices.DeviceData;

public class VoiceManagerData {
   public static ArrayList<VoiceManagerData> data = new ArrayList();
   public long userplaychannel = 0L;
   public long userplaysound = 0L;
   public boolean userplaymute = false;
   public long voicetimeout = 0L;
   public final ArrayList<RadioData> radioData = new ArrayList();
   public boolean isCanHearAll;
   short index;

   public VoiceManagerData(short var1) {
      this.index = var1;
   }

   public static VoiceManagerData get(short var0) {
      if (data.size() <= var0) {
         for(short var1 = (short)data.size(); var1 <= var0; ++var1) {
            VoiceManagerData var2 = new VoiceManagerData(var1);
            data.add(var2);
         }
      }

      VoiceManagerData var3 = (VoiceManagerData)data.get(var0);
      if (var3 == null) {
         var3 = new VoiceManagerData(var0);
         data.set(var0, var3);
      }

      return var3;
   }

   public static class RadioData {
      DeviceData deviceData;
      public int freq;
      public float distance;
      public short x;
      public short y;
      float lastReceiveDistance;

      public RadioData(float var1, float var2, float var3) {
         this((DeviceData)null, 0, var1, var2, var3);
      }

      public RadioData(int var1, float var2, float var3, float var4) {
         this((DeviceData)null, var1, var2, var3, var4);
      }

      public RadioData(DeviceData var1, float var2, float var3) {
         this(var1, var1.getChannel(), var1.getMicIsMuted() ? 0.0F : (float)var1.getTransmitRange(), var2, var3);
      }

      private RadioData(DeviceData var1, int var2, float var3, float var4, float var5) {
         this.deviceData = var1;
         this.freq = var2;
         this.distance = var3;
         this.x = (short)((int)var4);
         this.y = (short)((int)var5);
      }

      public boolean isTransmissionAvailable() {
         return this.freq != 0 && this.deviceData != null && this.deviceData.getIsTurnedOn() && this.deviceData.getIsTwoWay() && !this.deviceData.isNoTransmit() && !this.deviceData.getMicIsMuted();
      }

      public boolean isReceivingAvailable(int var1) {
         return this.freq != 0 && this.deviceData != null && this.deviceData.getIsTurnedOn() && this.deviceData.getChannel() == var1 && this.deviceData.getDeviceVolume() != 0.0F && !this.deviceData.isPlayingMedia();
      }

      public DeviceData getDeviceData() {
         return this.deviceData;
      }
   }

   public static enum VoiceDataSource {
      Unknown,
      Voice,
      Radio,
      Cheat;

      private VoiceDataSource() {
      }
   }
}
