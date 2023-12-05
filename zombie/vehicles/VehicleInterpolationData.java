package zombie.vehicles;

public class VehicleInterpolationData implements Comparable<VehicleInterpolationData> {
   protected long time;
   protected float x;
   protected float y;
   protected float z;
   protected float qx;
   protected float qy;
   protected float qz;
   protected float qw;
   protected float vx;
   protected float vy;
   protected float vz;
   protected float engineSpeed;
   protected float throttle;
   protected short wheelsCount = 4;
   protected float[] wheelSteering = new float[4];
   protected float[] wheelRotation = new float[4];
   protected float[] wheelSkidInfo = new float[4];
   protected float[] wheelSuspensionLength = new float[4];

   public VehicleInterpolationData() {
   }

   protected void setNumWheels(short var1) {
      if (var1 > this.wheelsCount) {
         this.wheelSteering = new float[var1];
         this.wheelRotation = new float[var1];
         this.wheelSkidInfo = new float[var1];
         this.wheelSuspensionLength = new float[var1];
      }

      this.wheelsCount = var1;
   }

   void copy(VehicleInterpolationData var1) {
      this.time = var1.time;
      this.x = var1.x;
      this.y = var1.y;
      this.z = var1.z;
      this.qx = var1.qx;
      this.qy = var1.qy;
      this.qz = var1.qz;
      this.qw = var1.qw;
      this.vx = var1.vx;
      this.vy = var1.vy;
      this.vz = var1.vz;
      this.engineSpeed = var1.engineSpeed;
      this.throttle = var1.throttle;
      this.setNumWheels(var1.wheelsCount);

      for(int var2 = 0; var2 < var1.wheelsCount; ++var2) {
         this.wheelSteering[var2] = var1.wheelSteering[var2];
         this.wheelRotation[var2] = var1.wheelRotation[var2];
         this.wheelSkidInfo[var2] = var1.wheelSkidInfo[var2];
         this.wheelSuspensionLength[var2] = var1.wheelSuspensionLength[var2];
      }

   }

   public int compareTo(VehicleInterpolationData var1) {
      return Long.compare(this.time, var1.time);
   }
}
