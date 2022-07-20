package FeaturePlaneExtractor;

import rts.GameState;
import rts.UnitAction;
import rts.units.Unit;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FeaturePlaneStack {

    enum FeaturePlaneID {
        BASE, BARRACKS, WORKER, LIGHT, RANGED, HEAVY,
        HEALTH_1, HEALTH_2, HEALTH_3, HEALTH_4, HEALTH_5,
        OWNER_0, OWNER_1,
        FR_0_25, FR_26_50, FR_51_80, FR_81_120, FR_121_,
        RESOURCES_1, RESOURCES_2, RESOURCES_3, RESOURCES_4, RESOURCES_5, RESOURCES_6_9, RESOURCES_10_
    }

    int depth;
    int height;
    int width;
    int winner;
    int game_time_percentage;
    ArrayList<FeaturePlane> planes;

    public FeaturePlaneStack(int height, int width, int depth) {
        this.depth = depth;
        this.height = height;
        this.width = width;

        this.planes = new ArrayList<FeaturePlane>();
        for(int i = 0; i < depth; ++i)
            planes.add(new FeaturePlane(height, width));
    }

    public void setFromGameState(GameState gs, int winner, int lastFrame) {

        this.winner = winner;
        double time_percentage = (double)gs.getTime() / (double)lastFrame;
        time_percentage *= 100;
        this.game_time_percentage = (int) (time_percentage);


        System.out.println("Setting from game state at frame:" + gs.getTime() + " (" + game_time_percentage + ") and winner:" + winner);
        List<Unit> units = gs.getUnits();
        Integer y = Integer.valueOf( units.size());
        //System.out.println("Found " + y.toString() + "Units");
        for(Unit u : units) {
            //System.out.println( u.getType().name + "("+ u.getPlayer() + ")" + " at (" + u.getX() + "," + u.getY() + ")");
            registerUnitType(u);
            registerUnitHP(u);
            registerUnitOwner(u);
            gs.getUnitAction(u);
            registerActionFramestoComp(u, gs.getUnitAction(u));
            //int framesComp = lastFrame - time;

        }


    }

    public void registerActionFramestoComp(Unit u, UnitAction action) {

        int x = u.getX();
        int y = u.getY();

        if(action != null) {
            int eta = action.ETA(u);
            //System.out.println("Unit " + u.getType().name + "with action " + action.getActionName() + " finishing in " + eta);

            if (eta >= 0 && eta <= 25)
                planes.get(FeaturePlaneID.FR_0_25.ordinal()).setValue(x, y, 1);
            else {
                if (eta <= 50)
                    planes.get(FeaturePlaneID.FR_26_50.ordinal()).setValue(x, y, 1);
                else {
                    if (eta <= 80)
                        planes.get(FeaturePlaneID.FR_51_80.ordinal()).setValue(x, y, 1);
                    else {
                        if (eta <= 120)
                            planes.get(FeaturePlaneID.FR_81_120.ordinal()).setValue(x, y, 1);
                        else
                            planes.get(FeaturePlaneID.FR_121_.ordinal()).setValue(x, y, 1);
                    }
                }
            }
        }

    }

    public void registerUnitOwner(Unit u) {
        int owner = u.getPlayer();
        int x = u.getX();
        int y = u.getY();

        if(owner == 0)
            planes.get(FeaturePlaneID.OWNER_0.ordinal()).setValue(x, y, 1);
        else {
            if(owner == 1)
                planes.get(FeaturePlaneID.OWNER_1.ordinal()).setValue(x, y, 1);
        }
    }

    public void registerUnitHP(Unit u) {
        int hp = u.getHitPoints();
        int x = u.getX();
        int y = u.getY();

        if( !("Resources".equals(u.getType().name) ) ) {

            switch (hp) {
                case 1:
                    planes.get(FeaturePlaneID.HEALTH_1.ordinal()).setValue(x, y, 1);
                    break;
                case 2:
                    planes.get(FeaturePlaneID.HEALTH_2.ordinal()).setValue(x, y, 1);
                    break;
                case 3:
                    planes.get(FeaturePlaneID.HEALTH_3.ordinal()).setValue(x, y, 1);
                    break;
                case 4:
                    planes.get(FeaturePlaneID.HEALTH_4.ordinal()).setValue(x, y, 1);
                    break;
                default:
                    if (hp >= 5)
                        planes.get(FeaturePlaneID.HEALTH_5.ordinal()).setValue(x, y, 1);
                    break;
            }
        }
    }

    public void registerUnitType(Unit u) {
        String name = u.getType().name;
        int x = u.getX();
        int y = u.getY();
        int resourcesQt = u.getResources();

        //System.out.println("Registering " + name + " at [" + x + "][" + y + "]");

        if (resourcesQt > 0)
            registerResourceQuantity(resourcesQt, x, y);

        if (name.equals("Base")) {
            planes.get(FeaturePlaneID.BASE.ordinal()).setValue(x, y, 1);
        }
        else {
            if (name.equals("Barracks")) {
                planes.get(FeaturePlaneID.BARRACKS.ordinal()).setValue(x, y, 1);
            } else {
                if (name.equals("Worker")) {
                    planes.get(FeaturePlaneID.WORKER.ordinal()).setValue(x, y, 1);
                } else {
                    if (name.equals("Light")) {
                        planes.get(FeaturePlaneID.LIGHT.ordinal()).setValue(x, y, 1);
                    } else {
                        if (name.equals("Ranged")) {
                            planes.get(FeaturePlaneID.RANGED.ordinal()).setValue(x, y, 1);
                        } else {
                            if (name.equals("Heavy")) {
                                planes.get(FeaturePlaneID.HEAVY.ordinal()).setValue(x, y, 1);
                            } else {
                                if(name.equals("Resource")) {
                                    //NOTHING TO DO;
                                }
                                else {
                                    System.out.println("Unknown unit type: " + name);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void registerResourceQuantity(int qt, int x, int y) {

        if(qt < 6) {
            switch (qt) {
                case 1:
                    planes.get(FeaturePlaneID.RESOURCES_1.ordinal()).setValue(x, y, 1);
                    break;
                case 2:
                    planes.get(FeaturePlaneID.RESOURCES_2.ordinal()).setValue(x, y, 1);
                    break;
                case 3:
                    planes.get(FeaturePlaneID.RESOURCES_3.ordinal()).setValue(x, y, 1);
                    break;
                case 4:
                    planes.get(FeaturePlaneID.RESOURCES_4.ordinal()).setValue(x, y, 1);
                    break;
                case 5:
                    planes.get(FeaturePlaneID.RESOURCES_5.ordinal()).setValue(x, y, 1);
                    break;
            }
        }
        else {
            if(qt < 10)
                planes.get(FeaturePlaneID.RESOURCES_6_9.ordinal()).setValue(x, y, 1);
            else
                planes.get(FeaturePlaneID.RESOURCES_10_.ordinal()).setValue(x, y, 1);
        }


    }

    public void printStack() {
        int i = 0;
        for(FeaturePlaneID id : FeaturePlaneID.values()) {
            //System.out.println("Feature Plane #" + id.ordinal() + " - " + id.toString());
            planes.get(id.ordinal()).print();
        }
    }

    public void toTextFile(FileWriter w) {

        try {
            w.write(String.valueOf(depth) + " " + String.valueOf(height) + " " + String.valueOf(width) + " " + String.valueOf(winner) + " " + String.valueOf(game_time_percentage));
            w.write('\n');
            for(FeaturePlane p : planes)
                p.toTextFile(w);
            w.flush();
            w.close();
        }
        catch (IOException e) {

        }

    }

    public int getDepth(){
        return depth;
    }

    public ArrayList<FeaturePlane> getPlanes(){
        return planes;
    }

    public void setPlanes(ArrayList<FeaturePlane> planes) {
        this.planes = planes;
    }

    public FeaturePlane getPlaneAtDepth(int d) {
        return planes.get(d);
    }

    public void setPlaneAtDepth(int d, FeaturePlane plane) {
        planes.set(d, plane);
    }
}
