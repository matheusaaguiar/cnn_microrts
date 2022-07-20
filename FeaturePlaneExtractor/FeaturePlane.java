package FeaturePlaneExtractor;

import java.io.FileWriter;
import java.io.IOException;

public class FeaturePlane {
    int height;
    int width;
    Integer [][] featurePlane;

    public FeaturePlane(int height, int width) {
        this.height = height;
        this.width = width;
        featurePlane = new Integer[height][width];
        for(int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                featurePlane[i][j] = 0;
            }
        }
    }

    public FeaturePlane(Integer[][] src) {
        height = src.length;
        width = src[0].length;
        //deep copy
        featurePlane = src.clone();
    }

    public void print() {
        System.out.println("------ " + height + "x" + width + " ------");
        System.out.print("   ");
        for(int x = 0; x < height; ++x) {
            System.out.print(x);
            System.out.print(' ');
        }
        System.out.print('\n');
        for(int x = 0; x < height+2; ++x) {
            System.out.print("--");
        }
        System.out.print('\n');
        for(int i = 0; i < height; ++i) {
            System.out.print(i + "| ");
            for (int j = 0; j < width; ++j) {
                System.out.print(featurePlane[i][j]);
                System.out.print(" ");
            }
            System.out.print('\n');
        }

    }

    public void toTextFile(FileWriter w) {
        try {
            for (int i = 0; i < height; ++i)
                for (int j = 0; j < width; ++j) {
                    w.write(String.valueOf(featurePlane[i][j]));
                    w.write(' ');
                }
            w.write('\n');
        }
        catch (IOException e) {
            System.out.println("Error" + e.toString());
        }
    }

    public int getHeight(){
        return height;
    }

    public int getWidth(){
        return width;
    }

    public boolean inRange(int i, int j)
    {
        if(i >= 0 && i < height && j >= 0 && j < width)
            return true;
        return false;
    }

    public Integer getValue(int i, int j){
        if(inRange(i,j))
            return featurePlane[i][j];
        return Integer.valueOf(-1);
    }

    public void setValue(int i, int j, int v) {
        if(inRange(i, j))
            featurePlane[i][j] = v;
    }
}
