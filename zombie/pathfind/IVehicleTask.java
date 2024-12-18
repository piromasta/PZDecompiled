package zombie.pathfind;

import zombie.vehicles.BaseVehicle;

interface IVehicleTask {
   void init(PolygonalMap2 var1, BaseVehicle var2);

   void execute();

   void release();
}
